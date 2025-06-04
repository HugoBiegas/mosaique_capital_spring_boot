# 🏛️ Mosaïque Capital - Plateforme de Gestion Patrimoniale

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Private-yellow.svg)]()

## 📋 Description du Projet

**Mosaïque Capital** est une plateforme complète de gestion patrimoniale qui permet aux utilisateurs de suivre, gérer et optimiser leur patrimoine financier. Le système couvre tous types d'actifs (immobilier, placements financiers, cryptomonnaies, liquidités) et offre des outils d'analyse avancés avec **intégration bancaire automatisée**.

### 🎯 Fonctionnalités Principales

#### 🔐 **Authentification & Sécurité**
- **Authentification JWT** avec refresh tokens
- **MFA (TOTP)** compatible Google Authenticator
- **Blacklist des tokens** avec Redis
- **Audit logs** complets
- **Chiffrement** des données sensibles

#### 🏦 **Intégration Bancaire**
- **Agrégation multi-providers** : Budget Insight, Bridge API, Linxo
- **Synchronisation automatique** des comptes et transactions
- **Catégorisation intelligente** des transactions
- **Webhooks sécurisés** pour mises à jour temps réel
- **Patterns de résilience** (Circuit Breaker, Retry, Rate Limiter)

#### 💰 **Gestion Patrimoniale**
- **Suivi d'actifs** multi-catégories
- **Valorisations historiques** avec graphiques
- **Analyse de portefeuille** avec répartition
- **Calcul patrimoine net** automatisé

#### 📊 **Analytics & Reporting**
- **Statistiques** revenus vs dépenses
- **Analyse par catégorie** de transactions
- **Alertes** de seuils budgétaires
- **Exports** et rapports PDF

---

## 🏗️ Architecture Technique

### Stack Technologique

| Composant | Technologie | Version | Rôle |
|-----------|-------------|---------|------|
| **Backend** | Spring Boot | 3.4.5 | API REST principale |
| **Sécurité** | Spring Security + JWT | 6.x | Authentification/Autorisation |
| **Base de données** | MySQL | 8.0 | Stockage principal |
| **Cache** | Redis | 7.2 | Sessions/Tokens/Rate limiting |
| **Build** | Maven | 3.8+ | Gestion des dépendances |
| **Conteneurisation** | Docker + Docker Compose | - | Déploiement |

### Architecture Microservice-Ready

```
┌─────────────────────────────────────────────────────────────┐
│                    🌐 API Gateway (Future)                  │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 🏛️ Mosaïque Capital API                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐│
│  │    Auth     │ │   Assets    │ │         Banking         ││
│  │   Service   │ │   Service   │ │         Services        ││
│  └─────────────┘ └─────────────┘ └─────────────────────────┘│
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│  📊 Data Layer: MySQL + Redis + External Banking APIs     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Installation et Configuration

### 1. Prérequis

```bash
# Versions requises
Java 21+
Maven 3.8+
Docker & Docker Compose
MySQL 8.0+ (ou via Docker)
Redis 7.0+ (ou via Docker)
```

### 2. Clone et Configuration

```bash
# Cloner le repository
git clone <repository-url>
cd mosaique-capital

# Copier le fichier d'environnement
cp .env.example .env

# Éditer les variables d'environnement
nano .env
```

### 3. Variables d'Environnement Critiques

```bash
# Base de données
DATABASE_USERNAME=mosaique_user
DATABASE_PASSWORD=your_secure_password_here
DATABASE_PORT=3307

# Redis
REDIS_PASSWORD=your_redis_password_here
REDIS_PORT=6380

# JWT (CRITIQUE - Générer avec: openssl rand -base64 64)
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here

# Banking API Credentials
APP_BANKING_BUDGET_INSIGHT_CLIENT_ID=your_bi_client_id
APP_BANKING_BUDGET_INSIGHT_CLIENT_SECRET=your_bi_secret
APP_BANKING_BUDGET_INSIGHT_WEBHOOK_SECRET=your_webhook_secret

# Application
SERVER_PORT=9999
SPRING_PROFILES_ACTIVE=dev
```

### 4. Démarrage avec Docker

```bash
# Démarrage de l'environnement complet
docker-compose up -d

# Vérification des logs
docker-compose logs -f app

# Santé des services
curl http://localhost:9999/actuator/health
```

### 5. Démarrage en Développement

```bash
# Démarrer seulement MySQL et Redis
docker-compose up -d mysql redis

