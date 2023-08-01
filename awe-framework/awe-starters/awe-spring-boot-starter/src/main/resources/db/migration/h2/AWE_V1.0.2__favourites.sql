-- ------------------------------------------------------
--  DDL for Table AweAppPar
--  Application parameters table: Allows to configure specific parameters in the application
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweUsrFav (
    IdeFav int NOT NULL PRIMARY KEY,    --  Table identifier
    Ope varchar(20) NOT NULL,           --  Username
    Opt varchar(100) NOT NULL,          --  Option name
    Ord int DEFAULT 0 NOT NULL          --  Option position
    );