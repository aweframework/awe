-- ------------------------------------------------------
--  Webhook launch
--  AweSchTskHok: the hook of a WEBHOOK task (one per task). The secret is not stored: SecPrp names the property that holds it.
--  AweSchHokMsg: the inbox of received webhooks, deduplicated by source (hook key) and message id.
--  AweSchTskPar.WhkPtr: JSON pointer of the webhook data that gives the value of a parameter.
--  Additive and backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
CREATE TABLE AweSchTskHok
(
    IdeTsk INTEGER          not NULL,
    HokKey VARCHAR(64)   not NULL,
    Act    INTEGER          not NULL,
    SecPrp VARCHAR(200)  not NULL,
    TypFlt VARCHAR(200),
    CONSTRAINT PK_AWESCHTSKHOK PRIMARY KEY (IdeTsk),
    CONSTRAINT UK_AWESCHTSKHOK UNIQUE (HokKey)
);

CREATE TABLE AweSchHokMsg
(
    Src    VARCHAR(64)   not NULL,
    MsgId  VARCHAR(256)  not NULL,
    RcvDat TIMESTAMP(3)          not NULL,
    Typ    VARCHAR(200),
    Pay    LONGVARCHAR         not NULL,
    Sta    VARCHAR(10)   not NULL,
    Att    INTEGER          not NULL,
    NxtDat TIMESTAMP(3)          not NULL,
    PrcDat TIMESTAMP(3),
    Err    VARCHAR(2000),
    IdeTsk INTEGER,
    ExeTsk INTEGER,
    IdeRun VARCHAR(36),
    CONSTRAINT PK_AWESCHHOKMSG PRIMARY KEY (Src, MsgId)
);

CREATE INDEX AweSchHokMsgI1 ON AweSchHokMsg (Sta, NxtDat);

ALTER TABLE AweSchTskPar ADD WhkPtr VARCHAR(250);
