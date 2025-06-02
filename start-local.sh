#!/bin/bash
# =================================================================
# SCRIPT DE DÉMARRAGE LOCAL - MOSAÏQUE CAPITAL
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

# Charger les variables d'environnement depuis .env
echo -e "${BLUE}🔧 Chargement des variables d'environnement...${NC}"
set -a  # Exporter automatiquement toutes les variables
source .env
set +a

echo -e "${GREEN}✅ Variables d'environnement chargées${NC}"

# Vérifier les variables critiques
echo -e "${BLUE}🔍 Vérification des variables critiques...${NC}"
critical_vars="APP_JWT_SECRET SPRING_DATASOURCE_PASSWORD"
missing_vars=""

for var in $critical_vars; do
    if [ -z "${!var}" ]; then
        missing_vars="$missing_vars $var"
    fi
done

if [ -n "$missing_vars" ]; then
    echo -e "${RED}❌ Variables manquantes dans .env :$missing_vars${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Variables critiques validées${NC}"

# Afficher la configuration
echo -e "${BLUE}📋 Configuration détectée :${NC}"
echo -e "   - Profil Spring: ${SPRING_PROFILES_ACTIVE:-dev}"
echo -e "   - Port serveur: ${SERVER_PORT:-9999}"
echo -e "   - Base de données: ${SPRING_DATASOURCE_URL:-Non définie}"
echo -e "   - Redis: ${SPRING_REDIS_HOST:-localhost}:${SPRING_REDIS_PORT:-6379}"

# Nettoyer et compiler
echo -e "${BLUE}🧹 Nettoyage et compilation...${NC}"
./mvnw clean compile

# Démarrer l'application
echo -e "${GREEN}🚀 Démarrage de l'application Spring Boot...${NC}"
./mvnw spring-boot:run

# Note: Les variables d'environnement sont automatiquement transmises à Maven