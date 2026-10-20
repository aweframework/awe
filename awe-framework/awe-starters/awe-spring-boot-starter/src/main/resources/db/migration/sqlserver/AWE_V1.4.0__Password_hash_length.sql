-- ------------------------------------------------------
--  Password hashes (#778)
--  Widen the password hash column of the users and their history: BCrypt hashes with their {bcrypt} prefix take
--  68 characters and the legacy RIPEMD-160 hashes (40 characters) keep validating.
--  The script can be applied again: a column is only changed while it is char or shorter than 255 characters, so a
--  column that is already wider is never shrunk.
-- ------------------------------------------------------
IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'ope' AND COLUMN_NAME = 'l1_pas'
            AND (DATA_TYPE = 'char' OR (CHARACTER_MAXIMUM_LENGTH < 255 AND CHARACTER_MAXIMUM_LENGTH <> -1)))
ALTER TABLE ope ALTER COLUMN l1_pas varchar(255) NULL;

IF EXISTS(SELECT *
          FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_NAME = 'HISope' AND COLUMN_NAME = 'l1_pas'
            AND (DATA_TYPE = 'char' OR (CHARACTER_MAXIMUM_LENGTH < 255 AND CHARACTER_MAXIMUM_LENGTH <> -1)))
ALTER TABLE HISope ALTER COLUMN l1_pas varchar(255) NULL;
