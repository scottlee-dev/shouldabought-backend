# ShouldaBought API

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue.svg)

ShouldaBought is a backend investment simulator that allows for virtual stock trading.

The application manages account cash balances, stock purchases and sales, portfolio valuation, realized and unrealized gain/loss, dividend eligibility, dividend payments, and automatic dividend reinvestment.

It is built with Java 21, Spring Boot, PostgreSQL, Alpha Vantage market data, and GitHub Actions.

---

## Features

### Account Management

- Create investment accounts with an initial cash balance
- Automatically record the initial balance as a `DEPOSIT` transaction
- Track available cash independently from stock holdings

### Stock Trading

- Buy stocks by:
   - share quantity
   - dollar amount
- Sell stocks by:
   - share quantity
   - dollar amount
- Supports fractional shares
- Automatically retrieves the latest available stock price from Alpha Vantage before BUY and SELL transactions
- Persists the latest retrieved price in PostgreSQL

### Transaction Ledger

All financial events are stored as immutable transaction records.

Supported transaction types:

- `DEPOSIT`
- `WITHDRAW`
- `BUY`
- `SELL`
- `DIVIDEND`

Each stock transaction records:

- symbol
- quantity
- execution price
- total transaction amount
- transaction timestamp

---

## Portfolio Calculation

The backend calculates:

- current cash balance
- stock market value
- total account value
- current positions
- average cost basis
- unrealized gain/loss
- unrealized gain/loss percentage
- realized gain/loss
- account allocation percentage

FIFO accounting is used when calculating the cost basis of sold shares.

All financial calculations use `BigDecimal` rather than floating-point types.

---

## Dividend Processing

ShouldaBought includes an automated dividend workflow.

### Dividend Synchronization

Dividend information is retrieved from the Alpha Vantage `DIVIDENDS` endpoint for currently held stocks.

Only dividend events that may still affect the simulator are retained.

### Dividend Eligibility

On or after the ex-dividend date, the backend determines the number of qualifying shares owned before the ex-dividend cutoff.

A unique dividend entitlement is created for each qualifying account and dividend event.

The entitlement system also supports catch-up processing so that a temporarily missed scheduled job does not permanently skip an eligible dividend.

### Dividend Payments

When the payment date arrives:

1. Dividend cash is credited to the account.
2. A `DIVIDEND` transaction is recorded.
3. The full dividend amount is automatically reinvested into the same stock.
4. The reinvestment is recorded as a normal `BUY` transaction.
5. The dividend entitlement is marked as paid.

This implements an automatic DRIP-style dividend reinvestment workflow.

Duplicate dividend payments are prevented using both entitlement state and dividend transaction identifiers.

---

## Market Data

Market and dividend data are retrieved using the Alpha Vantage API.

The application currently uses:

- `GLOBAL_QUOTE`
- `DIVIDENDS`
- `MARKET_STATUS`

Stock prices are refreshed:

- when a BUY transaction occurs
- when a SELL transaction occurs
- during scheduled morning market jobs
- during scheduled closing market jobs

### Market Data Limitation

The project uses the Alpha Vantage free tier.

Because guaranteed real-time pricing is not available on the free tier, ShouldaBought uses the **latest available market price** returned by Alpha Vantage.

The application is therefore intended as an investment simulation project rather than a live brokerage execution platform.

---

## Automated Market Jobs

Market maintenance is automated with GitHub Actions.

Two workflows run on weekdays using the `America/New_York` market timezone.

### Morning Job

Scheduled around market opening.

The morning job:

1. Checks U.S. equity market status
2. Skips processing when the market is closed
3. Refreshes prices for currently held stocks
4. Synchronizes dividend data
5. Creates missing dividend entitlements
6. Processes eligible dividend payments and DRIP transactions

### Closing Job

Scheduled near market close.

The closing job:

1. Refreshes prices for currently held stocks
2. Synchronizes dividend data

The backend is started temporarily inside the GitHub Actions runner, connects to the PostgreSQL database, executes the required job through a local API endpoint, and then exits.

This allows scheduled processing without requiring an always-on application server.

---

## Architecture

```text
Client
  |
  v
REST Controllers
  |
  v
Service Layer
  |
  +--------------------+
  |                    |
  v                    v
PostgreSQL        Alpha Vantage
                       API

GitHub Actions
      |
      v
Market Job Endpoints
```

