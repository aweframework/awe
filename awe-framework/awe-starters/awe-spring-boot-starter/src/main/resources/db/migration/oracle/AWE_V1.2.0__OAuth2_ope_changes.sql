--------------------------------------------------------
--  UPDATE l1_nom IN OPE
--------------------------------------------------------
ALTER TABLE ope MODIFY l1_nom CHAR(100);   --- Change l1_nom
ALTER TABLE HISope MODIFY l1_nom CHAR(100);