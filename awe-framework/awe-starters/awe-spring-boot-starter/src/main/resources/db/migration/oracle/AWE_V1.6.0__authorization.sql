-- ------------------------------------------------------
--  DDL for Table AweOpeRol
--  Additional profiles of a user: the profiles a user has besides the main one (ope.IdePro). The effective access of
--  a user is the union of the main profile and these profiles. The main profile is never repeated here.
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
--  Columns:
--    IdeAweOpeRol  Table identifier
--    IdeOpe        User key
--    IdePro        Additional profile key
-- ------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE AweOpeRol (IdeAweOpeRol number(10) CONSTRAINT pk_AweOpeRol PRIMARY KEY NOT NULL, IdeOpe number(5) NOT NULL, IdePro number(5) NOT NULL, CONSTRAINT uk_AweOpeRol UNIQUE (IdeOpe, IdePro), CONSTRAINT fk_AweOpeRol1 FOREIGN KEY (IdeOpe) REFERENCES ope (IdeOpe), CONSTRAINT fk_AweOpeRol2 FOREIGN KEY (IdePro) REFERENCES AwePro (IdePro))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

--  Audit of the table
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE HISAweOpeRol (HISope char(100) NOT NULL, HISdat date NOT NULL, HISact varchar2(1) NOT NULL, IdeAweOpeRol number(10) NULL, IdeOpe number(5) NULL, IdePro number(5) NULL)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX HISAweOpeRolI1 ON HISAweOpeRol (HISope, HISdat, HISact)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

--  Sequence of the table identifier
INSERT INTO AweKey (KeyNam, KeyVal)
SELECT 'OpeRolKey', 0
FROM DUAL
WHERE NOT EXISTS(SELECT * FROM AweKey WHERE KeyNam = 'OpeRolKey');

-- ------------------------------------------------------
--  DDL for Table AweProPer
--  Permissions of a profile. Per is a permission identifier of the catalog (Permissions.xml) or a wildcard: * (every
--  permission), users.* (every permission of a resource), *.read (one verb on every resource). The catalog stays in
--  the code, so an assignment that is no longer in the catalog is ignored, never deleted.
--  The script can be applied again: every object and row is created only when it does not exist.
-- ------------------------------------------------------
--  Columns:
--    IdeAweProPer  Table identifier
--    IdePro        Profile key
--    Per           Permission identifier or wildcard
-- ------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE AweProPer (IdeAweProPer number(10) CONSTRAINT pk_AweProPer PRIMARY KEY NOT NULL, IdePro number(5) NOT NULL, Per varchar2(100) NOT NULL, CONSTRAINT uk_AweProPer UNIQUE (IdePro, Per), CONSTRAINT fk_AweProPer1 FOREIGN KEY (IdePro) REFERENCES AwePro (IdePro))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

--  Audit of the table
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE HISAweProPer (HISope char(100) NOT NULL, HISdat date NOT NULL, HISact varchar2(1) NOT NULL, IdeAweProPer number(10) NULL, IdePro number(5) NULL, Per varchar2(100) NULL)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX HISAweProPerI1 ON HISAweProPer (HISope, HISdat, HISact)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

--  Sequence of the table identifier
INSERT INTO AweKey (KeyNam, KeyVal)
SELECT 'ProPerKey', 0
FROM DUAL
WHERE NOT EXISTS(SELECT * FROM AweKey WHERE KeyNam = 'ProPerKey');

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
