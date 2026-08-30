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
- `GET /warehouses/findAll?action=READ&main_id=MOD_WAREHOUSES`

## Article endpoints

- `articles/register-article.json` -> `POST /articles/register?action=WRITE&main_id=MOD_ARTICLES`
- `articles/edit-article.json` -> `PUT /articles/edit/{articleId}?action=WRITE&main_id=MOD_ARTICLES`
- `articles/delete-article.json` -> `DELETE /articles/delete/{articleId}?action=WRITE&main_id=MOD_ARTICLES` (path variable helper)

## User warehouse rules

- `users/register-user-with-one-warehouse.json` -> `POST /users/register?action=WRITE&main_id=MOD_USERS`
- `users/owner-restrict-admin-warehouses.json` -> `PUT /users/editWarehouses?action=WRITE&main_id=MOD_USERS`
- `users/owner-unrestrict-admin-warehouses.json` -> `PUT /users/editWarehouses?action=WRITE&main_id=MOD_USERS`
- `users/assign-warehouses-user-or-admin.json` -> `PUT /users/assignWarehouses?action=WRITE&main_id=MOD_USERS`

## Receipt helpers

- `receipts/delete-receipt-path-variable.json` -> `DELETE /receipts/delete/{receiptId}?action=WRITE&main_id=MOD_RECEIPTS`
- `receipts/owner-history-receipt.json` -> `GET /receipts/history/{receiptId}?action=READ&main_id=MOD_RECEIPTS`

## Warehouse endpoints

- `warehouses/register-warehouse.json` -> `POST /warehouses/register?action=WRITE&main_id=MOD_WAREHOUSES` (OWNER only)
- `warehouses/edit-warehouse-path-variable.json` + `warehouses/edit-warehouse-body.json` -> `PUT /warehouses/edit/{warehouseId}?action=WRITE&main_id=MOD_WAREHOUSES` (OWNER only)
- `warehouses/delete-warehouse-path-variable.json` -> `DELETE /warehouses/delete/{warehouseId}?action=WRITE&main_id=MOD_WAREHOUSES` (OWNER only)
- `warehouses/find-all-warehouses.json` -> `GET /warehouses/findAll?action=READ&main_id=MOD_WAREHOUSES`
- `warehouses/find-warehouse-by-id-path-variable.json` -> `GET /warehouses/find/{warehouseId}?action=READ&main_id=MOD_WAREHOUSES`

## Repair endpoints

- `repairs/register-repair.json` -> `POST /repairs/register?action=WRITE&main_id=MOD_REPAIRS`
- `repairs/edit-repair.json` -> `PUT /repairs/edit/{repairId}?action=WRITE&main_id=MOD_REPAIRS`
- `repairs/delete-repair.json` -> `DELETE /repairs/delete/{repairId}?action=WRITE&main_id=MOD_REPAIRS`
- `repairs/find-all-repairs.json` -> `GET /repairs/findAll?action=READ&main_id=MOD_REPAIRS`
- `repairs/find-by-warehouse-repairs.json` -> `GET /repairs/findByWarehouse?warehouseCode={warehouseCode}&action=READ&main_id=MOD_REPAIRS`
- `repairs/repair-history.json` -> `GET /repairs/history/{repairId}?action=READ&main_id=MOD_REPAIRS` (`USER` no permitido)

