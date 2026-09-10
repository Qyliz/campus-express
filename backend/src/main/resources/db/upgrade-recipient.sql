-- 可重复执行：为已有订单表补充收件联系方式。新库执行时不修改结构。
SET @recipient_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'express_order' AND column_name = 'delivery_email') = 0,
  'ALTER TABLE express_order ADD COLUMN delivery_email VARCHAR(255) DEFAULT NULL AFTER delivery_phone', 'SELECT 1');
PREPARE recipient_stmt FROM @recipient_ddl;
EXECUTE recipient_stmt;
DEALLOCATE PREPARE recipient_stmt;
SET @recipient_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'express_order' AND column_name = 'delivery_phone' AND is_nullable = 'NO') > 0,
  'ALTER TABLE express_order MODIFY COLUMN delivery_phone VARCHAR(20) DEFAULT NULL', 'SELECT 1');
PREPARE recipient_stmt FROM @recipient_ddl;
EXECUTE recipient_stmt;
DEALLOCATE PREPARE recipient_stmt;
SET @recipient_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'express_order' AND index_name = 'idx_order_delivery_phone') = 0,
  'CREATE INDEX idx_order_delivery_phone ON express_order (delivery_phone, create_time)', 'SELECT 1');
PREPARE recipient_stmt FROM @recipient_ddl;
EXECUTE recipient_stmt;
DEALLOCATE PREPARE recipient_stmt;
SET @recipient_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'express_order' AND index_name = 'idx_order_delivery_email') = 0,
  'CREATE INDEX idx_order_delivery_email ON express_order (delivery_email, create_time)', 'SELECT 1');
PREPARE recipient_stmt FROM @recipient_ddl;
EXECUTE recipient_stmt;
DEALLOCATE PREPARE recipient_stmt;
