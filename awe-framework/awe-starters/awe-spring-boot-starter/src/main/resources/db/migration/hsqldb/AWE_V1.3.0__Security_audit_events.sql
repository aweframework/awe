-- ------------------------------------------------------
--  DDL for Table AweSecAudEvt
--  Security audit events (append-only): logins, logouts, locks, password and second factor changes,
--  denied accesses and REST tokens. Rows are only deleted by the retention purge.
--  The script can be applied again: every object is created only when it does not exist.
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSecAudEvt (
    IdeSecAud varchar(36) NOT NULL PRIMARY KEY,      --  Event identifier (UUID)
    EvtDat timestamp NOT NULL,                       --  Event date
    EvtDay date NOT NULL,                            --  Event day (server time zone), for day filters and charts
    EvtTyp varchar(40) NOT NULL,                     --  Event type (LOGIN, LOGOUT, ACCOUNT_LOCKED...)
    EvtRes varchar(10) NOT NULL,                     --  Outcome (SUCCESS, FAILURE)
    EvtCau varchar(40),                              --  Cause (BAD_CREDENTIALS, LOCKED, EXPIRED...)
    UsrNam varchar(255),                             --  User name
    Ten varchar(100),                                --  Tenant (SSO tenant or database alias)
    Chn varchar(10),                                 --  Channel (WEB, SSO, REST, SYSTEM)
    IpAdr varchar(45),                               --  Client address
    UsrAgt varchar(512),                             --  Client user agent
    SesHsh varchar(64),                              --  SHA-256 of the session identifier
    CorIde varchar(64),                              --  Correlation identifier
    Det varchar(1000)                                --  Detail
);

CREATE INDEX IF NOT EXISTS AweSecAudEvtI1 ON AweSecAudEvt (EvtDat);
CREATE INDEX IF NOT EXISTS AweSecAudEvtI2 ON AweSecAudEvt (UsrNam, EvtDat);
CREATE INDEX IF NOT EXISTS AweSecAudEvtI3 ON AweSecAudEvt (EvtTyp, EvtDat);
CREATE INDEX IF NOT EXISTS AweSecAudEvtI4 ON AweSecAudEvt (EvtDay);
