-- ------------------------------------------------------
--  DDL for Table AweOpeRol
--  Additional profiles of a user: the profiles a user has besides the main one (ope.IdePro). The effective access of
--  a user is the union of the main profile and these profiles. The main profile is never repeated here.
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
IF NOT EXISTS(SELECT *
              FROM sys.tables
              WHERE name = 'AweOpeRol'
                AND type = 'U')
CREATE TABLE AweOpeRol (
    IdeAweOpeRol int NOT NULL PRIMARY KEY,   --  Table identifier
    IdeOpe int NOT NULL,                     --  User key
    IdePro int NOT NULL,                     --  Additional profile key
    CONSTRAINT uk_AweOpeRol UNIQUE (IdeOpe, IdePro),
    CONSTRAINT fk_AweOpeRol1 FOREIGN KEY (IdeOpe) REFERENCES ope (IdeOpe),
    CONSTRAINT fk_AweOpeRol2 FOREIGN KEY (IdePro) REFERENCES AwePro (IdePro)
    );

--  Audit of the table
IF NOT EXISTS(SELECT *
              FROM sys.tables
              WHERE name = 'HISAweOpeRol'
                AND type = 'U')
CREATE TABLE HISAweOpeRol (HISope char(100) NOT NULL, HISdat date NOT NULL, HISact varchar(1) NOT NULL, IdeAweOpeRol int NULL, IdeOpe int NULL, IdePro int NULL);

IF NOT EXISTS(SELECT *
              FROM sys.indexes
              WHERE name = 'HISAweOpeRolI1')
CREATE INDEX HISAweOpeRolI1 ON HISAweOpeRol (HISope, HISdat, HISact);

--  Sequence of the table identifier
IF NOT EXISTS(SELECT *
              FROM AweKey
              WHERE KeyNam = 'OpeRolKey')
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('OpeRolKey', 0);

-- ------------------------------------------------------
--  DDL for Table AweProPer
--  Permissions of a profile. Per is a permission identifier of the catalog (Permissions.xml) or a wildcard: * (every
--  permission), users.* (every permission of a resource), *.read (one verb on every resource). The catalog stays in
--  the code, so an assignment that is no longer in the catalog is ignored, never deleted.
--  The script can be applied again: every object and row is created only when it does not exist.
-- ------------------------------------------------------
IF NOT EXISTS(SELECT *
              FROM sys.tables
              WHERE name = 'AweProPer'
                AND type = 'U')
CREATE TABLE AweProPer (
    IdeAweProPer int NOT NULL PRIMARY KEY,   --  Table identifier
    IdePro int NOT NULL,                     --  Profile key
    Per varchar(100) NOT NULL,               --  Permission identifier or wildcard
    CONSTRAINT uk_AweProPer UNIQUE (IdePro, Per),
    CONSTRAINT fk_AweProPer1 FOREIGN KEY (IdePro) REFERENCES AwePro (IdePro)
    );

--  Audit of the table
IF NOT EXISTS(SELECT *
              FROM sys.tables
              WHERE name = 'HISAweProPer'
                AND type = 'U')
CREATE TABLE HISAweProPer (HISope char(100) NOT NULL, HISdat date NOT NULL, HISact varchar(1) NOT NULL, IdeAweProPer int NULL, IdePro int NULL, Per varchar(100) NULL);

IF NOT EXISTS(SELECT *
              FROM sys.indexes
              WHERE name = 'HISAweProPerI1')
CREATE INDEX HISAweProPerI1 ON HISAweProPer (HISope, HISdat, HISact);

--  Sequence of the table identifier
IF NOT EXISTS(SELECT *
              FROM AweKey
              WHERE KeyNam = 'ProPerKey')
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('ProPerKey', 0);

--  The administrator profile (ADM) gets every permission. The identifier is the next value of ProPerKey, and the key
--  is moved past the identifiers in use
INSERT INTO AweProPer (IdeAweProPer, IdePro, Per)
SELECT k.KeyVal + 1, p.IdePro, '*'
FROM AwePro p, AweKey k
WHERE p.Acr = 'ADM'
  AND k.KeyNam = 'ProPerKey'
  AND NOT EXISTS(SELECT * FROM AweProPer a WHERE a.IdePro = p.IdePro AND a.Per = '*');

UPDATE AweKey
SET KeyVal = (SELECT MAX(IdeAweProPer) FROM AweProPer)
WHERE KeyNam = 'ProPerKey'
  AND KeyVal < (SELECT COALESCE(MAX(IdeAweProPer), 0) FROM AweProPer);
