# Mosaïque Capital - Backend API

## 🏛️ Description du projet

Mosaïque Capital est une plateforme de gestion patrimoniale complète qui permet aux utilisateurs de suivre, gérer et optimiser leur patrimoine financier. Le système couvre les actifs de différentes natures (immobilier, placements financiers, cryptomonnaies, etc.) et offre des outils d'analyse et de reporting avancés.

Ce repository contient la partie backend sous forme d'API REST développée avec Spring Boot, incluant un système d'authentification à deux facteurs (MFA) complet et sécurisé.

## 🏗️ Architecture du projet

```
com.master.mosaique_capital
├── config             // Configurations Spring (sécurité, Redis, etc.)
│   ├── SecurityConfig.java
│   └── RedisConfig.java
├── controller         // Contrôleurs REST 
│   ├── AssetController.java
│   ├── AssetValuationController.java
│   ├── AuthController.java
│   ├── MfaController.java           # 🆕 Gestion MFA
│   └── PortfolioController.java
├── dto                // Objets de transfert de données
│   ├── asset/
│   ├── auth/
│   └── mfa/                         # 🆕 DTOs MFA
│       ├── MfaSetupResponse.java
│       ├── MfaVerificationRequest.java
│       ├── MfaDisableRequest.java
│       └── MfaStatusResponse.java
├── entity             // Entités JPA
├── service            // Services métier
│   ├── auth/
│   │   ├── AuthService.java
│   │   ├── TokenBlacklistService.java  # 🆕 Gestion blacklist tokens
│   │   └── UserDetailsServiceImpl.java
│   └── mfa/                            # 🆕 Services MFA
│       ├── MfaService.java
│       └── QrCodeService.java
├── security           // Implémentation JWT et sécurité
└── util               // Classes utilitaires
```

## 🛠️ Technologies utilisées

- **Java 21**
- **Spring Boot 3.4.5**
- **Spring Security 6** avec JWT
- **Spring Data JPA**
- **MySQL 8**
- **Redis** (pour blacklist des tokens)
- **Lombok**
- **MapStruct**
- **ZXing** (génération QR codes)
- **TOTP** (authentification 2 facteurs)
- **Validation API**

## 🚀 Installation et configuration

### 1. Prérequis

- **JDK 21**
- **MySQL 8.x**
- **Redis 6+** (recommandé pour la production)
- **Maven 3.8+**

### 2. Configuration des secrets 🔐

#### Méthode automatique (recommandée)

```bash
# Rendre le script exécutable
chmod +x generate-secrets.sh

# Générer tous les secrets automatiquement
./generate-secrets.sh
```

Ce script génère :
- ✅ **Secret JWT** cryptographiquement sécurisé
- ✅ **Mots de passe** pour base de données et Redis
- ✅ **Clés de chiffrement** pour les données sensibles
- ✅ **Fichier .env** complet
- ✅ **Docker Compose** pour développement

#### Méthode manuelle

```bash
# 1. Copier le fichier d'exemple
cp .env.example .env

# 2. Générer un secret JWT sécurisé
openssl rand -base64 32

# 3. Éditer le fichier .env avec vos valeurs
nano .env
```

### 3. Configuration de la base de données

```bash
# Démarrer MySQL et Redis avec Docker
docker-compose -f docker-compose.dev.yml up -d

# Ou configurer manuellement :
# Créer la base de données
mysql -u root -p -e "CREATE DATABASE mosaique_capital CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Exécuter le script SQL d'initialisation
mysql -u root -p mosaique_capital < docs/mosaique_capital.sql
```

### 4. Variables d'environnement essentielles

| Variable | Description | Exemple |
|----------|-------------|---------|
| `JWT_SECRET` | 🔑 Secret pour signer les tokens JWT | `VotreSecretTresSecurise...` |
| `DATABASE_PASSWORD` | 🗄️ Mot de passe base de données | `MotDePasseSecurise123!` |
| `REDIS_PASSWORD` | 🔴 Mot de passe Redis | `RedisPassword123!` |
| `SPRING_PROFILES_ACTIVE` | 🎯 Profil Spring actif | `dev` / `prod` |

### 5. Compilation et exécution

```bash
# Compilation
mvn clean install

# Lancement en développement
mvn spring-boot:run

# Ou avec profil spécifique
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🔐 APIs d'authentification

### Gestion des comptes

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/auth/signup` | POST | Création d'un compte utilisateur |
| `/api/auth/login` | POST | Connexion (avec MFA optionnel) |
| `/api/auth/refresh` | POST | Rafraîchissement du token |
| `/api/auth/logout` | POST | Déconnexion et invalidation token |
| `/api/auth/me` | GET | Informations utilisateur connecté |

### Authentification à deux facteurs (MFA) 🔐

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/mfa/setup` | POST | Configuration initiale MFA |
| `/api/mfa/qrcode` | GET | Téléchargement QR code (PNG) |
| `/api/mfa/verify` | POST | Activation MFA après scan |
| `/api/mfa/status` | GET | Statut MFA de l'utilisateur |
| `/api/mfa/validate` | POST | Validation d'un code TOTP |
| `/api/mfa/disable` | POST | Désactivation MFA |
| `/api/mfa/regenerate` | POST | Régénération du secret |

## 🏦 APIs de gestion patrimoniale

### Gestion des actifs

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/assets` | GET | Liste des actifs de l'utilisateur |
| `/api/assets/{id}` | GET | Détails d'un actif |
| `/api/assets/type/{type}` | GET | Actifs par type |
| `/api/assets` | POST | Création d'un actif |
| `/api/assets/{id}` | PUT | Mise à jour d'un actif |
| `/api/assets/{id}` | DELETE | Suppression d'un actif |

