--------------------------------------------------------
--  UPDATE l1_nom IN OPE
--------------------------------------------------------
DROP INDEX IF EXISTS opeI1 on ope
ALTER TABLE ope ALTER COLUMN l1_nom CHAR(100) NULL;   --- Change l1_nom
ALTER TABLE HISope ALTER COLUMN l1_nom CHAR(100) NULL;
CREATE UNIQUE INDEX opeI1 ON ope (l1_nom);