-- src/test/resources/schema-h2.sql
-- Schema complet basé sur mosaique_capital.sql adapté pour H2

-- Désactiver les contraintes FK pendant la création
SET REFERENTIAL_INTEGRITY FALSE;

-- Supprimer toutes les tables si elles existent (dans l'ordre inverse des dépendances)
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS data_imports;
DROP TABLE IF EXISTS api_keys;
DROP TABLE IF EXISTS shared_access;
DROP TABLE IF EXISTS tax_profiles;
DROP TABLE IF EXISTS dashboard_widgets;
DROP TABLE IF EXISTS user_dashboards;
DROP TABLE IF EXISTS notification_preferences;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS property_expenses;
DROP TABLE IF EXISTS rental_income;
DROP TABLE IF EXISTS real_estate_properties;
DROP TABLE IF EXISTS security_positions;
DROP TABLE IF EXISTS securities;
DROP TABLE IF EXISTS investment_accounts;
DROP TABLE IF EXISTS broker_connections;
DROP TABLE IF EXISTS crypto_assets;
DROP TABLE IF EXISTS crypto_wallets;
DROP TABLE IF EXISTS bank_transactions;
DROP TABLE IF EXISTS bank_accounts;
DROP TABLE IF EXISTS bank_connections;
DROP TABLE IF EXISTS asset_valuations;
DROP TABLE IF EXISTS assets;
DROP TABLE IF EXISTS asset_types;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;

-- Table users
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                       mfa_secret VARCHAR(255),
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP,
                       active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Table user_roles
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role VARCHAR(20) NOT NULL,
                            PRIMARY KEY (user_id, role),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table asset_types
CREATE TABLE asset_types (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             code VARCHAR(30) NOT NULL UNIQUE,
                             label VARCHAR(50) NOT NULL,
                             description TEXT,
                             parent_type_id BIGINT,
                             FOREIGN KEY (parent_type_id) REFERENCES asset_types(id) ON DELETE RESTRICT
);

-- Table assets
CREATE TABLE assets (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        description TEXT,
                        type_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        current_value DECIMAL(19,4) NOT NULL,
                        currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (type_id) REFERENCES asset_types(id) ON DELETE RESTRICT
);

-- Table asset_valuations
CREATE TABLE asset_valuations (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  asset_id BIGINT NOT NULL,
                                  valuation_date DATE NOT NULL,
                                  value DECIMAL(19,4) NOT NULL,
                                  currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                                  source VARCHAR(100),
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);

-- Table bank_connections
CREATE TABLE bank_connections (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  provider VARCHAR(50) NOT NULL,
                                  connection_id VARCHAR(255) NOT NULL,
                                  connection_status VARCHAR(20) NOT NULL,
                                  refresh_token VARCHAR(512),
                                  access_token VARCHAR(512),
                                  token_expires_at TIMESTAMP,
                                  last_sync_at TIMESTAMP,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table bank_accounts
CREATE TABLE bank_accounts (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               connection_id BIGINT NOT NULL,
                               account_id VARCHAR(255) NOT NULL,
                               name VARCHAR(100) NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               balance DECIMAL(19,4) NOT NULL,
                               currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                               last_sync_at TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (connection_id) REFERENCES bank_connections(id) ON DELETE CASCADE
);

-- Table bank_transactions
CREATE TABLE bank_transactions (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   account_id BIGINT NOT NULL,
                                   transaction_id VARCHAR(255) NOT NULL,
                                   amount DECIMAL(19,4) NOT NULL,
                                   currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                                   description VARCHAR(255),
                                   transaction_date DATE NOT NULL,
                                   value_date DATE,
                                   category VARCHAR(100),
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (account_id) REFERENCES bank_accounts(id) ON DELETE CASCADE
);

-- Table crypto_wallets
CREATE TABLE crypto_wallets (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                name VARCHAR(100) NOT NULL,
                                provider VARCHAR(50),
                                wallet_address VARCHAR(255),
                                api_key VARCHAR(255),
                                api_secret VARCHAR(512),
                                last_sync_at TIMESTAMP,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table crypto_assets
CREATE TABLE crypto_assets (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               wallet_id BIGINT NOT NULL,
                               symbol VARCHAR(20) NOT NULL,
                               name VARCHAR(100) NOT NULL,
                               balance DECIMAL(24,8) NOT NULL,
                               current_price DECIMAL(19,4),
                               value_fiat DECIMAL(19,4),
                               currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                               last_sync_at TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (wallet_id) REFERENCES crypto_wallets(id) ON DELETE CASCADE
);

-- Table broker_connections
CREATE TABLE broker_connections (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    broker_name VARCHAR(50) NOT NULL,
                                    account_number VARCHAR(100),
                                    api_key VARCHAR(255),
                                    api_secret VARCHAR(512),
                                    connection_status VARCHAR(20) NOT NULL,
                                    last_sync_at TIMESTAMP,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table investment_accounts
CREATE TABLE investment_accounts (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     broker_connection_id BIGINT NOT NULL,
                                     account_id VARCHAR(255) NOT NULL,
                                     name VARCHAR(100) NOT NULL,
                                     type VARCHAR(50) NOT NULL,
                                     total_value DECIMAL(19,4) NOT NULL,
                                     currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                                     last_sync_at TIMESTAMP,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     FOREIGN KEY (broker_connection_id) REFERENCES broker_connections(id) ON DELETE CASCADE
);

-- Table securities
CREATE TABLE securities (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            isin VARCHAR(12) UNIQUE,
                            symbol VARCHAR(20),
                            name VARCHAR(255) NOT NULL,
                            asset_class VARCHAR(50),
                            currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                            country VARCHAR(50),
                            sector VARCHAR(100),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table security_positions
CREATE TABLE security_positions (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    account_id BIGINT NOT NULL,
                                    security_id BIGINT NOT NULL,
                                    quantity DECIMAL(19,8) NOT NULL,
                                    average_price DECIMAL(19,4),
                                    current_price DECIMAL(19,4),
                                    value DECIMAL(19,4) NOT NULL,
                                    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                                    last_sync_at TIMESTAMP,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (account_id) REFERENCES investment_accounts(id) ON DELETE CASCADE,
                                    FOREIGN KEY (security_id) REFERENCES securities(id) ON DELETE RESTRICT
);

-- Table real_estate_properties
CREATE TABLE real_estate_properties (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        asset_id BIGINT NOT NULL,
                                        address TEXT NOT NULL,
                                        city VARCHAR(100) NOT NULL,
                                        postal_code VARCHAR(20) NOT NULL,
                                        country VARCHAR(50) NOT NULL DEFAULT 'France',
                                        property_type VARCHAR(50) NOT NULL,
                                        area DECIMAL(10,2),
                                        purchase_price DECIMAL(19,4),
                                        purchase_date DATE,
                                        acquisition_fees DECIMAL(19,4),
                                        renovation_cost DECIMAL(19,4),
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);

-- Table rental_income
CREATE TABLE rental_income (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               property_id BIGINT NOT NULL,
                               income_date DATE NOT NULL,
                               amount DECIMAL(19,4) NOT NULL,
                               currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                               notes TEXT,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (property_id) REFERENCES real_estate_properties(id) ON DELETE CASCADE
);

-- Table property_expenses
CREATE TABLE property_expenses (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   property_id BIGINT NOT NULL,
                                   expense_date DATE NOT NULL,
                                   amount DECIMAL(19,4) NOT NULL,
                                   currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
                                   category VARCHAR(50) NOT NULL,
                                   description TEXT,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (property_id) REFERENCES real_estate_properties(id) ON DELETE CASCADE
);

-- Table notifications
CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               message TEXT NOT NULL,
                               link VARCHAR(255),
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table notification_preferences
CREATE TABLE notification_preferences (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          user_id BIGINT NOT NULL,
                                          notification_type VARCHAR(50) NOT NULL,
                                          email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                          push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                          in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          UNIQUE (user_id, notification_type),
                                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table user_dashboards
CREATE TABLE user_dashboards (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 name VARCHAR(100) NOT NULL,
                                 is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                 layout TEXT,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table dashboard_widgets
CREATE TABLE dashboard_widgets (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   dashboard_id BIGINT NOT NULL,
                                   widget_type VARCHAR(50) NOT NULL,
                                   title VARCHAR(100) NOT NULL,
                                   settings TEXT,
                                   position_x INT NOT NULL,
                                   position_y INT NOT NULL,
                                   width INT NOT NULL,
                                   height INT NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (dashboard_id) REFERENCES user_dashboards(id) ON DELETE CASCADE
);

-- Table tax_profiles
CREATE TABLE tax_profiles (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              tax_year INT NOT NULL,
                              tax_residence VARCHAR(50) NOT NULL DEFAULT 'France',
                              household_situation VARCHAR(50),
                              dependents INT NOT NULL DEFAULT 0,
                              income_tax_bracket VARCHAR(50),
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE (user_id, tax_year),
                              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table shared_access
CREATE TABLE shared_access (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               owner_id BIGINT NOT NULL,
                               shared_with_id BIGINT NOT NULL,
                               access_level VARCHAR(20) NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               expires_at TIMESTAMP,
                               FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
                               FOREIGN KEY (shared_with_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table api_keys
CREATE TABLE api_keys (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          name VARCHAR(100) NOT NULL,
                          api_key VARCHAR(255) NOT NULL,
                          secret_hash VARCHAR(255) NOT NULL,
                          permissions TEXT,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          expires_at TIMESTAMP,
                          last_used_at TIMESTAMP,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table data_imports
CREATE TABLE data_imports (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              import_type VARCHAR(50) NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              filename VARCHAR(255),
                              records_processed INT,
                              records_imported INT,
                              error_message TEXT,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              completed_at TIMESTAMP,
                              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table audit_logs
CREATE TABLE audit_logs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            user_id BIGINT,
                            action VARCHAR(50) NOT NULL,
                            entity_type VARCHAR(50) NOT NULL,
                            entity_id BIGINT,
                            details TEXT,
                            ip_address VARCHAR(45),
                            user_agent VARCHAR(255),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_assets_owner ON assets(user_id);
CREATE INDEX idx_assets_type ON assets(type_id);
CREATE INDEX idx_valuations_asset ON asset_valuations(asset_id);
CREATE INDEX idx_valuations_date ON asset_valuations(valuation_date);
CREATE INDEX idx_transactions_account ON bank_transactions(account_id);
CREATE INDEX idx_transactions_date ON bank_transactions(transaction_date);
CREATE INDEX idx_securities_symbol ON securities(symbol);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- Réactiver les contraintes FK
SET REFERENTIAL_INTEGRITY TRUE;