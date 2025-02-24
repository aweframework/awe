-- ------------------------------------------------------
--  UPDATE l1_nom IN OPE
-- ------------------------------------------------------
ALTER TABLE ope ALTER COLUMN l1_nom SET DATA TYPE CHAR(100);   --- Change l1_nom
ALTER TABLE HISope ALTER COLUMN l1_nom SET DATA TYPE CHAR(100);