-- ------------------------------------------------------
--  Screen personalization history: HISdat keeps the time of the change, not only the day
--  (the audit of the rules of a screen shows when each change was made)
-- ------------------------------------------------------
DROP INDEX IF EXISTS HISAweScrCnfI1 ON HISAweScrCnf;
ALTER TABLE HISAweScrCnf ALTER COLUMN HISdat datetime NOT NULL;
CREATE INDEX HISAweScrCnfI1 ON HISAweScrCnf (HISope, HISdat, HISact);
