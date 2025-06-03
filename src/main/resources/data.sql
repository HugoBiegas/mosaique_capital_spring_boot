-- ============================================
-- DONNÉES INITIALES POUR LES TYPES D'ASSETS
-- ============================================
-- Fichier: src/main/resources/data.sql

-- Insertion sécurisée avec vérification d'existence
INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`) VALUES
    -- Types de base (sans parent)
    ('REAL_ESTATE', 'Immobilier', 'Biens immobiliers', NULL),
    ('FINANCIAL', 'Actifs financiers', 'Titres, actions, obligations, etc.', NULL),
    ('CASH', 'Liquidités', 'Comptes courants, livrets, etc.', NULL),
    ('CRYPTOCURRENCY', 'Cryptomonnaies', 'Bitcoin, Ethereum, etc.', NULL),
    ('OTHERS', 'Autres actifs', 'Objets de collection, etc.', NULL),
    ('LIABILITIES', 'Passifs', 'Dettes et emprunts', NULL);

-- Types immobiliers (avec référence au parent)
INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'REAL_ESTATE_RESIDENTIAL', 'Résidence principale', 'Résidence principale', at.id
FROM `asset_types` at WHERE at.code = 'REAL_ESTATE';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'REAL_ESTATE_RENTAL', 'Immobilier locatif', 'Immobilier de rendement', at.id
FROM `asset_types` at WHERE at.code = 'REAL_ESTATE';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'REAL_ESTATE_COMMERCIAL', 'Immobilier commercial', 'Locaux professionnels', at.id
FROM `asset_types` at WHERE at.code = 'REAL_ESTATE';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'REAL_ESTATE_LAND', 'Terrains', 'Terrains non bâtis', at.id
FROM `asset_types` at WHERE at.code = 'REAL_ESTATE';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'REAL_ESTATE_SCPI', 'SCPI', 'Sociétés civiles de placement immobilier', at.id
FROM `asset_types` at WHERE at.code = 'REAL_ESTATE';

-- Types financiers
INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'FINANCIAL_STOCKS', 'Actions', 'Titres de propriété d entreprises', at.id
FROM `asset_types` at WHERE at.code = 'FINANCIAL';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'FINANCIAL_BONDS', 'Obligations', 'Titres de créance', at.id
FROM `asset_types` at WHERE at.code = 'FINANCIAL';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'FINANCIAL_FUNDS', 'Fonds', 'OPCVM, ETF, etc.', at.id
FROM `asset_types` at WHERE at.code = 'FINANCIAL';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'FINANCIAL_LIFE_INSURANCE', 'Assurance-vie', 'Contrats d assurance-vie', at.id
FROM `asset_types` at WHERE at.code = 'FINANCIAL';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'FINANCIAL_PEA', 'PEA', 'Plan d Épargne en Actions', at.id
FROM `asset_types` at WHERE at.code = 'FINANCIAL';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'FINANCIAL_RETIREMENT', 'Épargne retraite', 'PER, PERP, Madelin, etc.', at.id
FROM `asset_types` at WHERE at.code = 'FINANCIAL';

-- Types de liquidités
INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'CASH_CURRENT', 'Compte courant', 'Comptes à vue', at.id
FROM `asset_types` at WHERE at.code = 'CASH';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'CASH_SAVINGS', 'Livrets', 'Livrets réglementés et livrets bancaires', at.id
FROM `asset_types` at WHERE at.code = 'CASH';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'CASH_TERM', 'Dépôts à terme', 'Comptes à terme', at.id
FROM `asset_types` at WHERE at.code = 'CASH';

-- Autres actifs
INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'OTHERS_ART', 'Art et collections', 'Œuvres d art, collections diverses', at.id
FROM `asset_types` at WHERE at.code = 'OTHERS';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'OTHERS_PRECIOUS', 'Métaux précieux', 'Or, argent, etc.', at.id
FROM `asset_types` at WHERE at.code = 'OTHERS';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'OTHERS_VEHICLES', 'Véhicules', 'Voitures, bateaux, etc.', at.id
FROM `asset_types` at WHERE at.code = 'OTHERS';

-- Passifs/Dettes
INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'LIABILITIES_MORTGAGE', 'Crédit immobilier', 'Emprunts pour l immobilier', at.id
FROM `asset_types` at WHERE at.code = 'LIABILITIES';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'LIABILITIES_CONSUMER', 'Crédit à la consommation', 'Emprunts pour des biens de consommation', at.id
FROM `asset_types` at WHERE at.code = 'LIABILITIES';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'LIABILITIES_STUDENT', 'Prêt étudiant', 'Emprunts pour les études', at.id
FROM `asset_types` at WHERE at.code = 'LIABILITIES';

INSERT IGNORE INTO `asset_types` (`code`, `label`, `description`, `parent_type_id`)
SELECT 'LIABILITIES_OTHER', 'Autres dettes', 'Autres types de dettes', at.id
FROM `asset_types` at WHERE at.code = 'LIABILITIES';