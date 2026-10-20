-- ------------------------------------------------------
--  Password hashes (#778)
--  Widen the password hash column of the users and their history: BCrypt hashes with their {bcrypt} prefix take
--  68 characters and the legacy RIPEMD-160 hashes (40 characters) keep validating.
--  The script can be applied again: a column is only changed while it is char or shorter than 255 characters, so a
--  column that is already wider is never shrunk.
-- ------------------------------------------------------
DO $$
DECLARE
  col record;
BEGIN
  FOR col IN SELECT table_name
             FROM information_schema.columns
             WHERE table_schema = current_schema() AND LOWER(table_name) IN ('ope', 'hisope') AND column_name = 'l1_pas'
               AND (data_type = 'character' OR character_maximum_length < 255) LOOP
    EXECUTE format('ALTER TABLE %I ALTER COLUMN l1_pas TYPE varchar(255)', col.table_name);
  END LOOP;
END
$$;
