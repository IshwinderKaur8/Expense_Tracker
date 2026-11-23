# Expense Tracker: REST API Design (MVP, v1)

Base path: `/api/v1`. All bodies are JSON. Dates are ISO-8601 (`2026-10-31`). Months are `YYYY-MM`.
The user is NEVER sent by the client. It is taken from the logged-in session (fixed user until login is built).

## Conventions
| Topic | Rule |
|-------|------|
| URLs | Plural nouns, no verbs: `/expenses`, not `/getExpenses` |
| Verbs | GET read, POST create, PUT replace/update, DELETE remove |
| Create | `201 Created` + `Location` header + created object |
| Update | `200 OK` + updated object |
| Delete | `204 No Content` |
| Not found | `404` (also when the item belongs to another user) |
| Validation error | `400` with field details |
| Conflict | `409` (e.g. category in use, duplicate name) |
| Lists | Paginated: `?page=0&size=20`, newest first |

## Error shape (same everywhere)
```json
{
  "timestamp": "2026-10-31T10:15:30+05:30",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Request has invalid fields",
  "details": [ { "field": "amount", "message": "must be greater than 0" } ],
  "path": "/api/v1/expenses"
}
```

## Categories (US-2)
| Method | Path | Body | Success | Errors |
|--------|------|------|---------|--------|
| GET | `/categories` | | 200 list | |
| POST | `/categories` | `{name, color?}` | 201 | 400, 409 duplicate name |
| PUT | `/categories/{id}` | `{name, color?}` | 200 | 400, 404, 409 |
| DELETE | `/categories/{id}` | | 204 | 404, 409 category has expenses |

## Expenses (US-1)
| Method | Path | Body / Query | Success | Errors |
|--------|------|--------------|---------|--------|
| POST | `/expenses` | `{amount, categoryId, expenseDate, description?}` | 201 | 400, 404 category |
| GET | `/expenses` | `from?, to?, categoryId?, page?, size?` | 200 page | 400 |
| GET | `/expenses/{id}` | | 200 | 404 |
| PUT | `/expenses/{id}` | same as POST | 200 | 400, 404 |
| DELETE | `/expenses/{id}` | | 204 | 404 |

Expense response:
```json
{ "id": 12, "amount": 249.50, "categoryId": 3, "categoryName": "Food",
  "expenseDate": "2026-10-31", "description": "Lunch", "createdAt": "2026-10-31T10:15:30+05:30" }
```

## Budgets (US-3)
| Method | Path | Body / Query | Success | Errors |
|--------|------|--------------|---------|--------|
| PUT | `/budgets` | `{month, categoryId?, amount}` (create or replace) | 200 | 400, 404 |
| GET | `/budgets` | `month` | 200 list | 400 |
| DELETE | `/budgets/{id}` | | 204 | 404 |

`categoryId` omitted means the overall budget for that month.

## Reports and charts (US-3, US-4)
| Method | Path | Query | Success | Errors |
|--------|------|-------|---------|--------|
| GET | `/reports/monthly` | `month` (required) | 200 | 400 invalid month |

One response feeds both the report page and the charts, so they can never disagree:
```json
{
  "month": "2026-10",
  "totalSpent": 18450.75,
  "overallBudget": 25000.00,
  "remaining": 6549.25,
  "byCategory": [
    { "categoryId": 3, "name": "Food", "color": "#FF8800",
      "spent": 7200.00, "percentOfTotal": 39.02, "budget": 8000.00 }
  ],
  "dailyTotals": [ { "date": "2026-10-01", "total": 450.00 } ]
}
```
An empty month returns zeros and empty lists, never an error.

## Not in v1 (deliberately)
Auth endpoints (`/auth/login`), export, splits, recurring, notifications. Each will be added as new paths. None of the above changes.


## Auth
| POST /auth/register | (201, 400, 409) |
| POST /auth/login | (form-encoded username/password; 200 or 401) |
| POST /auth/logout | (204) |
| GET /auth/me | (200 or 401) | 

- Every /api/** endpoint except register and login returns 401 without a session
- Changing requests need the X-XSRF-TOKEN header