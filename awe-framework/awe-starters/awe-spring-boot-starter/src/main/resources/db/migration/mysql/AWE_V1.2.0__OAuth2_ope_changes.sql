-- ------------------------------------------------------
--  UPDATE l1_nom IN OPE
-- ------------------------------------------------------
ALTER TABLE ope MODIFY l1_nom CHAR(100) NULL;
ALTER TABLE HISope MODIFY l1_nom CHAR(100) NULL;