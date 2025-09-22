-- ------------------------------------------------------
--  DDL for Table AweUsrFav (PostgreSQL)
--  Stores user favourites
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweUsrFav (
    IdeFav int NOT NULL PRIMARY KEY,    --  Table identifier
    Ope varchar(20) NOT NULL,           --  User id
    Opt varchar(100) NOT NULL,          --  Option name
    Ord int DEFAULT 0 NOT NULL          --  Option position
);
