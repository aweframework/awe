-- ------------------------------------------------------
--  DDL for Table AweSavSch
--  Saved searches of the advanced block of a window (#790): own searches of a user (Ope set) or searches published
--  for a profile (Pro set, the profile acronym); exactly one of both is set
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSavSch (
    IdeSavSch int NOT NULL PRIMARY KEY,      --  Table identifier
    Ope varchar(20) NULL,                    --  User id (own search)
    Pro varchar(3) NULL,                     --  Profile acronym (search published for the profile)
    Scr varchar(100) NOT NULL,               --  Screen id
    WinId varchar(100) NOT NULL,             --  Window id
    Nam varchar(100) NOT NULL,               --  Search name
    Val varchar(4000) NOT NULL,              --  Values (JSON with the selected values of each search criterion)
    CONSTRAINT uk_AweSavSch UNIQUE (Ope, Pro, Scr, WinId, Nam),
    CONSTRAINT ck_AweSavSch_Own CHECK ((Ope IS NULL AND Pro IS NOT NULL AND Pro <> '') OR (Ope IS NOT NULL AND Ope <> '' AND Pro IS NULL))
    );

--  Sequence of the table identifier
INSERT INTO AweKey (KeyNam, KeyVal)
SELECT 'SavSchKey', 0
FROM DUAL
WHERE NOT EXISTS(SELECT * FROM AweKey WHERE KeyNam = 'SavSchKey');
