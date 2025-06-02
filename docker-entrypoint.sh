#!/bin/sh
# =================================================================
# DOCKER ENTRYPOINT - MOSAÏQUE CAPITAL
# =================================================================
# Script d'entrée optimisé avec vérifications et attente des services

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

# Fonction pour attendre qu'un service soit disponible
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local timeout=${4:-60}

    log "Attente du service $service_name ($host:$port)..."

    local counter=0
    while ! nc -z "$host" "$port"; do
        if [ $counter -ge $timeout ]; then
            error "Timeout lors de l'attente du service $service_name après ${timeout}s"
            exit 1
        fi
        counter=$((counter + 1))
        sleep 1
    done

    success "Service $service_name est disponible !"
}

# Fonction pour vérifier la connexion Redis avec authentification
check_redis_connection() {
    local redis_host=${SPRING_REDIS_HOST:-redis}
    local redis_port=${SPRING_REDIS_PORT:-6379}
    local redis_password=${SPRING_REDIS_PASSWORD:-}

    log "Vérification de la connexion Redis..."

    if [ -n "$redis_password" ]; then
        # Test avec mot de passe
        if echo "AUTH $redis_password\nPING\nQUIT" | nc "$redis_host" "$redis_port" | grep -q "PONG"; then
            success "Connexion Redis avec authentification réussie"
            return 0
        else
            error "Échec de la connexion Redis avec authentification"
            return 1
        fi
    else
        # Test sans mot de passe
        if echo "PING\nQUIT" | nc "$redis_host" "$redis_port" | grep -q "PONG"; then
            success "Connexion Redis sans authentification réussie"
            return 0
        else
            error "Échec de la connexion Redis sans authentification"
            return 1
        fi
    fi
}

# Fonction pour vérifier la connexion MySQL
check_mysql_connection() {
    local mysql_host=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's/.*\/\/\([^:]*\):.*/\1/p')
    local mysql_port=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')

    if [ -z "$mysql_host" ]; then
        mysql_host="mysql"
    fi

    if [ -z "$mysql_port" ]; then
        mysql_port="3306"
    fi

    log "Vérification de la connexion MySQL ($mysql_host:$mysql_port)..."

    wait_for_service "$mysql_host" "$mysql_port" "MySQL" 120
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

# Attente des services de dépendance
log "Vérification des services de dépendance..."

# Attendre MySQL
check_mysql_connection

# Attendre Redis
wait_for_service "${SPRING_REDIS_HOST:-redis}" "${SPRING_REDIS_PORT:-6379}" "Redis" 60

# Vérifier la connexion Redis avec authentification
check_redis_connection

# Attendre un peu plus pour s'assurer que les services sont complètement prêts
log "Services disponibles. Attente supplémentaire de 5 secondes..."
sleep 5

# Affichage de la configuration JVM
log "Configuration JVM: $JAVA_OPTS"

# Vérification de l'espace disque
log "Vérification de l'espace disque..."
df -h /app

# Création des répertoires de logs si nécessaire
mkdir -p /app/logs

# Affichage du statut final avant démarrage
success "Tous les prérequis sont satisfaits. Démarrage de l'application..."
log "Commande: $*"

# Démarrage de l'application
exec "$@"