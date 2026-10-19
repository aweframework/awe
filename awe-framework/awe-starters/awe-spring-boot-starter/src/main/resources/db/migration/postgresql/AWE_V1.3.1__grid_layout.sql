-- ------------------------------------------------------
--  DDL for Table AweUsrGrdCnf
--  Per-user grid layout: column order and hidden columns of a grid in a screen
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweUsrGrdCnf (
    IdeUsrGrdCnf int NOT NULL PRIMARY KEY,   --  Table identifier
    Ope varchar(20) NOT NULL,                --  User id
    Scr varchar(100) NOT NULL,               --  Screen id
    GrdId varchar(100) NOT NULL,             --  Grid id
    Lay varchar(4000) NOT NULL,              --  Layout (JSON with column order and hidden columns)
    CONSTRAINT uk_AweUsrGrdCnf UNIQUE (Ope, Scr, GrdId)
    );

--  Sequence of the table identifier
INSERT INTO AweKey (KeyNam, KeyVal)
SELECT 'GrdCnfKey', 0
WHERE NOT EXISTS(SELECT * FROM AweKey WHERE KeyNam = 'GrdCnfKey');
