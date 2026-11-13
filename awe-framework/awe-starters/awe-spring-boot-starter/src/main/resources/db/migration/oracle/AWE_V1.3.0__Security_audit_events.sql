-- ------------------------------------------------------
--  DDL for Table AweSecAudEvt
--  Security audit events (append-only): logins, logouts, locks, password and second factor changes,
--  denied accesses and REST tokens. Rows are only deleted by the retention purge.
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
--  Columns:
--    IdeSecAud  Event identifier (UUID)
--    EvtDat     Event date
--    EvtDay     Event day (server time zone), for day filters and charts
--    EvtTyp     Event type (LOGIN, LOGOUT, ACCOUNT_LOCKED...)
--    EvtRes     Outcome (SUCCESS, FAILURE)
--    EvtCau     Cause (BAD_CREDENTIALS, LOCKED, EXPIRED...)
--    UsrNam     User name
--    Ten        Tenant (SSO tenant or database alias)
--    Chn        Channel (WEB, SSO, REST, SYSTEM)
--    IpAdr      Client address
--    UsrAgt     Client user agent
--    SesHsh     SHA-256 of the session identifier
--    CorIde     Correlation identifier
--    Det        Detail
-- ------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE AweSecAudEvt (IdeSecAud varchar2(36) NOT NULL CONSTRAINT pk_AweSecAudEvt PRIMARY KEY, EvtDat timestamp NOT NULL, EvtDay date NOT NULL, EvtTyp varchar2(40) NOT NULL, EvtRes varchar2(10) NOT NULL, EvtCau varchar2(40), UsrNam varchar2(255), Ten varchar2(100), Chn varchar2(10), IpAdr varchar2(45), UsrAgt varchar2(512), SesHsh varchar2(64), CorIde varchar2(64), Det varchar2(1000))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX AweSecAudEvtI1 ON AweSecAudEvt (EvtDat)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX AweSecAudEvtI2 ON AweSecAudEvt (UsrNam, EvtDat)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX AweSecAudEvtI3 ON AweSecAudEvt (EvtTyp, EvtDat)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX AweSecAudEvtI4 ON AweSecAudEvt (EvtDay)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
/
