# Bruno request samples (dev/local only)

These files are local testing helpers and are intentionally stored outside `src/main/resources`, so they are not bundled into production artifacts.

## Required query params for protected endpoints

- Except `POST /auth/login`, every request must send:
  - `action=READ|WRITE|NONE`
  - `main_id=<MODULE_MAIN_ID>` for `READ` and `WRITE`
- Or headers (same behavior):
  - `X-Action: READ|WRITE|NONE`
  - `X-Module-Main-Id: <MODULE_MAIN_ID>`

Examples:

- `POST /articles/register?action=WRITE&main_id=MOD_ARTICLES`
- `GET /repairs/findAll?action=READ&main_id=MOD_REPAIRS`
- `PUT /users/edit?action=WRITE&main_id=MOD_USERS`

## Article endpoints

- `articles/register-article.json` -> `POST /articles/register?action=WRITE&main_id=MOD_ARTICLES`
- `articles/edit-article.json` -> `PUT /articles/edit/{articleId}?action=WRITE&main_id=MOD_ARTICLES`
- `articles/delete-article.json` -> `DELETE /articles/delete/{articleId}?action=WRITE&main_id=MOD_ARTICLES` (path variable helper)

## User warehouse rules

- `users/register-user-with-one-warehouse.json` -> `POST /users/register?action=WRITE&main_id=MOD_USERS`
- `users/owner-restrict-admin-warehouses.json` -> `PUT /users/editWarehouses?action=WRITE&main_id=MOD_USERS`
- `users/owner-unrestrict-admin-warehouses.json` -> `PUT /users/editWarehouses?action=WRITE&main_id=MOD_USERS`

## Receipt helpers

- `receipts/delete-receipt-path-variable.json` -> `DELETE /receipts/delete/{receiptId}?action=WRITE&main_id=MOD_RECEIPTS`
- `receipts/owner-history-receipt.json` -> `GET /receipts/history/{receiptId}?action=READ&main_id=MOD_RECEIPTS`

