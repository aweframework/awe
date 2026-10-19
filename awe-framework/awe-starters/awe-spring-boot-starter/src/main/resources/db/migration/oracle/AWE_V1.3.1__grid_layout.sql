-- ------------------------------------------------------
--  DDL for Table AweUsrGrdCnf
--  Per-user grid layout: column order and hidden columns of a grid in a screen
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
--  Columns:
--    IdeUsrGrdCnf  Table identifier
--    Ope           User id
--    Scr           Screen id
--    GrdId         Grid id
--    Lay           Layout (JSON with column order and hidden columns)
-- ------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE AweUsrGrdCnf (IdeUsrGrdCnf number(10) CONSTRAINT pk_AweUsrGrdCnf PRIMARY KEY NOT NULL, Ope varchar2(20) NOT NULL, Scr varchar2(100) NOT NULL, GrdId varchar2(100) NOT NULL, Lay varchar2(4000) NOT NULL, CONSTRAINT uk_AweUsrGrdCnf UNIQUE (Ope, Scr, GrdId))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

--  Sequence of the table identifier
INSERT INTO AweKey (KeyNam, KeyVal)
SELECT 'GrdCnfKey', 0
FROM DUAL
WHERE NOT EXISTS(SELECT * FROM AweKey WHERE KeyNam = 'GrdCnfKey');