# Lancer l'application en mode dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 📚 Documentation API Complète

### 🔐 **Authentification** (`/api/auth/*`)

| Endpoint | Méthode | Description | Body |
|----------|---------|-------------|------|
| `/api/auth/signup` | POST | Création compte utilisateur | `SignupRequest` |
| `/api/auth/login` | POST | Connexion (avec MFA optionnel) | `LoginRequest` |
| `/api/auth/refresh` | POST | Rafraîchissement token | `{refreshToken}` |
| `/api/auth/logout` | POST | Déconnexion et invalidation | - |
| `/api/auth/me` | GET | Informations utilisateur | - |

### 🔒 **MFA/2FA** (`/api/mfa/*`)

| Endpoint | Méthode | Description | Sécurité |
|----------|---------|-------------|----------|
| `/api/mfa/setup` | POST | Configuration initiale MFA | 🔑 USER |
| `/api/mfa/qrcode` | GET | QR code PNG pour setup | 🔑 USER |
| `/api/mfa/verify` | POST | Activation après scan QR | 🔑 USER |
| `/api/mfa/status` | GET | Statut MFA utilisateur | 🔑 USER |
| `/api/mfa/validate` | POST | Validation code TOTP | 🔑 USER |
| `/api/mfa/disable` | POST | Désactivation MFA | 🔑 USER |
| `/api/mfa/regenerate` | POST | Nouveau secret MFA | 🔑 USER |

### 💎 **Gestion d'Actifs** (`/api/assets/*`)

| Endpoint | Méthode | Description | Fonctionnalité |
|----------|---------|-------------|----------------|
| `/api/assets` | GET | Liste actifs utilisateur | Pagination |
| `/api/assets/{id}` | GET | Détails actif spécifique | Ownership check |
| `/api/assets/type/{type}` | GET | Actifs par type | `?includeSubTypes=true` |
| `/api/assets` | POST | Création nouvel actif | Validation |
| `/api/assets/{id}` | PUT | Mise à jour actif | Ownership check |
| `/api/assets/{id}` | DELETE | Suppression actif | Ownership check |

### 📈 **Valorisations** (`/api/valuations/*`)

| Endpoint | Méthode | Description | Paramètres |
|----------|---------|-------------|------------|
| `/api/valuations/asset/{assetId}` | GET | Historique valorisations | - |
| `/api/valuations/asset/{assetId}/range` | GET | Valorisations période | `startDate`, `endDate` |
| `/api/valuations/{id}` | GET | Détails valorisation | - |
| `/api/valuations` | POST | Ajout valorisation | Auto-update current value |
| `/api/valuations/{id}` | DELETE | Suppression valorisation | - |

### 📊 **Portfolio** (`/api/portfolio/*`)

| Endpoint | Méthode | Description | Retour |
|----------|---------|-------------|--------|
| `/api/portfolio/summary` | GET | Résumé patrimoine global | Total, répartition |
| `/api/portfolio/distribution` | GET | Répartition par catégorie | Graphiques ready |

---

## 🏦 **APIs Banking** *(NOUVELLES FONCTIONNALITÉS)*

### 🔗 **Connexions Bancaires** (`/api/banking/connections/*`)

| Endpoint | Méthode | Description | Provider Support |
|----------|---------|-------------|------------------|
| `/api/banking/providers` | GET | Liste providers disponibles | Budget Insight, Bridge, Linxo |
| `/api/banking/connections` | GET | Connexions utilisateur | Avec statuts temps réel |
| `/api/banking/connections/{id}` | GET | Détails connexion | Health check |
| `/api/banking/connections` | POST | Nouvelle connexion bancaire | Multi-provider |
| `/api/banking/connections/{id}/confirm` | POST | Confirmation authentification forte | SCA handling |
| `/api/banking/connections/{id}/sync` | POST | Synchronisation manuelle | Force refresh |
| `/api/banking/connections/sync-all` | POST | Sync toutes connexions | Batch processing |
| `/api/banking/connections/{id}/health` | GET | État santé connexion | Real-time status |
| `/api/banking/connections/{id}` | DELETE | Suppression connexion | Cascade delete |

### 💳 **Comptes Bancaires** (`/api/banking/accounts/*`)

