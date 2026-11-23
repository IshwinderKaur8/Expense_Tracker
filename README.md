# Expense Tracker

![CI](https://github.com/IshwinderKaur8/Expense_Tracker/actions/workflows/ci.yml/badge.svg)

A personal-finance web app: record expenses by category, set monthly budgets, and see
spending in a monthly report with charts.

**Stack:** Java 21, Spring Boot, MySQL 8, Flyway, Spring Security (session login),
Chart.js, Docker, GitHub Actions.

## Run it (Docker)

```
cp .env.example .env        # then edit the passwords
docker compose up --build
```
Open http://localhost:8080 and create an account.

## Run the tests

Requires Docker (integration tests start a throwaway MySQL with Testcontainers):
```
./mvnw verify
```

## How it is organised

Package by feature, each with controller, service, repository and DTOs:
`category/`, `expense/`, `budget/`, `report/`, `auth/`, plus shared code in `common/`.
Database changes are versioned Flyway migrations in `src/main/resources/db/migration`.

More detail: [docs/API_DESIGN.md](docs/API_DESIGN.md) and [PROJECT_PLAN.md](PROJECT_PLAN.md).