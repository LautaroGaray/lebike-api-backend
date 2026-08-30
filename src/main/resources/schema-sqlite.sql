-- SQLite Schema with Unique Indexes and Other Indexes

-- Create UNIQUE indexes to enforce uniqueness constraints
-- SQLite uses UNIQUE INDEX instead of UNIQUE CONSTRAINT
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE UNIQUE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

CREATE UNIQUE INDEX IF NOT EXISTS idx_permissions_code ON permissions(code);

CREATE UNIQUE INDEX IF NOT EXISTS idx_modules_main_id ON modules(main_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_modules_name ON modules(name);

-- Composite unique indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_role_permissions_unique ON role_permissions(role_id, module_id, permission_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_permissions_unique ON user_permissions(user_id, module_id, permission_id);

-- Create regular indexes for foreign key columns (improve join performance)
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);

CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_module_id ON role_permissions(module_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);

CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_module_id ON user_permissions(module_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_permission_id ON user_permissions(permission_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_warehouses_allowed_unique ON users_warehouses_allowed(user_id, warehouse_id);
CREATE INDEX IF NOT EXISTS idx_users_warehouses_allowed_user_id ON users_warehouses_allowed(user_id);
CREATE INDEX IF NOT EXISTS idx_users_warehouses_allowed_warehouse_id ON users_warehouses_allowed(warehouse_id);

CREATE INDEX IF NOT EXISTS idx_modules_parent_id ON modules(parent_id);

-- Repair indexes
CREATE INDEX IF NOT EXISTS idx_repair_warehouse_id ON repair(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_repair_user_id ON repair(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_repair_articles_unique ON repair_articles(repair_id, article_id);
CREATE INDEX IF NOT EXISTS idx_repair_articles_repair_id ON repair_articles(repair_id);
CREATE INDEX IF NOT EXISTS idx_repair_articles_article_id ON repair_articles(article_id);

-- RepairAudit indexes
CREATE INDEX IF NOT EXISTS idx_repair_audit_repair_id ON repair_audit(repair_id);
CREATE INDEX IF NOT EXISTS idx_repair_audit_user_id ON repair_audit(user_id);

-- Status seed catalog (idempotent)
INSERT INTO status (status, description) VALUES (0, 'deleted')
ON CONFLICT(status) DO UPDATE SET description = excluded.description;
INSERT INTO status (status, description) VALUES (10, 'new')
ON CONFLICT(status) DO UPDATE SET description = excluded.description;
INSERT INTO status (status, description) VALUES (55, 'on preparation')
ON CONFLICT(status) DO UPDATE SET description = excluded.description;
INSERT INTO status (status, description) VALUES (75, 'ready to dispatched')
ON CONFLICT(status) DO UPDATE SET description = excluded.description;
INSERT INTO status (status, description) VALUES (95, 'dispatched')
ON CONFLICT(status) DO UPDATE SET description = excluded.description;
INSERT INTO status (status, description) VALUES (110, 'receipt')
ON CONFLICT(status) DO UPDATE SET description = excluded.description;

-- Document key seed catalog (idempotent)
INSERT INTO "key" (incremental_key, incremental_letter_key, prefix, target_destiny)
VALUES (0, 'AAAA', 'RCP', 'RECEIPT')
ON CONFLICT(target_destiny) DO UPDATE SET
	prefix = excluded.prefix,
	incremental_key = "key".incremental_key,
	incremental_letter_key = "key".incremental_letter_key;
INSERT INTO "key" (incremental_key, incremental_letter_key, prefix, target_destiny)
VALUES (0, 'AAAA', 'ART', 'ARTICLE')
ON CONFLICT(target_destiny) DO UPDATE SET
	prefix = excluded.prefix,
	incremental_key = "key".incremental_key,
	incremental_letter_key = "key".incremental_letter_key;
INSERT INTO "key" (incremental_key, incremental_letter_key, prefix, target_destiny)
VALUES (0, 'AAAA', 'ORD', 'ORDER')
ON CONFLICT(target_destiny) DO UPDATE SET
	prefix = excluded.prefix,
	incremental_key = "key".incremental_key,
	incremental_letter_key = "key".incremental_letter_key;
INSERT INTO "key" (incremental_key, incremental_letter_key, prefix, target_destiny)
VALUES (0, 'AAAA', 'RPR', 'REPAIR')
ON CONFLICT(target_destiny) DO UPDATE SET
	prefix = excluded.prefix,
	incremental_key = "key".incremental_key,
	incremental_letter_key = "key".incremental_letter_key;

-- ReceiptStatusHistory indexes
CREATE INDEX IF NOT EXISTS idx_receipt_status_history_receipt_id ON receipt_status_history(receipt_id);


