-- ------------------------------------------------------
--  DDL for Table AweUsrWinCnf
--  Per-user window state: whether the advanced block of a window in a screen was left open or closed
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
--  Columns:
--    IdeUsrWinCnf  Table identifier
--    Ope           User id
--    Scr           Screen id
--    WinId         Window id
--    AdvOpn        Advanced block left open (1) or closed (0)
-- ------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE AweUsrWinCnf (IdeUsrWinCnf number(10) CONSTRAINT pk_AweUsrWinCnf PRIMARY KEY NOT NULL, Ope varchar2(20) NOT NULL, Scr varchar2(100) NOT NULL, WinId varchar2(100) NOT NULL, AdvOpn number(1) NOT NULL, CONSTRAINT uk_AweUsrWinCnf UNIQUE (Ope, Scr, WinId))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

--  Sequence of the table identifier
INSERT INTO AweKey (KeyNam, KeyVal)
SELECT 'WinCnfKey', 0
FROM DUAL
WHERE NOT EXISTS(SELECT * FROM AweKey WHERE KeyNam = 'WinCnfKey');