### Valorisations

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/valuations/asset/{assetId}` | GET | Historique des valorisations |
| `/api/valuations/asset/{assetId}/range` | GET | Valorisations par période |
| `/api/valuations/{id}` | GET | Détails d'une valorisation |
| `/api/valuations` | POST | Ajout d'une valorisation |
| `/api/valuations/{id}` | DELETE | Suppression d'une valorisation |

### Analyse de portefeuille

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/portfolio/summary` | GET | Résumé du patrimoine |
| `/api/portfolio/distribution` | GET | Répartition par catégorie |

## 🧪 Tests et exemples

### Configuration MFA complète

```bash
# 1. Créer un compte
curl -X POST "http://localhost:9999/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPassword123!"
  }'

# 2. Se connecter
curl -X POST "http://localhost:9999/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPassword123!"
  }'

# 3. Configurer MFA (token requis)
curl -X POST "http://localhost:9999/api/mfa/setup" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 4. Télécharger le QR code
curl -X GET "http://localhost:9999/api/mfa/qrcode" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --output qr-code.png

# 5. Activer MFA (après scan du QR code)
curl -X POST "http://localhost:9999/api/mfa/verify" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code": "123456"}'

# 6. Se connecter avec MFA
curl -X POST "http://localhost:9999/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPassword123!",
    "mfaCode": "789012"
  }'
```

### Tests avec collection Postman

Une collection Postman complète est disponible dans `docs/Mosaïque Capital API - Tests Complets.postman_collection.json` avec :

- ✅ **Tests automatisés** de tous les endpoints
- ✅ **Variables dynamiques** pour les tokens
- ✅ **Scénarios complets** MFA
- ✅ **Tests d'erreur** et de sécurité

## 🔒 Sécurité et bonnes pratiques

### Fonctionnalités de sécurité

- 🔐 **JWT avec blacklist** (invalidation côté serveur)
- 🔑 **MFA TOTP** (compatible Google Authenticator)
- 🛡️ **QR codes générés côté serveur** (sécurisé)
- 🚫 **Protection CSRF** et headers sécurisés
- 📝 **Logs d'audit** détaillés
- 🔄 **Rotation automatique** des tokens

### Configuration de production

```properties
# Variables essentielles pour la production
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=VotreSecretProductionTresLong256Bits...
DATABASE_PASSWORD=MotDePasseComplexeProd
REDIS_PASSWORD=RedisPasswordComplexeProd
SSL_ENABLED=true
COOKIE_SECURE=true
LOG_LEVEL_ROOT=WARN
```

### Checklist de déploiement

- [ ] ✅ **Secrets générés** avec `generate-secrets.sh`
- [ ] ✅ **Base de données** configurée et sécurisée
- [ ] ✅ **Redis** configuré pour la production
- [ ] ✅ **SSL/HTTPS** activé
- [ ] ✅ **Logs** configurés et monitored
- [ ] ✅ **Backup** base de données planifié
- [ ] ✅ **Monitoring** (Actuator + Prometheus)

## 🐳 Déploiement Docker

### Développement

```bash
# Démarrer l'environnement complet
docker-compose -f docker-compose.dev.yml up -d

# Vérifier les services
docker-compose -f docker-compose.dev.yml ps
```

### Production (exemple)

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  app:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      JWT_SECRET_FILE: /run/secrets/jwt_secret
      DATABASE_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - jwt_secret
      - db_password
    depends_on:
      - mysql
      - redis

secrets:
  jwt_secret:
    external: true
  db_password:
    external: true
```

## 📊 Monitoring et observabilité

### Endpoints Actuator

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | État de santé de l'application |
| `/actuator/info` | Informations sur l'application |
| `/actuator/prometheus` | Métriques pour Prometheus |

### Métriques clés

- 📈 **Taux de réussite** de l'authentification
- 🔐 **Utilisation MFA** (activation, validation)
- ⚡ **Performance** des endpoints
- 🗄️ **Connexions** base de données et Redis
- 💾 **Utilisation mémoire** et CPU

## 🔄 Roadmap et évolutions

### Version actuelle (v1.0)

- ✅ **Authentification JWT** complète
- ✅ **MFA TOTP** avec QR codes
- ✅ **Gestion d'actifs** basique
- ✅ **API RESTful** documentée

### Prochaines versions

#### v1.1 - Intégrations financières
- 🏦 **Budget Insight** pour agrégation bancaire
- 📊 **APIs de cotations** en temps réel
- 💰 **Support cryptomonnaies**

#### v1.2 - Analyse avancée
- 📈 **Moteur d'analyse patrimoniale**
- 🎯 **Recommandations personnalisées**
- 📋 **Rapports PDF automatisés**

#### v1.3 - Fonctionnalités avancées
- 💸 **Moteur fiscal** français
- 👥 **Gestion multi-comptes** (famille)
- 🤖 **Intelligence artificielle**
