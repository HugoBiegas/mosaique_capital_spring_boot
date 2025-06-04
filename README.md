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
# 🏗️ Architecture Mosaïque Capital - Schéma Complet

## 📋 Vue d'ensemble de l'architecture

```mermaid
graph TB
    subgraph "🌐 Frontend (Client)"
        UI[Interface Utilisateur]
        AUTH_UI[Pages Authentification]
        BANKING_UI[Interface Bancaire]
        PORTFOLIO_UI[Gestion Portfolio]
        DASHBOARD_UI[Dashboard]
    end

    subgraph "🔐 Couche Sécurité"
        JWT[JWT Filter]
        MFA[MFA Verification]
        CORS[CORS Configuration]
    end

    subgraph "🚀 Backend Spring Boot"
        subgraph "📡 Contrôleurs REST"
            AUTH_CTRL[AuthController]
            MFA_CTRL[MfaController]
            BANKING_CTRL[BankingController]
            WEBHOOK_CTRL[WebhookController]
            ASSET_CTRL[AssetController]
            PORTFOLIO_CTRL[PortfolioController]
            VALUATION_CTRL[ValuationController]
        end
        
        subgraph "⚙️ Services Métier"
            AUTH_SVC[AuthService]
            MFA_SVC[MfaService]
            BANKING_SVC[BankConnectionService]
            ACCOUNT_SVC[BankAccountService]
            TRANSACTION_SVC[BankTransactionService]
            SYNC_SVC[BankAccountSyncService]
            ASSET_SVC[AssetService]
            VALUATION_SVC[AssetValuationService]
            SCHEDULER[BankingSyncScheduler]
        end
        
        subgraph "🔌 Services Externes"
            AGGREGATION[BankAggregationService]
            TINK[TinkService]
            BUDGET_INSIGHT[BudgetInsightService]
            BRIDGE[BridgeApiService]
            LINXO[LinxoService]
        end
        
        subgraph "🗃️ Repositories"
            USER_REPO[UserRepository]
            BANK_REPO[BankConnectionRepository]
            ACCOUNT_REPO[BankAccountRepository]
            TRANSACTION_REPO[BankTransactionRepository]
            ASSET_REPO[AssetRepository]
            VALUATION_REPO[ValuationRepository]
        end
    end

    subgraph "💾 Couche Données"
        MYSQL[(MySQL Database)]
        REDIS[(Redis Cache)]
    end

    subgraph "🏦 APIs Bancaires Externes"
        TINK_API[Tink API<br/>🆓 GRATUIT]
        BI_API[Budget Insight API]
        BRIDGE_API[Bridge API]
        LINXO_API[Linxo API]
    end

    subgraph "🔄 Resilience & Monitoring"
        CIRCUIT[Circuit Breaker]
        RETRY[Retry Logic]
        RATE_LIMIT[Rate Limiter]
        HEALTH[Health Checks]
    end

    %% Connexions Frontend → Backend
    UI --> JWT
    AUTH_UI --> AUTH_CTRL
    BANKING_UI --> BANKING_CTRL
    PORTFOLIO_UI --> ASSET_CTRL
    PORTFOLIO_UI --> PORTFOLIO_CTRL
    DASHBOARD_UI --> VALUATION_CTRL

    %% Sécurité
    JWT --> AUTH_CTRL
    JWT --> MFA_CTRL
    JWT --> BANKING_CTRL
    JWT --> ASSET_CTRL
    MFA --> MFA_SVC

    %% Services
    AUTH_CTRL --> AUTH_SVC
    MFA_CTRL --> MFA_SVC
    BANKING_CTRL --> BANKING_SVC
    BANKING_CTRL --> ACCOUNT_SVC
    BANKING_CTRL --> TRANSACTION_SVC
    ASSET_CTRL --> ASSET_SVC
    PORTFOLIO_CTRL --> ASSET_SVC
    VALUATION_CTRL --> VALUATION_SVC

    %% Banking Flow
    BANKING_SVC --> AGGREGATION
    AGGREGATION --> TINK
    AGGREGATION --> BUDGET_INSIGHT
    AGGREGATION --> BRIDGE
    AGGREGATION --> LINXO
    SYNC_SVC --> AGGREGATION
    SCHEDULER --> SYNC_SVC

    %% Resilience
    TINK --> CIRCUIT
    BUDGET_INSIGHT --> CIRCUIT
    BRIDGE --> CIRCUIT
    LINXO --> CIRCUIT
    CIRCUIT --> RETRY
    RETRY --> RATE_LIMIT

    %% APIs Externes
    TINK --> TINK_API
    BUDGET_INSIGHT --> BI_API
    BRIDGE --> BRIDGE_API
    LINXO --> LINXO_API

    %% Webhooks
    TINK_API -.->|Callback| WEBHOOK_CTRL
    BI_API -.->|Webhook| WEBHOOK_CTRL
    BRIDGE_API -.->|Webhook| WEBHOOK_CTRL

    %% Données
    AUTH_SVC --> USER_REPO
    BANKING_SVC --> BANK_REPO
    ACCOUNT_SVC --> ACCOUNT_REPO
    TRANSACTION_SVC --> TRANSACTION_REPO
    ASSET_SVC --> ASSET_REPO
    VALUATION_SVC --> VALUATION_REPO

    %% Base de données
    USER_REPO --> MYSQL
    BANK_REPO --> MYSQL
    ACCOUNT_REPO --> MYSQL
    TRANSACTION_REPO --> MYSQL
    ASSET_REPO --> MYSQL
    VALUATION_REPO --> MYSQL

    %% Cache
    AUTH_SVC --> REDIS
    JWT --> REDIS
    TINK --> REDIS
    BUDGET_INSIGHT --> REDIS

    classDef frontend fill:#e1f5fe
    classDef security fill:#fff3e0
    classDef controller fill:#f3e5f5
    classDef service fill:#e8f5e8
    classDef external fill:#fff9c4
    classDef database fill:#fce4ec
    classDef resilience fill:#f1f8e9

    class UI,AUTH_UI,BANKING_UI,PORTFOLIO_UI,DASHBOARD_UI frontend
    class JWT,MFA,CORS security
    class AUTH_CTRL,MFA_CTRL,BANKING_CTRL,WEBHOOK_CTRL,ASSET_CTRL,PORTFOLIO_CTRL,VALUATION_CTRL controller
    class AUTH_SVC,MFA_SVC,BANKING_SVC,ACCOUNT_SVC,TRANSACTION_SVC,SYNC_SVC,ASSET_SVC,VALUATION_SVC,SCHEDULER service
    class AGGREGATION,TINK,BUDGET_INSIGHT,BRIDGE,LINXO,TINK_API,BI_API,BRIDGE_API,LINXO_API external
    class MYSQL,REDIS,USER_REPO,BANK_REPO,ACCOUNT_REPO,TRANSACTION_REPO,ASSET_REPO,VALUATION_REPO database
    class CIRCUIT,RETRY,RATE_LIMIT,HEALTH resilience
```

