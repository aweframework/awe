-- ------------------------------------------------------
--  Email servers (#562, #870): longer server name, user and password, security mode (NONE, STARTTLS, SSL) and the
--  Spring Security OAuth2 client registration used to get the SMTP token. Ath keeps its values (0 none, 1 user
--  and password) and adds 2 (OAuth2). The script can be applied again: it only widens shorter columns and only adds
--  missing ones.
-- ------------------------------------------------------
DO $$
DECLARE
  col record;
BEGIN
  FOR col IN SELECT table_name, column_name, CASE LOWER(column_name) WHEN 'srvnam' THEN 100 WHEN 'emlusr' THEN 255 ELSE 512 END AS size
             FROM information_schema.columns
             WHERE table_schema = current_schema() AND LOWER(table_name) IN ('aweemlsrv', 'hisaweemlsrv')
               AND ((LOWER(column_name) = 'srvnam' AND character_maximum_length < 100)
                 OR (LOWER(column_name) = 'emlusr' AND character_maximum_length < 255)
                 OR (LOWER(column_name) = 'emlpwd' AND character_maximum_length < 512)) LOOP
    EXECUTE format('ALTER TABLE %I ALTER COLUMN %I TYPE varchar(%s)', col.table_name, col.column_name, col.size);
  END LOOP;
END
$$;
ALTER TABLE AweEmlSrv ADD COLUMN IF NOT EXISTS SecMod varchar(10) NULL;  --  Security mode: NONE, STARTTLS or SSL (NULL is NONE)
ALTER TABLE AweEmlSrv ADD COLUMN IF NOT EXISTS OauReg varchar(100) NULL;  --  OAuth2 client registration id
ALTER TABLE HISAweEmlSrv ADD COLUMN IF NOT EXISTS SecMod varchar(10) NULL;  --  Security mode: NONE, STARTTLS or SSL (NULL is NONE)
ALTER TABLE HISAweEmlSrv ADD COLUMN IF NOT EXISTS OauReg varchar(100) NULL;  --  OAuth2 client registration id
