-- ------------------------------------------------------
--  Default saved search of a window (#915)
--  AweUsrWinCnf gains the default saved search of the user for the window (DefSavSch, an AweSavSch id); its AdvOpn
--  becomes nullable, so a row stored only for the default search leaves the state of the advanced block unknown.
--  AweSavSch gains Def: 1 marks a search published for a profile as the default search of that profile (only a
--  published search can be one).
--  The script can be applied again: a column or a constraint is only added when it is missing, and making AdvOpn
--  nullable twice changes nothing.
-- ------------------------------------------------------
--  Default saved search of the user
IF NOT EXISTS(SELECT *
              FROM INFORMATION_SCHEMA.COLUMNS
              WHERE TABLE_NAME = 'AweUsrWinCnf' AND COLUMN_NAME = 'DefSavSch')
ALTER TABLE AweUsrWinCnf ADD DefSavSch int NULL;

--  Advanced block left open (1), closed (0) or unknown (null)
ALTER TABLE AweUsrWinCnf ALTER COLUMN AdvOpn int NULL;

--  Default search of its profile (1) or not (0)
IF NOT EXISTS(SELECT *
              FROM INFORMATION_SCHEMA.COLUMNS
              WHERE TABLE_NAME = 'AweSavSch' AND COLUMN_NAME = 'Def')
ALTER TABLE AweSavSch ADD Def int NOT NULL CONSTRAINT df_AweSavSch_Def DEFAULT 0;

--  SQL Server compiles a batch as a whole: the constraint on the new column goes in its own batch
GO

--  Only a search published for a profile is the default search of the profile
IF NOT EXISTS(SELECT *
              FROM sys.check_constraints
              WHERE name = 'ck_AweSavSch_Def')
ALTER TABLE AweSavSch ADD CONSTRAINT ck_AweSavSch_Def CHECK (Def = 0 OR (Def = 1 AND Pro IS NOT NULL));
