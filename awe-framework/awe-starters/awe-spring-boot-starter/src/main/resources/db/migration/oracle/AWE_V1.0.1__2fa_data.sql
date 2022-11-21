--------------------------------------------------------
--  ADD enable2fa and secret2fa columns to ope table
--------------------------------------------------------
ALTER TABLE ope ADD enable2fa INT DEFAULT 0 NOT NULL;
ALTER TABLE ope ADD secret2fa VARCHAR2(128) NULL;

ALTER TABLE HISope ADD enable2fa INT NULL;
ALTER TABLE HISope ADD secret2fa VARCHAR2(128) NULL;