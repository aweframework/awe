-- ------------------------------------------------------
--  DDL for Table AweUsrWinCnf
--  Per-user window state: whether the advanced block of a window in a screen was left open or closed
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
IF NOT EXISTS(SELECT *
              FROM sys.tables
              WHERE name = 'AweUsrWinCnf'
                AND type = 'U')
CREATE TABLE AweUsrWinCnf (
    IdeUsrWinCnf int NOT NULL PRIMARY KEY,   --  Table identifier
    Ope varchar(20) NOT NULL,                --  User id
    Scr varchar(100) NOT NULL,               --  Screen id
    WinId varchar(100) NOT NULL,             --  Window id
    AdvOpn int NOT NULL,                     --  Advanced block left open (1) or closed (0)
    CONSTRAINT uk_AweUsrWinCnf UNIQUE (Ope, Scr, WinId)
    );

--  Sequence of the table identifier
IF NOT EXISTS(SELECT *
              FROM AweKey
              WHERE KeyNam = 'WinCnfKey')
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('WinCnfKey', 0);
