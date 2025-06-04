#!/bin/bash
# =================================================================
# DOCKER ENTRYPOINT SIMPLIFIÉ - MOSAÏQUE CAPITAL
# =================================================================
# Script d'entrée qui ne se bloque pas

set -e

# Couleurs pour les logs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction de logging
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] SUCCESS: $1${NC}"
}

# Fonction pour attendre qu'un service soit disponible (SIMPLE)
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local timeout=${4:-30}

    log "Attente du service $service_name ($host:$port)..."

    local counter=0
    while ! nc -z "$host" "$port" 2>/dev/null; do
        if [ $counter -ge $timeout ]; then
            warn "Timeout lors de l'attente du service $service_name après ${timeout}s"
            warn "Démarrage de l'application quand même..."
            return 0
        fi
        counter=$((counter + 1))
        sleep 1
    done

    success "Service $service_name est disponible !"
}

# Affichage des informations de démarrage
log "==================================================================="
log "                    MOSAÏQUE CAPITAL API"
log "==================================================================="
log "Version: 1.0.0"
log "Build Date: ${BUILD_DATE:-unknown}"
log "VCS Ref: ${VCS_REF:-dev}"
log "Profile: ${SPRING_PROFILES_ACTIVE:-default}"
log "Port: ${SERVER_PORT:-9999}"
log "==================================================================="

# Vérification des variables d'environnement essentielles
log "Vérification des variables d'environnement..."

required_vars="SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD APP_JWT_SECRET"
missing_vars=""

for var in $required_vars; do
    eval "value=\${$var}"
    if [ -z "$value" ]; then
        missing_vars="$missing_vars $var"
    fi
done

if [ -n "$missing_vars" ]; then
    error "Variables d'environnement manquantes:$missing_vars"
    exit 1
fi

success "Variables d'environnement validées"

# Attente des services de dépendance (SIMPLE ET RAPIDE)
log "Vérification des services de dépendance..."

# Attendre MySQL (maximum 30 secondes)
wait_for_service "mysql" "3306" "MySQL" 30

# Attendre Redis (maximum 20 secondes)
wait_for_service "redis" "6379" "Redis" 20

# Attendre un peu pour s'assurer que les services sont prêts
log "Services vérifiés. Attente supplémentaire de 3 secondes..."
sleep 3

# Affichage de la configuration JVM
log "Configuration JVM: $JAVA_OPTS"

# Création des répertoires de logs si nécessaire
mkdir -p /app/logs

# Affichage du statut final avant démarrage
success "Démarrage de l'application Spring Boot..."
log "Commande: $*"

# Démarrage de l'application
exec "$@"