-- ------------------------------------------------------
--  NOTIFIER DDL (PostgreSQL)
-- ------------------------------------------------------

-- Subscriptions
CREATE TABLE IF NOT EXISTS AweSub (
    Ide INTEGER           NOT NULL PRIMARY KEY,
    Acr VARCHAR(10)       NOT NULL,
    Nom VARCHAR(30)       NOT NULL,
    Des VARCHAR(250),
    Act INTEGER DEFAULT 1 NOT NULL
);

-- User subscriptions
CREATE TABLE IF NOT EXISTS AweSubUsr (
    Ide    INTEGER   NOT NULL PRIMARY KEY,
    IdeOpe INTEGER   NOT NULL,
    IdeSub INTEGER   NOT NULL,
    SubNot INTEGER DEFAULT 1 NOT NULL,
    SubEma INTEGER DEFAULT 0 NOT NULL
);

-- Notifications
CREATE TABLE IF NOT EXISTS AweNot (
    Ide    INTEGER     NOT NULL PRIMARY KEY,
    IdeSub INTEGER     NOT NULL,
    Typ    VARCHAR(10) NOT NULL,
    Ico    VARCHAR(30) NOT NULL,
    Nom    VARCHAR(30) NOT NULL,
    Des    VARCHAR(250),
    Scr    VARCHAR(100),
    Cod    VARCHAR(100),
    Dat    DATE        NOT NULL
);

-- User notifications
CREATE TABLE IF NOT EXISTS AweNotUsr (
    Ide    INTEGER           NOT NULL PRIMARY KEY,
    IdeOpe INTEGER           NOT NULL,
    IdeNot INTEGER           NOT NULL,
    Unr    INTEGER DEFAULT 1 NOT NULL
);

-- Historic tables
CREATE TABLE IF NOT EXISTS HISAweSub (
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    Acr    VARCHAR(10),
    Nom    VARCHAR(30),
    Des    VARCHAR(250),
    Act    INTEGER
);

CREATE TABLE IF NOT EXISTS HISAweSubUsr (
    HISope VARCHAR(20),
    HISdat DATE,
    HISact VARCHAR(1),
    Ide    INTEGER,
    IdeOpe INTEGER,
    IdeSub INTEGER,
    SubNot INTEGER,
    SubEma INTEGER
);

CREATE TABLE IF NOT EXISTS HISAweNot (
    HISope VARCHAR(20) NOT NULL,
    HISdat DATE        NOT NULL,
    HISact VARCHAR(1)  NOT NULL,
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

CREATE TABLE IF NOT EXISTS HISAweNotUsr (
    HISope    VARCHAR(20) NOT NULL,
    HISdat    DATE        NOT NULL,
    HISact    VARCHAR(1)  NOT NULL,
    Ide       INTEGER,
    IdeOpe    INTEGER,
    IdeNot    INTEGER,
    Unr       INTEGER
);

-- Indexes
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESUB ON AweSub (Ide);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESUBUSR ON AweSubUsr (Ide);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWENOT ON AweNot (Ide);
CREATE UNIQUE INDEX IF NOT EXISTS PK_AWENOTUSR ON AweNotUsr (Ide);
CREATE INDEX IF NOT EXISTS HISAweSubI1 ON HISAweSub (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweSubUsrI1 ON HISAweSubUsr (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweNotI1 ON HISAweNot (HISope, HISdat, HISact);
CREATE INDEX IF NOT EXISTS HISAweNotUsrI1 ON HISAweNotUsr (HISope, HISdat, HISact);

-- Notifier sequences
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('Sub', (SELECT coalesce(max(Ide),0) + 1 FROM AweSub));
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('SubUsr', (SELECT coalesce(max(Ide),0) + 1 FROM AweSubUsr));
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('Not', (SELECT coalesce(max(Ide),0) + 1 FROM AweNot));
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('NotUsr', (SELECT coalesce(max(Ide),0) + 1 FROM AweNotUsr));
