#!/bin/bash
# =================================================================
# SCRIPT DE DÉMARRAGE LOCAL AMÉLIORÉ - MOSAÏQUE CAPITAL
# =================================================================
# Usage: ./start-local.sh

set -e

# Couleurs pour les logs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}==================================================================="
echo -e "              MOSAÏQUE CAPITAL - DÉMARRAGE LOCAL"
echo -e "===================================================================${NC}"

# Vérifier si le fichier .env existe
if [ ! -f .env ]; then
    echo -e "${RED}❌ Fichier .env non trouvé !${NC}"
    echo -e "${YELLOW}💡 Copiez .env.example vers .env et configurez vos variables${NC}"
    exit 1
fi

# ✅ CORRECTION : Fonction sécurisée pour charger les variables .env
load_env_vars() {
    echo -e "${BLUE}🔧 Chargement sécurisé des variables d'environnement...${NC}"

    # Lire le fichier .env ligne par ligne et exporter les variables
    while IFS='=' read -r key value; do
        # Ignorer les lignes vides et les commentaires
        [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]] && continue

        # Nettoyer la clé (supprimer les espaces)
        key=$(echo "$key" | xargs)

        # Nettoyer la valeur (supprimer les guillemets si présents)
        value=$(echo "$value" | sed 's/^["'\'']//' | sed 's/["'\'']$//')

        # Exporter la variable
        if [[ -n "$key" && -n "$value" ]]; then
            export "$key"="$value"
            echo -e "   ✓ ${key}"
        fi
    done < .env
}

# Charger les variables d'environnement
load_env_vars

echo -e "${GREEN}✅ Variables d'environnement chargées${NC}"

# Vérifier les variables critiques
echo -e "${BLUE}🔍 Vérification des variables critiques...${NC}"
critical_vars=("APP_JWT_SECRET" "DATABASE_PASSWORD")
missing_vars=()

for var in "${critical_vars[@]}"; do
    if [[ -z "${!var}" ]]; then
        missing_vars+=("$var")
    fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
    echo -e "${RED}❌ Variables manquantes dans .env : ${missing_vars[*]}${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Variables critiques validées${NC}"

# Afficher la configuration
echo -e "${BLUE}📋 Configuration détectée :${NC}"
echo -e "   - Profil Spring: ${SPRING_PROFILES_ACTIVE:-dev}"
echo -e "   - Port serveur: ${SERVER_PORT:-9999}"
echo -e "   - Base de données: ${DATABASE_USERNAME:-Non défini}@localhost:${DATABASE_PORT:-3306}"
echo -e "   - Redis: localhost:${REDIS_PORT:-6379}"

# Vérifier la disponibilité de Maven
if ! command -v ./mvnw &> /dev/null; then
    echo -e "${RED}❌ Maven Wrapper (mvnw) non trouvé !${NC}"
    exit 1
fi

# Rendre mvnw exécutable si nécessaire
if [[ ! -x ./mvnw ]]; then
    echo -e "${YELLOW}🔧 Rendre mvnw exécutable...${NC}"
    chmod +x ./mvnw
fi

# Nettoyer et compiler
echo -e "${BLUE}🧹 Nettoyage et compilation...${NC}"
if ! ./mvnw clean compile -q; then
    echo -e "${RED}❌ Erreur lors de la compilation${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Compilation réussie${NC}"

# Démarrer l'application
echo -e "${GREEN}🚀 Démarrage de l'application Spring Boot...${NC}"
echo -e "${YELLOW}💡 Pour arrêter l'application : Ctrl+C${NC}"
echo -e ""

# Démarrage avec gestion d'erreur
if ! ./mvnw spring-boot:run; then
    echo -e "${RED}❌ Erreur lors du démarrage de l'application${NC}"
    exit 1
fi