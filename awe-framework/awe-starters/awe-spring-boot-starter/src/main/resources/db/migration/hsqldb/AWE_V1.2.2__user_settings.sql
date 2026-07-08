-- ------------------------------------------------------
--  DDL for Table AweUserSettings
--  Per-user settings table: Stores the avatar image reference token for each user
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweUserSettings (
    IdeUsrSet int NOT NULL PRIMARY KEY,     --  Table identifier
    Ope varchar(20) NOT NULL,               --  Username
    AvatarImage varchar(4000),              --  Avatar image reference token
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
    );
