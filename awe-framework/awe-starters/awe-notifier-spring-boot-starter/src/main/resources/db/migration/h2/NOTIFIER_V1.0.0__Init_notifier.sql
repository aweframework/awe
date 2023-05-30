--------------------------------------------------------
--  NOTIFIER DDL
--------------------------------------------------------

--------------------------------------------------------
--  DDL for Table AweSub
--  Subscriptions
--------------------------------------------------------
create TABLE IF NOT EXISTS AweSub
(
    Ide INTEGER           not NULL,
    Acr VARCHAR(10)       not NULL,
    Nom VARCHAR(30)       not NULL,
    Des VARCHAR(250),
    Act INTEGER DEFAULT 1 not NULL
);

--------------------------------------------------------
--  DDL for Table AweSubUsr
--  User subscriptions
--------------------------------------------------------
create TABLE IF NOT EXISTS AweSubUsr
(
    Ide    INTEGER           not NULL,
    IdeOpe INTEGER           not NULL,
    IdeSub INTEGER           not NULL,
    SubNot INTEGER DEFAULT 1 not NULL,
    SubEma INTEGER DEFAULT 0 not NULL
);

--------------------------------------------------------
--  DDL for Table AweNot
--  Notifications
--------------------------------------------------------
create TABLE IF NOT EXISTS AweNot
(
    Ide    INTEGER     not NULL,
    IdeSub INTEGER     not NULL,
    Typ    VARCHAR(10) not NULL,
    Ico    VARCHAR(30) not NULL,
    Nom    VARCHAR(30) not NULL,
    Des    VARCHAR(250),
    Scr    VARCHAR(100),
    Cod    VARCHAR(100),
    Dat    DATE        not NULL
);

--------------------------------------------------------
--  DDL for Table AweNotUsr
--  User notifications
--------------------------------------------------------
create TABLE IF NOT EXISTS AweNotUsr
(
    Ide    INTEGER           not NULL,
    IdeOpe INTEGER           not NULL,
    IdeNot INTEGER           not NULL,
    Unr    INTEGER DEFAULT 0 not NULL
);

--------------------------------------------------------
--  DDL for HISTORIC TABLES
--------------------------------------------------------

create TABLE IF NOT EXISTS HISAweSub
(
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    Acr    VARCHAR(10),
    Nom    VARCHAR(30),
    Des    VARCHAR(250),
    Act    INTEGER
);

create TABLE IF NOT EXISTS HISAweSubUsr
(
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    IdeOpe INTEGER,
    IdeSub INTEGER,
    SubNot INTEGER,
    SubEma INTEGER
);

create TABLE IF NOT EXISTS HISAweNot
(
    HISope VARCHAR(20) not NULL,
    HISdat DATE        not NULL,
    HISact VARCHAR(1)  not NULL,
    Ide    INTEGER,
    IdeSub INTEGER,
    Typ    VARCHAR(10),
    Ico    VARCHAR(30),
    Nom    VARCHAR(30),
    Des    VARCHAR(250),
    Scr    VARCHAR(100),
    Cod    VARCHAR(100),
    Dat    DATE
);

create TABLE IF NOT EXISTS HISAweNotUsr
(
    HISope    VARCHAR(20) not NULL,
    HISdat    DATE        not NULL,
    HISact    VARCHAR(1)  not NULL,
    Ide       INTEGER,
    IdeOpe    INTEGER,
    IdeNot    INTEGER,
    Unr       INTEGER
);

--------------------------------------------------------
--  DDL for CONSTRAINTS
--------------------------------------------------------

create UNIQUE INDEX IF NOT EXISTS PK_AWESUB ON AweSub (Ide);
create UNIQUE INDEX IF NOT EXISTS PK_AWESUBUSR ON AweSubUsr (Ide);
create UNIQUE INDEX IF NOT EXISTS PK_AWENOT ON AweNot (Ide);
create UNIQUE INDEX IF NOT EXISTS PK_AWENOTUSR ON AweNotUsr (Ide);
create INDEX IF NOT EXISTS HISAweSubI1 ON HISAweSub (HISope, HISdat, HISact);
create INDEX IF NOT EXISTS HISAweSubUsrI1 ON HISAweSubUsr (HISope, HISdat, HISact);
create INDEX IF NOT EXISTS HISAweNotI1 ON HISAweNot (HISope, HISdat, HISact);
create INDEX IF NOT EXISTS HISAweNotUsrI1 ON HISAweNotUsr (HISope, HISdat, HISact);
alter table AweSub add CONSTRAINT IF NOT EXISTS PK_AWESUB PRIMARY KEY (Ide);
alter table AweSubUsr add CONSTRAINT IF NOT EXISTS PK_AWESUBUSR PRIMARY KEY (Ide);
alter table AweNot add CONSTRAINT IF NOT EXISTS PK_AWENOT PRIMARY KEY (Ide);
alter table AweNotUsr add CONSTRAINT IF NOT EXISTS PK_AWENOTUSR PRIMARY KEY (Ide);

-- Notifier sequences
insert into AweKey (KeyNam, KeyVal) values ('Sub', (select coalesce(max(Ide),0) + 1 from AweSub));
insert into AweKey (KeyNam, KeyVal) values ('SubUsr', (select coalesce(max(Ide),0) + 1 from AweSubUsr));
insert into AweKey (KeyNam, KeyVal) values ('Not', (select coalesce(max(Ide),0) + 1 from AweNot));
insert into AweKey (KeyNam, KeyVal) values ('NotUsr', (select coalesce(max(Ide),0) + 1 from AweNotUsr));

