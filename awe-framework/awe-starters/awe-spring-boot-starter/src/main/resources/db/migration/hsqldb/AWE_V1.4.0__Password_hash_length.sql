-- ------------------------------------------------------
--  Password hashes (#778)
--  Widen the password hash column of the users and their history: BCrypt hashes with their {bcrypt} prefix take
--  68 characters and the legacy RIPEMD-160 hashes (40 characters) keep validating.
--  The script can be applied again: setting the same type twice changes nothing.
-- ------------------------------------------------------
ALTER TABLE ope ALTER COLUMN l1_pas SET DATA TYPE varchar(255);
ALTER TABLE HISope ALTER COLUMN l1_pas SET DATA TYPE varchar(255);
