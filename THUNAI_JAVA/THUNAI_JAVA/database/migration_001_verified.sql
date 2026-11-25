-- Only run this if you already created the tables from the first version of schema.sql
USE thunai_db;
ALTER TABLE users ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN otp_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN otp_expires DATETIME(6);
ALTER TABLE users ADD COLUMN otp_attempts INT NOT NULL DEFAULT 0;