| Endpoint | Méthode | Description | Fonctionnalité |
|----------|---------|-------------|----------------|
| `/api/banking/accounts` | GET | Tous comptes utilisateur | Multi-banques |
| `/api/banking/accounts/{id}` | GET | Détails compte spécifique | Solde temps réel |
| `/api/banking/connections/{connectionId}/accounts` | GET | Comptes d'une connexion | Par provider |
| `/api/banking/summary` | GET | Résumé financier global | Assets/Liabilities |

### 💰 **Transactions** (`/api/banking/transactions/*`)

| Endpoint | Méthode | Description | Fonctionnalités |
|----------|---------|-------------|-----------------|
| `/api/banking/transactions/search` | POST | Recherche avancée | Filtres multiples |
| `/api/banking/accounts/{accountId}/transactions` | GET | Transactions d'un compte | Pagination |
| `/api/banking/transactions/{id}` | GET | Détails transaction | - |
| `/api/banking/transactions/{id}/category` | PATCH | Mise à jour catégorie | Catégorisation manuelle |
| `/api/banking/transactions/statistics/categories` | GET | Stats par catégorie | Période configurable |
| `/api/banking/transactions/statistics/cash-flow` | GET | Analyse revenus/dépenses | Taux d'épargne |

### 🔔 **Webhooks Banking** (`/api/banking/webhooks/*`)

| Endpoint | Méthode | Description | Sécurité |
|----------|---------|-------------|----------|
| `/api/banking/webhooks/budget-insight` | POST | Webhook Budget Insight | HMAC-SHA256 |
| `/api/banking/webhooks/linxo` | POST | Webhook Linxo | Signature vérifiée |
| `/api/banking/webhooks/health` | GET | Test connectivité | Public |

### 📋 **Monitoring Banking** (`/api/banking/status`)

| Endpoint | Méthode | Description | Métriques |
|----------|---------|-------------|-----------|
| `/api/banking/status` | GET | Statut global banking | Connexions, comptes, dernière sync |

---

## 🔧 Fonctionnalités Avancées

### 🛡️ **Patterns de Résilience Banking**

```yaml
# Circuit Breaker par Provider
Budget Insight: 60% failure rate, 45s timeout
Bridge API: 50% failure rate, 30s timeout  
Linxo: 40% failure rate, 60s timeout

# Rate Limiting par Provider
Budget Insight: 100 req/min
Bridge API: 75 req/min
Linxo: 30 req/min

# Retry avec Backoff Exponentiel
Max attempts: 3
Backoff: 1s, 2s, 4s
```

### 🔄 **Synchronisation Automatique**

```yaml
# Scheduling
Sync automatique: Toutes les 6h
Nettoyage: Quotidien à 2h
Rapport santé: Lundi 9h

# Batch Processing
Max connexions/batch: 5
Pause entre batches: 2s
Timeout par connexion: 60s
```

### 🎯 **Catégorisation Intelligente**

```yaml
Catégories supportées:
- Alimentation (Monoprix, Carrefour...)
- Transport (SNCF, RATP, Uber...)
- Shopping (Amazon, Fnac...)
- Santé (Pharmacie, Médecin...)
- Logement (Loyer, EDF, Internet...)
- Loisirs (Netflix, Spotify...)
```

---

## 📊 Monitoring et Observabilité

### 🏥 **Health Checks**

```bash
# Application principale
curl http://localhost:9999/actuator/health

# Détails des composants
curl http://localhost:9999/actuator/health/db
curl http://localhost:9999/actuator/health/redis

# Métriques Prometheus
curl http://localhost:9999/actuator/prometheus
```

### 📈 **Métriques Clés**

| Métrique | Description | Seuil Critique |
|----------|-------------|----------------|
| `banking.connections.active` | Connexions bancaires actives | < 90% |
| `auth.mfa.usage_rate` | Taux d'adoption MFA | < 40% |
| `jwt.tokens.blacklisted` | Tokens révoqués | > 1000/h |
| `banking.sync.success_rate` | Succès synchronisation | < 95% |
| `api.response_time` | Temps de réponse moyen | > 2s |

