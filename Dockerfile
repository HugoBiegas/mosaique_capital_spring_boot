# =================================================================
# DOCKERFILE OPTIMISÉ - MOSAÏQUE CAPITAL SPRING BOOT
# =================================================================
# Multi-stage build optimisé pour la production avec cache layers

# =================================================================
# STAGE 1: BUILD - Compilation avec cache Maven optimisé
# =================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Métadonnées du builder
LABEL stage=builder
LABEL maintainer="Mosaique Capital Team"

# Variables d'environnement pour Maven
ENV MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository -Xmx2048m -XX:+UseG1GC"

# Répertoire de travail
WORKDIR /app

# Copier les fichiers de configuration Maven en premier (optimisation cache)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Rendre mvnw exécutable
RUN chmod +x mvnw

# Pré-télécharger les dépendances Maven (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compilation et packaging avec optimisations
RUN ./mvnw clean package -DskipTests -B -q \
    -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true

# Vérifier que le JAR a été créé et le renommer
RUN mv target/Mosaique_Capital-*.jar target/app.jar && \
    ls -la target/app.jar

# =================================================================
# STAGE 2: RUNTIME - Image finale optimisée
# =================================================================
FROM eclipse-temurin:21.0.1_12-jre-alpine AS runtime

# Arguments de build
ARG BUILD_DATE
ARG VCS_REF

# Métadonnées de l'image finale
LABEL maintainer="Mosaique Capital Team" \
      version="1.0.0" \
      description="Mosaique Capital - API Spring Boot avec MFA et Redis" \
      build-date=$BUILD_DATE \
      vcs-ref=$VCS_REF

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xmx768m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    TZ=Europe/Paris \
    USER_ID=1001 \
    GROUP_ID=1001 \
    SERVER_PORT=9999

# Installation des packages nécessaires et configuration timezone
RUN apk add --no-cache \
        tzdata \
        curl \
        netcat-openbsd \
        dumb-init && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apk del tzdata && \
    rm -rf /var/cache/apk/*

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -g $GROUP_ID -S appgroup && \
    adduser -u $USER_ID -S appuser -G appgroup

# Créer les répertoires nécessaires
RUN mkdir -p /app/logs /app/config /app/tmp && \
    chown -R appuser:appgroup /app

# Répertoire de travail
WORKDIR /app

# Copier le JAR depuis le stage builder
COPY --from=builder --chown=appuser:appgroup /app/target/app.jar app.jar

# Copier le script d'entrée optimisé
COPY docker-entrypoint.sh /app/
RUN chown appuser:appgroup /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh

# Changer vers l'utilisateur non-root
USER appuser

# Exposer le port de l'application
EXPOSE $SERVER_PORT

# Health check optimisé
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:$SERVER_PORT/actuator/health || exit 1

# Volume pour les logs
VOLUME ["/app/logs"]

# Point d'entrée avec dumb-init pour la gestion des signaux
ENTRYPOINT ["/usr/bin/dumb-init", "--", "./docker-entrypoint.sh"]

# Commande par défaut
CMD ["java", "-jar", "app.jar"]