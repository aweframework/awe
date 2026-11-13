-- ------------------------------------------------------
--  DDL for Table AweSavSch
--  Saved searches of the advanced block of a window (#790): own searches of a user (Ope set) or searches published
--  for a profile (Pro set, the profile acronym); exactly one of both is set
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
--  Columns:
--    IdeSavSch  Table identifier
--    Ope        User id (own search)
--    Pro        Profile acronym (search published for the profile)
--    Scr        Screen id
--    WinId      Window id
--    Nam        Search name
--    Val        Values (JSON with the selected values of each search criterion)
-- ------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE AweSavSch (IdeSavSch number(10) CONSTRAINT pk_AweSavSch PRIMARY KEY NOT NULL, Ope varchar2(20) NULL, Pro varchar2(3) NULL, Scr varchar2(100) NOT NULL, WinId varchar2(100) NOT NULL, Nam varchar2(100) NOT NULL, Val varchar2(4000) NOT NULL, CONSTRAINT uk_AweSavSch UNIQUE (Ope, Pro, Scr, WinId, Nam), CONSTRAINT ck_AweSavSch_Own CHECK ((Ope IS NULL AND Pro IS NOT NULL AND Pro <> '''') OR (Ope IS NOT NULL AND Ope <> '''' AND Pro IS NULL)))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

--  Sequence of the table identifier
INSERT INTO AweKey (KeyNam, KeyVal)
SELECT 'SavSchKey', 0
FROM DUAL
WHERE NOT EXISTS(SELECT * FROM AweKey WHERE KeyNam = 'SavSchKey');
