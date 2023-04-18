-- ------------------------------------------------------
--  SCHEDULER DDL
-- ------------------------------------------------------

-- ------------------------------------------------------
--  DDL for Table AweSchCal
--  Calendar list
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchCal
(
    Ide INTEGER not NULL,
    Des VARCHAR(250) not NULL,
    Act INTEGER DEFAULT 1 not NULL,
    Nom VARCHAR(100) not NULL
);

-- ------------------------------------------------------
--  DDL for Table AweSchTskFilMod
--  Calendar dates
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchTskFilMod
(
    IdeTsk INTEGER not NULL,
    FilPth VARCHAR(256) not NULL,
    ModDat DATE
);

-- ------------------------------------------------------
--  DDL for Table AweSchCalDat
--  Task file
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchCalDat
(
    Ide    INTEGER not NULL,
    IdeCal INTEGER not NULL,
    Nom    VARCHAR(40) not NULL,
    Dat    DATE not NULL
);

-- ------------------------------------------------------
--  DDL for Table AweSchExe
--  Task executions
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchExe
(
    IdeTsk INTEGER not NULL,
    GrpTsk VARCHAR(40) not NULL,
    ExeTsk INTEGER not NULL,
    IniDat DATETIME not NULL,
    EndDat DATETIME,
    ExeTim INTEGER,
    Sta    INTEGER not NULL,
    LchBy  VARCHAR(200),
    Des    VARCHAR(2000)
);

-- ------------------------------------------------------
--  DDL for Table AweSchSrv
--  Scheduler servers
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchSrv
(
    Ide INTEGER not NULL,
    Nom VARCHAR(40) not NULL,
    Pro VARCHAR(10) not NULL,
    Hst VARCHAR(40) not NULL,
    Prt VARCHAR(10) not NULL,
    Act INTEGER DEFAULT 1 not NULL
);

-- ------------------------------------------------------
--  DDL for Table AweSchTsk
--  Scheduler tasks
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchTsk
(
    Ide       INTEGER not NULL,
    Nam       VARCHAR(40) not NULL,
    Des       VARCHAR(250),
    NumStoExe INTEGER,
    TimOutExe INTEGER,
    TypExe    INTEGER not NULL,
    IdeSrvExe INTEGER,
    CmdExe    VARCHAR(250) not NULL,
    TypLch    INTEGER not NULL,
    LchDepErr INTEGER DEFAULT 0 not NULL,
    LchDepWrn INTEGER DEFAULT 0 not NULL,
    LchSetWrn INTEGER DEFAULT 0 not NULL,
    RepTyp    INTEGER DEFAULT 0 not NULL,
    RepEmaSrv INTEGER,
    RepSndSta VARCHAR(20),
    RepEmaDst VARCHAR(250),
    RepTit    VARCHAR(100),
    RepMsg    VARCHAR(250),
    Act       INTEGER DEFAULT 1 not NULL,
    RepUsrDst VARCHAR(250),
    RepMntId  VARCHAR(200),
    CmdExePth VARCHAR(200)
);

-- ------------------------------------------------------
--  DDL for Table AweSchTskDpn
--  Task dependencies
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchTskDpn
(
    IdeTsk INTEGER NOT NULL,
    IdePrn INTEGER NOT NULL,
    IsBlk  INTEGER,
    DpnOrd INTEGER
);

-- ------------------------------------------------------
--  DDL for Table AweSchTskLch
--  Task launchers
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchTskLch
(
    Ide      INTEGER NOT NULL,
    IdeTsk   INTEGER,
    RptNum   INTEGER,
    RptTyp   INTEGER,
    IniDat   DATE,
    EndDat   DATE,
    IniTim   VARCHAR(8),
    EndTim   VARCHAR(8),
    IdeCal   INTEGER,
    IdSrv    INTEGER,
    Pth      VARCHAR(250),
    Pat      VARCHAR(250),
    ExeHrs   VARCHAR(200),
    ExeMth   VARCHAR(200),
    ExeWek   VARCHAR(200),
    ExeDay   VARCHAR(200),
    ExeDte   DATE,
    ExeTim   VARCHAR(8),
    WeekDays VARCHAR(200),
    ExeYrs   VARCHAR(200),
    ExeMin   VARCHAR(200),
    ExeSec   VARCHAR(200),
    SrvUsr   VARCHAR(200),
    SrvPwd   VARCHAR(200)
);

-- ------------------------------------------------------
--  DDL for Table AweSchTskPar
--  Task parameters
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchTskPar
(
    Ide    INTEGER NOT NULL,
    IdeTsk INTEGER,
    Nam    VARCHAR(40) NOT NULL,
    Val    VARCHAR(400),
    Src    INTEGER NOT NULL,
    Typ    VARCHAR(100) NOT NULL
);

-- ------------------------------------------------------
--  DDL for HISTORIC TABLES
-- ------------------------------------------------------

CREATE TABLE IF NOT EXISTS HISAweSchCal
(
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    Nom    VARCHAR(40),
    Des    VARCHAR(250),
    Act    INTEGER
);

CREATE TABLE IF NOT EXISTS HISAweSchCalDat
(
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    IdeCal INTEGER,
    Nom    VARCHAR(40),
    Dat    DATE
);

CREATE TABLE IF NOT EXISTS HISAweSchSrv
(
    HISope VARCHAR(20) not NULL,
    HISdat DATE not NULL,
    HISact VARCHAR(1) not NULL,
    Ide    INTEGER,
    Nom    VARCHAR(40),
    Pro    VARCHAR(10),
    Hst    VARCHAR(40),
    Prt    VARCHAR(10),
    Act    INTEGER
);

