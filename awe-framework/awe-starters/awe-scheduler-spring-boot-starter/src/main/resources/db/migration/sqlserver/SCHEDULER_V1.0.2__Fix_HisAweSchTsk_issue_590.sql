-- ------------------------------------------------------
--  Set db and site in HISAweSchTsk as nullables
-- ------------------------------------------------------
ALTER TABLE HISAweSchTsk ALTER COLUMN db VARCHAR(200) NULL;
ALTER TABLE HISAweSchTsk ALTER COLUMN site VARCHAR(200) NULL;