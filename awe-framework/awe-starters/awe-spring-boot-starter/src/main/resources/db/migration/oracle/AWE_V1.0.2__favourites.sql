-- ------------------------------------------------------
--  DDL for Table AweAppPar
--  Application parameters table: Allows to configure specific parameters in the application
-- ------------------------------------------------------
CREATE TABLE AweUsrFav (
    IdeFav number(5) CONSTRAINT pk_AweUsrFav PRIMARY KEY NOT NULL,     --  Table identifier
    Ope varchar2(20) NOT NULL,                                         --  User id
    Opt varchar2(100) NOT NULL,                                        --   Option name
    Ord number(5) DEFAULT 0 NULL                                       --   Option position
    );