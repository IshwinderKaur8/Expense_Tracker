# Expense Tracker: Project Plan

## 1. Problem Statement
Budget-conscious students, professionals and households struggle to build consistent personal-finance habits because spending is scattered and invisible. This app gives them one place to record expenses, see where money goes through charts, and receive automated monthly reports against a budget.

Differentiators: visual analytics, automated monthly reporting, flexible category management.

## 2. Scope

### MVP (Release 1)
| ID | Story | Notes |
|----|-------|-------|
| US-1 | Add / edit / delete expenses | CRUD |
| US-2 | Custom categories | Category entity, per user |
| US-3 | Monthly summary report: total, per-category breakdown, vs budget | Needs Budget entity |
| US-4 | Chart of monthly expenses | Chart.js on top of report API |

### Later (ordered by dependency)
| ID | Story | Depends on |
|----|-------|-----------|
| US-7 | Overspending warning (yellow/red) | Budget (from US-3) |
| US-6 | Export CSV / PDF | Report data |
| US-8 | Auto-categorise by keyword | Category |
| US-9 | Recurring subscriptions list | Recurring entity |
| US-5 + US-10 | Split expenses (single and bulk, even/custom) | Expense, Participants |
| US-12 | Reminder notifications | Scheduler |
| US-11 | Keyboard shortcuts | Frontend |

## 3. Non-Functional Requirements
- Users: single user in MVP, multi-user later (schema carries `user_id` from day one).
- Auth: login required (see decision D-3).
- Retention: 12 months, with download/export before purge (Release 3).
- Performance (draft target): monthly report < 1s for 5,000 expenses.
- Money: stored as DECIMAL, handled as BigDecimal. Never float/double.

## 4. Decisions Log
| ID | Decision | Reason |
|----|----------|--------|
| D-1 | Java + Spring Boot, Maven, MySQL 8, Chart.js frontend | Industry standard, matches goals |
| D-2 | Layered, package-by-feature architecture | Add/remove features with minimal ripple |
| D-3 | `user_id` on all data tables from day one; login added right after core CRUD | Cheap now, painful later |
| D-4 | JDK 21 LTS for the project | Spring-supported LTS |

## 5. Open Questions
| Question | Decision | Reason |
|----|----------|--------|
| Currency | Single currency (INR) | Multi-currency needs exchange rates and conversion. That’s a whole feature, so it goes in the “later” list.|
|Income or expenses only? |	Expenses only |	Your stories never mention income, so adding it now would be scope creep.|
| Retention | Rolling 12 months, based on expense_date | Easy to explain and easy to implement as a scheduled job later.|
