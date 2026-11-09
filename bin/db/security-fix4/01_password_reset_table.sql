-- Apply once to the existing database. No member, company, post or request data is deleted.
USE `seinfomationa_secure`;
CREATE TABLE IF NOT EXISTS `seinfo_password_reset` (
  `reset_key` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `user_uid` INT NOT NULL,
  `password_version` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `created_at` DATETIME NOT NULL,
  `expires_at` DATETIME NOT NULL,
  PRIMARY KEY (`reset_key`),
  UNIQUE KEY `uq_password_reset_user` (`user_uid`),
  KEY `idx_password_reset_expiry` (`expires_at`),
  CONSTRAINT `chk_password_reset_key` CHECK (`reset_key` REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB;
