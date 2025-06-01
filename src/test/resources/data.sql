-- src/test/resources/data.sql
-- Nettoyage des données existantes
DELETE FROM asset_valuations;
DELETE FROM assets;
DELETE FROM asset_types;
DELETE FROM user_roles;
DELETE FROM users;

-- Insertion de types d'actifs pour les tests
INSERT INTO asset_types (id, code, label, description) VALUES
    (1, 'REAL_ESTATE', 'Immobilier', 'Biens immobiliers'),
    (2, 'STOCK', 'Actions', 'Actions en bourse'),
    (3, 'BOND', 'Obligations', 'Titres de créance'),
    (4, 'BANK_ACCOUNT', 'Compte bancaire', 'Comptes bancaires'),
    (5, 'LIFE_INSURANCE', 'Assurance-vie', 'Contrats d''assurance-vie'),
    (6, 'INVESTMENT_FUND', 'Fonds d''investissement', 'OPCVM, ETF, etc.'),
    (7, 'CRYPTOCURRENCY', 'Cryptomonnaie', 'Bitcoin, Ethereum, etc.'),
    (8, 'OTHER', 'Autre', 'Autres types d''actifs');

-- Reset auto-increment pour asset_types
ALTER TABLE asset_types ALTER COLUMN id RESTART WITH 9;