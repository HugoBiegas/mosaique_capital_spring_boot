#!/bin/bash
# =================================================================
# SCRIPT DE NETTOYAGE ET REDÉMARRAGE - MOSAÏQUE CAPITAL
# =================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Fonctions d'affichage
success() { echo -e "${GREEN}✅ $1${NC}"; }
error() { echo -e "${RED}❌ $1${NC}"; }
warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
info() { echo -e "${BLUE}ℹ️  $1${NC}"; }

echo -e "${BLUE}"
echo "================================================================="
echo "     NETTOYAGE ET REDÉMARRAGE MOSAÏQUE CAPITAL"
echo "================================================================="
echo -e "${NC}"

# =================================================================
# 1. ARRÊT DE TOUS LES CONTENEURS
# =================================================================
info "1. Arrêt de tous les conteneurs Docker..."
if docker-compose ps -q | grep -q .; then
    docker-compose down
    success "Conteneurs arrêtés"
else
    info "Aucun conteneur en cours d'exécution"
fi

# =================================================================
# 2. NETTOYAGE DES IMAGES ET CACHE
# =================================================================
info "2. Nettoyage des images Docker..."
docker-compose down --rmi all --volumes --remove-orphans 2>/dev/null || true
success "Images supprimées"

# =================================================================
# 3. SAUVEGARDE DE L'ANCIEN .env
# =================================================================
info "3. Sauvegarde de l'ancien .env..."
if [ -f ".env" ]; then
    cp .env .env.backup.$(date +%Y%m%d_%H%M%S)
    success "Ancien .env sauvegardé"
else
    warning "Aucun .env trouvé"
fi

# =================================================================
# 4. VÉRIFICATION DU NOUVEAU .env
# =================================================================
info "4. Vérification du nouveau .env..."
if [ ! -f ".env" ]; then
    error "Veuillez créer le nouveau fichier .env avec les corrections fournies"
    exit 1
fi

# Vérification que les variables problématiques ne sont plus présentes
if grep -q '${BI_CLIENT_ID}' .env 2>/dev/null; then
    error "Le .env contient encore des références à \${BI_CLIENT_ID}"
    error "Veuillez utiliser le .env corrigé fourni"
    exit 1
fi

if grep -q '${BRIDGE_CLIENT_ID}' .env 2>/dev/null; then
    error "Le .env contient encore des références à \${BRIDGE_CLIENT_ID}"
    error "Veuillez utiliser le .env corrigé fourni"
    exit 1
fi

# Vérification que les variables essentielles sont présentes
required_vars=(
    "APP_BANKING_BUDGET_INSIGHT_ENABLED"
    "APP_BANKING_DEFAULT_PROVIDER"
    "SERVER_PORT"
    "DATABASE_PASSWORD"
)

missing_vars=()
source .env

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        missing_vars+=("$var")
    fi
done

if [ ${#missing_vars[@]} -gt 0 ]; then
    error "Variables manquantes dans le .env:"
    for var in "${missing_vars[@]}"; do
        echo -e "${RED}  - $var${NC}"
    done
    exit 1
fi

success "Nouveau .env validé"

# =================================================================
# 5. REBUILD COMPLET
# =================================================================
info "5. Reconstruction des images..."
docker-compose build --no-cache app
success "Images reconstruites"

# =================================================================
# 6. DÉMARRAGE PROPRE
# =================================================================
info "6. Démarrage des services..."
docker-compose up -d

# Attente du démarrage
info "Attente du démarrage des services..."
sleep 10

# =================================================================
# 7. VÉRIFICATION DE SANTÉ
# =================================================================
info "7. Vérification de santé des services..."

# Vérification MySQL
if docker-compose exec mysql mysqladmin ping -h localhost -u root -p${DATABASE_PASSWORD} --silent 2>/dev/null; then
    success "MySQL fonctionne"
else
    warning "MySQL pourrait encore démarrer..."
fi

# Vérification Redis
if docker-compose exec redis redis-cli ping 2>/dev/null | grep -q PONG; then
    success "Redis fonctionne"
else
    warning "Redis pourrait encore démarrer..."
fi

# Vérification Application (attendre jusqu'à 180 secondes)
info "Vérification de l'application Spring Boot..."
for i in {1..36}; do
    if curl -s http://localhost:${SERVER_PORT}/actuator/health >/dev/null 2>&1; then
        success "Application Spring Boot fonctionne"
        break
    else
        if [ $i -eq 36 ]; then
            warning "Application Spring Boot prend du temps à démarrer"
            warning "Vérifiez les logs avec: docker-compose logs -f app"
        else
            echo -n "."
            sleep 5
        fi
    fi
done

# =================================================================
# 8. AFFICHAGE DES LOGS RÉCENTS
# =================================================================
info "8. Logs récents de l'application..."
echo -e "${YELLOW}"
docker-compose logs --tail=20 app
echo -e "${NC}"

# =================================================================
# 9. RAPPORT FINAL
# =================================================================
echo -e "${BLUE}"
echo "================================================================="
echo "                    RAPPORT FINAL"
echo "================================================================="
echo -e "${NC}"

success "🎉 Redémarrage terminé !"
echo ""
info "Services disponibles :"
echo -e "${GREEN}  • Application: http://localhost:${SERVER_PORT}${NC}"
echo -e "${GREEN}  • Health Check: http://localhost:${SERVER_PORT}/actuator/health${NC}"
echo -e "${GREEN}  • Adminer (DB): http://localhost:${ADMINER_PORT}${NC}"
echo -e "${GREEN}  • Redis UI: http://localhost:${REDIS_UI_PORT}${NC}"
echo ""
info "Commandes utiles :"
echo -e "${BLUE}  • Voir les logs: docker-compose logs -f app${NC}"
echo -e "${BLUE}  • Redémarrer: docker-compose restart app${NC}"
echo -e "${BLUE}  • Arrêter: docker-compose down${NC}"
echo ""

# Test final de l'API
if curl -s http://localhost:${SERVER_PORT}/actuator/health | grep -q '"status":"UP"'; then
    success "🚀 API MOSAÏQUE CAPITAL OPÉRATIONNELLE !"
else
    warning "⏳ L'API démarre encore, patientez quelques minutes..."
fi