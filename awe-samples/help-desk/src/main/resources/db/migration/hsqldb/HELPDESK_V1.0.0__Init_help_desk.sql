-- ------------------------------------------------------
--  Beacon Desk (AWE sample application): help desk
--  Schema, read models (views), profiles, demo users and demo data. Bulk rows are generated here with
--  INSERT ... SELECT over a number sequence: deterministic, varied and relative to the date of the first start.
--  Demo credentials: see the README of the module (sample values, never use them in production).
-- ------------------------------------------------------

CREATE TABLE HdkTeam (
    IdeTeam int          NOT NULL PRIMARY KEY,
    IdePar  int          NULL,                      -- Parent team: the teams are a tree
    Nam     varchar(60)  NOT NULL,
    Des     varchar(200) NULL,
    CONSTRAINT fk_HdkTeam_par FOREIGN KEY (IdePar) REFERENCES HdkTeam (IdeTeam)
);

-- Which team handles each category of ticket (enumerated HdkCategory)
CREATE TABLE HdkCat (
    Cat     varchar(10) NOT NULL PRIMARY KEY,
    IdeTeam int         NOT NULL,
    CONSTRAINT fk_HdkCat_team FOREIGN KEY (IdeTeam) REFERENCES HdkTeam (IdeTeam)
);

CREATE TABLE HdkAgt (
    IdeAgt  int         NOT NULL PRIMARY KEY,
    IdeOpe  int         NOT NULL,                   -- Login of the agent (ope)
    Nam     varchar(60) NOT NULL,
    IdeTeam int         NOT NULL,
    CONSTRAINT fk_HdkAgt_ope FOREIGN KEY (IdeOpe) REFERENCES ope (IdeOpe),
    CONSTRAINT fk_HdkAgt_team FOREIGN KEY (IdeTeam) REFERENCES HdkTeam (IdeTeam)
);

CREATE TABLE HdkReq (
    IdeReq int         NOT NULL PRIMARY KEY,        -- Requester (customer)
    IdeOpe int         NULL,                        -- Login of the requester, empty when it cannot sign in
    Nam    varchar(60) NOT NULL,
    Eml    varchar(80) NOT NULL,
    Org    varchar(40) NULL,
    CONSTRAINT fk_HdkReq_ope FOREIGN KEY (IdeOpe) REFERENCES ope (IdeOpe),
    CONSTRAINT uk_HdkReq_ope UNIQUE (IdeOpe)
);

CREATE TABLE HdkTkt (
    IdeTkt  int           NOT NULL PRIMARY KEY,
    Cod     varchar(10) GENERATED ALWAYS AS ('HD-' || CAST(IdeTkt AS VARCHAR(6))),   -- Ticket code (for the agents)
    -- Public access key: with the email of the requester it opens the public status check, so it is not a counter. Here it is
    -- scrambled from the key of the ticket; a production application would store a random token generated when it is created
    AccKey  varchar(8) GENERATED ALWAYS AS (SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 1103515245 + 12345, 33554393) / 1, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 1103515245 + 12345, 33554393) / 32, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 1103515245 + 12345, 33554393) / 1024, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 1103515245 + 12345, 33554393) / 32768, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 1103515245 + 12345, 33554393) / 1048576, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 214013 * 2531011 + 4421, 32749) / 1, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 214013 * 2531011 + 4421, 32749) / 32, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(IdeTkt AS BIGINT) * 214013 * 2531011 + 4421, 32749) / 1024, 32), 1)),
    IdeReq  int           NOT NULL,
    IdeTeam int           NOT NULL,
    Cat     varchar(10)   NOT NULL,                 -- Category (enumerated HdkCategory)
    Pri     varchar(10)   NOT NULL,                 -- LOW, NORMAL, HIGH or CRITICAL (enumerated HdkPriority)
    Sta     varchar(10)   NOT NULL,                 -- NEW, OPEN, PENDING, RESOLVED or CLOSED (enumerated HdkStatus)
    Sub     varchar(120)  NOT NULL,
    Dsc     varchar(1000) NULL,
    IdeAgt  int           NULL,                     -- Assigned agent
    CreDat  timestamp     NOT NULL,
    UpdDat  timestamp     NOT NULL,
    ResDat  timestamp     NULL,                     -- Resolved on
    Chn     varchar(10)   NOT NULL,                 -- WEB, EMAIL or API (enumerated HdkChannel)
    CONSTRAINT fk_HdkTkt_req FOREIGN KEY (IdeReq) REFERENCES HdkReq (IdeReq),
    CONSTRAINT fk_HdkTkt_team FOREIGN KEY (IdeTeam) REFERENCES HdkTeam (IdeTeam),
    CONSTRAINT fk_HdkTkt_agt FOREIGN KEY (IdeAgt) REFERENCES HdkAgt (IdeAgt),
    CONSTRAINT ck_HdkTkt_pri CHECK (Pri IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_HdkTkt_sta CHECK (Sta IN ('NEW', 'OPEN', 'PENDING', 'RESOLVED', 'CLOSED'))
);
CREATE INDEX HdkTktI1 ON HdkTkt (IdeReq);
CREATE INDEX HdkTktI2 ON HdkTkt (Sta);

