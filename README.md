# Personal Finance Dashboard (KG)

A comprehensive web-based **Personal Finance & Tax Assistant** optimized for PC environments. This application helps users manage budgets, track expenses and income, monitor assets, and perform tax calculations and year-end settlement simulations based on **Korean Tax Laws**.

## 🚀 Key Features

### 1. Budget & Ledger (가계부)
- **Budgeting:** Set monthly budgets by category.
- **Transaction Tracking:** Record actual spending and income.
- **Planning:** Schedule future expenses and "confirm" them when paid.
- **Visualization:** Calendar or grid views for easy management.

### 2. Asset Management (자산 관리)
- **Dashboard:** View net worth (Assets - Debts).
- **Asset Types:** Manage Cash, Savings, Stocks, and Debts.
- **Investment Tracking:** Input purchase price vs. current value to track stock returns.

### 3. Tax & Simulation (세무 보조)
- **Korean Tax Law Compliance:** Logic tailored to local regulations.
- **Year-End Settlement (연말정산):** Simulate deductions based on salary, credit/debit card usage, and cash receipts.
- **Strategic Advice:** Receive recommendations (e.g., "Use a debit card for the remaining year to maximize deductions").
- **Capital Gains:** Calculate estimated taxes on stock profits.

## 🛠 Tech Stack

### Backend
- **Language:** Java 17+
- **Framework:** Spring Boot 3.x
- **Database:** MySQL 8.0 (running via Docker)
- **ORM:** Spring Data JPA (Hibernate)
- **Build Tool:** Gradle

### Frontend
- **Framework:** React (Vite)
- **Language:** TypeScript
- **Styling:** Tailwind CSS (with Shadcn/UI or MUI)
- **HTTP Client:** Axios

## 📂 Project Structure

```
account/
├── backend/            # Spring Boot Application
│   ├── src/main/java   # Controller, Service, Domain (Entity), Repository
│   └── ...
├── frontend/           # React Application
│   ├── src/pages       # Dashboard, Budget, Assets, Tax
│   └── ...
├── docker-compose.yml  # Database configuration
└── ...
```

## ⚡ Getting Started

### Prerequisites
- Java 17 or higher
- Node.js (v18+ recommended)
- Docker & Docker Compose

### 1. Database Setup
Start the MySQL container:
```bash
docker-compose up -d
```

### 2. Backend Setup
Navigate to the backend directory and run the application:
```bash
cd backend
./gradlew bootRun
```
* The server will start on `http://localhost:8080`.

### 3. Frontend Setup
Navigate to the frontend directory, install dependencies, and start the dev server:
```bash
cd frontend
npm install
npm run dev
```
* The UI will be available at `http://localhost:5173` (default Vite port).

## 📅 Roadmap & Status

Current focus is on integrating core logic components:
- [ ] **Automated Tax Simulation:** Linking actual transaction data (Card/Cash) directly to the year-end tax settlement calculator.
- [ ] **Asset Synchronization:** Automatically updating asset balances (e.g., Bank Account) when transactions occur.

---
*Created for personal use to optimize financial planning and tax efficiency.*
