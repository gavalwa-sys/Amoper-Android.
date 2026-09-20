-- AMOPER Mobile API: bearer token auth for the Android app.
-- Run once after the base schema / other migrations are applied.

CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    device_label VARCHAR(190),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_used_at DATETIME NULL,
    expires_at DATETIME NULL,
    revoked_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_api_tokens_user (user_id, revoked_at)
) ENGINE=InnoDB;