-- Priorities an agent may set (Agt = 1): CRITICAL is reserved to the supervisors (the target HdkTktPrio reads this table)
CREATE TABLE HdkPri (
    Pri varchar(10) NOT NULL PRIMARY KEY,
    Agt int         NOT NULL
);
INSERT INTO HdkPri (Pri, Agt) VALUES ('LOW', 1), ('NORMAL', 1), ('HIGH', 1), ('CRITICAL', 0);

CREATE TABLE HdkMsg (
    IdeMsg int           NOT NULL PRIMARY KEY,
    IdeTkt int           NOT NULL,
    MsgDat timestamp     NOT NULL,
    Aut    varchar(60)   NOT NULL,                  -- Author
    Typ    varchar(10)   NOT NULL,                  -- REQUESTER, PUBLIC (reply the requester sees) or INTERNAL (note)
    Txt    varchar(1000) NOT NULL,
    CONSTRAINT fk_HdkMsg_tkt FOREIGN KEY (IdeTkt) REFERENCES HdkTkt (IdeTkt),
    CONSTRAINT ck_HdkMsg_typ CHECK (Typ IN ('REQUESTER', 'PUBLIC', 'INTERNAL'))
);
CREATE INDEX HdkMsgI1 ON HdkMsg (IdeTkt);

CREATE TABLE HdkArt (
    IdeArt int           NOT NULL PRIMARY KEY,
    Cat    varchar(10)   NOT NULL,
    Tit    varchar(120)  NOT NULL,
    Txt    varchar(1500) NOT NULL,
    Pub    int DEFAULT 0 NOT NULL,                  -- Published (1) or draft (0)
    Vws    int DEFAULT 0 NOT NULL,                  -- Views
    UpdDat timestamp     NOT NULL
);

CREATE TABLE HdkSvc (
    IdeSvc int         NOT NULL PRIMARY KEY,
    Nam    varchar(40) NOT NULL,
    Sta    varchar(12) NOT NULL                     -- OPERATIONAL, DEGRADED or OUTAGE (enumerated HdkServiceStatus)
);

-- ------------------------------------------------------
--  Read models
-- ------------------------------------------------------
CREATE VIEW HdkSelfReq AS
SELECT r.IdeReq, TRIM(u.l1_nom) AS Usr FROM HdkReq r JOIN ope u ON u.IdeOpe = r.IdeOpe;

CREATE VIEW HdkSelfAgt AS
SELECT a.IdeAgt, a.Nam, TRIM(u.l1_nom) AS Usr FROM HdkAgt a JOIN ope u ON u.IdeOpe = a.IdeOpe;

-- SLA: hours to resolve by priority
CREATE VIEW HdkTktLst AS
SELECT t.IdeTkt, t.Cod, t.AccKey, t.IdeReq, r.Nam AS ReqNam, r.Eml AS ReqEml, t.IdeTeam, m.Nam AS TeamNam, t.Cat, t.Pri, t.Sta, t.Sub, t.Dsc,
       t.IdeAgt, a.Nam AS AgtNam, t.CreDat, t.UpdDat, t.ResDat, t.Chn,
       t.CreDat + (CASE t.Pri WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 8 WHEN 'NORMAL' THEN 24 ELSE 72 END) HOUR AS Due,
       CASE WHEN COALESCE(t.ResDat, CURRENT_TIMESTAMP) >
                 t.CreDat + (CASE t.Pri WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 8 WHEN 'NORMAL' THEN 24 ELSE 72 END) HOUR
            THEN 'BREACHED' ELSE 'OK' END AS Sla,
       CASE t.Sta WHEN 'NEW' THEN 'warning' WHEN 'OPEN' THEN 'info' WHEN 'RESOLVED' THEN 'success' ELSE 'neutral' END AS Ton,
       CASE t.Pri WHEN 'CRITICAL' THEN 'danger' WHEN 'HIGH' THEN 'warning' ELSE 'neutral' END AS PriTon
