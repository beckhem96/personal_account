# Project Context: Personal Finance Dashboard

## 1. Project Overview
A **Personal Finance & Tax Assistant Web Application** optimized for PC.
Users can manage budgets, record income/expenses (ledger), track assets, and perform tax calculations/year-end settlement simulations based on Korean Tax Law.

**Domain Language:** Korean (Commits, UI, Comments, Docs).

## 2. Tech Stack & Architecture

### Backend
*   **Framework:** Spring Boot 3.4.1 (Java 17+)
*   **Build:** Gradle
*   **DB:** MySQL 8.0 (Docker) + Spring Data JPA (Hibernate)
*   **API:** RESTful, strict DTO usage (Java records)
*   **Architecture:** Controller -> Service -> Repository -> Database

### Frontend
*   **Framework:** React 19 + TypeScript 5.9 + Vite 7
*   **Styling:** Tailwind CSS 4
*   **HTTP:** Axios (proxied to :8080 in dev)
*   **Components:** Function components, Hooks, Lucide React icons, Recharts

## 3. Data Model
*   **Category:** INCOME, EXPENSE, TRANSFER.
*   **Transaction:** Date, amount, memo, payment method, category(FK), card(FK), confirmed/planned status.
*   **Budget:** Monthly (Year/Month), Amount, Category.
*   **Asset:** Cash, Savings, Stock, Debt.
*   **RecurringTransaction:** Automated monthly transaction generation.

## 4. Key Conventions
*   **Money:** Always use `BigDecimal`.
*   **Entities:** Protected no-args constructor, Builder/Static Factory methods, no setters.
*   **DTOs:** Java Records. Separate Request/Response.
*   **Styles:** Tailwind utility classes only. `cn()` for conditional merging.
*   **API:** `/api` prefix.

## 5. Core Features
*   **Budget/Ledger:** Monthly budget setting, Transaction recording (Planned vs Confirmed).
*   **Assets:** Net worth calculation (Assets - Debt).
*   **Tax:** Korean Stock Capital Gains Tax, Year-end Settlement Simulation (Credit vs Check card balancing).
