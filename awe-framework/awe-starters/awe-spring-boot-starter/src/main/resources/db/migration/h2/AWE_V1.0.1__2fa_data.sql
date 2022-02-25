--------------------------------------------------------
--  ADD enable2fa and secret2fa columns to ope table
--------------------------------------------------------
ALTER TABLE ope ADD COLUMN IF NOT EXISTS enable2fa INT DEFAULT 0 NOT NULL;
ALTER TABLE ope ADD COLUMN IF NOT EXISTS secret2fa VARCHAR(128) NULL;

ALTER TABLE HISope ADD COLUMN IF NOT EXISTS enable2fa INT NULL;
ALTER TABLE HISope ADD COLUMN IF NOT EXISTS secret2fa VARCHAR(128) NULL;