FROM HdkTkt t
         JOIN HdkReq r ON r.IdeReq = t.IdeReq
         JOIN HdkTeam m ON m.IdeTeam = t.IdeTeam
         LEFT JOIN HdkAgt a ON a.IdeAgt = t.IdeAgt;

CREATE VIEW HdkMsgLst AS
SELECT m.IdeMsg, m.IdeTkt, t.IdeReq, t.Cod, m.MsgDat, m.Aut, m.Typ, m.Txt,
       CASE m.Typ WHEN 'INTERNAL' THEN 'warning' WHEN 'PUBLIC' THEN 'info' ELSE 'neutral' END AS Ton
FROM HdkMsg m JOIN HdkTkt t ON t.IdeTkt = m.IdeTkt;

-- Public ticket check: status and public replies of one ticket (access key AND requester email)
CREATE VIEW HdkTrkSta AS
SELECT t.AccKey, LOWER(r.Eml) AS Eml, t.Sta, t.UpdDat
FROM HdkTkt t JOIN HdkReq r ON r.IdeReq = t.IdeReq;

CREATE VIEW HdkTrkRep AS
SELECT t.AccKey, LOWER(r.Eml) AS Eml, m.MsgDat, m.Aut, m.Txt
FROM HdkMsg m JOIN HdkTkt t ON t.IdeTkt = m.IdeTkt JOIN HdkReq r ON r.IdeReq = t.IdeReq
WHERE m.Typ = 'PUBLIC';

CREATE VIEW HdkTeamTree AS
SELECT t.IdeTeam AS Ide, t.IdePar AS Par, CASE WHEN EXISTS (SELECT 1 FROM HdkTeam c WHERE c.IdePar = t.IdeTeam) THEN 0 ELSE 1 END AS IsLeaf,
       t.Nam, t.Des,
       (SELECT COUNT(*) FROM HdkAgt a WHERE a.IdeTeam = t.IdeTeam OR a.IdeTeam IN (SELECT c.IdeTeam FROM HdkTeam c WHERE c.IdePar = t.IdeTeam)) AS Agts,
       (SELECT COUNT(*) FROM HdkTkt k WHERE k.Sta NOT IN ('RESOLVED', 'CLOSED')
           AND (k.IdeTeam = t.IdeTeam OR k.IdeTeam IN (SELECT c.IdeTeam FROM HdkTeam c WHERE c.IdePar = t.IdeTeam))) AS Opn
FROM HdkTeam t;

CREATE VIEW HdkArtLst AS
SELECT IdeArt, Cat, Tit, Txt, Pub, Vws, UpdDat FROM HdkArt;

CREATE VIEW HdkDay AS
SELECT d.Dat, (SELECT COUNT(*) FROM HdkTkt k WHERE TRUNC(k.CreDat) = d.Dat) AS Cre,
       (SELECT COUNT(*) FROM HdkTkt k WHERE TRUNC(k.ResDat) = d.Dat) AS Res
FROM (SELECT DISTINCT TRUNC(CreDat) AS Dat FROM HdkTkt) d;

CREATE VIEW HdkBacklog AS
SELECT Pri, COUNT(*) AS Cnt FROM HdkTkt WHERE Sta NOT IN ('RESOLVED', 'CLOSED') GROUP BY Pri;

CREATE VIEW HdkResTeam AS
SELECT m.Nam AS Team, ROUND(AVG(DATEDIFF('minute', t.CreDat, t.ResDat) / 60.0), 1) AS Hrs
FROM HdkTkt t JOIN HdkTeam m ON m.IdeTeam = t.IdeTeam
WHERE t.ResDat IS NOT NULL
GROUP BY m.Nam;

