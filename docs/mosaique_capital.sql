-- -----------------------------------------------------
-- Schema mosaique_capital
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `mosaique_capital` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mosaique_capital`;

-- -----------------------------------------------------
-- Table `users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT,
                                       `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `first_name` VARCHAR(50) NULL,
    `last_name` VARCHAR(50) NULL,
    `mfa_enabled` TINYINT(1) NOT NULL DEFAULT 0,
    `mfa_secret` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL,
    `last_login_at` DATETIME NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `email_UNIQUE` (`email` ASC),
    UNIQUE INDEX `username_UNIQUE` (`username` ASC)
    );

-- -----------------------------------------------------
-- Table `user_roles`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_roles` (
                                            `user_id` BIGINT NOT NULL,
                                            `role` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`user_id`, `role`),
    CONSTRAINT `fk_roles_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `asset_types`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `asset_types` (
                                             `id` BIGINT NOT NULL AUTO_INCREMENT,
                                             `code` VARCHAR(30) NOT NULL,
    `label` VARCHAR(50) NOT NULL,
    `description` TEXT NULL,
    `parent_type_id` BIGINT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `code_UNIQUE` (`code` ASC),
    INDEX `fk_asset_types_parent_idx` (`parent_type_id` ASC),
    CONSTRAINT `fk_asset_types_parent`
    FOREIGN KEY (`parent_type_id`)
    REFERENCES `asset_types` (`id`)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `assets` - ✅ MISE À JOUR AVEC GESTION DES VENTES
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `assets` (
                                        `id` BIGINT NOT NULL AUTO_INCREMENT,
                                        `name` VARCHAR(100) NOT NULL,
    `description` TEXT NULL,
    `type_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `current_value` DECIMAL(19,4) NOT NULL,
    `acquisition_value` DECIMAL(19,4) NULL COMMENT 'Prix d''acquisition (historique)',
    `acquisition_date` DATE NULL COMMENT 'Date d''acquisition',
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `is_liability` TINYINT(1) NOT NULL DEFAULT 0,

    -- ✅ NOUVEAUX CHAMPS POUR LA GESTION DES VENTES
    `status` ENUM('ACTIVE', 'SOLD', 'PENDING_SALE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Statut de l''actif',
    `sale_date` DATE NULL COMMENT 'Date de vente de l''actif',
    `sale_price` DECIMAL(19,4) NULL COMMENT 'Prix de vente réalisé',
    `sale_notes` TEXT NULL COMMENT 'Notes et commentaires sur la vente',
    `purchase_price` DECIMAL(19,4) NULL COMMENT 'Prix d''achat pour calcul plus-values',
    `purchase_date` DATE NULL COMMENT 'Date d''achat pour calcul plus-values',

    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_assets_users_idx` (`user_id` ASC),
    INDEX `fk_assets_types_idx` (`type_id` ASC),

    -- ✅ NOUVEAUX INDEX POUR OPTIMISER LES REQUÊTES DE VENTE
    INDEX `idx_assets_status` (`status` ASC),
    INDEX `idx_assets_user_status` (`user_id` ASC, `status` ASC),
    INDEX `idx_assets_sale_date` (`sale_date` ASC),
    INDEX `idx_assets_user_sale_date` (`user_id` ASC, `sale_date` ASC),

    CONSTRAINT `fk_assets_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
    CONSTRAINT `fk_assets_types`
    FOREIGN KEY (`type_id`)
    REFERENCES `asset_types` (`id`)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION,

    -- ✅ CONTRAINTES MÉTIER POUR LA SÉCURITÉ DES DONNÉES
    CONSTRAINT `chk_sale_price_positive`
    CHECK (`sale_price` IS NULL OR `sale_price` > 0),

    CONSTRAINT `chk_purchase_price_positive`
    CHECK (`purchase_price` IS NULL OR `purchase_price` > 0),

    CONSTRAINT `chk_sale_complete`
    CHECK (
(`sale_date` IS NULL AND `sale_price` IS NULL) OR
(`sale_date` IS NOT NULL AND `sale_price` IS NOT NULL)
    ),

    CONSTRAINT `chk_sold_assets_complete`
    CHECK (
(`status` != 'SOLD') OR
(`status` = 'SOLD' AND `sale_date` IS NOT NULL AND `sale_price` IS NOT NULL)
    ),

    CONSTRAINT `chk_current_value_positive`
    CHECK (`current_value` > 0)
    );

