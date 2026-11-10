-- ------------------------------------------------------
--  Email servers (#562, #870): longer server name, user and password, security mode (NONE, STARTTLS, SSL) and the
--  Spring Security OAuth2 client registration used to get the SMTP token. Ath keeps its values (0 none, 1 user
--  and password) and adds 2 (OAuth2). The script can be applied again: it only widens shorter columns and only adds
--  missing ones.
-- ------------------------------------------------------
DECLARE
  PROCEDURE widen_column(table_name VARCHAR2, column_name VARCHAR2, column_size NUMBER) IS
    current_size NUMBER;
  BEGIN
    SELECT char_length INTO current_size
    FROM user_tab_columns
    WHERE table_name = UPPER(widen_column.table_name) AND column_name = UPPER(widen_column.column_name);
    IF current_size < column_size THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || table_name || ' MODIFY (' || column_name || ' varchar2(' || column_size || '))';
    END IF;
  END;
  PROCEDURE add_column(table_name VARCHAR2, column_name VARCHAR2, column_type VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ' || table_name || ' ADD (' || column_name || ' ' || column_type || ' NULL)';
  EXCEPTION
    WHEN OTHERS THEN
      --  ORA-01430: the column already exists
      IF SQLCODE != -1430 THEN
        RAISE;
      END IF;
  END;
BEGIN
  widen_column('AweEmlSrv', 'SrvNam', 100);
  widen_column('AweEmlSrv', 'EmlUsr', 255);
  widen_column('AweEmlSrv', 'EmlPwd', 512);
  add_column('AweEmlSrv', 'SecMod', 'varchar2(10)');  --  Security mode: NONE, STARTTLS or SSL (NULL is NONE)
  add_column('AweEmlSrv', 'OauReg', 'varchar2(100)');  --  OAuth2 client registration id
  widen_column('HISAweEmlSrv', 'SrvNam', 100);
  widen_column('HISAweEmlSrv', 'EmlUsr', 255);
  widen_column('HISAweEmlSrv', 'EmlPwd', 512);
  add_column('HISAweEmlSrv', 'SecMod', 'varchar2(10)');
  add_column('HISAweEmlSrv', 'OauReg', 'varchar2(100)');
END;
/