CREATE TABLE IF NOT EXISTS HISAweSchTsk
(
    HISope    VARCHAR(20) not NULL,
    HISdat    DATE not NULL,
    HISact    VARCHAR(1) not NULL,
    Ide       INTEGER,
    IdePar    INTEGER,
    Nam       VARCHAR(40),
    Des       VARCHAR(250),
    NumStoExe INTEGER,
    TimOutExe INTEGER,
    TypExe    INTEGER,
    IdeSrvExe INTEGER,
    CmdExe    VARCHAR(250),
    TypLch    INTEGER,
    LchDepErr INTEGER,
    LchDepWrn INTEGER,
    LchSetWrn INTEGER,
    BlkPar    INTEGER,
    RepTyp    INTEGER,
    RepEmaSrv INTEGER,
    RepSndSta VARCHAR(20),
    RepEmaDst VARCHAR(250),
    RepTit    VARCHAR(100),
    RepMsg    VARCHAR(250),
    Act       INTEGER,
    RepUsrDst VARCHAR(250),
    RepMntId  VARCHAR(200),
    CmdExePth VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS HISAweSchTskLch
(
    HISope   VARCHAR(20) not NULL,
    HISdat   DATE not NULL,
    HISact   VARCHAR(1) not NULL,
    Ide      INTEGER,
    IdeTsk   INTEGER,
    RptNum   INTEGER,
    RptTyp   INTEGER,
    IniDat   DATE,
    EndDat   DATE,
    IniTim   VARCHAR(8),
    EndTim   VARCHAR(8),
    IdeCal   INTEGER,
    IdSrv    INTEGER,
    Pth      VARCHAR(250),
    Pat      VARCHAR(250),
    ExeMth   VARCHAR(200),
    ExeWek   VARCHAR(200),
    ExeDay   VARCHAR(200),
    ExeHrs   VARCHAR(200),
    ExeDte   DATE,
    ExeTim   VARCHAR(8),
    WeekDays VARCHAR(200),
    ExeYrs   VARCHAR(200),
    ExeMin   VARCHAR(200),
    ExeSec   VARCHAR(200),
    SrvUsr   VARCHAR(200),
    SrvPwd   VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS HISAweSchTskPar
(
    HISope VARCHAR(20) not NULL,
    HISdat DATE not NULL,
    HISact VARCHAR(1) not NULL,
    Ide    INTEGER,
    IdeTsk INTEGER,
    Nam    VARCHAR(40),
    Val    VARCHAR(400),
    Src    INTEGER,
    Typ    VARCHAR(100)
);

-- ------------------------------------------------------
--  DDL for CONSTRAINTS
-- ------------------------------------------------------

CREATE UNIQUE INDEX NOM_UQ ON AweSchCal (Nom);
CREATE UNIQUE INDEX PK_AWESCHCAL ON AweSchCal (Ide);
CREATE UNIQUE INDEX PK_AWESCHCALDAT ON AweSchCalDat (Ide);
CREATE INDEX AWESCHEXEI1 ON AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat);
CREATE UNIQUE INDEX PK_AWESCHSRV ON AweSchSrv (Ide);
CREATE UNIQUE INDEX PK_AWESCHTSK ON AweSchTsk (Ide);
CREATE UNIQUE INDEX SYS_C00164575 ON AweSchTskDpn (IdeTsk, IDEPRN);
CREATE UNIQUE INDEX PK_AWESCHTSKLCH ON AweSchTskLch (Ide);
CREATE UNIQUE INDEX PK_AWESCHTSKPAR ON AweSchTskPar (Ide);
CREATE INDEX HISAweSchCalI1 ON HISAweSchCal (HISope, HISdat, HISact);
CREATE INDEX HISAweSchCalDatI1 ON HISAweSchCalDat (HISope, HISdat, HISact);
CREATE INDEX HISAweSchSrvI1 ON HISAweSchSrv (HISope, HISdat, HISact);
CREATE INDEX HISAweSchTskI1 ON HISAweSchTsk (HISope, HISdat, HISact);
CREATE INDEX HISAweSchTskLchI1 ON HISAweSchTskLch (HISope, HISdat, HISact);
CREATE INDEX HISAweSchTskParI1 ON HISAweSchTskPar (HISope, HISdat, HISact);
ALTER TABLE AweSchCal ADD CONSTRAINT PK_AWESCHCAL PRIMARY KEY (Ide);
ALTER TABLE AweSchCalDat ADD CONSTRAINT PK_AWESCHCALDAT PRIMARY KEY (Ide);
ALTER TABLE AweSchTskFilMod ADD PRIMARY KEY (IdeTsk, FILPTH);
ALTER TABLE AweSchSrv ADD CONSTRAINT PK_AWESCHSRV PRIMARY KEY (Ide);
ALTER TABLE AweSchTsk ADD CONSTRAINT PK_AWESCHTSK PRIMARY KEY (Ide);
ALTER TABLE AweSchTskDpn ADD PRIMARY KEY (IdeTsk, IDEPRN);
ALTER TABLE AweSchTskLch ADD CONSTRAINT PK_AWESCHTSKLCH PRIMARY KEY (Ide);
ALTER TABLE AweSchTskPar ADD CONSTRAINT PK_AWESCHTSKPAR PRIMARY KEY (Ide);

-- Scheduler sequences
Insert into AweKey (KeyNam, KeyVal) values ('SchTskSrv', (select coalesce(max(Ide),0) + 1 from AweSchSrv));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskCal', (select coalesce(max(Ide),0) + 1 from AweSchCal));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskCalDat', (select coalesce(max(Ide),0) + 1 from AweSchCalDat));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskKey', (select coalesce(max(Ide),0) + 1 from AweSchTsk));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskLch', (select coalesce(max(Ide),0) + 1 from AweSchTskLch));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskPar', (select coalesce(max(Ide),0) + 1 from AweSchTskPar));