CREATE VIEW HdkSlaCnt AS
SELECT Sla, COUNT(*) AS Cnt FROM HdkTktLst WHERE CreDat >= CURRENT_TIMESTAMP - 30 DAY GROUP BY Sla;

CREATE VIEW HdkKpi AS
SELECT (SELECT COUNT(*) FROM HdkTkt WHERE Sta NOT IN ('RESOLVED', 'CLOSED')) AS Opn,
       (SELECT COUNT(*) FROM HdkTkt WHERE Sta = 'NEW') AS Nw,
       (SELECT COALESCE(ROUND(100.0 * SUM(CASE WHEN Sla = 'BREACHED' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 1), 0)
        FROM HdkTktLst WHERE CreDat >= CURRENT_TIMESTAMP - 30 DAY) AS Brc,
       (SELECT COALESCE(ROUND(AVG(DATEDIFF('minute', CreDat, ResDat) / 60.0), 1), 0) FROM HdkTkt WHERE ResDat IS NOT NULL
        AND ResDat >= CURRENT_TIMESTAMP - 30 DAY) AS Hrs,
       (SELECT COUNT(*) FROM HdkTkt WHERE Sta = 'OPEN' AND IdeAgt IS NULL) AS Una
FROM DUAL;

CREATE VIEW HdkReqKpi AS
SELECT r.IdeReq,
       (SELECT COUNT(*) FROM HdkTkt t WHERE t.IdeReq = r.IdeReq AND t.Sta IN ('NEW', 'OPEN')) AS Opn,
       (SELECT COUNT(*) FROM HdkTkt t WHERE t.IdeReq = r.IdeReq AND t.Sta = 'PENDING') AS Pnd,
       (SELECT COUNT(*) FROM HdkTkt t WHERE t.IdeReq = r.IdeReq AND t.Sta = 'RESOLVED') AS Rsv
FROM HdkReq r;

-- ------------------------------------------------------
--  Profiles (restriction files profile/<Res>.xml) and demo users
--    agent       works the tickets of its teams
--    supervisor  everything of the agent plus reassignment, critical priority, the knowledge base, the teams and the
--                SLA dashboard
--    requester   customers: only their own tickets
--  Users: alex, blake, casey (agents) and sam (supervisor) with password helpdemo; emma, liam, olivia (requesters)
--  with password deskdemo
-- ------------------------------------------------------
INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (2, 'AGT', 'Agent', 11, 'hd-queue', 'agent', 1);
INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (3, 'SUP', 'Supervisor', 11, 'hd-dashboard', 'supervisor', 1);
INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (4, 'REQ', 'Requester', 11, 'hd-my-tickets', 'requester', 1);
UPDATE AweKey SET KeyVal = 4 WHERE KeyNam = 'ProKey';

INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck) VALUES
 (101, 'alex', '{bcrypt}$2y$10$wDMcRXyerc0e7AomhcktjOgmT/2Uc0xSnz7du5RcYFGOU9JL09ckG', 0, 1, 'en-GB', 'alex@beacon.example', 'Alex Morgan', 2, 11, 'hd-queue', 'agent', 0),
 (102, 'blake', '{bcrypt}$2y$10$wDMcRXyerc0e7AomhcktjOgmT/2Uc0xSnz7du5RcYFGOU9JL09ckG', 0, 1, 'es-ES', 'blake@beacon.example', 'Blake Rivera', 2, 11, 'hd-queue', 'agent', 0),
 (103, 'casey', '{bcrypt}$2y$10$wDMcRXyerc0e7AomhcktjOgmT/2Uc0xSnz7du5RcYFGOU9JL09ckG', 0, 1, 'fr-FR', 'casey@beacon.example', 'Casey Laurent', 2, 11, 'hd-queue', 'agent', 0),
 (104, 'sam', '{bcrypt}$2y$10$wDMcRXyerc0e7AomhcktjOgmT/2Uc0xSnz7du5RcYFGOU9JL09ckG', 0, 1, 'en-GB', 'sam@beacon.example', 'Sam Patel', 3, 11, 'hd-dashboard', 'supervisor', 0),
 (201, 'emma', '{bcrypt}$2y$10$oVIkl3WLDOAleFyL.DTQkO1TRDq.Sg3mzjYHPe6K8PK0zwslLNlQm', 0, 1, 'en-GB', 'emma.clark@northwind.example', 'Emma Clark', 4, 11, 'hd-my-tickets', 'requester', 0),
 (202, 'liam', '{bcrypt}$2y$10$oVIkl3WLDOAleFyL.DTQkO1TRDq.Sg3mzjYHPe6K8PK0zwslLNlQm', 0, 1, 'es-ES', 'liam.walker@contoso.example', 'Liam Walker', 4, 11, 'hd-my-tickets', 'requester', 0),
 (203, 'olivia', '{bcrypt}$2y$10$oVIkl3WLDOAleFyL.DTQkO1TRDq.Sg3mzjYHPe6K8PK0zwslLNlQm', 0, 1, 'fr-FR', 'olivia.reed@fabrikam.example', 'Olivia Reed', 4, 11, 'hd-my-tickets', 'requester', 0);