-- -----------------------------------------------------
-- Table `asset_valuations`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `asset_valuations` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                  `asset_id` BIGINT NOT NULL,
                                                  `valuation_date` DATE NOT NULL,
                                                  `value` DECIMAL(19,4) NOT NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `source` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_valuations_assets_idx` (`asset_id` ASC),
    INDEX `idx_valuations_date` (`valuation_date` ASC),
    CONSTRAINT `fk_valuations_assets`
    FOREIGN KEY (`asset_id`)
    REFERENCES `assets` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `bank_connections`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `bank_connections` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                  `user_id` BIGINT NOT NULL,
                                                  `provider` VARCHAR(50) NOT NULL,
    `connection_id` VARCHAR(255) NOT NULL,
    `connection_status` VARCHAR(20) NOT NULL,
    `refresh_token` VARCHAR(512) NULL,
    `access_token` VARCHAR(512) NULL,
    `token_expires_at` DATETIME NULL,
    `last_sync_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_bank_connections_users_idx` (`user_id` ASC),
    CONSTRAINT `fk_bank_connections_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `bank_accounts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `bank_accounts` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `connection_id` BIGINT NOT NULL,
                                               `account_id` VARCHAR(255) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `balance` DECIMAL(19,4) NOT NULL,
    `iban` VARCHAR(100) NOT NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `last_sync_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_bank_accounts_connections_idx` (`connection_id` ASC),
    CONSTRAINT `fk_bank_accounts_connections`
    FOREIGN KEY (`connection_id`)
    REFERENCES `bank_connections` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `bank_transactions`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `bank_transactions` (
                                                   `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                   `account_id` BIGINT NOT NULL,
                                                   `transaction_id` VARCHAR(255) NOT NULL,
    `amount` DECIMAL(19,4) NOT NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `description` VARCHAR(255) NULL,
    `transaction_date` DATE NOT NULL,
    `value_date` DATE NULL,
    `category` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_transactions_accounts_idx` (`account_id` ASC),
    INDEX `idx_transactions_date` (`transaction_date` ASC),
    CONSTRAINT `fk_transactions_accounts`
    FOREIGN KEY (`account_id`)
    REFERENCES `bank_accounts` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `crypto_wallets`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `crypto_wallets` (
                                                `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                `user_id` BIGINT NOT NULL,
                                                `name` VARCHAR(100) NOT NULL,
    `provider` VARCHAR(50) NULL,
    `wallet_address` VARCHAR(255) NULL,
    `api_key` VARCHAR(255) NULL,
    `api_secret` VARCHAR(512) NULL,
    `last_sync_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_crypto_wallets_users_idx` (`user_id` ASC),
    CONSTRAINT `fk_crypto_wallets_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `crypto_assets`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `crypto_assets` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `wallet_id` BIGINT NOT NULL,
                                               `symbol` VARCHAR(20) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `balance` DECIMAL(24,8) NOT NULL,
    `current_price` DECIMAL(19,4) NULL,
    `value_fiat` DECIMAL(19,4) NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `last_sync_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_crypto_assets_wallets_idx` (`wallet_id` ASC),
    CONSTRAINT `fk_crypto_assets_wallets`
    FOREIGN KEY (`wallet_id`)
    REFERENCES `crypto_wallets` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `broker_connections`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `broker_connections` (
                                                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                    `user_id` BIGINT NOT NULL,
                                                    `broker_name` VARCHAR(50) NOT NULL,
    `account_number` VARCHAR(100) NULL,
    `api_key` VARCHAR(255) NULL,
    `api_secret` VARCHAR(512) NULL,
    `connection_status` VARCHAR(20) NOT NULL,
    `last_sync_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_broker_connections_users_idx` (`user_id` ASC),
    CONSTRAINT `fk_broker_connections_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `investment_accounts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `investment_accounts` (
                                                     `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                     `broker_connection_id` BIGINT NOT NULL,
                                                     `account_id` VARCHAR(255) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `total_value` DECIMAL(19,4) NOT NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `last_sync_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_investment_accounts_connections_idx` (`broker_connection_id` ASC),
    CONSTRAINT `fk_investment_accounts_connections`
    FOREIGN KEY (`broker_connection_id`)
    REFERENCES `broker_connections` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `securities`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `securities` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                                            `isin` VARCHAR(12) NULL,
    `symbol` VARCHAR(20) NULL,
    `name` VARCHAR(255) NOT NULL,
    `asset_class` VARCHAR(50) NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `country` VARCHAR(50) NULL,
    `sector` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `isin_UNIQUE` (`isin` ASC),
    INDEX `idx_securities_symbol` (`symbol` ASC)
    );