### 📝 **Logs Structurés**

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "level": "INFO",
  "service": "banking",
  "event": "connection_sync",
  "user_id": "12345",
  "connection_id": "bi_67890",
  "provider": "budget-insight",
  "accounts_synced": 3,
  "transactions_synced": 45,
  "duration_ms": 2150
}
```

---

## 🔒 Sécurité et Conformité

### 🛡️ **Mesures de Sécurité**

#### **Authentification**
- JWT avec signature HMAC-SHA256
- Refresh tokens rotatifs
- Blacklist temps réel avec Redis
- MFA TOTP (RFC 6238)

#### **Banking APIs**
- Webhooks signés (HMAC-SHA256)
- TLS 1.3 obligatoire
- Rate limiting par IP/utilisateur
- Audit trails complets

#### **Données Sensibles**
- Mots de passe hachés (BCrypt)
- Secrets MFA chiffrés
- Logs masqués (PII/credentials)
- Tokens expiration courte (15min)

### 📋 **Conformité**

| Standard | Statut | Description |
|----------|--------|-------------|
| **PSD2** | ✅ | Intégration APIs régulées |
| **RGPD** | ✅ | Audit logs, masquage données |
| **PCI DSS** | 🔄 | En cours (pas de cartes stockées) |
| **ISO 27001** | 📋 | Framework sécurité |

---

## 🚦 Environnements et Déploiement

### 🏗️ **Profils Spring**

```yaml
# Développement
spring.profiles.active=dev
- H2 en mémoire optionnel
- Logs debug activés
- Hot reload activé
- Banking en mode sandbox

# Test
spring.profiles.active=test  
- Base test dédiée
- Mocks banking providers
- Données factices
- Tests d'intégration

# Production
spring.profiles.active=prod
- MySQL cluster
- Redis cluster
- Banking APIs réelles
- Monitoring complet
```

### 🐳 **Docker Production**

```bash
# Build optimisé
docker build -t mosaique-capital:latest .

# Déploiement avec secrets
docker-compose -f docker-compose.prod.yml up -d

# Scaling horizontal
docker-compose up -d --scale app=3
```

---


## 🛠️ Développement et Contribution

### 🏗️ **Structure du Projet**

```
src/main/java/com/master/mosaique_capital/
├── config/               # Configuration Spring
│   ├── SecurityConfig.java
│   ├── BankingConfig.java
│   └── BankingResilienceConfig.java
├── controller/           # Contrôleurs REST
│   ├── AuthController.java
│   ├── MfaController.java
│   ├── AssetController.java
│   └── BankingController.java      # NOUVEAU
├── service/              # Logique métier
│   ├── auth/
│   ├── banking/                    # NOUVEAU
│   │   ├── BankConnectionService.java
│   │   ├── BankAccountSyncService.java
│   │   └── external/
│   └── mfa/
├── entity/               # Entités JPA
├── dto/                  # Data Transfer Objects
│   ├── auth/
│   ├── banking/                    # NOUVEAU
│   └── mfa/
├── repository/           # Repositories JPA
├── security/             # Sécurité JWT
├── mapper/               # MapStruct mappers
└── exception/            # Gestion d'erreurs
```


### 📝 **Conventions**

- **Branches** : `feature/banking-integration`, `hotfix/security-patch`
- **Commits** : `feat(banking): add Budget Insight integration`
- **PR Reviews** : Obligatoire, 2 approbations minimum
- **Documentation** : Swagger + README à jour

---

## 🗺️ Roadmap

### Version Actuelle (v1.0) ✅
- ✅ Authentification JWT + MFA
- ✅ Gestion d'actifs patrimoniaux
- ✅ **Intégration bancaire multi-providers**
- ✅ **Synchronisation automatique**
- ✅ **Patterns de résilience**

### v1.1 - Amélioration Banking 🚧
- 🔄 Support Tink/Nordigen
- 🔄 Agrégation cryptomonnaies
- 🔄 Alertes temps réel
- 🔄 Export données (PDF/Excel)

### v1.2 - Intelligence Artificielle 📋
- 📋 Catégorisation IA
- 📋 Prédictions dépenses
- 📋 Conseils personnalisés
- 📋 Détection fraude

### v1.3 - Évolutions Avancées 🎯
- 🎯 Moteur fiscal français
- 🎯 Gestion multi-comptes famille
- 🎯 API publique partenaires
- 🎯 Mobile app native

---

## 📄 Licence et Mentions

**© 2025 Mosaïque Capital - Tous droits réservés**

### 🏛️ **Providers Bancaires**
- **Budget Insight (Powens)** - Leader européen agrégation
- **Bridge API** - Solution moderne haute performance
- **Linxo Connect** - Plateforme Crédit Agricole

### 🔧 **Technologies Open Source**
- Spring Boot, Spring Security, Redis, MySQL
- Resilience4j, MapStruct, Lombok
- Docker, Maven, JUnit