UPDATE AweKey SET KeyVal = 300 WHERE KeyNam = 'OpeKey';

-- ------------------------------------------------------
--  Master data
-- ------------------------------------------------------
INSERT INTO HdkTeam (IdeTeam, IdePar, Nam, Des) VALUES
 (1, NULL, 'IT Support', 'Employees and systems'), (2, 1, 'Network and Infrastructure', 'VPN, Wi-Fi, servers'),
 (3, 1, 'Applications and Access', 'Software, accounts, permissions'), (4, 1, 'Hardware and Devices', 'Laptops, phones, printers'),
 (5, NULL, 'Customer Care', 'Customers and their accounts'), (6, 5, 'Billing and Accounts', 'Invoices, payments, plans');
INSERT INTO HdkCat (Cat, IdeTeam) VALUES ('NETWORK', 2), ('SOFTWARE', 3), ('HARDWARE', 4), ('ACCESS', 3), ('BILLING', 6), ('OTHER', 5);
INSERT INTO HdkAgt (IdeAgt, IdeOpe, Nam, IdeTeam) VALUES
 (1, 101, 'Alex Morgan', 2), (2, 102, 'Blake Rivera', 3), (3, 103, 'Casey Laurent', 4), (4, 104, 'Sam Patel', 5);
INSERT INTO HdkReq (IdeReq, IdeOpe, Nam, Eml, Org) VALUES
 (1, 201, 'Emma Clark', 'emma.clark@northwind.example', 'Northwind'), (2, 202, 'Liam Walker', 'liam.walker@contoso.example', 'Contoso'),
 (3, 203, 'Olivia Reed', 'olivia.reed@fabrikam.example', 'Fabrikam');
INSERT INTO HdkReq (IdeReq, IdeOpe, Nam, Eml, Org)
SELECT 3 + n, NULL, f.nm || ' ' || l.nm, LOWER(f.nm || '.' || l.nm || '@' || o.nm || '.example'), UPPER(SUBSTRING(o.nm, 1, 1)) || SUBSTRING(o.nm, 2)
FROM UNNEST(SEQUENCE_ARRAY(1, 12, 1)) AS t (n)
 JOIN (VALUES (0, CAST('Noah' AS VARCHAR(200))), (1, 'Mia'), (2, 'Lucas'), (3, 'Zoe'), (4, 'Ethan'), (5, 'Ava')) AS f (i, nm) ON f.i = MOD(n, 6)
 JOIN (VALUES (0, CAST('Parker' AS VARCHAR(200))), (1, 'Hughes'), (2, 'Bell'), (3, 'Cooper'), (4, 'Ward')) AS l (i, nm) ON l.i = MOD(n, 5)
 JOIN (VALUES (0, CAST('northwind' AS VARCHAR(200))), (1, 'contoso'), (2, 'fabrikam'), (3, 'adatum')) AS o (i, nm) ON o.i = MOD(n, 4);
INSERT INTO HdkSvc (IdeSvc, Nam, Sta) VALUES (1, 'Email', 'OPERATIONAL'), (2, 'VPN', 'DEGRADED'), (3, 'Wi-Fi', 'OPERATIONAL'),
 (4, 'File storage', 'OPERATIONAL'), (5, 'Ticket portal', 'OPERATIONAL'), (6, 'Phone system', 'OUTAGE');

