-- ------------------------------------------------------
--  Default saved search of a window (#915)
--  AweUsrWinCnf gains the default saved search of the user for the window (DefSavSch, an AweSavSch id); its AdvOpn
--  becomes nullable, so a row stored only for the default search leaves the state of the advanced block unknown.
--  AweSavSch gains Def: 1 marks a search published for a profile as the default search of that profile (only a
--  published search can be one).
--  The script can be applied again: a column or a constraint is only added when it is missing, and making AdvOpn
--  nullable twice changes nothing.
-- ------------------------------------------------------
--  Columns:
--    AweUsrWinCnf.DefSavSch  Default saved search of the user
--    AweUsrWinCnf.AdvOpn     Advanced block left open (1), closed (0) or unknown (null)
--    AweSavSch.Def           Default search of its profile (1) or not (0)
-- ------------------------------------------------------
DECLARE
  PROCEDURE run(ddl VARCHAR2, ignored_code NUMBER) IS
  BEGIN
    EXECUTE IMMEDIATE ddl;
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE != ignored_code THEN
        RAISE;
      END IF;
  END;
BEGIN
  --  ORA-01430: the column already exists
  run('ALTER TABLE AweUsrWinCnf ADD (DefSavSch number(10) NULL)', -1430);
  --  ORA-01451: the column is already nullable
  run('ALTER TABLE AweUsrWinCnf MODIFY (AdvOpn NULL)', -1451);
  run('ALTER TABLE AweSavSch ADD (Def number(1) DEFAULT 0 NOT NULL)', -1430);
  --  ORA-02264: the constraint already exists
  run('ALTER TABLE AweSavSch ADD CONSTRAINT ck_AweSavSch_Def CHECK (Def = 0 OR (Def = 1 AND Pro IS NOT NULL))', -2264);
END;
/
