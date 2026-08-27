# Bruno request samples (dev/local only)

These files are local testing helpers and are intentionally stored outside `src/main/resources`, so they are not bundled into production artifacts.

## Article endpoints

- `articles/register-article.json` -> `POST /articles/register`
- `articles/edit-article.json` -> `PUT /articles/edit/{articleId}`
- `articles/delete-article.json` -> `DELETE /articles/delete/{articleId}` (path variable helper)

## User warehouse rules

- `users/register-user-with-one-warehouse.json` -> `POST /users/register` (USER with exactly one warehouse)
- `users/owner-restrict-admin-warehouses.json` -> `PUT /users/editWarehouses` (OWNER restricts ADMIN access)
- `users/owner-unrestrict-admin-warehouses.json` -> `PUT /users/editWarehouses` (OWNER restores full ADMIN access)

## Receipt helpers

- `receipts/delete-receipt-path-variable.json` -> `DELETE /receipts/delete/{receiptId}`
- `receipts/owner-history-receipt.json` -> `GET /receipts/history/{receiptId}` (OWNER only)