-- 600 tickets over 60 days: the age decides the status, the number the priority, the category, the requester and the channel
INSERT INTO HdkTkt (IdeTkt, IdeReq, IdeTeam, Cat, Pri, Sta, Sub, Dsc, IdeAgt, CreDat, UpdDat, ResDat, Chn)
SELECT x.n, x.req, c.IdeTeam, x.cat, x.pri, x.sta, s.nm, s.nm || '. Reported through the support desk; see the conversation for details.',
       CASE WHEN x.sta = 'NEW' THEN NULL ELSE CASE c.IdeTeam WHEN 2 THEN 1 WHEN 3 THEN 2 WHEN 4 THEN 3 ELSE 4 END END,
       x.cre,
       CASE WHEN x.sta IN ('RESOLVED', 'CLOSED') THEN x.cre + x.hrs HOUR ELSE LEAST(x.cre + (1 + MOD(x.n, 12)) HOUR, CURRENT_TIMESTAMP) END,
       CASE WHEN x.sta IN ('RESOLVED', 'CLOSED') THEN x.cre + x.hrs HOUR END,
       CASE MOD(x.n, 4) WHEN 0 THEN 'EMAIL' WHEN 1 THEN 'WEB' WHEN 2 THEN 'WEB' ELSE 'API' END
FROM (SELECT y.n, y.req, y.cat, y.pri, y.cre, y.hrs,
             CASE WHEN y.age > 10 THEN CASE WHEN MOD(y.n, 12) = 0 THEN 'RESOLVED' ELSE 'CLOSED' END
                  WHEN y.age > 3 THEN CASE MOD(y.n, 5) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'OPEN' WHEN 2 THEN 'RESOLVED' WHEN 3 THEN 'CLOSED' ELSE 'OPEN' END
                  WHEN y.age > 0 THEN CASE MOD(y.n, 4) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'NEW' ELSE 'OPEN' END
                  ELSE CASE WHEN MOD(y.n, 3) = 0 THEN 'OPEN' ELSE 'NEW' END END AS sta
      FROM (SELECT n, 1 + MOD(n * 7, 15) AS req, MOD(n * 7, 60) AS age,
                   CASE MOD(n, 6) WHEN 0 THEN 'NETWORK' WHEN 1 THEN 'SOFTWARE' WHEN 2 THEN 'HARDWARE' WHEN 3 THEN 'ACCESS' WHEN 4 THEN 'BILLING' ELSE 'OTHER' END AS cat,
                   CASE MOD(n, 10) WHEN 0 THEN 'CRITICAL' WHEN 1 THEN 'HIGH' WHEN 2 THEN 'HIGH' WHEN 8 THEN 'LOW' WHEN 9 THEN 'LOW' ELSE 'NORMAL' END AS pri,
                   CURRENT_TIMESTAMP - MOD(n * 7, 60) DAY - MOD(n * 13, 24) HOUR AS cre, 1 + MOD(n * 3, 36) AS hrs
            FROM UNNEST(SEQUENCE_ARRAY(1, 600, 1)) AS t (n)) y) x
 JOIN HdkCat c ON c.Cat = x.cat
 JOIN (VALUES (CAST('NETWORK' AS VARCHAR(200)), 0, CAST('VPN disconnects every few minutes' AS VARCHAR(200))), ('NETWORK', 1, 'No Wi-Fi in meeting room 3'), ('NETWORK', 2, 'Slow access to the file server'),
              ('SOFTWARE', 0, 'The CRM freezes when exporting'), ('SOFTWARE', 1, 'Install the design suite on my laptop'), ('SOFTWARE', 2, 'Email client asks for the password again'),
              ('HARDWARE', 0, 'Laptop battery drains in one hour'), ('HARDWARE', 1, 'Printer on floor 2 jams every page'), ('HARDWARE', 2, 'Replace a broken external monitor'),
              ('ACCESS', 0, 'Reset my password'), ('ACCESS', 1, 'Request access to the finance shared drive'), ('ACCESS', 2, 'Two-factor code is not accepted'),
              ('BILLING', 0, 'Invoice shows a wrong amount'), ('BILLING', 1, 'Change the billing address'), ('BILLING', 2, 'Payment taken twice'),
              ('OTHER', 0, 'Question about the support plan'), ('OTHER', 1, 'Feedback about the service'), ('OTHER', 2, 'Request a training session')) AS s (cat, i, nm)
   ON s.cat = x.cat AND s.i = MOD(x.n, 3);

