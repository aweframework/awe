-- ------------------------------------------------------
--  DDL for Table AweUserSettings
--  Per-user settings table: Stores the avatar image reference token for each user
-- ------------------------------------------------------
CREATE TABLE AweUserSettings (
    IdeUsrSet number(5) CONSTRAINT pk_AweUserSettings PRIMARY KEY NOT NULL,     --  Table identifier
    Ope varchar2(20) NOT NULL,                                                  --  User id
    AvatarImage varchar2(4000),                                                 --  Avatar image reference token
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
    );