The project is organized by domain:

```text
account/
dividend/
market/
transaction/
```

---

## Database Model

| Table | Description |
| --- | --- |
| `accounts` | Account metadata and available cash |
| `transactions` | Immutable financial transaction ledger |
| `stock_prices` | Latest available price for each tracked symbol |
| `dividends` | Dividend market events retrieved from Alpha Vantage |
| `dividend_entitlements` | Account-level dividend eligibility and payment state |

Important database constraints include:

- unique stock symbol in `stock_prices`
- unique dividend external identifier
- unique `(account_id, dividend_id)` entitlement pair

---

## API Endpoints

### Accounts

```http
POST /api/accounts
GET  /api/accounts/{id}
```

### Trading

```http
POST /api/accounts/{id}/buy
POST /api/accounts/{id}/sell
```

### Portfolio

```http
GET /api/accounts/{id}/transactions
GET /api/accounts/{id}/positions
GET /api/accounts/{id}/balance
GET /api/accounts/{id}/portfolio
```

### System

```http
GET /api/health
```

### Automated Market Jobs

```http
POST /api/jobs/market/morning
POST /api/jobs/market/closing
```

---

## Example

Create an account:

```http
POST /api/accounts
Content-Type: application/json

{
  "name": "Test Account",
  "initialCash": 20000
}
```

Buy $1,000 of AAPL:

```http
POST /api/accounts/1/buy
Content-Type: application/json

{
  "symbol": "AAPL",
  "cashAmount": 1000
}
```

Because fractional shares are supported, the backend calculates the number of shares that can be purchased using the latest available market price.

Sell $100 of AAPL:

```http
POST /api/accounts/1/sell
Content-Type: application/json

{
  "symbol": "AAPL",
  "cashAmount": 100
}
```

Retrieve the complete portfolio:

```http
GET /api/accounts/1/portfolio
```

---

## Local Development

### Prerequisites

- JDK 21+
- PostgreSQL
- Git
- Alpha Vantage API key

Maven installation is not required when using the included Maven Wrapper.

### 1. Clone the Repository

```bash
git clone https://github.com/scottlee-dev/shouldabought-backend.git
cd shouldabought-backend
```

### 2. Create the Database

```sql
CREATE DATABASE shouldabought_db;
```

### 3. Configure Local Properties

Create or configure:

```text
src/main/resources/application-local.properties
```

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/shouldabought_db
spring.datasource.username=your_username
spring.datasource.password=your_password

alphavantage.api-key=your_alpha_vantage_api_key
```

Do not commit local credentials or API keys to Git.

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

---

## Technology Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- PostgreSQL
- Alpha Vantage API
- Maven
- GitHub Actions

---

## Engineering Decisions

### BigDecimal for Financial Calculations

`BigDecimal` is used throughout the financial domain to avoid floating-point precision problems.

Internal calculations retain high precision while monetary values returned through portfolio APIs are rounded to appropriate display precision.

### FIFO Cost Basis

Stock sales consume the oldest purchase lots first.

This allows the backend to calculate both remaining cost basis and realized gain/loss after partial sales.

### Transaction-Based Holdings

Stock holdings are derived from BUY and SELL transactions rather than maintained as a separate mutable position table.

This preserves the transaction ledger as the source of truth.

### Market-Time Consistency

Market-related timestamps and scheduled processing use:

```text
America/New_York
```

to remain consistent with U.S. equity market dates.

### External Scheduling

Spring's internal scheduler is intentionally not used.

GitHub Actions triggers the morning and closing jobs instead, allowing the backend to run only when scheduled processing is required.

---

## Current Scope

ShouldaBought is a portfolio project and investment simulator, not a brokerage platform.

Current limitations include:

- market pricing depends on Alpha Vantage free-tier data availability
- prices are not guaranteed to be real-time
- authentication and multi-user authorization are not yet implemented
- API error responses can be further standardized
- additional automated integration tests can be added

---

## Author

**Scott Lee**

Software Engineer | Backend Development

[LinkedIn](https://www.linkedin.com/in/scott-lee-dev/) | [GitHub](https://github.com/scottlee-dev)