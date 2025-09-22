-- ------------------------------------------------------
--  SCHEDULER DDL (PostgreSQL)
-- ------------------------------------------------------

-- Calendar list
CREATE TABLE IF NOT EXISTS AweSchCal (
    Ide INTEGER NOT NULL,
    Des VARCHAR(250) NOT NULL,
    Act INTEGER DEFAULT 1 NOT NULL,
    Nom VARCHAR(100) NOT NULL
);

-- Task file modified dates
CREATE TABLE IF NOT EXISTS AweSchTskFilMod (
    IdeTsk INTEGER NOT NULL,
    FilPth VARCHAR(256) NOT NULL,
    ModDat DATE
);

-- Calendar dates
CREATE TABLE IF NOT EXISTS AweSchCalDat (
    Ide    INTEGER NOT NULL,
    IdeCal INTEGER NOT NULL,
    Nom    VARCHAR(40) NOT NULL,
    Dat    DATE NOT NULL
);

-- Task executions
CREATE TABLE IF NOT EXISTS AweSchExe (
    IdeTsk INTEGER NOT NULL,
    GrpTsk VARCHAR(40) NOT NULL,
    ExeTsk INTEGER NOT NULL,
    IniDat TIMESTAMP NOT NULL,
    EndDat TIMESTAMP,
    ExeTim INTEGER,
    Sta    INTEGER NOT NULL,
    LchBy  VARCHAR(200),
    Des    VARCHAR(2000)
);

-- Scheduler servers
CREATE TABLE IF NOT EXISTS AweSchSrv (
    Ide INTEGER NOT NULL,
    Nom VARCHAR(40) NOT NULL,
    Pro VARCHAR(10) NOT NULL,
    Hst VARCHAR(40) NOT NULL,
    Prt VARCHAR(10) NOT NULL,
    Act INTEGER DEFAULT 1 NOT NULL
);

-- Scheduler tasks
CREATE TABLE IF NOT EXISTS AweSchTsk (
    Ide       INTEGER NOT NULL,
    Nam       VARCHAR(40) NOT NULL,
    Des       VARCHAR(250),
    NumStoExe INTEGER,
    TimOutExe INTEGER,
    TypExe    INTEGER NOT NULL,
    IdeSrvExe INTEGER,
    CmdExe    VARCHAR(250) NOT NULL,
    TypLch    INTEGER NOT NULL,
    LchDepErr INTEGER DEFAULT 0 NOT NULL,
    LchDepWrn INTEGER DEFAULT 0 NOT NULL,
    LchSetWrn INTEGER DEFAULT 0 NOT NULL,
    RepTyp    INTEGER DEFAULT 0 NOT NULL,
    RepEmaSrv INTEGER,
    RepSndSta VARCHAR(20),
    RepEmaDst VARCHAR(250),
    RepTit    VARCHAR(100),
    RepMsg    VARCHAR(250),
    Act       INTEGER DEFAULT 1 NOT NULL,
    RepUsrDst VARCHAR(250),
    RepMntId  VARCHAR(200),
    CmdExePth VARCHAR(200)
);

-- Task dependencies
CREATE TABLE IF NOT EXISTS AweSchTskDpn (
    IdeTsk INTEGER NOT NULL,
    IdePrn INTEGER NOT NULL,
    IsBlk  INTEGER,
    DpnOrd INTEGER
);

-- Task launchers
CREATE TABLE IF NOT EXISTS AweSchTskLch (
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

-- Task parameters
CREATE TABLE IF NOT EXISTS AweSchTskPar (
    Ide    INTEGER NOT NULL,
    IdeTsk INTEGER,
    Nam    VARCHAR(40) NOT NULL,
    Val    VARCHAR(400),
    Src    INTEGER NOT NULL,
    Typ    VARCHAR(100) NOT NULL
);

-- Historic tables
CREATE TABLE IF NOT EXISTS HISAweSchCal (
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    Nom    VARCHAR(40),
    Des    VARCHAR(250),
    Act    INTEGER
);

CREATE TABLE IF NOT EXISTS HISAweSchCalDat (
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    IdeCal INTEGER,
    Nom    VARCHAR(40),
    Dat    DATE
);

CREATE TABLE IF NOT EXISTS HISAweSchSrv (
    HISope VARCHAR(20) NOT NULL,
    HISdat DATE NOT NULL,
    HISact VARCHAR(1) NOT NULL,
    Ide    INTEGER,
    Nom    VARCHAR(40),
    Pro    VARCHAR(10),
    Hst    VARCHAR(40),
    Prt    VARCHAR(10),
    Act    INTEGER
);

CREATE TABLE IF NOT EXISTS HISAweSchTsk (
    HISope    VARCHAR(20) NOT NULL,
    HISdat    DATE NOT NULL,
    HISact    VARCHAR(1) NOT NULL,
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

CREATE TABLE IF NOT EXISTS HISAweSchTskLch (
    HISope   VARCHAR(20) NOT NULL,
    HISdat   DATE NOT NULL,
    HISact   VARCHAR(1) NOT NULL,
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

CREATE TABLE IF NOT EXISTS HISAweSchTskPar (
    HISope VARCHAR(20) NOT NULL,
    HISdat DATE NOT NULL,
    HISact VARCHAR(1) NOT NULL,
    Ide    INTEGER,
    IdeTsk INTEGER,
    Nam    VARCHAR(40),
    Val    VARCHAR(400),
    Src    INTEGER,
    Typ    VARCHAR(100)
);

-- Indexes & constraints
CREATE UNIQUE INDEX IF NOT EXISTS NOM_UQ ON AweSchCal (Nom);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESCHCAL ON AweSchCal (Ide);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESCHCALDAT ON AweSchCalDat (Ide);
CREATE INDEX IF NOT EXISTS AWESCHEXEI1 ON AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESCHSRV ON AweSchSrv (Ide);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESCHTSK ON AweSchTsk (Ide);
CREATE UNIQUE INDEX IF NOT EXISTS SYS_C00164575 ON AweSchTskDpn (IdeTsk, IDEPRN);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESCHTSKLCH ON AweSchTskLch (Ide);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESCHTSKPAR ON AweSchTskPar (Ide);
CREATE INDEX IF NOT EXISTS HISAweSchCalI1 ON HISAweSchCal (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweSchCalDatI1 ON HISAweSchCalDat (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweSchSrvI1 ON HISAweSchSrv (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweSchTskI1 ON HISAweSchTsk (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweSchTskLchI1 ON HISAweSchTskLch (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweSchTskParI1 ON HISAweSchTskPar (HISope, HISdat, HISact);

ALTER TABLE AweSchTskFilMod ADD PRIMARY KEY (IdeTsk, FILPTH);
ALTER TABLE AweSchTskDpn ADD PRIMARY KEY (IdeTsk, IDEPRN);

-- Scheduler sequences
Insert into AweKey (KeyNam, KeyVal) values ('SchTskSrv', (select coalesce(max(Ide),0) + 1 from AweSchSrv));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskCal', (select coalesce(max(Ide),0) + 1 from AweSchCal));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskCalDat', (select coalesce(max(Ide),0) + 1 from AweSchCalDat));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskKey', (select coalesce(max(Ide),0) + 1 from AweSchTsk));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskLch', (select coalesce(max(Ide),0) + 1 from AweSchTskLch));
Insert into AweKey (KeyNam, KeyVal) values ('SchTskPar', (select coalesce(max(Ide),0) + 1 from AweSchTskPar));
