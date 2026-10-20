-- ------------------------------------------------------
--  Password hashes (#778)
--  Widen the password hash column of the users and their history: BCrypt hashes with their {bcrypt} prefix take
--  68 characters and the legacy RIPEMD-160 hashes (40 characters) keep validating.
--  The script can be applied again: a column is only changed while it is CHAR or shorter than 255 characters, so a
--  column that is already wider is never shrunk.
-- ------------------------------------------------------
BEGIN
  FOR col IN (SELECT table_name
              FROM user_tab_columns
              WHERE table_name IN ('OPE', 'HISOPE') AND column_name = 'L1_PAS'
                AND (data_type = 'CHAR' OR char_length < 255)) LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE ' || col.table_name || ' MODIFY (l1_pas varchar2(255))';
  END LOOP;
END;
/
