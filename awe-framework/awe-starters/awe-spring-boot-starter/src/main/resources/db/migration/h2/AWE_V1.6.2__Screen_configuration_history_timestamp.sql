-- ------------------------------------------------------
--  Screen personalization history: HISdat keeps the time of the change, not only the day
--  (the audit of the rules of a screen shows when each change was made)
-- ------------------------------------------------------
ALTER TABLE HISAweScrCnf ALTER COLUMN HISdat SET DATA TYPE TIMESTAMP;