## 🔄 Flux d'API Détaillés

### 1. 🔐 Flux d'Authentification

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthController
    participant AS as AuthService
    participant U as UserRepository
    participant J as JwtTokenProvider
    participant R as Redis
    participant M as MfaService

    %% Registration
    C->>+A: POST /api/auth/signup
    A->>+AS: registerUser(request)
    AS->>+U: existsByUsername/Email
    U-->>-AS: boolean
    AS->>+U: save(user)
    U-->>-AS: User
    AS-->>-A: void
    A-->>-C: 201 Created

    %% Login
    C->>+A: POST /api/auth/login
    A->>+AS: authenticateUser(request)
    AS->>+U: findByUsername
    U-->>-AS: User
    alt MFA Enabled
        AS->>+M: verifyMfaCode
        M-->>-AS: boolean
    end
    AS->>+J: createToken/createRefreshToken
    J-->>-AS: tokens
    AS->>+R: cache token
    R-->>-AS: ok
    AS-->>-A: JwtResponse
    A-->>-C: 200 OK + tokens

    %% MFA Setup
    C->>+A: POST /api/mfa/setup
    A->>+M: generateMfaSetup
    M-->>-A: MfaSetupResponse
    A-->>-C: QR Code + secret
```

### 2. 🏦 Flux d'Agrégation Bancaire (Tink)

```mermaid
sequenceDiagram
    participant C as Client
    participant BC as BankingController
    participant BCS as BankConnectionService
    participant BAS as BankAggregationService
    participant TS as TinkService
    participant TA as Tink API
    participant SYNC as SyncService
    participant DB as Database

    %% Initiation connexion
    C->>+BC: POST /api/banking/connections
    BC->>+BCS: initiateConnection
    BCS->>+BAS: initiateConnection("tink")
    BAS->>+TS: initiateConnection
    TS->>+TA: POST /token/new
    TA-->>-TS: access_token
    TS->>+TA: GET /institutions?country=FR
    TA-->>-TS: banks list
    TS->>+TA: POST /agreements/enduser
    TA-->>-TS: agreement_id
    TS->>+TA: POST /requisitions
    TA-->>-TS: requisition_id + link
    TS-->>-BAS: connection_id
    BAS-->>-BCS: connection_id
    BCS->>+DB: save(connection)
    DB-->>-BCS: saved
    BCS-->>-BC: BankConnectionDto
    BC-->>-C: 201 Created + redirect URL

    %% Callback après auth
    TA->>+BC: GET /webhooks/tink/callback?ref=xxx
    BC-->>-C: Redirect to frontend

    %% Polling status
    loop Every 3-5 seconds
        C->>+BC: GET /webhooks/tink/status/{id}
        BC->>+BCS: isConnectionHealthy
        BCS->>+TS: checkHealth
        TS->>+TA: GET /requisitions/{id}
        TA-->>-TS: status
        TS-->>-BCS: boolean
        BCS-->>-BC: status
        BC-->>-C: connection status
    end

    %% Auto sync when active
    BCS->>+SYNC: syncAccountsForConnection
    SYNC->>+TS: getAccounts
    TS->>+TA: GET /accounts
    TA-->>-TS: accounts
    TS-->>-SYNC: ExternalAccountDto[]
    SYNC->>+TS: getTransactions
    TS->>+TA: GET /accounts/{id}/transactions
    TA-->>-TS: transactions
    TS-->>-SYNC: ExternalTransactionDto[]
    SYNC->>+DB: save accounts + transactions
    DB-->>-SYNC: saved
    SYNC-->>-BCS: BankSyncResponse
