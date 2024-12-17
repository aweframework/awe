-- ------------------------------------------------------
--  UPDATE LANGUAGE IN OPE
-- ------------------------------------------------------
ALTER TABLE ope MODIFY l1_lan CHAR(5) NULL;
ALTER TABLE HISope MODIFY l1_lan CHAR(5) NULL;

UPDATE ope SET l1_lan = 'es-ES' WHERE l1_lan = 'ESP';
UPDATE ope SET l1_lan = 'en-GB' WHERE l1_lan = 'ENG';
UPDATE ope SET l1_lan = 'fr-FR' WHERE l1_lan = 'FRA';