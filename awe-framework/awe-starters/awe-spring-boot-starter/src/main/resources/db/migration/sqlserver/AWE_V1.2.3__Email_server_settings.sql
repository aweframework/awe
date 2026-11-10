-- ------------------------------------------------------
--  Email servers (#562, #870): longer server name, user and password, security mode (NONE, STARTTLS, SSL) and the
--  Spring Security OAuth2 client registration used to get the SMTP token. Ath keeps its values (0 none, 1 user
--  and password) and adds 2 (OAuth2). The script can be applied again: it only widens shorter columns and only adds
--  missing ones.
-- ------------------------------------------------------
--  The unique index on SrvNam has to be dropped to widen the column
IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'AweEmlSrv' AND COLUMN_NAME = 'SrvNam' AND CHARACTER_MAXIMUM_LENGTH < 100)
BEGIN
  DROP INDEX IF EXISTS AweEmlSrvI1 ON AweEmlSrv;
  ALTER TABLE AweEmlSrv ALTER COLUMN SrvNam varchar(100) NOT NULL;
END;

IF NOT EXISTS(SELECT *
              FROM sys.indexes
              WHERE name = 'AweEmlSrvI1')
CREATE UNIQUE INDEX AweEmlSrvI1 ON AweEmlSrv (SrvNam);

IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'AweEmlSrv' AND COLUMN_NAME = 'EmlUsr' AND CHARACTER_MAXIMUM_LENGTH < 255 AND CHARACTER_MAXIMUM_LENGTH <> -1)
ALTER TABLE AweEmlSrv ALTER COLUMN EmlUsr varchar(255) NULL;

IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'AweEmlSrv' AND COLUMN_NAME = 'EmlPwd' AND CHARACTER_MAXIMUM_LENGTH < 512 AND CHARACTER_MAXIMUM_LENGTH <> -1)
ALTER TABLE AweEmlSrv ALTER COLUMN EmlPwd varchar(512) NULL;

IF NOT EXISTS(SELECT *
              FROM INFORMATION_SCHEMA.COLUMNS
              WHERE TABLE_NAME = 'AweEmlSrv' AND COLUMN_NAME = 'SecMod')
ALTER TABLE AweEmlSrv ADD SecMod varchar(10) NULL;

IF NOT EXISTS(SELECT *
              FROM INFORMATION_SCHEMA.COLUMNS
              WHERE TABLE_NAME = 'AweEmlSrv' AND COLUMN_NAME = 'OauReg')
ALTER TABLE AweEmlSrv ADD OauReg varchar(100) NULL;

IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'HISAweEmlSrv' AND COLUMN_NAME = 'SrvNam' AND CHARACTER_MAXIMUM_LENGTH < 100 AND CHARACTER_MAXIMUM_LENGTH <> -1)
ALTER TABLE HISAweEmlSrv ALTER COLUMN SrvNam varchar(100) NULL;

IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'HISAweEmlSrv' AND COLUMN_NAME = 'EmlUsr' AND CHARACTER_MAXIMUM_LENGTH < 255 AND CHARACTER_MAXIMUM_LENGTH <> -1)
ALTER TABLE HISAweEmlSrv ALTER COLUMN EmlUsr varchar(255) NULL;

IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'HISAweEmlSrv' AND COLUMN_NAME = 'EmlPwd' AND CHARACTER_MAXIMUM_LENGTH < 512 AND CHARACTER_MAXIMUM_LENGTH <> -1)
ALTER TABLE HISAweEmlSrv ALTER COLUMN EmlPwd varchar(512) NULL;

IF NOT EXISTS(SELECT *
              FROM INFORMATION_SCHEMA.COLUMNS
              WHERE TABLE_NAME = 'HISAweEmlSrv' AND COLUMN_NAME = 'SecMod')
ALTER TABLE HISAweEmlSrv ADD SecMod varchar(10) NULL;

IF NOT EXISTS(SELECT *
              FROM INFORMATION_SCHEMA.COLUMNS
              WHERE TABLE_NAME = 'HISAweEmlSrv' AND COLUMN_NAME = 'OauReg')
ALTER TABLE HISAweEmlSrv ADD OauReg varchar(100) NULL;
