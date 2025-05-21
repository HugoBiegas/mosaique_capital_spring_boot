-- Insertion de types d'actifs pour les tests
INSERT INTO asset_types (code, label, description) VALUES
                                                       ('REAL_ESTATE', 'Immobilier', 'Biens immobiliers'),
                                                       ('STOCK', 'Actions', 'Actions en bourse'),
                                                       ('BOND', 'Obligations', 'Titres de créance'),
                                                       ('BANK_ACCOUNT', 'Compte bancaire', 'Comptes bancaires'),
                                                       ('LIFE_INSURANCE', 'Assurance-vie', 'Contrats d''assurance-vie'),
                                                       ('INVESTMENT_FUND', 'Fonds d''investissement', 'OPCVM, ETF, etc.'),
                                                       ('CRYPTOCURRENCY', 'Cryptomonnaie', 'Bitcoin, Ethereum, etc.'),
                                                       ('OTHER', 'Autre', 'Autres types d''actifs');