-- -----------------------------------------------------
-- Table `security_positions`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `security_positions` (
                                                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                    `account_id` BIGINT NOT NULL,
                                                    `security_id` BIGINT NOT NULL,
                                                    `quantity` DECIMAL(19,8) NOT NULL,
    `average_price` DECIMAL(19,4) NULL,
    `current_price` DECIMAL(19,4) NULL,
    `value` DECIMAL(19,4) NOT NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `last_sync_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_positions_accounts_idx` (`account_id` ASC),
    INDEX `fk_positions_securities_idx` (`security_id` ASC),
    CONSTRAINT `fk_positions_accounts`
    FOREIGN KEY (`account_id`)
    REFERENCES `investment_accounts` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
    CONSTRAINT `fk_positions_securities`
    FOREIGN KEY (`security_id`)
    REFERENCES `securities` (`id`)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `real_estate_properties`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `real_estate_properties` (
                                                        `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                        `asset_id` BIGINT NOT NULL,
                                                        `address` TEXT NOT NULL,
                                                        `city` VARCHAR(100) NOT NULL,
    `postal_code` VARCHAR(20) NOT NULL,
    `country` VARCHAR(50) NOT NULL DEFAULT 'France',
    `property_type` VARCHAR(50) NOT NULL,
    `area` DECIMAL(10,2) NULL,
    `purchase_price` DECIMAL(19,4) NULL,
    `purchase_date` DATE NULL,
    `acquisition_fees` DECIMAL(19,4) NULL,
    `renovation_cost` DECIMAL(19,4) NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_properties_assets_idx` (`asset_id` ASC),
    CONSTRAINT `fk_properties_assets`
    FOREIGN KEY (`asset_id`)
    REFERENCES `assets` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `rental_income`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `rental_income` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `property_id` BIGINT NOT NULL,
                                               `income_date` DATE NOT NULL,
                                               `amount` DECIMAL(19,4) NOT NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `notes` TEXT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_rental_income_properties_idx` (`property_id` ASC),
    CONSTRAINT `fk_rental_income_properties`
    FOREIGN KEY (`property_id`)
    REFERENCES `real_estate_properties` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `property_expenses`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `property_expenses` (
                                                   `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                   `property_id` BIGINT NOT NULL,
                                                   `expense_date` DATE NOT NULL,
                                                   `amount` DECIMAL(19,4) NOT NULL,
    `currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `category` VARCHAR(50) NOT NULL,
    `description` TEXT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_property_expenses_properties_idx` (`property_id` ASC),
    CONSTRAINT `fk_property_expenses_properties`
    FOREIGN KEY (`property_id`)
    REFERENCES `real_estate_properties` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `notifications`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `user_id` BIGINT NOT NULL,
                                               `type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT NOT NULL,
    `link` VARCHAR(255) NULL,
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_notifications_users_idx` (`user_id` ASC),
    CONSTRAINT `fk_notifications_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `notification_preferences`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `notification_preferences` (
                                                          `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                          `user_id` BIGINT NOT NULL,
                                                          `notification_type` VARCHAR(50) NOT NULL,
    `email_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `push_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `in_app_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `user_type_UNIQUE` (`user_id` ASC, `notification_type` ASC),
    CONSTRAINT `fk_notification_prefs_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `user_dashboards`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_dashboards` (
                                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                 `user_id` BIGINT NOT NULL,
                                                 `name` VARCHAR(100) NOT NULL,
    `is_default` TINYINT(1) NOT NULL DEFAULT 0,
    `layout` JSON NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_dashboards_users_idx` (`user_id` ASC),
    CONSTRAINT `fk_dashboards_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `dashboard_widgets`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `dashboard_widgets` (
                                                   `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                   `dashboard_id` BIGINT NOT NULL,
                                                   `widget_type` VARCHAR(50) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `settings` JSON NULL,
    `position_x` INT NOT NULL,
    `position_y` INT NOT NULL,
    `width` INT NOT NULL,
    `height` INT NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_widgets_dashboards_idx` (`dashboard_id` ASC),
    CONSTRAINT `fk_widgets_dashboards`
    FOREIGN KEY (`dashboard_id`)
    REFERENCES `user_dashboards` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `tax_profiles`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tax_profiles` (
                                              `id` BIGINT NOT NULL AUTO_INCREMENT,
                                              `user_id` BIGINT NOT NULL,
                                              `tax_year` INT NOT NULL,
                                              `tax_residence` VARCHAR(50) NOT NULL DEFAULT 'France',
    `household_situation` VARCHAR(50) NULL,
    `dependents` INT NOT NULL DEFAULT 0,
    `income_tax_bracket` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `user_year_UNIQUE` (`user_id` ASC, `tax_year` ASC),
    CONSTRAINT `fk_tax_profiles_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `shared_access`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `shared_access` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT,
                                               `owner_id` BIGINT NOT NULL,
                                               `shared_with_id` BIGINT NOT NULL,
                                               `access_level` VARCHAR(20) NOT NULL,
    `created_at` DATETIME NOT NULL,
    `expires_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_shared_access_owner_idx` (`owner_id` ASC),
    INDEX `fk_shared_access_shared_idx` (`shared_with_id` ASC),
    CONSTRAINT `fk_shared_access_owner`
    FOREIGN KEY (`owner_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
    CONSTRAINT `fk_shared_access_shared`
    FOREIGN KEY (`shared_with_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `api_keys`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `api_keys` (
                                          `id` BIGINT NOT NULL AUTO_INCREMENT,
                                          `user_id` BIGINT NOT NULL,
                                          `name` VARCHAR(100) NOT NULL,
    `api_key` VARCHAR(255) NOT NULL,
    `secret_hash` VARCHAR(255) NOT NULL,
    `permissions` JSON NULL,
    `created_at` DATETIME NOT NULL,
    `expires_at` DATETIME NULL,
    `last_used_at` DATETIME NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    INDEX `fk_api_keys_users_idx` (`user_id` ASC),
    CONSTRAINT `fk_api_keys_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `data_imports`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `data_imports` (
                                              `id` BIGINT NOT NULL AUTO_INCREMENT,
                                              `user_id` BIGINT NOT NULL,
                                              `import_type` VARCHAR(50) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `filename` VARCHAR(255) NULL,
    `records_processed` INT NULL,
    `records_imported` INT NULL,
    `error_message` TEXT NULL,
    `created_at` DATETIME NOT NULL,
    `completed_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_imports_users_idx` (`user_id` ASC),
    CONSTRAINT `fk_imports_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
    );

-- -----------------------------------------------------
-- Table `audit_logs`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_logs` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                                            `user_id` BIGINT NULL,
                                            `action` VARCHAR(50) NOT NULL,
    `entity_type` VARCHAR(50) NOT NULL,
    `entity_id` BIGINT NULL,
    `details` JSON NULL,
    `ip_address` VARCHAR(45) NULL,
    `user_agent` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_audit_logs_user` (`user_id` ASC),
    INDEX `idx_audit_logs_action` (`action` ASC),
    INDEX `idx_audit_logs_entity` (`entity_type` ASC, `entity_id` ASC)
    );

-- ============================================
-- ✅ VUES UTILITAIRES POUR LA GESTION DES VENTES
-- ============================================

-- Vue pour les actifs actifs uniquement
CREATE OR REPLACE VIEW `v_active_assets` AS
SELECT
    a.id,
    a.name,
    a.description,
    a.current_value,
    a.purchase_price,
    a.purchase_date,
    a.currency,
    a.status,
    at.code as type_code,
    at.label as type_label,
    u.username as owner_name,
    a.created_at,
    a.updated_at,
    -- Calcul de la plus-value latente
    CASE
        WHEN a.purchase_price IS NOT NULL THEN
            (a.current_value - a.purchase_price)
        ELSE 0
        END as unrealized_gain,
    -- Calcul du pourcentage de plus-value latente
    CASE
        WHEN a.purchase_price IS NOT NULL AND a.purchase_price > 0 THEN
            ROUND(((a.current_value - a.purchase_price) / a.purchase_price) * 100, 2)
        ELSE 0
        END as unrealized_gain_percentage
FROM `assets` a
         JOIN `asset_types` at ON a.type_id = at.id
    JOIN `users` u ON a.user_id = u.id
WHERE a.status = 'ACTIVE';

-- Vue pour les ventes avec calcul de plus-value réalisée
CREATE OR REPLACE VIEW `v_asset_sales` AS
SELECT
    a.id,
    a.name,
    a.description,
    a.sale_date,
    a.sale_price,
    a.purchase_price,
    a.sale_notes,
    a.currency,
    at.code as type_code,
    at.label as type_label,
    u.username as owner_name,
    a.created_at as asset_created_at,
    a.updated_at as asset_updated_at,
    -- Calcul de la plus-value réalisée
    CASE
        WHEN a.purchase_price IS NOT NULL THEN
            (a.sale_price - a.purchase_price)
        ELSE (a.sale_price - a.acquisition_value)
        END as realized_gain,
    -- Calcul du pourcentage de plus-value réalisée
    CASE
        WHEN a.purchase_price IS NOT NULL AND a.purchase_price > 0 THEN
            ROUND(((a.sale_price - a.purchase_price) / a.purchase_price) * 100, 2)
        WHEN a.acquisition_value IS NOT NULL AND a.acquisition_value > 0 THEN
            ROUND(((a.sale_price - a.acquisition_value) / a.acquisition_value) * 100, 2)
        ELSE 0
        END as realized_gain_percentage,
    -- Durée de détention en jours
    CASE
        WHEN a.purchase_date IS NOT NULL THEN
            DATEDIFF(a.sale_date, a.purchase_date)
        WHEN a.acquisition_date IS NOT NULL THEN
            DATEDIFF(a.sale_date, a.acquisition_date)
        ELSE NULL
        END as holding_period_days
FROM `assets` a
         JOIN `asset_types` at ON a.type_id = at.id
    JOIN `users` u ON a.user_id = u.id
WHERE a.status = 'SOLD';

-- Vue des statistiques de ventes par utilisateur
CREATE OR REPLACE VIEW `v_user_sales_stats` AS
SELECT
    u.id as user_id,
    u.username,
    COUNT(a.id) as total_sales,
    SUM(a.sale_price) as total_sale_value,
    AVG(a.sale_price) as average_sale_price,
    SUM(CASE
            WHEN a.purchase_price IS NOT NULL THEN (a.sale_price - a.purchase_price)
            ELSE (a.sale_price - a.acquisition_value)
        END) as total_realized_gain,
    AVG(CASE
            WHEN a.purchase_price IS NOT NULL AND a.purchase_price > 0 THEN
                ((a.sale_price - a.purchase_price) / a.purchase_price) * 100
            WHEN a.acquisition_value IS NOT NULL AND a.acquisition_value > 0 THEN
                ((a.sale_price - a.acquisition_value) / a.acquisition_value) * 100
            ELSE 0
        END) as average_gain_percentage,
    MIN(a.sale_date) as first_sale_date,
    MAX(a.sale_date) as last_sale_date
FROM `users` u
         LEFT JOIN `assets` a ON u.id = a.user_id AND a.status = 'SOLD'
GROUP BY u.id, u.username;

-- ============================================
-- DONNÉES INITIALES
-- ============================================

-- Initial Data for Asset Types (inchangé)
INSERT INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`) VALUES
                                                                                 ('REAL_ESTATE', 'Immobilier', 'Biens immobiliers', NULL),
                                                                                 ('REAL_ESTATE_RESIDENTIAL', 'Résidence principale', 'Résidence principale', 1),
                                                                                 ('REAL_ESTATE_RENTAL', 'Immobilier locatif', 'Immobilier de rendement', 1),
                                                                                 ('REAL_ESTATE_COMMERCIAL', 'Immobilier commercial', 'Locaux professionnels', 1),
                                                                                 ('REAL_ESTATE_LAND', 'Terrains', 'Terrains non bâtis', 1),
                                                                                 ('REAL_ESTATE_SCPI', 'SCPI', 'Sociétés civiles de placement immobilier', 1),
                                                                                 ('FINANCIAL', 'Actifs financiers', 'Titres, actions, obligations, etc.', NULL),
                                                                                 ('FINANCIAL_STOCKS', 'Actions', 'Titres de propriété d entreprises', 7),
                                                                                 ('FINANCIAL_BONDS', 'Obligations', 'Titres de créance', 7),
                                                                                 ('FINANCIAL_FUNDS', 'Fonds', 'OPCVM, ETF, etc.', 7),
                                                                                 ('FINANCIAL_LIFE_INSURANCE', 'Assurance-vie', 'Contrats d assurance-vie', 7),
                                                                                 ('FINANCIAL_PEA', 'PEA', 'Plan d Épargne en Actions', 7),
                                                                                 ('FINANCIAL_RETIREMENT', 'Épargne retraite', 'PER, PERP, Madelin, etc.', 7),
                                                                                 ('CASH', 'Liquidités', 'Comptes courants, livrets, etc.', NULL),
                                                                                 ('CASH_CURRENT', 'Compte courant', 'Comptes à vue', 14),
                                                                                 ('CASH_SAVINGS', 'Livrets', 'Livrets réglementés et livrets bancaires', 14),
                                                                                 ('CASH_TERM', 'Dépôts à terme', 'Comptes à terme', 14),
                                                                                 ('CRYPTOCURRENCY', 'Cryptomonnaies', 'Bitcoin, Ethereum, etc.', NULL),
                                                                                 ('OTHERS', 'Autres actifs', 'Objets de collection, etc.', NULL),
                                                                                 ('OTHERS_ART', 'Art et collections', 'Œuvres d art, collections diverses', 19),
                                                                                 ('OTHERS_PRECIOUS', 'Métaux précieux', 'Or, argent, etc.', 19),
                                                                                 ('OTHERS_VEHICLES', 'Véhicules', 'Voitures, bateaux, etc.', 19),
                                                                                 ('LIABILITIES', 'Passifs', 'Dettes et emprunts', NULL),
                                                                                 ('LIABILITIES_MORTGAGE', 'Crédit immobilier', 'Emprunts pour l immobilier', 23),
                                                                                 ('LIABILITIES_CONSUMER', 'Crédit à la consommation', 'Emprunts pour des biens de consommation', 23),
                                                                                 ('LIABILITIES_STUDENT', 'Prêt étudiant', 'Emprunts pour les études', 23),
                                                                                 ('LIABILITIES_OTHER', 'Autres dettes', 'Autres types de dettes', 23);

-- ============================================
-- ✅ MISE À JOUR DES ACTIFS EXISTANTS (si migration)
-- ============================================

-- Initialiser les nouveaux champs pour les actifs existants
UPDATE `assets`
SET
    `purchase_price` = COALESCE(`purchase_price`, `acquisition_value`, `current_value`),
    `purchase_date` = COALESCE(`purchase_date`, `acquisition_date`, DATE(`created_at`))
WHERE `purchase_price` IS NULL OR `purchase_date` IS NULL;

-- ============================================
-- PROCÉDURES STOCKÉES UTILITAIRES
-- ============================================

DELIMITER //

-- Procédure pour calculer les statistiques de performance d'un portefeuille
CREATE PROCEDURE `sp_portfolio_performance`(IN p_user_id BIGINT)
BEGIN
SELECT
    'Active Assets' as category,
    COUNT(*) as asset_count,
    SUM(current_value) as total_value,
    SUM(CASE
            WHEN purchase_price IS NOT NULL THEN (current_value - purchase_price)
            ELSE 0
        END) as unrealized_gain
FROM assets
WHERE user_id = p_user_id AND status = 'ACTIVE'

UNION ALL

SELECT
    'Sold Assets' as category,
    COUNT(*) as asset_count,
    SUM(sale_price) as total_value,
    SUM(CASE
            WHEN purchase_price IS NOT NULL THEN (sale_price - purchase_price)
            ELSE (sale_price - acquisition_value)
        END) as realized_gain
FROM assets
WHERE user_id = p_user_id AND status = 'SOLD';
END //

-- Procédure pour identifier les actifs suggérés à la vente
CREATE PROCEDURE `sp_suggest_assets_for_sale`(IN p_user_id BIGINT, IN p_min_gain_percent DECIMAL(5,2))
BEGIN
SELECT
    a.id,
    a.name,
    a.current_value,
    a.purchase_price,
    (a.current_value - a.purchase_price) as potential_gain,
    ROUND(((a.current_value - a.purchase_price) / a.purchase_price) * 100, 2) as gain_percentage,
    DATEDIFF(CURDATE(), a.purchase_date) as holding_days,
    at.label as asset_type
FROM assets a
         JOIN asset_types at ON a.type_id = at.id
WHERE
    a.user_id = p_user_id
  AND a.status = 'ACTIVE'
  AND a.purchase_price IS NOT NULL
  AND a.purchase_price > 0
  AND ((a.current_value - a.purchase_price) / a.purchase_price) * 100 >= p_min_gain_percent
ORDER BY gain_percentage DESC
    LIMIT 10;
END //

DELIMITER ;

-- ============================================
-- INDEX SUPPLÉMENTAIRES POUR PERFORMANCE
-- ============================================

-- Index composé pour les requêtes de performance par utilisateur
CREATE INDEX `idx_assets_user_status_type` ON `assets` (`user_id`, `status`, `type_id`);

-- Index pour les requêtes de gain/perte
CREATE INDEX `idx_assets_purchase_price` ON `assets` (`purchase_price`);

-- Index pour les requêtes par période de vente
CREATE INDEX `idx_assets_sale_date_user` ON `assets` (`sale_date`, `user_id`) WHERE `sale_date` IS NOT NULL;

-- ============================================
-- COMMENTAIRES FINAUX
-- ============================================

-- Ajout de commentaires sur la table assets pour documentation
ALTER TABLE `assets` COMMENT = 'Table principale des actifs avec gestion complète des ventes et calculs de plus-values';

-- Vérifications post-création
SELECT 'Schema créé avec succès - Gestion des ventes d''actifs activée' as status;