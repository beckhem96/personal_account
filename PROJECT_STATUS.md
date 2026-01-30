# Project Status: Personal Finance Dashboard

## 1. Project Overview
This project is a web-based **Personal Finance & Tax Assistant** optimized for PC environments.
It enables users to manage budgets, track expenses/income (ledger), monitor assets, and perform tax calculations/simulations based on Korean tax laws.

## 2. System Architecture

### Backend
*   **Framework:** Java 17+, Spring Boot 3.x
*   **Build Tool:** Gradle
*   **Database:** MySQL (Dockerized), JPA (Hibernate)
*   **API Style:** RESTful API
*   **Key Modules:**
    *   `controller`: API Endpoints
    *   `service`: Business Logic (Tax calculation, etc.)
    *   `repository`: Data Access
    *   `domain`: JPA Entities
    *   `dto`: Data Transfer Objects

### Frontend
*   **Framework:** React (Vite) with TypeScript
*   **Styling:** Tailwind CSS + UI Component Library (Shadcn/UI or MUI)
*   **HTTP Client:** Axios
*   **Structure:**
    *   `pages`: Dashboard, Budget, Assets, Tax
    *   `components`: Reusable UI elements

## 3. Database Schema
Based on the current domain entities:

*   **Transaction** (Ledger)
    *   `id`: PK
    *   `date`: Date of transaction
    *   `amount`: Transaction amount
    *   `memo`: Description
    *   `paymentMethod`: Enum (CARD, CASH, etc.)
    *   `category_id`: FK to Category
    *   `card_id`: FK to Card (Nullable)
    *   `isConfirmed`: Boolean (for planned vs actual)

*   **Budget**
    *   `id`: PK
    *   `year`, `month`: Target period
    *   `amount`: Budget limit
    *   `category_id`: FK to Category

*   **Asset**
    *   `id`: PK
    *   `type`: Enum (CASH, SAVINGS, STOCK, DEBT, etc.)
    *   `name`: Asset name
    *   `balance`: Current value
    *   `purchasePrice`: For stocks (to calc return)

*   **Card**
    *   `id`: PK
    *   `name`: Card name
    *   `type`: Enum (CREDIT, DEBIT)

*   **RecurringTransaction** (Fixed Expenses)
    *   `id`: PK
    *   `name`, `amount`, `dayOfMonth`
    *   `paymentMethod`: Enum
    *   `card_id`: FK to Card
    *   `category_id`: FK to Category

*   **Category**
    *   `id`: PK
    *   `name`: Category name

## 4. Implemented Features
*   **Budget Management:** CRUD for monthly budgets by category.
*   **Transaction Tracking:** Record actual and planned expenses/income.
*   **Asset Management:** Track Cash, Stocks, Savings, and Debts.
*   **Recurring Transactions:** Manage fixed monthly costs.
*   **Tax/Card Management:** Basic controller support for tax and cards.

## 5. Remaining Tasks & Roadmap
The following tasks are prioritized to complete the core logic:

### 5.1. Connect Consumption/Budget to Tax Settlement
*   **Goal:** Automate the "Year-End Tax Settlement (연말정산)" simulation using actual transaction data.
*   **Current State:** Likely requires manual input or disconnected logic.
*   **Action:**
    *   Aggregate `Transaction` records by `year` and `PaymentMethod` (Credit Card, Debit Card, Cash Receipt).
    *   Feed this aggregated data into the Tax Calculation Service to automatically estimate deductions.

### 5.2. Link Cash Flow to Assets
*   **Goal:** Synchronize `Transaction` entries with `Asset` balances.
*   **Current State:** Transactions are recorded, but `Asset.balance` (e.g., Bank Account, Cash Wallet) might not update automatically.
*   **Action:**
    *   Implement an event listener or service logic: When a `Transaction` is created/confirmed, update the corresponding `Asset` (e.g., subtract amount from "Bank Account A" if paid by Check Card linked to it).
    *   Handle "Transfer" category to move funds between Assets without affecting Net Worth (optional but recommended).