-- Conversation: the requester opens it, the agent answers, some notes, and a closing reply when resolved
INSERT INTO HdkMsg (IdeMsg, IdeTkt, MsgDat, Aut, Typ, Txt)
SELECT t.IdeTkt * 10 + k.k, t.IdeTkt,
       CASE k.k WHEN 1 THEN t.CreDat WHEN 2 THEN t.CreDat + 1 HOUR WHEN 3 THEN t.CreDat + 2 HOUR ELSE t.ResDat END,
       CASE WHEN k.k = 1 THEN r.Nam ELSE a.Nam END,
       CASE k.k WHEN 1 THEN 'REQUESTER' WHEN 3 THEN 'INTERNAL' ELSE 'PUBLIC' END,
       CASE k.k WHEN 1 THEN 'Hello, ' || t.Sub || '. Could you please help?' WHEN 2 THEN 'Thanks for contacting us. We are looking into it and will update you soon.'
                WHEN 3 THEN 'Checked the logs and the monitoring: reproduced, waiting for the vendor.' ELSE 'This is solved now. Please tell us if it happens again.' END
FROM HdkTkt t
 JOIN HdkReq r ON r.IdeReq = t.IdeReq
 LEFT JOIN HdkAgt a ON a.IdeAgt = t.IdeAgt
 JOIN UNNEST(SEQUENCE_ARRAY(1, 4, 1)) AS k (k)
   ON (k.k = 1) OR (k.k = 2 AND t.Sta <> 'NEW') OR (k.k = 3 AND t.Sta <> 'NEW' AND MOD(t.IdeTkt, 2) = 0)
      OR (k.k = 4 AND t.Sta IN ('RESOLVED', 'CLOSED'));

-- 25 articles, five per category: every seventh is a draft
INSERT INTO HdkArt (IdeArt, Cat, Tit, Txt, Pub, Vws, UpdDat)
SELECT a.i, a.cat, a.nm, 'How to: ' || LOWER(a.nm) || '. 1) Check the basics and restart the affected application. 2) Follow the steps of this guide. 3) If the problem remains, open a ticket and mention this article.',
       CASE WHEN MOD(a.i, 7) = 0 THEN 0 ELSE 1 END, 40 + MOD(a.i * 97, 900), CURRENT_TIMESTAMP - MOD(a.i * 5, 90) DAY
FROM (VALUES (1, CAST('NETWORK' AS VARCHAR(200)), CAST('Connect to the VPN' AS VARCHAR(200))), (2, 'NETWORK', 'Fix a slow Wi-Fi connection'), (3, 'NETWORK', 'Map the shared network drive'), (4, 'NETWORK', 'Use the guest Wi-Fi'), (5, 'NETWORK', 'Troubleshoot DNS problems'),
             (6, 'SOFTWARE', 'Install approved software'), (7, 'SOFTWARE', 'Clear the browser cache'), (8, 'SOFTWARE', 'Set up your email on a phone'), (9, 'SOFTWARE', 'Update the CRM client'), (10, 'SOFTWARE', 'Recover an unsaved document'),
             (11, 'HARDWARE', 'Request a replacement laptop'), (12, 'HARDWARE', 'Connect to a printer'), (13, 'HARDWARE', 'Calibrate an external monitor'), (14, 'HARDWARE', 'Extend the battery life'), (15, 'HARDWARE', 'Return old equipment'),
             (16, 'ACCESS', 'Reset your password'), (17, 'ACCESS', 'Set up two-factor authentication'), (18, 'ACCESS', 'Request access to a shared drive'), (19, 'ACCESS', 'Unlock your account'), (20, 'ACCESS', 'Change your security questions'),
             (21, 'BILLING', 'Read your invoice'), (22, 'BILLING', 'Update the billing address'), (23, 'BILLING', 'Choose a payment method'), (24, 'BILLING', 'Ask for a refund'), (25, 'OTHER', 'Contact the support desk')) AS a (i, cat, nm);

INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('TktKey', 600);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('MsgKey', 7000);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('ArtKey', 25);
