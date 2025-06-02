# =================================================================
# DOCKERFILE - MOSAÏQUE CAPITAL SPRING BOOT
# =================================================================
# Multi-stage build pour optimiser la taille de l'image finale

# =================================================================
# STAGE 1: BUILD - Compilation de l'application
# =================================================================
FROM maven:3.9.6-openjdk-21 AS builder

# Métadonnées
LABEL stage=builder
LABEL maintainer="Mosaique Capital Team"

# Variables d'environnement pour Maven
ENV MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository -Xmx1024m -XX:+UseG1GC"

# Répertoire de travail
WORKDIR /app

# Copier les fichiers de configuration Maven en premier (pour le cache Docker)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Rendre mvnw exécutable
RUN chmod +x mvnw

# Télécharger les dépendances (cette couche sera mise en cache)
RUN ./mvnw dependency:resolve dependency:resolve-sources

# Copier le code source
COPY src ./src

# Compilation et packaging (skip tests pour accélérer le build)
RUN ./mvnw clean package -DskipTests -Dmaven.javadoc.skip=true

# Vérifier que le JAR a été créé
RUN ls -la target/

# =================================================================
# STAGE 2: RUNTIME - Image finale légère
# =================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Métadonnées de l'image finale
LABEL maintainer="Mosaique Capital Team"
LABEL version="1.0.0"
LABEL description="Mosaique Capital - API Spring Boot avec MFA"

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV TZ=Europe/Paris
ENV USER_ID=1001
ENV GROUP_ID=1001

# Installation des packages nécessaires
RUN apk add --no-cache \
    tzdata \
    curl \
    netcat-openbsd \
    && cp /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && apk del tzdata

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -g $GROUP_ID -S appgroup && \
    adduser -u $USER_ID -S appuser -G appgroup

# Créer les répertoires nécessaires
RUN mkdir -p /app/logs /app/config /app/tmp && \
    chown -R appuser:appgroup /app

# Répertoire de travail
WORKDIR /app

# Copier le JAR depuis le stage builder
COPY --from=builder --chown=appuser:appgroup /app/target/Mosaique_Capital-*.jar app.jar

# Copier un script de démarrage personnalisé
COPY --chown=appuser:appgroup docker-entrypoint.sh /app/

# Rendre le script exécutable
RUN chmod +x docker-entrypoint.sh

# Changer vers l'utilisateur non-root
USER appuser

# Exposer le port de l'application
EXPOSE 9999

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9999/actuator/health || exit 1

# Volume pour les logs
VOLUME ["/app/logs"]

# Point d'entrée
ENTRYPOINT ["./docker-entrypoint.sh"]

# Commande par défaut
CMD ["java", "-jar", "app.jar"]