```

### 3. 💰 Flux de Gestion des Assets

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AssetController
    participant AS as AssetService
    participant AR as AssetRepository
    participant ATR as AssetTypeRepository
    participant VC as ValuationController
    participant VS as ValuationService
    participant VR as ValuationRepository

    %% Create Asset
    C->>+AC: POST /api/assets
    AC->>+AS: createAsset(request)
    AS->>+ATR: findByCode(type)
    ATR-->>-AS: AssetTypeEntity
    AS->>+AR: save(asset)
    AR-->>-AS: Asset
    AS-->>-AC: AssetDto
    AC-->>-C: 201 Created

    %% Get Portfolio Summary
    C->>+AC: GET /api/portfolio/summary
    AC->>+AS: getTotalPatrimony
    AS->>+AR: sumTotalPatrimony
    AR-->>-AS: BigDecimal
    AS->>+AR: getAssetDistributionByType
    AR-->>-AS: List<Projection>
    AS-->>-AC: summary data
    AC-->>-C: portfolio summary

    %% Add Valuation
    C->>+VC: POST /api/valuations
    VC->>+VS: createValuation
    VS->>+AS: findAssetById
    AS->>+AR: findById
    AR-->>-AS: Asset
    AS-->>-VS: Asset
    VS->>+VR: save(valuation)
    VR-->>-VS: AssetValuation
    VS->>+AS: updateAsset (if current date)
    AS-->>-VS: updated
    VS-->>-VC: ValuationDto
    VC-->>-C: 201 Created
```

### 4. 🔄 Flux de Synchronisation Automatique

```mermaid
sequenceDiagram
    participant S as Scheduler
    participant SYNC as SyncService
    participant BR as BankConnectionRepo
    participant BAS as BankAggregationService
    participant EXT as External APIs
    participant N as NotificationService
    participant DB as Database

    %% Scheduled sync (every 6h)
    S->>+SYNC: scheduledSyncAll()
    SYNC->>+BR: findConnectionsNeedingSync
    BR-->>-SYNC: List<BankConnection>
    
    loop For each batch
        SYNC->>+BAS: syncAccountsForConnection
        BAS->>+EXT: getAccounts + getTransactions
        EXT-->>-BAS: account/transaction data
        BAS->>+DB: update accounts/transactions
        DB-->>-BAS: saved
        BAS-->>-SYNC: BankSyncResponse
        
        alt If many new transactions
            SYNC->>+N: notifyNewTransactions
            N-->>-SYNC: notification sent
        end
        
        alt If sync fails repeatedly
            SYNC->>+DB: mark connection as ERROR
            DB-->>-SYNC: updated
            SYNC->>+N: notifyConnectionError
            N-->>-SYNC: notification sent
        end
    end
    SYNC-->>-S: sync completed
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