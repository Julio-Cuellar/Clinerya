CREATE TABLE core.system_configs (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    description TEXT
);

INSERT INTO core.system_configs (key, value, description)
VALUES ('backup_retention_days', '365', 'Días de retención para backups locales')
ON CONFLICT (key) DO NOTHING;
