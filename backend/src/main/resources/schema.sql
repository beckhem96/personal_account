-- Personal Finance Dashboard DDL
-- MySQL 8.0

-- Drop tables if exist (in reverse dependency order)
DROP TABLE IF EXISTS recurring_transaction;
DROP TABLE IF EXISTS budget;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS card;
DROP TABLE IF EXISTS asset;
DROP TABLE IF EXISTS category;

-- Category Table
CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Card Table
CREATE TABLE card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Asset Table
CREATE TABLE asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    balance DECIMAL(19,2) NOT NULL,
    purchase_price DECIMAL(19,2),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Transaction Table
CREATE TABLE transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    date DATE NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    memo VARCHAR(255),
    payment_method VARCHAR(20) NOT NULL,
    category_id BIGINT NOT NULL,
    card_id BIGINT,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_transaction_card FOREIGN KEY (card_id) REFERENCES card(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Budget Table
CREATE TABLE budget (
    id BIGINT NOT NULL AUTO_INCREMENT,
    year INT NOT NULL,
    month INT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Recurring Transaction Table
CREATE TABLE recurring_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    day_of_month INT NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    card_id BIGINT,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_recurring_card FOREIGN KEY (card_id) REFERENCES card(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for better performance
CREATE INDEX idx_transaction_date ON transaction(date);
CREATE INDEX idx_transaction_category ON transaction(category_id);
CREATE INDEX idx_budget_year_month ON budget(year, month);
CREATE INDEX idx_budget_category ON budget(category_id);
CREATE INDEX idx_recurring_category ON recurring_transaction(category_id);
