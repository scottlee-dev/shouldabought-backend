# ShouldaBought API

![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3%2B-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-blue.svg)

A RESTful backend service designed for financial simulation, portfolio management, and tracking "What-If" investment scenarios.

Built with Java 21, Spring Boot, and PostgreSQL, this system maintains an immutable transaction ledger to log deposits, cash balances, and stock buy/sell operations with high precision.

---

## Getting Started: Local Setup

You can clone and run this application locally on your machine in under 3 minutes.

### Prerequisites
* **JDK 21+**
* **PostgreSQL**
* **Git** & **Maven**

### 1. Database Configuration
Open your PostgreSQL terminal (or pgAdmin/DBeaver) and create an empty database named `shouldabought_db`:

```sql
CREATE DATABASE shouldabought_db;
```

### 2. Configure Application Properties

Configure your `application-local.properties` or `application.properties` with your database credentials:

```properties
spring.application.name=backend

spring.datasource.url=jdbc:postgresql://localhost:5432/shouldabought_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### 3. Clone, Build, and Run

Open your terminal and execute the following commands:

```bash
# Clone the repository
git clone [https://github.com/scottlee-dev/shouldabought-backend.git](https://github.com/scottlee-dev/shouldabought-backend.git)

cd shouldabought-backend

# Build and start the Spring Boot server
./mvnw clean spring-boot:run
```

Once the server starts, the API will be accessible at:

* **Base URL:** `http://localhost:8080/api/accounts`

---

## Core Domain & Business Logic

The system is architected around financial accuracy and ledger integrity.

1. **Account Classification (`REALITY`, `SCENARIO`):**
   Supports primary trading accounts (`REALITY`) as well as hypothetical branch accounts (`SCENARIO`) for simulating historical investment decisions.

2. **Atomic Ledger Logging:**
   Account creation automatically generates an initial `DEPOSIT` transaction record. Account balances and transaction logs are updated atomically within transaction boundaries.

3. **Stock Purchasing & Balance Validation:**
   When executing stock purchases, the backend validates available cash before updating the portfolio. If funds are sufficient, cash is deducted, and a detailed `BUY` transaction record (ticker, quantity, execution price, total amount) is persisted.

4. **Monetary & Fractional Precision:**
   All cash balances and stock calculations utilize `BigDecimal` with explicit scale definitions (`scale = 4` for total amounts, `scale = 6` for quantities and stock prices) to prevent floating-point rounding errors.

---

## Engineering Highlights & Architectural Decisions

### 1. Atomic Transaction Boundaries (`@Transactional`)
* Encapsulates account updates and transaction logging within single service methods.
* **Reason:** Ensures database integrity where either both the account balance and transaction record are successfully persisted, or the entire operation is rolled back.

### 2. Constructor Dependency Injection
* Replaces field injection (`@Autowired`) with constructor-based dependency injection.
* **Reason:** Guarantees immutability and simplifies testability without hidden framework dependencies.

### 3. DTO & Java Record Encapsulation
* Uses Java Records (`TransactionBuyRequest`, `TransactionResponse`) for API input/output mapping.
* **Reason:** Prevents direct exposure of internal JPA entities through API endpoints and enforces immutability across controller layers.

---

## Relational Database Schema

| Table | Description | Key Columns |
| --- | --- | --- |
| **`accounts`** | Stores account metadata and real-time available cash balance. | `id`, `name`, `cash`, `type` |
| **`transactions`** | An immutable transaction ledger recording all financial events. | `id`, `account_id`, `type`, `amount`, `symbol`, `quantity`, `price`, `created_at` |

---

## Author

**Scott Lee**

* Software Engineer | Systems & Backend Architecture
* Connect with me on [LinkedIn](https://www.linkedin.com/in/scott-lee-dev/) | [GitHub](https://github.com/scottlee-dev)