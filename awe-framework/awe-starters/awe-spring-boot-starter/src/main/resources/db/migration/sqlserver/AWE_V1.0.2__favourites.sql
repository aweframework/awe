-- ------------------------------------------------------
--  DDL for Table AweAppPar
--  Application parameters table: Allows to configure specific parameters in the application
-- ------------------------------------------------------
IF NOT EXISTS(SELECT *
              FROM sys.tables
              WHERE name = 'AweUsrFav'
                AND type = 'U')
CREATE TABLE AweUsrFav (
    IdeFav int NOT NULL PRIMARY KEY,      --  Table identifier
    Ope varchar(20) NOT NULL,             --  User id
    Opt varchar(100) NOT NULL,            --   Option name
    Ord int DEFAULT 0 NOT NULL            --   Option position
    );