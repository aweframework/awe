-- ------------------------------------------------------
--  UPDATE LANGUAGE IN OPE (PostgreSQL)
-- ------------------------------------------------------
ALTER TABLE ope ALTER COLUMN l1_lan TYPE CHAR(5);
ALTER TABLE HISope ALTER COLUMN l1_lan TYPE CHAR(5);

UPDATE ope SET l1_lan = 'es-ES' WHERE l1_lan = 'ESP';
UPDATE ope SET l1_lan = 'en-GB' WHERE l1_lan = 'ENG';
UPDATE ope SET l1_lan = 'fr-FR' WHERE l1_lan = 'FRA';
