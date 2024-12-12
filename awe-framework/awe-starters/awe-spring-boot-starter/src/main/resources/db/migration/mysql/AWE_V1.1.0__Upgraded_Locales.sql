--------------------------------------------------------
--  UPDATE LANGUAGE IN OPE
--------------------------------------------------------
ALTER TABLE ope ALTER COLUMN l1_lan SET CHAR(5) NULL;   --- Change l1_lan
ALTER TABLE HISope ALTER COLUMN l1_lan SET CHAR(5) NULL;

UPDATE ope SET l1_lan = 'es-ES' WHERE l1_lan = 'ESP';
UPDATE ope SET l1_lan = 'en-GB' WHERE l1_lan = 'ENG';
UPDATE ope SET l1_lan = 'fr-FR' WHERE l1_lan = 'FRA';