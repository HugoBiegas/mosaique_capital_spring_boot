# Mosaïque Capital - Backend API

## Description du projet

Mosaïque Capital est une plateforme de gestion patrimoniale complète qui permet aux utilisateurs de suivre, gérer et optimiser leur patrimoine financier. Le système couvre les actifs de différentes natures (immobilier, placements financiers, cryptomonnaies, etc.) et offre des outils d'analyse et de reporting.

Ce repository contient la partie backend sous forme d'API REST développée avec Spring Boot.

## Structure du projet

```
com.master.mosaique_capital
├── config         // Configurations Spring (sécurité, etc.)
│   └── SecurityConfig.java
├── controller     // Contrôleurs REST 
│   ├── AssetController.java
│   ├── AssetValuationController.java
│   ├── AuthController.java
│   └── PortfolioController.java
├── dto            // Objets de transfert de données
│   ├── asset
│   │   ├── AssetCreateRequest.java
│   │   ├── AssetDto.java
│   │   └── AssetValuationDto.java
│   └── auth
│       ├── JwtResponse.java
│       ├── LoginRequest.java
│       └── SignupRequest.java
├── entity         // Entités JPA
│   ├── Asset.java
│   ├── AssetTypeEntity.java
│   ├── AssetValuation.java
│   └── User.java
├── enums          // Énumérations
│   ├── AssetType.java
│   └── Role.java
├── exception      // Gestion des exceptions
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidCredentialsException.java
│   └── ResourceNotFoundException.java
├── mapper         // Mappers DTO <-> Entity
│   ├── AssetMapper.java
│   ├── AssetValuationMapper.java
│   ├── UserMapper.java
│   └── impl
│       ├── AssetMapperImpl.java
│       ├── AssetValuationMapperImpl.java
│       └── UserMapperImpl.java
├── repository     // Repositories JPA
│   ├── AssetRepository.java
│   ├── AssetValuationRepository.java
│   └── UserRepository.java
├── security       // Implémentation JWT et sécurité
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
├── service        // Services métier
│   ├── AssetService.java
│   ├── AssetValuationService.java
│   └── auth
│       ├── AuthService.java
│       └── UserDetailsServiceImpl.java
└── util           // Classes utilitaires
    └── SecurityUtils.java
```

## Technologies utilisées

- Java 21
- Spring Boot 3.4.5
- Spring Security avec JWT
- Spring Data JPA
- MySQL 8
- Lombok
- MapStruct
- Validation API
- TOTP pour l'authentification à 2 facteurs

## Installation et configuration

1. **Prérequis**
    - JDK 21
    - MySQL 8.x
    - Maven 3.8+

2. **Configuration de la base de données**
    - Créer une base de données MySQL (si non existante)
   ```sql
   CREATE DATABASE mosaique_capital CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
    - Configuration dans `application.properties`
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/mosaique_capital?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=votre_utilisateur
   spring.datasource.password=votre_mot_de_passe
   ```

3. **Compilation et exécution**
   ```bash
   # Compilation
   mvn clean install
   
   # Lancement
   mvn spring-boot:run
   ```

## Fonctionnalités actuelles (MVP)

- Gestion des utilisateurs et authentification sécurisée (JWT + 2FA)
- CRUD pour les actifs patrimoniaux
- Suivi des valorisations d'actifs
- Calculs de patrimoine net et répartition

## API Endpoints

### Authentification

- `POST /api/auth/signup` - Création d'un compte utilisateur
- `POST /api/auth/login` - Connexion et récupération d'un token JWT
- `POST /api/auth/setup-mfa` - Configuration de l'authentification à 2 facteurs
- `POST /api/auth/verify-mfa` - Vérification d'un code 2FA

### Gestion des actifs

- `GET /api/assets` - Liste des actifs de l'utilisateur
- `GET /api/assets/{id}` - Détails d'un actif
- `GET /api/assets/type/{type}` - Liste des actifs par type
- `POST /api/assets` - Création d'un actif
- `PUT /api/assets/{id}` - Mise à jour d'un actif
- `DELETE /api/assets/{id}` - Suppression d'un actif

### Valorisations

- `GET /api/valuations/asset/{assetId}` - Historique des valorisations d'un actif
- `GET /api/valuations/asset/{assetId}/range` - Valorisations par plage de dates
- `GET /api/valuations/{id}` - Détails d'une valorisation
- `POST /api/valuations` - Ajout d'une valorisation
- `DELETE /api/valuations/{id}` - Suppression d'une valorisation

### Portfolio

- `GET /api/portfolio/summary` - Résumé du patrimoine
- `GET /api/portfolio/distribution` - Répartition du patrimoine par catégorie

## Tests de l'API

Un export de collection Postman est disponible pour faciliter les tests des endpoints. Consultez le fichier `postman.json` dans le répertoire `docs`.

## Roadmap

Les fonctionnalités futures prévues par ordre de priorité :

### Priorité 1 - Fonctionnalités essentielles
- Intégration avec Budget Insight pour l'agrégation bancaire
- Moteur d'analyse patrimoniale avancé
- Système de notifications
- API mobile optimisée

### Priorité 2 - Fonctionnalités avancées
- Intégration avec les courtiers financiers
- Support des cryptomonnaies
- Moteur fiscal
- API de personnalisation

### Priorité 3 et plus
- Agrégation financière étendue
- Moteur d'évaluation immobilière
- Analyse de risques avancée
- Gestion multi-comptes (familiale)
- Moteur d'optimisation fiscale et patrimoniale
- Infrastructure d'intelligence artificielle

## Contribution

Les contributions sont les bienvenues. Veuillez suivre ces étapes :

1. Forker le projet
2. Créer votre branche de fonctionnalité (`git checkout -b feature/nouvelle-fonction`)
3. Commiter vos changements (`git commit -m 'Ajout de la fonctionnalité X'`)
4. Pousser sur la branche (`git push origin feature/nouvelle-fonction`)
5. Ouvrir une Pull Request

## Licence

Ce projet est sous licence propriétaire. © 2025 Mosaïque Capital.