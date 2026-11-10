-- Apply only to the account database. This migration never modifies game accounts.
CREATE TABLE IF NOT EXISTS web_admin_schema (
    id TINYINT NOT NULL PRIMARY KEY,
    version INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO web_admin_schema (id, version) VALUES (1, 1);

CREATE TABLE IF NOT EXISTS web_admin (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40) COLLATE utf8mb4_unicode_ci NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    security_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_web_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS web_admin_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    actor_name VARCHAR(40) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target VARCHAR(160) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY ix_web_admin_audit_time (created_at, id),
    KEY ix_web_admin_audit_actor (actor_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
