-- ------------------------------------------------------
--  DDL for Table AweUserSettings
--  Per-user settings table: Stores the avatar image reference token for each user
-- ------------------------------------------------------
IF NOT EXISTS(SELECT *
              FROM sys.tables
              WHERE name = 'AweUserSettings'
                AND type = 'U')
CREATE TABLE AweUserSettings (
    IdeUsrSet int NOT NULL PRIMARY KEY,      --  Table identifier
    Ope varchar(20) NOT NULL,                --  User id
    AvatarImage varchar(4000),               --  Avatar image reference token
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
    );
