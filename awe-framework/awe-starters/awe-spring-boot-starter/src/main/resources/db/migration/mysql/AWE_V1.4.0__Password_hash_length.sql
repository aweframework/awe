-- ------------------------------------------------------
--  Password hashes (#778)
--  Widen the password hash column of the users and their history: BCrypt hashes with their {bcrypt} prefix take
--  68 characters and the legacy RIPEMD-160 hashes (40 characters) keep validating.
--  The script can be applied again: a column is only changed while it is char or shorter than 255 characters, so a
--  column that is already wider is never shrunk.
-- ------------------------------------------------------
SET @alter_ope = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE ope MODIFY l1_pas varchar(255) NULL', 'DO 0')
                  FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND LOWER(table_name) = 'ope' AND LOWER(column_name) = 'l1_pas'
                    AND (data_type = 'char' OR character_maximum_length < 255));
PREPARE alter_ope FROM @alter_ope;
EXECUTE alter_ope;
DEALLOCATE PREPARE alter_ope;

SET @alter_hisope = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE HISope MODIFY l1_pas varchar(255) NULL', 'DO 0')
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisope' AND LOWER(column_name) = 'l1_pas'
                       AND (data_type = 'char' OR character_maximum_length < 255));
PREPARE alter_hisope FROM @alter_hisope;
EXECUTE alter_hisope;
DEALLOCATE PREPARE alter_hisope;
