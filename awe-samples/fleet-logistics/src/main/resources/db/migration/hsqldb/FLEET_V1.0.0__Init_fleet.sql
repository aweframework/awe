-- ------------------------------------------------------
--  Meridian Freight (AWE sample application): fleet and logistics
--  Schema, read models (views), profiles, demo users, scheduled tasks and demo data. Most rows are generated here
--  with INSERT ... SELECT over a number sequence, so the data is varied but deterministic and relative to the date
--  of the first start. Demo credentials: see the README of the module (sample values, never use them in production).
-- ------------------------------------------------------

-- ------------------------------------------------------
--  Tables
-- ------------------------------------------------------
CREATE TABLE FltReg (
    Reg varchar(10) NOT NULL PRIMARY KEY,           -- Region code (enumerated FltRegion)
    Cit varchar(40) NOT NULL,                       -- Hub city of the region
    Pfx varchar(2)  NOT NULL,                       -- First digits of the postal codes of the region
    Lat decimal(9, 6) NOT NULL,                     -- Latitude of the hub city (map)
    Lon decimal(9, 6) NOT NULL                      -- Longitude of the hub city (map)
);

-- The four stops of the delivery routes of each region, in order (real towns around the hub city)
CREATE TABLE FltRegStp (
    Reg varchar(10)  NOT NULL,                      -- Region code
    Seq int          NOT NULL,                      -- Order in the route
    Loc varchar(60)  NOT NULL,                      -- Town of the stop
    Lat decimal(9, 6) NOT NULL,                     -- Latitude of the stop (map)
    Lon decimal(9, 6) NOT NULL,                     -- Longitude of the stop (map)
    CONSTRAINT pk_FltRegStp PRIMARY KEY (Reg, Seq),
    CONSTRAINT fk_FltRegStp_reg FOREIGN KEY (Reg) REFERENCES FltReg (Reg)
);

-- Road between the stops of each region, in order: precomputed once with OSRM from OpenStreetMap data (c) OpenStreetMap
-- contributors, ODbL. No routing service is called at runtime
CREATE TABLE FltRegGeo (
    Reg varchar(10)  NOT NULL,                      -- Region code
    Seq int          NOT NULL,                      -- Order of the point in the road
    Lat decimal(9, 6) NOT NULL,                     -- Latitude of the point (map)
    Lon decimal(9, 6) NOT NULL,                     -- Longitude of the point (map)
    CONSTRAINT pk_FltRegGeo PRIMARY KEY (Reg, Seq),
    CONSTRAINT fk_FltRegGeo_reg FOREIGN KEY (Reg) REFERENCES FltReg (Reg)
);

CREATE TABLE FltCus (
    IdeCus int          NOT NULL PRIMARY KEY,       -- Customer account
    IdeOpe int          NULL,                       -- Login of the account (ope), empty when it cannot sign in
    Nam    varchar(80)  NOT NULL,                   -- Company name
    Eml    varchar(80)  NULL,
    Tel    varchar(20)  NULL,
    Adr    varchar(120) NULL,
    Cit    varchar(60)  NULL,
    CONSTRAINT fk_FltCus_ope FOREIGN KEY (IdeOpe) REFERENCES ope (IdeOpe),
    CONSTRAINT uk_FltCus_ope UNIQUE (IdeOpe)
);

CREATE TABLE FltDrv (
    IdeDrv int         NOT NULL PRIMARY KEY,        -- Driver
    Nam    varchar(80) NOT NULL,
    Lic    varchar(12) NOT NULL,                    -- Licence number
    Pho    varchar(20) NULL,
    Sta    varchar(10) NOT NULL,                    -- ACTIVE or ON_LEAVE (enumerated FltDriverStatus)
    Reg    varchar(10) NOT NULL                     -- Home region
);

CREATE TABLE FltVeh (
    IdeVeh int         NOT NULL PRIMARY KEY,        -- Vehicle
    Plt    varchar(10) NOT NULL,                    -- Plate
    Typ    varchar(10) NOT NULL,                    -- VAN, TRUCK or HEAVY (enumerated FltVehicleType)
    Cap    int         NOT NULL,                    -- Capacity in kg
    Mil    int         NOT NULL,                    -- Mileage in km
    Sta    varchar(12) NOT NULL,                    -- AVAILABLE, ON_ROUTE or MAINTENANCE (enumerated FltVehicleStatus)
    NxtSvc date        NOT NULL,                    -- Next maintenance
    Reg    varchar(10) NOT NULL                     -- Base region
);

CREATE TABLE FltRte (
    IdeRte int         NOT NULL PRIMARY KEY,        -- Route
    Cod    varchar(10) NOT NULL,
    RteDat date        NOT NULL,
    Reg    varchar(10) NOT NULL,
    IdeVeh int         NULL,                        -- Assigned vehicle (empty until the dispatcher assigns one)
    IdeDrv int         NULL,                        -- Assigned driver
    Sta    varchar(12) NOT NULL,                    -- PLANNED, IN_PROGRESS or COMPLETED (enumerated FltRouteStatus)
    CONSTRAINT fk_FltRte_veh FOREIGN KEY (IdeVeh) REFERENCES FltVeh (IdeVeh) ON DELETE SET NULL,
    CONSTRAINT fk_FltRte_drv FOREIGN KEY (IdeDrv) REFERENCES FltDrv (IdeDrv) ON DELETE SET NULL
);

CREATE TABLE FltShp (
    IdeShp int           NOT NULL PRIMARY KEY,      -- Shipment
    Cod    varchar(12)   NOT NULL,                  -- Tracking code
    IdeCus int           NOT NULL,
    OrgReg varchar(10)   NOT NULL,                  -- Origin region
    DstReg varchar(10)   NOT NULL,                  -- Destination region
    DstPst varchar(5)    NOT NULL,                  -- Destination postal code
    Wgt    decimal(8, 1) NOT NULL,                  -- Weight in kg
    Pri    varchar(10)   NOT NULL,                  -- STANDARD or EXPRESS (enumerated FltPriority)
    Sta    varchar(18)   NOT NULL,                  -- Status (enumerated FltShipmentStatus)
    CreDat date          NOT NULL,                  -- Created on
    Eta    timestamp     NOT NULL,                  -- Estimated arrival
    DelDat timestamp     NULL,                      -- Delivered on
    Fee    decimal(9, 2) NOT NULL,                  -- Price in euros
    CONSTRAINT fk_FltShp_cus FOREIGN KEY (IdeCus) REFERENCES FltCus (IdeCus),
    CONSTRAINT uk_FltShp_cod UNIQUE (Cod)
);
CREATE INDEX FltShpI1 ON FltShp (IdeCus);
CREATE INDEX FltShpI2 ON FltShp (Sta);

CREATE TABLE FltStp (
    IdeStp int          NOT NULL PRIMARY KEY,       -- Stop of a route
    IdeRte int          NOT NULL,
    Seq    int          NOT NULL,                   -- Order in the route
    Loc    varchar(60)  NOT NULL,
    Lat    decimal(9, 6) NOT NULL,                  -- Latitude of the stop (map)
    Lon    decimal(9, 6) NOT NULL,                  -- Longitude of the stop (map)
    IdeShp int          NULL,                       -- Shipment delivered or picked up at the stop
    CONSTRAINT fk_FltStp_rte FOREIGN KEY (IdeRte) REFERENCES FltRte (IdeRte)
);

CREATE TABLE FltEvt (
    IdeEvt int          NOT NULL PRIMARY KEY,       -- Tracking event
    IdeShp int          NOT NULL,
    EvtDat timestamp    NOT NULL,
    Sta    varchar(18)  NOT NULL,                   -- Status reached (enumerated FltShipmentStatus)
    Loc    varchar(60)  NOT NULL,
    CONSTRAINT fk_FltEvt_shp FOREIGN KEY (IdeShp) REFERENCES FltShp (IdeShp)
);
CREATE INDEX FltEvtI1 ON FltEvt (IdeShp);

CREATE TABLE FltInv (
    IdeInv int           NOT NULL PRIMARY KEY,
    IdeCus int           NOT NULL,
    InvDat date          NOT NULL,
    Amt    decimal(11, 2) NOT NULL,
    Sta    varchar(10)   NOT NULL,                  -- PAID, PENDING or OVERDUE (enumerated FltInvoiceStatus)
    CONSTRAINT fk_FltInv_cus FOREIGN KEY (IdeCus) REFERENCES FltCus (IdeCus)
);

-- Quote requests of the public form: anybody can add one, only the dispatchers read them
CREATE TABLE FltQuo (
    IdeQuo int          NOT NULL PRIMARY KEY,
    QuoDat timestamp    NOT NULL,
    Nam    varchar(80)  NOT NULL,
    Eml    varchar(80)  NOT NULL,
    Tel    varchar(20)  NULL,
    OrgReg varchar(10)  NOT NULL,
    DstReg varchar(10)  NOT NULL,
    Wgt    decimal(8, 1) NOT NULL,
    Nte    varchar(400) NULL,
    Sta    varchar(10)  NOT NULL,                   -- PENDING or HANDLED (enumerated FltRequestStatus)
    CONSTRAINT ck_FltQuo_name CHECK (CHAR_LENGTH(TRIM(Nam)) >= 2),
    CONSTRAINT ck_FltQuo_email CHECK (Eml LIKE '%_@_%._%'),
    CONSTRAINT ck_FltQuo_region CHECK (OrgReg IN ('NORTH', 'SOUTH', 'EAST', 'WEST', 'CENTRAL')
        AND DstReg IN ('NORTH', 'SOUTH', 'EAST', 'WEST', 'CENTRAL')),
    CONSTRAINT ck_FltQuo_weight CHECK (Wgt > 0 AND Wgt <= 30000),
    CONSTRAINT ck_FltQuo_status CHECK (Sta IN ('PENDING', 'HANDLED'))
);

-- Pickup requests of the customers
CREATE TABLE FltPck (
    IdePck int          NOT NULL PRIMARY KEY,
    IdeCus int          NOT NULL,
    PckDat date         NOT NULL,                   -- Requested pickup date
    Adr    varchar(120) NOT NULL,
    Pcs    int          NOT NULL,                   -- Number of pieces
    Wgt    decimal(8, 1) NOT NULL,
    Nte    varchar(400) NULL,
    Sta    varchar(10)  NOT NULL,                   -- PENDING, HANDLED or CANCELLED (enumerated FltRequestStatus)
    CONSTRAINT fk_FltPck_cus FOREIGN KEY (IdeCus) REFERENCES FltCus (IdeCus)
);

-- ------------------------------------------------------
--  Read models: the lists, the dashboard and the public tracking read these views
-- ------------------------------------------------------
-- Customer account of the connected user (base of the "my shipments" filter)
CREATE VIEW FltSelfCus AS
SELECT c.IdeCus, TRIM(u.l1_nom) AS Usr
FROM FltCus c
         JOIN ope u ON u.IdeOpe = c.IdeOpe;

CREATE VIEW FltShpLst AS
SELECT s.IdeShp, s.Cod, s.IdeCus, c.Nam AS CusNam, s.OrgReg, s.DstReg, s.DstPst, r.Cit AS DstCit, s.Wgt, s.Pri, s.Sta,
       s.CreDat, s.Eta, s.DelDat, s.Fee,
       CASE s.Sta WHEN 'DELIVERED' THEN 'success' WHEN 'DELAYED' THEN 'danger' WHEN 'CREATED' THEN 'neutral'
                  ELSE 'info' END AS Ton
FROM FltShp s
         JOIN FltCus c ON c.IdeCus = s.IdeCus
         JOIN FltReg r ON r.Reg = s.DstReg;

CREATE VIEW FltEvtLst AS
SELECT e.IdeEvt, e.IdeShp, s.IdeCus, s.Cod, e.EvtDat, e.Sta, e.Loc
FROM FltEvt e
         JOIN FltShp s ON s.IdeShp = e.IdeShp;

-- Public tracking: the query asks for the code AND the destination postal code, and returns the timeline of that
-- shipment only (no customer, no weight, no price)
CREATE VIEW FltTrk AS
SELECT s.Cod, s.DstPst, e.EvtDat, e.Sta, e.Loc
FROM FltShp s
         JOIN FltEvt e ON e.IdeShp = s.IdeShp;

-- Public tracking points: the origin hub, the central hub (Madrid, when the shipment does not start or end there) and the
-- destination, numbered in order. The query that reads it asks for the code AND the postal code, like FltTrk
CREATE VIEW FltTrkPts AS
SELECT s.Cod, s.DstPst, 1 AS Seq, 'Hub ' || o.Cit AS Loc, o.Lat, o.Lon
FROM FltShp s
         JOIN FltReg o ON o.Reg = s.OrgReg
UNION ALL
SELECT s.Cod, s.DstPst, 2, 'Central hub, ' || h.Cit, h.Lat, h.Lon
FROM FltShp s
         JOIN FltReg h ON h.Reg = 'CENTRAL'
WHERE s.OrgReg <> 'CENTRAL' AND s.DstReg <> 'CENTRAL'
UNION ALL
SELECT s.Cod, s.DstPst,
       CASE WHEN s.OrgReg = 'CENTRAL' OR s.DstReg = 'CENTRAL' THEN 2 ELSE 3 END,
       'Delivery, ' || d.Cit,
       CAST(d.Lat + (MOD(CAST(s.DstPst AS INTEGER), 50) - 25) * 0.004 AS DECIMAL(9, 6)),
       CAST(d.Lon + (MOD(CAST(s.DstPst AS INTEGER), 37) - 18) * 0.004 AS DECIMAL(9, 6))
FROM FltShp s
         JOIN FltReg d ON d.Reg = s.DstReg;

CREATE VIEW FltVehLst AS
SELECT v.IdeVeh, v.Plt, v.Typ, v.Cap, v.Mil, v.Sta, v.NxtSvc, v.Reg,
       CASE v.Sta WHEN 'AVAILABLE' THEN 'success' WHEN 'ON_ROUTE' THEN 'info' ELSE 'warning' END AS Ton,
       CASE WHEN v.NxtSvc < TRUNC(CURRENT_DATE) THEN 'danger'
            WHEN v.NxtSvc < TRUNC(CURRENT_DATE) + 14 DAY THEN 'warning' ELSE 'success' END AS SvcTon,
       CASE WHEN v.NxtSvc < TRUNC(CURRENT_DATE) THEN 'OVERDUE'
            WHEN v.NxtSvc < TRUNC(CURRENT_DATE) + 14 DAY THEN 'DUE_SOON' ELSE 'OK' END AS SvcSta
FROM FltVeh v;

CREATE VIEW FltRteLst AS
SELECT r.IdeRte, r.Cod, r.RteDat, r.Reg, r.IdeVeh, r.IdeDrv, r.Sta, v.Plt, d.Nam AS DrvNam,
       CASE r.Sta WHEN 'COMPLETED' THEN 'success' WHEN 'IN_PROGRESS' THEN 'info' ELSE 'neutral' END AS Ton
FROM FltRte r
         LEFT JOIN FltVeh v ON v.IdeVeh = r.IdeVeh
         LEFT JOIN FltDrv d ON d.IdeDrv = r.IdeDrv;

-- The routes of today, for the assignment grid
CREATE VIEW FltRteToday AS
SELECT * FROM FltRteLst WHERE RteDat = TRUNC(CURRENT_DATE);

-- Routes and their stops as a tree: the stops have the route as parent (ids of the stops start at 100000)
CREATE VIEW FltRteTree AS
SELECT r.IdeRte AS Ide, CAST(NULL AS INTEGER) AS Par, 0 AS IsLeaf, r.Cod AS Nam, r.RteDat AS RteDat, r.Reg AS Reg,
       v.Plt AS Plt, d.Nam AS DrvNam, r.Sta AS Sta, 0 AS Seq
FROM FltRte r
         LEFT JOIN FltVeh v ON v.IdeVeh = r.IdeVeh
         LEFT JOIN FltDrv d ON d.IdeDrv = r.IdeDrv
UNION ALL
SELECT 100000 + p.IdeStp, p.IdeRte, 1, p.Loc, CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR(10)),
       CAST(NULL AS VARCHAR(10)), CAST(NULL AS VARCHAR(80)), CAST(NULL AS VARCHAR(12)), p.Seq
FROM FltStp p;

-- Stops of a route for the map of the route planner: the key is the id of the row of the tree (the route, or one of
-- its stops, which draws the route it belongs to)
CREATE VIEW FltRtePts AS
SELECT p.IdeRte AS Ide, p.Seq, p.Loc, p.Lat, p.Lon
FROM FltStp p
UNION ALL
SELECT 100000 + p.IdeStp, q.Seq, q.Loc, q.Lat, q.Lon
FROM FltStp p
         JOIN FltStp q ON q.IdeRte = p.IdeRte;

-- Road of a route for the map of the route planner (same key as FltRtePts): the precomputed road of its region
CREATE VIEW FltRteGeo AS
SELECT r.IdeRte AS Ide, g.Seq, g.Lat, g.Lon
FROM FltRte r
         JOIN FltRegGeo g ON g.Reg = r.Reg
UNION ALL
SELECT 100000 + p.IdeStp, g.Seq, g.Lat, g.Lon
FROM FltStp p
         JOIN FltRte r ON r.IdeRte = p.IdeRte
         JOIN FltRegGeo g ON g.Reg = r.Reg;

-- Vehicles on route for the dashboard map. The position is the second stop of the last route assigned to the vehicle
-- (the stop it is heading to) and a vehicle without route is placed at its base city; a small offset per vehicle keeps the
-- markers of vehicles that share a stop apart
CREATE VIEW FltVehMap AS
SELECT v.IdeVeh AS Ide, v.Plt, d.Nam AS DrvNam, r.Cod AS RteCod,
       CAST(COALESCE(p.Lat, g.Lat) + (MOD(v.IdeVeh * 7, 11) - 5) * 0.012 AS DECIMAL(9, 6)) AS Lat,
       CAST(COALESCE(p.Lon, g.Lon) + (MOD(v.IdeVeh * 5, 9) - 4) * 0.015 AS DECIMAL(9, 6)) AS Lon,
       v.Plt || ' - ' || COALESCE(d.Nam, '') AS Lbl,
       (SELECT COUNT(*) FROM FltStp x WHERE x.IdeRte = r.IdeRte AND x.IdeShp IS NOT NULL) AS Cnt,
       (SELECT COALESCE(SUM(s.Wgt), 0) FROM FltStp x JOIN FltShp s ON s.IdeShp = x.IdeShp WHERE x.IdeRte = r.IdeRte) AS Wgt
FROM FltVeh v
         JOIN FltReg g ON g.Reg = v.Reg
         LEFT JOIN FltRte r ON r.IdeRte = (SELECT MAX(x.IdeRte) FROM FltRte x WHERE x.IdeVeh = v.IdeVeh)
         LEFT JOIN FltDrv d ON d.IdeDrv = r.IdeDrv
         LEFT JOIN FltStp p ON p.IdeRte = r.IdeRte AND p.Seq = 2
WHERE v.Sta = 'ON_ROUTE';

CREATE VIEW FltInvLst AS
SELECT i.IdeInv, i.IdeCus, i.InvDat, i.Amt, i.Sta,
       CASE i.Sta WHEN 'PAID' THEN 'success' WHEN 'PENDING' THEN 'warning' ELSE 'danger' END AS Ton
FROM FltInv i;

CREATE VIEW FltPckLst AS
SELECT p.IdePck, p.IdeCus, c.Nam AS CusNam, p.PckDat, p.Adr, p.Pcs, p.Wgt, p.Nte, p.Sta,
       CASE p.Sta WHEN 'HANDLED' THEN 'success' WHEN 'CANCELLED' THEN 'danger' ELSE 'warning' END AS Ton
FROM FltPck p
         JOIN FltCus c ON c.IdeCus = p.IdeCus;

-- Dashboard
CREATE VIEW FltDayDel AS
SELECT TRUNC(DelDat) AS Dat, COUNT(*) AS Cnt
FROM FltShp
WHERE DelDat IS NOT NULL
GROUP BY TRUNC(DelDat);

CREATE VIEW FltWeekOtp AS
SELECT TO_CHAR(DelDat, 'IYYY-"W"IW') AS Wk,
       ROUND(100.0 * SUM(CASE WHEN DelDat <= Eta THEN 1 ELSE 0 END) / COUNT(*), 1) AS Otp
FROM FltShp
WHERE DelDat IS NOT NULL
GROUP BY TO_CHAR(DelDat, 'IYYY-"W"IW');

CREATE VIEW FltRegSta AS
SELECT DstReg AS Reg,
       SUM(CASE WHEN Sta = 'DELIVERED' THEN 1 ELSE 0 END) AS Del,
       SUM(CASE WHEN Sta IN ('PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY') THEN 1 ELSE 0 END) AS Trn,
       SUM(CASE WHEN Sta = 'DELAYED' THEN 1 ELSE 0 END) AS Dly,
       SUM(CASE WHEN Sta = 'CREATED' THEN 1 ELSE 0 END) AS Cre
FROM FltShp
GROUP BY DstReg;

CREATE VIEW FltVehCnt AS
SELECT Sta, COUNT(*) AS Cnt
FROM FltVeh
GROUP BY Sta;

CREATE VIEW FltKpi AS
SELECT (SELECT COUNT(*) FROM FltShp WHERE DelDat >= TRUNC(CURRENT_DATE) - 7 DAY) AS Del7,
       (SELECT COUNT(*) FROM FltShp WHERE DelDat >= TRUNC(CURRENT_DATE) - 14 DAY
                                      AND DelDat < TRUNC(CURRENT_DATE) - 7 DAY) AS Del14,
       (SELECT COALESCE(ROUND(100.0 * SUM(CASE WHEN DelDat <= Eta THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 1), 0)
        FROM FltShp WHERE DelDat >= TRUNC(CURRENT_DATE) - 30 DAY) AS Otp,
       (SELECT COUNT(*) FROM FltShp WHERE Sta = 'DELAYED') AS Dly,
       (SELECT COUNT(*) FROM FltShp WHERE Sta NOT IN ('DELIVERED')) AS Opn,
       (SELECT COUNT(*) FROM FltVeh WHERE Sta = 'ON_ROUTE') AS Rou,
       (SELECT COUNT(*) FROM FltVeh WHERE NxtSvc < TRUNC(CURRENT_DATE) + 14 DAY) AS Svc
FROM DUAL;

-- Key figures of the customer area (one row per account)
CREATE VIEW FltCusKpi AS
SELECT c.IdeCus,
       (SELECT COUNT(*) FROM FltShp s WHERE s.IdeCus = c.IdeCus AND s.Sta NOT IN ('DELIVERED')) AS Opn,
       (SELECT COUNT(*) FROM FltShp s WHERE s.IdeCus = c.IdeCus AND s.Sta = 'DELAYED') AS Dly,
       (SELECT COUNT(*) FROM FltShp s WHERE s.IdeCus = c.IdeCus AND s.DelDat >= TRUNC(CURRENT_DATE) - 30 DAY) AS Del30,
       (SELECT COALESCE(SUM(i.Amt), 0) FROM FltInv i WHERE i.IdeCus = c.IdeCus AND i.Sta <> 'PAID') AS Due
FROM FltCus c;

-- Notifications of the scheduled task FltFlagLate. Late = in transit with the estimated arrival already past. Each view
-- returns the nine columns of AweNot (the notifier table) and no row when nothing is late, so the task only notifies
-- when it flags something. Ids are the last one of AweNot plus the account (or 1), so they never repeat
CREATE VIEW FltNotDsp AS
SELECT (SELECT COALESCE(MAX(Ide), 0) FROM AweNot) + 1 AS Ide, b.Ide AS IdeSub, 'WARNING' AS Typ,
       'exclamation-triangle' AS Ico, 'Late shipments' AS Nom,
       CAST(l.n AS VARCHAR(6)) || ' shipments are late and were flagged as delayed.' AS Des, 'fleet-shipments' AS Scr,
       'FLT-LATE' AS Cod, CURRENT_TIMESTAMP AS Dat
FROM (SELECT COUNT(*) AS n FROM FltShp WHERE Sta = 'IN_TRANSIT' AND Eta < CURRENT_TIMESTAMP) l
         JOIN AweSub b ON b.Acr = 'FLEET'
WHERE l.n > 0;

CREATE VIEW FltNotCus AS
SELECT (SELECT COALESCE(MAX(Ide), 0) FROM AweNot) + l.IdeCus AS Ide, b.Ide AS IdeSub, 'WARNING' AS Typ,
       'exclamation-triangle' AS Ico, 'Shipments delayed' AS Nom,
       CAST(l.n AS VARCHAR(6)) || ' of your shipments are running late. We are sorry.' AS Des,
       'fleet-my-shipments' AS Scr, 'FLT-LATE' AS Cod, CURRENT_TIMESTAMP AS Dat
FROM (SELECT IdeCus, COUNT(*) AS n FROM FltShp WHERE Sta = 'IN_TRANSIT' AND Eta < CURRENT_TIMESTAMP
      GROUP BY IdeCus) l
         JOIN AweSub b ON b.Acr = 'CUS' || CAST(l.IdeCus AS VARCHAR(3));

CREATE VIEW FltEvtLate AS
SELECT (SELECT COALESCE(MAX(IdeEvt), 0) FROM FltEvt) + s.IdeShp AS IdeEvt, s.IdeShp AS IdeShp,
       CURRENT_TIMESTAMP AS EvtDat, 'DELAYED' AS Sta, 'Hub ' || r.Cit AS Loc
FROM FltShp s
         JOIN FltReg r ON r.Reg = s.DstReg
WHERE s.Sta = 'IN_TRANSIT' AND s.Eta < CURRENT_TIMESTAMP;

-- ------------------------------------------------------
--  Profiles: the restriction file (Res) is application/fleet/profile/<Res>.xml
--    dispatcher  operations staff
--    customer    customers: only their own shipments, pickups and invoices
-- ------------------------------------------------------
INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act)
VALUES (2, 'DSP', 'Dispatcher', 11, 'fleet-dashboard', 'dispatcher', 1);
INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act)
VALUES (3, 'CUS', 'Customer', 11, 'fleet-my-shipments', 'customer', 1);
UPDATE AweKey SET KeyVal = 3 WHERE KeyNam = 'ProKey';

-- Demo users: dispatchers maria and jorge (password fleetdemo); customers acme, globex and initech (password shipdemo)
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (101, 'maria', '{bcrypt}$2y$10$qOUAorhq2P0j03xiGPaAMumOniLfjZMookn6HPIYXfSB23buDxoGO', 0, 1, 'en-GB',
        'maria@meridian.example', 'Maria Torres', 2, 11, 'fleet-dashboard', 'dispatcher', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (102, 'jorge', '{bcrypt}$2y$10$qOUAorhq2P0j03xiGPaAMumOniLfjZMookn6HPIYXfSB23buDxoGO', 0, 1, 'es-ES',
        'jorge@meridian.example', 'Jorge Vidal', 2, 11, 'fleet-dashboard', 'dispatcher', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (201, 'acme', '{bcrypt}$2y$10$zNFyaWyul23DOMNjissePeDrCCopwCcI65HgnSOVSXBFjkoGl0Oiq', 0, 1, 'en-GB',
        'logistics@acme.example', 'Acme Logistics', 3, 11, 'fleet-my-shipments', 'customer', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (202, 'globex', '{bcrypt}$2y$10$zNFyaWyul23DOMNjissePeDrCCopwCcI65HgnSOVSXBFjkoGl0Oiq', 0, 1, 'es-ES',
        'envios@globex.example', 'Globex Iberia', 3, 11, 'fleet-my-shipments', 'customer', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (203, 'initech', '{bcrypt}$2y$10$zNFyaWyul23DOMNjissePeDrCCopwCcI65HgnSOVSXBFjkoGl0Oiq', 0, 1, 'fr-FR',
        'logistique@initech.example', 'Initech France', 3, 11, 'fleet-my-shipments', 'customer', 0);
UPDATE AweKey SET KeyVal = 300 WHERE KeyNam = 'OpeKey';

-- ------------------------------------------------------
--  Master data
-- ------------------------------------------------------
INSERT INTO FltReg (Reg, Cit, Pfx, Lat, Lon) VALUES
    ('NORTH', 'Bilbao', '48', 43.263000, -2.935000), ('SOUTH', 'Seville', '41', 37.389100, -5.984500),
    ('EAST', 'Valencia', '46', 39.469900, -0.376300), ('WEST', 'Vigo', '36', 42.240600, -8.720700),
    ('CENTRAL', 'Madrid', '28', 40.416800, -3.703800);

-- Real stops of the delivery routes of each region (towns near the hub city), in order
INSERT INTO FltRegStp (Reg, Seq, Loc, Lat, Lon) VALUES
    ('NORTH', 1, 'Getxo', 43.3560, -3.0110), ('NORTH', 2, 'Barakaldo', 43.2960, -2.9890),
    ('NORTH', 3, 'Galdakao', 43.2300, -2.8420), ('NORTH', 4, 'Durango', 43.1700, -2.6310),
    ('SOUTH', 1, 'Alcalá de Guadaíra', 37.3380, -5.8390), ('SOUTH', 2, 'Carmona', 37.4710, -5.6460),
    ('SOUTH', 3, 'Utrera', 37.1850, -5.7810), ('SOUTH', 4, 'Dos Hermanas', 37.2830, -5.9210),
    ('EAST', 1, 'Paterna', 39.5030, -0.4400), ('EAST', 2, 'Torrent', 39.4370, -0.4650),
    ('EAST', 3, 'Alzira', 39.1510, -0.4350), ('EAST', 4, 'Gandia', 38.9670, -0.1810),
    ('WEST', 1, 'Pontevedra', 42.4310, -8.6440), ('WEST', 2, 'Redondela', 42.2830, -8.6090),
    ('WEST', 3, 'O Porriño', 42.1610, -8.6200), ('WEST', 4, 'Baiona', 42.1180, -8.8500),
    ('CENTRAL', 1, 'Alcalá de Henares', 40.4820, -3.3640), ('CENTRAL', 2, 'Torrejón de Ardoz', 40.4590, -3.4800),
    ('CENTRAL', 3, 'Getafe', 40.3050, -3.7330), ('CENTRAL', 4, 'Móstoles', 40.3220, -3.8650);

-- Road through the four stops of each region (OSRM over OpenStreetMap data, (c) OpenStreetMap contributors, ODbL; simplified)
INSERT INTO FltRegGeo (Reg, Seq, Lat, Lon) VALUES
    ('NORTH', 1, 43.35599, -3.01102), ('NORTH', 2, 43.35659, -3.01299), ('NORTH', 3, 43.35766, -3.00947), ('NORTH', 4, 43.35930, -3.00659),
    ('NORTH', 5, 43.36168, -3.00468), ('NORTH', 6, 43.36241, -3.00255), ('NORTH', 7, 43.36014, -3.00095), ('NORTH', 8, 43.35465, -2.99971),
    ('NORTH', 9, 43.34989, -2.99737), ('NORTH', 10, 43.34709, -2.99381), ('NORTH', 11, 43.34576, -2.99317), ('NORTH', 12, 43.34425, -2.99378),
    ('NORTH', 13, 43.34172, -2.99745), ('NORTH', 14, 43.33373, -3.00135), ('NORTH', 15, 43.33280, -3.00117), ('NORTH', 16, 43.31665, -2.96952),
    ('NORTH', 17, 43.30962, -2.96235), ('NORTH', 18, 43.30539, -2.96107), ('NORTH', 19, 43.30394, -2.96138), ('NORTH', 20, 43.29681, -2.96821),
    ('NORTH', 21, 43.29435, -2.98003), ('NORTH', 22, 43.29199, -2.98595), ('NORTH', 23, 43.29485, -2.98883), ('NORTH', 24, 43.29572, -2.98942),
    ('NORTH', 25, 43.29617, -2.98890), ('NORTH', 26, 43.29574, -2.98837), ('NORTH', 27, 43.29534, -2.98900), ('NORTH', 28, 43.29558, -2.99073),
    ('NORTH', 29, 43.29422, -2.98925), ('NORTH', 30, 43.29106, -2.98834), ('NORTH', 31, 43.29080, -2.98589), ('NORTH', 32, 43.28411, -2.98701),
    ('NORTH', 33, 43.28133, -2.98567), ('NORTH', 34, 43.27758, -2.98063), ('NORTH', 35, 43.27464, -2.97807), ('NORTH', 36, 43.27372, -2.96935),
    ('NORTH', 37, 43.26069, -2.95858), ('NORTH', 38, 43.25890, -2.95449), ('NORTH', 39, 43.25658, -2.95266), ('NORTH', 40, 43.25520, -2.94975),
    ('NORTH', 41, 43.25330, -2.94844), ('NORTH', 42, 43.25169, -2.94546), ('NORTH', 43, 43.24815, -2.94199), ('NORTH', 44, 43.24749, -2.94001),
    ('NORTH', 45, 43.24783, -2.93613), ('NORTH', 46, 43.24698, -2.93373), ('NORTH', 47, 43.24235, -2.92810), ('NORTH', 48, 43.23897, -2.92576),
    ('NORTH', 49, 43.23762, -2.92106), ('NORTH', 50, 43.23336, -2.91544), ('NORTH', 51, 43.23351, -2.90701), ('NORTH', 52, 43.23098, -2.88638),
    ('NORTH', 53, 43.23116, -2.87161), ('NORTH', 54, 43.23484, -2.85928), ('NORTH', 55, 43.23595, -2.84932), ('NORTH', 56, 43.23428, -2.84235),
    ('NORTH', 57, 43.23052, -2.84251), ('NORTH', 58, 43.23062, -2.84189), ('NORTH', 59, 43.22978, -2.84221), ('NORTH', 60, 43.22893, -2.84086),
    ('NORTH', 61, 43.23025, -2.83662), ('NORTH', 62, 43.22935, -2.83458), ('NORTH', 63, 43.23201, -2.82764), ('NORTH', 64, 43.23193, -2.82508),
    ('NORTH', 65, 43.23114, -2.82058), ('NORTH', 66, 43.22897, -2.81890), ('NORTH', 67, 43.22673, -2.81989), ('NORTH', 68, 43.22739, -2.82079),
    ('NORTH', 69, 43.22904, -2.82073), ('NORTH', 70, 43.23240, -2.81461), ('NORTH', 71, 43.23519, -2.80775), ('NORTH', 72, 43.23579, -2.80202),
    ('NORTH', 73, 43.23548, -2.79925), ('NORTH', 74, 43.23160, -2.78891), ('NORTH', 75, 43.23184, -2.77751), ('NORTH', 76, 43.23040, -2.75182),
    ('NORTH', 77, 43.22709, -2.74412), ('NORTH', 78, 43.22272, -2.72872), ('NORTH', 79, 43.22049, -2.72651), ('NORTH', 80, 43.21531, -2.72456),
    ('NORTH', 81, 43.21294, -2.72213), ('NORTH', 82, 43.21147, -2.71854), ('NORTH', 83, 43.21000, -2.70976), ('NORTH', 84, 43.20426, -2.70253),
    ('NORTH', 85, 43.20066, -2.69537), ('NORTH', 86, 43.19802, -2.69306), ('NORTH', 87, 43.19247, -2.69132), ('NORTH', 88, 43.18968, -2.68786),
    ('NORTH', 89, 43.18800, -2.68161), ('NORTH', 90, 43.18647, -2.66760), ('NORTH', 91, 43.18455, -2.65895), ('NORTH', 92, 43.17967, -2.64390),
    ('NORTH', 93, 43.17724, -2.64293), ('NORTH', 94, 43.17564, -2.64384), ('NORTH', 95, 43.17536, -2.63108), ('NORTH', 96, 43.17294, -2.62439),
    ('NORTH', 97, 43.17254, -2.61993), ('NORTH', 98, 43.17062, -2.61911), ('NORTH', 99, 43.16916, -2.61992), ('NORTH', 100, 43.16902, -2.62580),
    ('NORTH', 101, 43.16995, -2.63102), ('SOUTH', 1, 37.33801, -5.83899), ('SOUTH', 2, 37.33740, -5.83815), ('SOUTH', 3, 37.33898, -5.83886),
    ('SOUTH', 4, 37.34154, -5.82972), ('SOUTH', 5, 37.34748, -5.82467), ('SOUTH', 6, 37.35070, -5.81879), ('SOUTH', 7, 37.35222, -5.80973),
    ('SOUTH', 8, 37.35846, -5.79110), ('SOUTH', 9, 37.36453, -5.77609), ('SOUTH', 10, 37.36626, -5.76696), ('SOUTH', 11, 37.37126, -5.76768),
    ('SOUTH', 12, 37.37678, -5.76972), ('SOUTH', 13, 37.38008, -5.76899), ('SOUTH', 14, 37.38253, -5.76705), ('SOUTH', 15, 37.38736, -5.76005),
    ('SOUTH', 16, 37.38878, -5.75402), ('SOUTH', 17, 37.39205, -5.74639), ('SOUTH', 18, 37.39372, -5.73673), ('SOUTH', 19, 37.39836, -5.73245),
    ('SOUTH', 20, 37.40070, -5.72755), ('SOUTH', 21, 37.40500, -5.72521), ('SOUTH', 22, 37.40718, -5.72262), ('SOUTH', 23, 37.40820, -5.71957),
    ('SOUTH', 24, 37.40816, -5.71604), ('SOUTH', 25, 37.40331, -5.70684), ('SOUTH', 26, 37.40820, -5.70480), ('SOUTH', 27, 37.41138, -5.70501),
    ('SOUTH', 28, 37.41319, -5.70411), ('SOUTH', 29, 37.42203, -5.69413), ('SOUTH', 30, 37.42837, -5.68825), ('SOUTH', 31, 37.43815, -5.67636),
    ('SOUTH', 32, 37.44526, -5.67140), ('SOUTH', 33, 37.44870, -5.66656), ('SOUTH', 34, 37.46284, -5.65406), ('SOUTH', 35, 37.46440, -5.65118),
    ('SOUTH', 36, 37.46818, -5.64807), ('SOUTH', 37, 37.47059, -5.64366), ('SOUTH', 38, 37.47032, -5.64486), ('SOUTH', 39, 37.47116, -5.64514),
    ('SOUTH', 40, 37.47068, -5.64623), ('SOUTH', 41, 37.47001, -5.64508), ('SOUTH', 42, 37.46938, -5.64569), ('SOUTH', 43, 37.46525, -5.65455),
    ('SOUTH', 44, 37.46846, -5.65987), ('SOUTH', 45, 37.47019, -5.67865), ('SOUTH', 46, 37.47316, -5.68723), ('SOUTH', 47, 37.47113, -5.68973),
    ('SOUTH', 48, 37.46943, -5.69616), ('SOUTH', 49, 37.47000, -5.70882), ('SOUTH', 50, 37.44429, -5.78794), ('SOUTH', 51, 37.43125, -5.81858),
    ('SOUTH', 52, 37.43210, -5.83741), ('SOUTH', 53, 37.43329, -5.84052), ('SOUTH', 54, 37.43292, -5.84479), ('SOUTH', 55, 37.43122, -5.84575),
    ('SOUTH', 56, 37.42782, -5.84289), ('SOUTH', 57, 37.42159, -5.84318), ('SOUTH', 58, 37.41732, -5.84520), ('SOUTH', 59, 37.40793, -5.85217),
    ('SOUTH', 60, 37.40130, -5.85557), ('SOUTH', 61, 37.39320, -5.85810), ('SOUTH', 62, 37.38142, -5.85995), ('SOUTH', 63, 37.37504, -5.86239),
    ('SOUTH', 64, 37.36634, -5.86880), ('SOUTH', 65, 37.35567, -5.88129), ('SOUTH', 66, 37.34869, -5.88599), ('SOUTH', 67, 37.34228, -5.88798),
    ('SOUTH', 68, 37.33296, -5.88812), ('SOUTH', 69, 37.32724, -5.88948), ('SOUTH', 70, 37.32298, -5.89215), ('SOUTH', 71, 37.32008, -5.89527),
    ('SOUTH', 72, 37.31869, -5.89957), ('SOUTH', 73, 37.31676, -5.89974), ('SOUTH', 74, 37.31516, -5.89557), ('SOUTH', 75, 37.30647, -5.88644),
    ('SOUTH', 76, 37.29675, -5.87885), ('SOUTH', 77, 37.28724, -5.87308), ('SOUTH', 78, 37.27963, -5.86468), ('SOUTH', 79, 37.26827, -5.85687),
    ('SOUTH', 80, 37.25589, -5.84565), ('SOUTH', 81, 37.24647, -5.84016), ('SOUTH', 82, 37.23156, -5.82322), ('SOUTH', 83, 37.22354, -5.81954),
    ('SOUTH', 84, 37.20976, -5.80454), ('SOUTH', 85, 37.20538, -5.80209), ('SOUTH', 86, 37.20032, -5.79790), ('SOUTH', 87, 37.19785, -5.79735),
    ('SOUTH', 88, 37.19775, -5.79606), ('SOUTH', 89, 37.18659, -5.78721), ('SOUTH', 90, 37.18521, -5.78451), ('SOUTH', 91, 37.18444, -5.77936),
    ('SOUTH', 92, 37.18521, -5.78451), ('SOUTH', 93, 37.18686, -5.78724), ('SOUTH', 94, 37.19849, -5.79687), ('SOUTH', 95, 37.20988, -5.80439),
    ('SOUTH', 96, 37.22404, -5.81966), ('SOUTH', 97, 37.23167, -5.82304), ('SOUTH', 98, 37.24657, -5.84000), ('SOUTH', 99, 37.25561, -5.84521),
    ('SOUTH', 100, 37.26794, -5.85638), ('SOUTH', 101, 37.27975, -5.86456), ('SOUTH', 102, 37.28811, -5.87354), ('SOUTH', 103, 37.29705, -5.87884),
    ('SOUTH', 104, 37.30841, -5.88741), ('SOUTH', 105, 37.30846, -5.88845), ('SOUTH', 106, 37.30732, -5.88894), ('SOUTH', 107, 37.30057, -5.89892),
    ('SOUTH', 108, 37.29724, -5.90571), ('SOUTH', 109, 37.29440, -5.90954), ('SOUTH', 110, 37.29034, -5.91293), ('SOUTH', 111, 37.28880, -5.91579),
    ('SOUTH', 112, 37.28372, -5.91560), ('SOUTH', 113, 37.28379, -5.91769), ('SOUTH', 114, 37.28260, -5.92028), ('SOUTH', 115, 37.28313, -5.92085),
    ('EAST', 1, 39.50313, -0.44007), ('EAST', 2, 39.50408, -0.44101), ('EAST', 3, 39.50258, -0.44229), ('EAST', 4, 39.50282, -0.44549),
    ('EAST', 5, 39.49402, -0.45101), ('EAST', 6, 39.48757, -0.44302), ('EAST', 7, 39.48730, -0.43633), ('EAST', 8, 39.48619, -0.43438),
    ('EAST', 9, 39.48222, -0.43330), ('EAST', 10, 39.47627, -0.42927), ('EAST', 11, 39.46851, -0.43298), ('EAST', 12, 39.46471, -0.43960),
    ('EAST', 13, 39.46413, -0.44757), ('EAST', 14, 39.45819, -0.44726), ('EAST', 15, 39.45388, -0.45102), ('EAST', 16, 39.45154, -0.45561),
    ('EAST', 17, 39.44975, -0.45635), ('EAST', 18, 39.44842, -0.46097), ('EAST', 19, 39.44472, -0.46252), ('EAST', 20, 39.44273, -0.46192),
    ('EAST', 21, 39.43702, -0.46512), ('EAST', 22, 39.43754, -0.47063), ('EAST', 23, 39.43248, -0.48321), ('EAST', 24, 39.42823, -0.49983),
    ('EAST', 25, 39.41519, -0.51110), ('EAST', 26, 39.41588, -0.50758), ('EAST', 27, 39.41317, -0.50259), ('EAST', 28, 39.40909, -0.49819),
    ('EAST', 29, 39.40315, -0.49464), ('EAST', 30, 39.39578, -0.48790), ('EAST', 31, 39.37801, -0.46599), ('EAST', 32, 39.37314, -0.45687),
    ('EAST', 33, 39.37000, -0.45379), ('EAST', 34, 39.36429, -0.45007), ('EAST', 35, 39.35900, -0.44099), ('EAST', 36, 39.34994, -0.43068),
    ('EAST', 37, 39.34405, -0.42872), ('EAST', 38, 39.31286, -0.44124), ('EAST', 39, 39.30900, -0.44397), ('EAST', 40, 39.27895, -0.46008),
    ('EAST', 41, 39.27515, -0.46061), ('EAST', 42, 39.26711, -0.45851), ('EAST', 43, 39.26123, -0.46000), ('EAST', 44, 39.25741, -0.46331),
    ('EAST', 45, 39.25202, -0.47310), ('EAST', 46, 39.24886, -0.47662), ('EAST', 47, 39.21150, -0.49772), ('EAST', 48, 39.20670, -0.49833),
    ('EAST', 49, 39.19729, -0.49454), ('EAST', 50, 39.19503, -0.49498), ('EAST', 51, 39.19398, -0.49667), ('EAST', 52, 39.19298, -0.49446),
    ('EAST', 53, 39.18722, -0.49245), ('EAST', 54, 39.18065, -0.48583), ('EAST', 55, 39.17842, -0.48149), ('EAST', 56, 39.17529, -0.46975),
    ('EAST', 57, 39.16804, -0.46173), ('EAST', 58, 39.16111, -0.45602), ('EAST', 59, 39.16151, -0.45417), ('EAST', 60, 39.16036, -0.44894),
    ('EAST', 61, 39.15455, -0.44857), ('EAST', 62, 39.14742, -0.44103), ('EAST', 63, 39.15051, -0.43807), ('EAST', 64, 39.15253, -0.43764),
    ('EAST', 65, 39.15161, -0.43671), ('EAST', 66, 39.15128, -0.43420), ('EAST', 67, 39.15726, -0.43150), ('EAST', 68, 39.15904, -0.42967),
    ('EAST', 69, 39.16048, -0.43141), ('EAST', 70, 39.16141, -0.42877), ('EAST', 71, 39.16148, -0.42522), ('EAST', 72, 39.16029, -0.42059),
    ('EAST', 73, 39.16043, -0.40884), ('EAST', 74, 39.16142, -0.40695), ('EAST', 75, 39.16280, -0.39078), ('EAST', 76, 39.16197, -0.38553),
    ('EAST', 77, 39.16201, -0.37352), ('EAST', 78, 39.16284, -0.36969), ('EAST', 79, 39.16260, -0.35796), ('EAST', 80, 39.16322, -0.35600),
    ('EAST', 81, 39.16101, -0.35054), ('EAST', 82, 39.15510, -0.34108), ('EAST', 83, 39.14870, -0.32442), ('EAST', 84, 39.14669, -0.32243),
    ('EAST', 85, 39.14417, -0.32190), ('EAST', 86, 39.14185, -0.30941), ('EAST', 87, 39.12728, -0.29188), ('EAST', 88, 39.12689, -0.28964),
    ('EAST', 89, 39.13204, -0.28672), ('EAST', 90, 39.13191, -0.29027), ('EAST', 91, 39.12788, -0.29711), ('EAST', 92, 39.12850, -0.29883),
    ('EAST', 93, 39.12922, -0.29871), ('EAST', 94, 39.12920, -0.29766), ('EAST', 95, 39.12470, -0.29329), ('EAST', 96, 39.11390, -0.27610),
    ('EAST', 97, 39.10599, -0.26557), ('EAST', 98, 39.10076, -0.26069), ('EAST', 99, 39.09343, -0.25820), ('EAST', 100, 39.09120, -0.25670),
    ('EAST', 101, 39.08406, -0.24585), ('EAST', 102, 39.08101, -0.24446), ('EAST', 103, 39.07444, -0.24410), ('EAST', 104, 39.07004, -0.24277),
    ('EAST', 105, 39.06500, -0.23944), ('EAST', 106, 39.06114, -0.23442), ('EAST', 107, 39.05499, -0.22992), ('EAST', 108, 39.04892, -0.22979),
    ('EAST', 109, 39.04361, -0.22796), ('EAST', 110, 39.03657, -0.22681), ('EAST', 111, 39.03298, -0.22504), ('EAST', 112, 39.02776, -0.22049),
    ('EAST', 113, 39.02542, -0.21945), ('EAST', 114, 39.02278, -0.21973), ('EAST', 115, 39.02148, -0.22212), ('EAST', 116, 39.02050, -0.22215),
    ('EAST', 117, 39.01855, -0.21800), ('EAST', 118, 39.01799, -0.21476), ('EAST', 119, 39.00959, -0.20269), ('EAST', 120, 39.00370, -0.19716),
    ('EAST', 121, 38.99589, -0.18423), ('EAST', 122, 38.99208, -0.18480), ('EAST', 123, 38.98833, -0.18750), ('EAST', 124, 38.98593, -0.18586),
    ('EAST', 125, 38.96879, -0.18072), ('EAST', 126, 38.96800, -0.17783), ('EAST', 127, 38.96686, -0.17875), ('EAST', 128, 38.96765, -0.18052),
    ('WEST', 1, 42.43056, -8.64291), ('WEST', 2, 42.43429, -8.64157), ('WEST', 3, 42.43597, -8.64791), ('WEST', 4, 42.43733, -8.64842),
    ('WEST', 5, 42.43749, -8.65209), ('WEST', 6, 42.43825, -8.65219), ('WEST', 7, 42.43883, -8.65112), ('WEST', 8, 42.43805, -8.65042),
    ('WEST', 9, 42.43495, -8.65206), ('WEST', 10, 42.43123, -8.65593), ('WEST', 11, 42.42788, -8.65597), ('WEST', 12, 42.42370, -8.65149),
    ('WEST', 13, 42.42166, -8.65062), ('WEST', 14, 42.41893, -8.65103), ('WEST', 15, 42.41348, -8.65381), ('WEST', 16, 42.40949, -8.65385),
    ('WEST', 17, 42.40246, -8.64967), ('WEST', 18, 42.39758, -8.64806), ('WEST', 19, 42.39176, -8.64150), ('WEST', 20, 42.38556, -8.63990),
    ('WEST', 21, 42.37920, -8.63671), ('WEST', 22, 42.37310, -8.63648), ('WEST', 23, 42.36733, -8.63950), ('WEST', 24, 42.36744, -8.63372),
    ('WEST', 25, 42.35348, -8.62654), ('WEST', 26, 42.35088, -8.62382), ('WEST', 27, 42.34890, -8.61434), ('WEST', 28, 42.34756, -8.61257),
    ('WEST', 29, 42.34292, -8.61174), ('WEST', 30, 42.34183, -8.61003), ('WEST', 31, 42.33494, -8.60676), ('WEST', 32, 42.33002, -8.60676),
    ('WEST', 33, 42.32721, -8.60586), ('WEST', 34, 42.32448, -8.60930), ('WEST', 35, 42.32437, -8.61198), ('WEST', 36, 42.32201, -8.61512),
    ('WEST', 37, 42.31991, -8.61462), ('WEST', 38, 42.31831, -8.61640), ('WEST', 39, 42.31544, -8.61726), ('WEST', 40, 42.31277, -8.61642),
    ('WEST', 41, 42.31130, -8.61487), ('WEST', 42, 42.30870, -8.60809), ('WEST', 43, 42.30571, -8.60341), ('WEST', 44, 42.29991, -8.60259),
    ('WEST', 45, 42.29820, -8.60644), ('WEST', 46, 42.29455, -8.60530), ('WEST', 47, 42.29268, -8.60706), ('WEST', 48, 42.28813, -8.60880),
    ('WEST', 49, 42.28651, -8.61057), ('WEST', 50, 42.28420, -8.60939), ('WEST', 51, 42.28213, -8.61107), ('WEST', 52, 42.28097, -8.61089),
    ('WEST', 53, 42.28187, -8.61021), ('WEST', 54, 42.28186, -8.60900), ('WEST', 55, 42.28523, -8.60854), ('WEST', 56, 42.28653, -8.60987),
    ('WEST', 57, 42.28717, -8.60959), ('WEST', 58, 42.28280, -8.61130), ('WEST', 59, 42.28174, -8.61343), ('WEST', 60, 42.27824, -8.61259),
    ('WEST', 61, 42.27666, -8.61032), ('WEST', 62, 42.26997, -8.61006), ('WEST', 63, 42.26702, -8.60670), ('WEST', 64, 42.26207, -8.60332),
    ('WEST', 65, 42.25675, -8.60282), ('WEST', 66, 42.24511, -8.59655), ('WEST', 67, 42.24050, -8.59666), ('WEST', 68, 42.23841, -8.59499),
    ('WEST', 69, 42.23464, -8.59641), ('WEST', 70, 42.22579, -8.59621), ('WEST', 71, 42.22306, -8.59733), ('WEST', 72, 42.22099, -8.59623),
    ('WEST', 73, 42.21763, -8.59652), ('WEST', 74, 42.21577, -8.59810), ('WEST', 75, 42.21005, -8.59919), ('WEST', 76, 42.19975, -8.60417),
    ('WEST', 77, 42.19478, -8.60531), ('WEST', 78, 42.19310, -8.60755), ('WEST', 79, 42.18370, -8.61344), ('WEST', 80, 42.18055, -8.61340),
    ('WEST', 81, 42.17797, -8.61569), ('WEST', 82, 42.17071, -8.61935), ('WEST', 83, 42.16599, -8.62081), ('WEST', 84, 42.16545, -8.62018),
    ('WEST', 85, 42.16206, -8.62002), ('WEST', 86, 42.16119, -8.61904), ('WEST', 87, 42.16212, -8.61995), ('WEST', 88, 42.16611, -8.62039),
    ('WEST', 89, 42.16678, -8.62468), ('WEST', 90, 42.14925, -8.62616), ('WEST', 91, 42.14831, -8.62768), ('WEST', 92, 42.14670, -8.63867),
    ('WEST', 93, 42.14883, -8.64152), ('WEST', 94, 42.15251, -8.64331), ('WEST', 95, 42.16131, -8.64483), ('WEST', 96, 42.16661, -8.65010),
    ('WEST', 97, 42.16884, -8.65125), ('WEST', 98, 42.17092, -8.65142), ('WEST', 99, 42.17675, -8.64988), ('WEST', 100, 42.18597, -8.65130),
    ('WEST', 101, 42.18813, -8.65359), ('WEST', 102, 42.19026, -8.66020), ('WEST', 103, 42.19165, -8.66177), ('WEST', 104, 42.19445, -8.66292),
    ('WEST', 105, 42.19525, -8.66588), ('WEST', 106, 42.19451, -8.66755), ('WEST', 107, 42.19002, -8.67164), ('WEST', 108, 42.18465, -8.67277),
    ('WEST', 109, 42.18276, -8.67485), ('WEST', 110, 42.18211, -8.67963), ('WEST', 111, 42.18427, -8.68479), ('WEST', 112, 42.18483, -8.68831),
    ('WEST', 113, 42.18413, -8.69212), ('WEST', 114, 42.18245, -8.69484), ('WEST', 115, 42.18041, -8.69626), ('WEST', 116, 42.17474, -8.69767),
    ('WEST', 117, 42.17156, -8.70304), ('WEST', 118, 42.16960, -8.70469), ('WEST', 119, 42.16733, -8.70489), ('WEST', 120, 42.16220, -8.70271),
    ('WEST', 121, 42.15998, -8.70353), ('WEST', 122, 42.15825, -8.70547), ('WEST', 123, 42.15322, -8.71837), ('WEST', 124, 42.15205, -8.72418),
    ('WEST', 125, 42.15324, -8.73251), ('WEST', 126, 42.15244, -8.74334), ('WEST', 127, 42.14830, -8.74911), ('WEST', 128, 42.14618, -8.75568),
    ('WEST', 129, 42.14366, -8.76004), ('WEST', 130, 42.14093, -8.76266), ('WEST', 131, 42.13478, -8.76615), ('WEST', 132, 42.12942, -8.76653),
    ('WEST', 133, 42.12631, -8.76558), ('WEST', 134, 42.12403, -8.76666), ('WEST', 135, 42.12264, -8.76873), ('WEST', 136, 42.12190, -8.77190),
    ('WEST', 137, 42.12355, -8.77965), ('WEST', 138, 42.12246, -8.78437), ('WEST', 139, 42.11691, -8.78927), ('WEST', 140, 42.11236, -8.79001),
    ('WEST', 141, 42.10607, -8.79436), ('WEST', 142, 42.10435, -8.79937), ('WEST', 143, 42.10155, -8.80259), ('WEST', 144, 42.10155, -8.80549),
    ('WEST', 145, 42.10362, -8.80807), ('WEST', 146, 42.10408, -8.81022), ('WEST', 147, 42.10286, -8.81723), ('WEST', 148, 42.10044, -8.82368),
    ('WEST', 149, 42.10114, -8.82452), ('WEST', 150, 42.10558, -8.82535), ('WEST', 151, 42.10812, -8.82743), ('WEST', 152, 42.10996, -8.82788),
    ('WEST', 153, 42.11093, -8.83480), ('WEST', 154, 42.11326, -8.83686), ('WEST', 155, 42.11456, -8.84104), ('WEST', 156, 42.11738, -8.84450),
    ('WEST', 157, 42.11828, -8.84656), ('WEST', 158, 42.11687, -8.84888), ('WEST', 159, 42.11764, -8.84917), ('WEST', 160, 42.11805, -8.85134),
    ('WEST', 161, 42.11807, -8.85000), ('CENTRAL', 1, 40.48237, -3.36242), ('CENTRAL', 2, 40.47729, -3.36011), ('CENTRAL', 3, 40.47037, -3.37514),
    ('CENTRAL', 4, 40.46984, -3.38329), ('CENTRAL', 5, 40.47405, -3.39302), ('CENTRAL', 6, 40.47216, -3.39805), ('CENTRAL', 7, 40.47227, -3.40790),
    ('CENTRAL', 8, 40.46806, -3.42295), ('CENTRAL', 9, 40.46883, -3.43757), ('CENTRAL', 10, 40.46765, -3.44209), ('CENTRAL', 11, 40.46686, -3.45132),
    ('CENTRAL', 12, 40.46711, -3.46989), ('CENTRAL', 13, 40.46570, -3.48001), ('CENTRAL', 14, 40.46641, -3.48536), ('CENTRAL', 15, 40.46486, -3.48776),
    ('CENTRAL', 16, 40.46400, -3.48784), ('CENTRAL', 17, 40.46382, -3.48442), ('CENTRAL', 18, 40.46156, -3.48401), ('CENTRAL', 19, 40.46146, -3.48506),
    ('CENTRAL', 20, 40.46093, -3.48340), ('CENTRAL', 21, 40.45710, -3.48170), ('CENTRAL', 22, 40.45782, -3.48025), ('CENTRAL', 23, 40.45957, -3.48114),
    ('CENTRAL', 24, 40.45930, -3.48188), ('CENTRAL', 25, 40.45850, -3.48159), ('CENTRAL', 26, 40.45592, -3.47972), ('CENTRAL', 27, 40.45545, -3.48384),
    ('CENTRAL', 28, 40.45395, -3.48437), ('CENTRAL', 29, 40.44468, -3.48163), ('CENTRAL', 30, 40.44326, -3.48332), ('CENTRAL', 31, 40.43891, -3.50107),
    ('CENTRAL', 32, 40.43456, -3.50749), ('CENTRAL', 33, 40.43041, -3.50192), ('CENTRAL', 34, 40.42490, -3.49927), ('CENTRAL', 35, 40.41911, -3.50035),
    ('CENTRAL', 36, 40.41458, -3.50438), ('CENTRAL', 37, 40.41168, -3.51078), ('CENTRAL', 38, 40.40853, -3.52823), ('CENTRAL', 39, 40.40861, -3.53363),
    ('CENTRAL', 40, 40.41078, -3.54422), ('CENTRAL', 41, 40.41081, -3.55753), ('CENTRAL', 42, 40.40922, -3.56462), ('CENTRAL', 43, 40.40696, -3.56876),
    ('CENTRAL', 44, 40.40416, -3.57180), ('CENTRAL', 45, 40.39398, -3.57823), ('CENTRAL', 46, 40.38691, -3.58594), ('CENTRAL', 47, 40.37763, -3.59197),
    ('CENTRAL', 48, 40.36997, -3.60322), ('CENTRAL', 49, 40.36021, -3.61281), ('CENTRAL', 50, 40.35701, -3.61698), ('CENTRAL', 51, 40.35475, -3.62296),
    ('CENTRAL', 52, 40.35308, -3.63426), ('CENTRAL', 53, 40.34772, -3.64823), ('CENTRAL', 54, 40.33774, -3.66329), ('CENTRAL', 55, 40.33281, -3.67546),
    ('CENTRAL', 56, 40.32669, -3.68614), ('CENTRAL', 57, 40.32594, -3.69089), ('CENTRAL', 58, 40.32586, -3.70551), ('CENTRAL', 59, 40.32849, -3.71796),
    ('CENTRAL', 60, 40.32326, -3.72138), ('CENTRAL', 61, 40.31842, -3.73002), ('CENTRAL', 62, 40.30762, -3.73512), ('CENTRAL', 63, 40.30435, -3.73552),
    ('CENTRAL', 64, 40.30510, -3.72667), ('CENTRAL', 65, 40.30414, -3.72560), ('CENTRAL', 66, 40.30052, -3.72844), ('CENTRAL', 67, 40.29884, -3.73266),
    ('CENTRAL', 68, 40.29619, -3.73570), ('CENTRAL', 69, 40.29591, -3.73788), ('CENTRAL', 70, 40.29670, -3.73848), ('CENTRAL', 71, 40.29642, -3.73771),
    ('CENTRAL', 72, 40.29486, -3.73816), ('CENTRAL', 73, 40.28089, -3.74800), ('CENTRAL', 74, 40.28039, -3.75101), ('CENTRAL', 75, 40.28266, -3.75492),
    ('CENTRAL', 76, 40.29502, -3.76433), ('CENTRAL', 77, 40.30083, -3.77132), ('CENTRAL', 78, 40.30385, -3.77723), ('CENTRAL', 79, 40.31091, -3.79577),
    ('CENTRAL', 80, 40.31408, -3.81105), ('CENTRAL', 81, 40.31952, -3.82313), ('CENTRAL', 82, 40.32399, -3.83710), ('CENTRAL', 83, 40.32629, -3.84199),
    ('CENTRAL', 84, 40.32416, -3.84485), ('CENTRAL', 85, 40.32394, -3.85240), ('CENTRAL', 86, 40.32264, -3.85253), ('CENTRAL', 87, 40.31952, -3.85494),
    ('CENTRAL', 88, 40.32253, -3.86374), ('CENTRAL', 89, 40.32199, -3.86499);

INSERT INTO FltCus (IdeCus, IdeOpe, Nam, Eml, Tel, Adr, Cit) VALUES
    (1, 201, 'Acme Logistics', 'logistics@acme.example', '910 000 101', 'Calle Industria 4', 'Madrid'),
    (2, 202, 'Globex Iberia', 'envios@globex.example', '910 000 102', 'Avenida del Puerto 88', 'Valencia'),
    (3, 203, 'Initech France', 'logistique@initech.example', '910 000 103', 'Rue du Commerce 12', 'Bilbao'),
    (4, NULL, 'Umbrella Pharma', 'supply@umbrella.example', '910 000 104', 'Poligono Sur 7', 'Seville');

-- ------------------------------------------------------
--  Generated data (names come from small lists joined by the number: MOD(n, size))
-- ------------------------------------------------------
INSERT INTO FltDrv (IdeDrv, Nam, Lic, Pho, Sta, Reg)
SELECT n, f.nm || ' ' || l.nm, 'DL' || CAST(700000 + n * 137 AS VARCHAR(8)), '600 200 ' || CAST(100 + n AS VARCHAR(3)),
       CASE WHEN MOD(n, 11) = 0 THEN 'ON_LEAVE' ELSE 'ACTIVE' END,
       CASE MOD(n, 5) WHEN 0 THEN 'NORTH' WHEN 1 THEN 'SOUTH' WHEN 2 THEN 'EAST' WHEN 3 THEN 'WEST' ELSE 'CENTRAL' END
FROM UNNEST(SEQUENCE_ARRAY(1, 30, 1)) AS t (n)
         JOIN (VALUES (0, CAST('Carlos' AS VARCHAR(200))), (1, 'Lucia'), (2, 'Miguel'), (3, 'Elena'), (4, 'David'), (5, 'Paula'),
                      (6, 'Andres'), (7, 'Nuria'), (8, 'Sergio'), (9, 'Marta')) AS f (i, nm) ON f.i = MOD(n, 10)
         JOIN (VALUES (0, CAST('Garcia' AS VARCHAR(200))), (1, 'Lopez'), (2, 'Martin'), (3, 'Sanz'), (4, 'Romero'), (5, 'Navarro'),
                      (6, 'Ruiz')) AS l (i, nm) ON l.i = MOD(n, 7);

INSERT INTO FltVeh (IdeVeh, Plt, Typ, Cap, Mil, Sta, NxtSvc, Reg)
SELECT n, CAST(1000 + n * 37 AS VARCHAR(5)) || '-MF',
       CASE MOD(n, 3) WHEN 0 THEN 'HEAVY' WHEN 1 THEN 'VAN' ELSE 'TRUCK' END,
       CASE MOD(n, 3) WHEN 0 THEN 18000 WHEN 1 THEN 1200 ELSE 6000 END,
       20000 + MOD(n * 7919, 180000),
       CASE WHEN MOD(n, 9) = 0 THEN 'MAINTENANCE' WHEN MOD(n, 3) = 1 THEN 'ON_ROUTE' ELSE 'AVAILABLE' END,
       TRUNC(CURRENT_DATE) + (MOD(n * 17, 90) - 15) DAY,
       CASE MOD(n, 5) WHEN 0 THEN 'NORTH' WHEN 1 THEN 'SOUTH' WHEN 2 THEN 'EAST' WHEN 3 THEN 'WEST' ELSE 'CENTRAL' END
FROM UNNEST(SEQUENCE_ARRAY(1, 25, 1)) AS t (n);

-- 40 routes: eight per day from two days ago to two days ahead. Today's routes (n MOD 5 = 2) are unassigned when n is
-- odd: the dispatcher assigns them in the assignments screen
INSERT INTO FltRte (IdeRte, Cod, RteDat, Reg, IdeVeh, IdeDrv, Sta)
SELECT n, 'R' || CAST(1000 + n AS VARCHAR(4)), TRUNC(CURRENT_DATE) + (MOD(n, 5) - 2) DAY,
       CASE MOD(n, 5) WHEN 0 THEN 'NORTH' WHEN 1 THEN 'SOUTH' WHEN 2 THEN 'EAST' WHEN 3 THEN 'WEST' ELSE 'CENTRAL' END,
       CASE WHEN MOD(n, 5) = 2 AND MOD(n, 2) = 1 THEN NULL ELSE 1 + MOD(n, 25) END,
       CASE WHEN MOD(n, 5) = 2 AND MOD(n, 2) = 1 THEN NULL ELSE 1 + MOD(n * 3, 30) END,
       CASE WHEN MOD(n, 5) < 2 THEN 'COMPLETED' WHEN MOD(n, 5) = 2 THEN 'IN_PROGRESS' ELSE 'PLANNED' END
FROM UNNEST(SEQUENCE_ARRAY(1, 40, 1)) AS t (n);

-- 1000 shipments over 90 days. The tracking code is public (with the postal code), so it is a non-sequential code derived from the
-- number with a scrambling formula (8 characters of a 32 letter alphabet), not a counter.
--  Age and transit time decide the status; every 11th shipment that is 2 to 8 days old is
-- in transit with a past estimated arrival (the scheduled task flags it), every 10th shipment that is 5 to 12 days old is stuck as delayed
INSERT INTO FltShp (IdeShp, Cod, IdeCus, OrgReg, DstReg, DstPst, Wgt, Pri, Sta, CreDat, Eta, DelDat, Fee)
SELECT x.n, 'MF-' || x.code, 1 + MOD(x.n * 7, 4), x.org, x.dst,
       r.Pfx || LPAD(CAST(MOD(x.n * 7919, 1000) AS VARCHAR(3)), 3, '0'),
       10 + MOD(x.n * 31, 2000) / 2.0, CASE WHEN MOD(x.n, 6) = 0 THEN 'EXPRESS' ELSE 'STANDARD' END, x.sta, x.cre,
       CASE WHEN x.late = 1 THEN CURRENT_TIMESTAMP - 3 HOUR ELSE x.cre + x.dur DAY + 12 HOUR END,
       CASE WHEN x.sta = 'DELIVERED' THEN x.cre + x.dur DAY + (CASE WHEN MOD(x.n, 9) = 0 THEN 30 ELSE 6 END) HOUR END,
       ROUND(35 + (10 + MOD(x.n * 31, 2000) / 2.0) * 0.45 + CASE WHEN MOD(x.n, 6) = 0 THEN 40 ELSE 0 END, 2)
FROM (SELECT y.n, y.code, y.org, y.dst, y.cre, y.dur, y.age, y.late,
             CASE WHEN y.late = 1 THEN 'IN_TRANSIT'
                  WHEN y.age BETWEEN 5 AND 12 AND MOD(y.n, 10) = 0 THEN 'DELAYED'
                  WHEN y.age > y.dur THEN 'DELIVERED'
                  WHEN y.age = y.dur THEN 'OUT_FOR_DELIVERY' WHEN y.age = 0 THEN 'CREATED' WHEN y.age = 1 THEN 'PICKED_UP'
                  ELSE 'IN_TRANSIT' END AS sta
      FROM (SELECT n, SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 2654435761 + 40503, 33554393) / 1, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 2654435761 + 40503, 33554393) / 32, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 2654435761 + 40503, 33554393) / 1024, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 2654435761 + 40503, 33554393) / 32768, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 2654435761 + 40503, 33554393) / 1048576, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 320723177 + 977, 32749) / 1, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 320723177 + 977, 32749) / 32, 32), 1) || SUBSTRING('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + MOD(MOD(CAST(n AS BIGINT) * 320723177 + 977, 32749) / 1024, 32), 1) AS code, TRUNC(CURRENT_DATE) - MOD(n * 37, 90) DAY AS cre, MOD(n * 37, 90) AS age, 1 + MOD(n, 4) AS dur,
                   CASE MOD(n, 5) WHEN 0 THEN 'NORTH' WHEN 1 THEN 'SOUTH' WHEN 2 THEN 'EAST' WHEN 3 THEN 'WEST'
                                  ELSE 'CENTRAL' END AS org,
                   CASE MOD(n * 3, 5) WHEN 0 THEN 'NORTH' WHEN 1 THEN 'SOUTH' WHEN 2 THEN 'EAST' WHEN 3 THEN 'WEST'
                                      ELSE 'CENTRAL' END AS dst,
                   CASE WHEN MOD(n, 11) = 0 AND MOD(n * 37, 90) BETWEEN 2 AND 8 THEN 1 ELSE 0 END AS late
            FROM UNNEST(SEQUENCE_ARRAY(1, 1000, 1)) AS t (n)) y) x
         JOIN FltReg r ON r.Reg = x.dst;

-- Tracking events: one per step reached (a delayed shipment has a DELAYED event instead of OUT_FOR_DELIVERY)
INSERT INTO FltEvt (IdeEvt, IdeShp, EvtDat, Sta, Loc)
SELECT s.IdeShp * 10 + k.k, s.IdeShp,
       CASE WHEN k.k = 5 THEN s.DelDat ELSE CAST(s.CreDat AS TIMESTAMP) + (k.k * 9) HOUR END,
       CASE WHEN k.k = 4 AND s.Sta = 'DELAYED' THEN 'DELAYED'
            ELSE CASE k.k WHEN 1 THEN 'CREATED' WHEN 2 THEN 'PICKED_UP' WHEN 3 THEN 'IN_TRANSIT'
                          WHEN 4 THEN 'OUT_FOR_DELIVERY' ELSE 'DELIVERED' END END,
       CASE WHEN k.k <= 2 THEN 'Hub ' || o.Cit WHEN k.k = 3 THEN 'Central hub, Madrid' ELSE 'Hub ' || d.Cit END
FROM FltShp s
         JOIN FltReg o ON o.Reg = s.OrgReg
         JOIN FltReg d ON d.Reg = s.DstReg
         JOIN UNNEST(SEQUENCE_ARRAY(1, 5, 1)) AS k (k)
              ON k.k <= CASE s.Sta WHEN 'CREATED' THEN 1 WHEN 'PICKED_UP' THEN 2 WHEN 'IN_TRANSIT' THEN 3
                                   WHEN 'DELAYED' THEN 4 WHEN 'OUT_FOR_DELIVERY' THEN 4 ELSE 5 END;

-- Four stops per route, each with a shipment. The stops are the real towns of the region (FltRegStp) in order
INSERT INTO FltStp (IdeStp, IdeRte, Seq, Loc, Lat, Lon, IdeShp)
SELECT r.IdeRte * 10 + k.k, r.IdeRte, k.k, g.Loc, g.Lat, g.Lon, 1 + MOD(r.IdeRte * 4 + k.k * 17, 1000)
FROM FltRte r
         JOIN UNNEST(SEQUENCE_ARRAY(1, 4, 1)) AS k (k) ON 1 = 1
         JOIN FltRegStp g ON g.Reg = r.Reg AND g.Seq = k.k;

-- Monthly invoices of the last 90 days: the amount is the sum of the fees of the shipments of that period
INSERT INTO FltInv (IdeInv, IdeCus, InvDat, Amt, Sta)
SELECT c.IdeCus * 10 + m.m, c.IdeCus, TRUNC(CURRENT_DATE) - (m.m * 30) DAY,
       (SELECT COALESCE(SUM(s.Fee), 0) FROM FltShp s
        WHERE s.IdeCus = c.IdeCus AND s.CreDat > TRUNC(CURRENT_DATE) - ((m.m + 1) * 30) DAY
          AND s.CreDat <= TRUNC(CURRENT_DATE) - (m.m * 30) DAY),
       CASE m.m WHEN 0 THEN 'PENDING' WHEN 1 THEN CASE WHEN MOD(c.IdeCus, 2) = 0 THEN 'OVERDUE' ELSE 'PAID' END
                ELSE 'PAID' END
FROM FltCus c
         JOIN UNNEST(SEQUENCE_ARRAY(0, 2, 1)) AS m (m) ON 1 = 1;

INSERT INTO FltQuo (IdeQuo, QuoDat, Nam, Eml, Tel, OrgReg, DstReg, Wgt, Nte, Sta) VALUES
    (1, CURRENT_TIMESTAMP - 3 HOUR, 'Pedro Alonso', 'pedro@mail.example', '611 000 001', 'CENTRAL', 'EAST', 850.0, 'Weekly pallets', 'PENDING'),
    (2, CURRENT_TIMESTAMP - 1 DAY, 'Sara Bell', 'sara@mail.example', NULL, 'NORTH', 'SOUTH', 120.5, NULL, 'PENDING'),
    (3, CURRENT_TIMESTAMP - 5 DAY, 'Ivan Petrov', 'ivan@mail.example', '611 000 003', 'WEST', 'CENTRAL', 4300.0, 'Machinery', 'HANDLED');

INSERT INTO FltPck (IdePck, IdeCus, PckDat, Adr, Pcs, Wgt, Nte, Sta) VALUES
    (1, 1, TRUNC(CURRENT_DATE) + 1 DAY, 'Calle Industria 4, Madrid', 6, 420.0, 'Dock 3', 'PENDING'),
    (2, 2, TRUNC(CURRENT_DATE) + 2 DAY, 'Avenida del Puerto 88, Valencia', 2, 80.0, NULL, 'PENDING'),
    (3, 1, TRUNC(CURRENT_DATE) - 3 DAY, 'Calle Industria 4, Madrid', 10, 900.0, NULL, 'HANDLED'),
    (4, 3, TRUNC(CURRENT_DATE) + 4 DAY, 'Rue du Commerce 12, Bilbao', 1, 35.0, 'Fragile', 'PENDING');

-- ------------------------------------------------------
--  Notifier: a subscription for the dispatchers (FLEET) and one per customer account (CUS1 ... CUS4). A notification
--  reaches the users subscribed to its subscription, so a customer only sees the ones of its own account
-- ------------------------------------------------------
INSERT INTO AweSub (Ide, Acr, Nom, Des, Act) VALUES
    (1, 'FLEET', 'Fleet alerts', 'Late shipments and fleet events for the dispatchers', 1),
    (11, 'CUS1', 'Acme shipments', 'Shipment alerts of Acme Logistics', 1),
    (12, 'CUS2', 'Globex shipments', 'Shipment alerts of Globex Iberia', 1),
    (13, 'CUS3', 'Initech shipments', 'Shipment alerts of Initech France', 1),
    (14, 'CUS4', 'Umbrella shipments', 'Shipment alerts of Umbrella Pharma', 1);
INSERT INTO AweSubUsr (Ide, IdeOpe, IdeSub, SubNot, SubEma) VALUES
    (1, 101, 1, 1, 0), (2, 102, 1, 1, 0), (3, 201, 11, 1, 0), (4, 202, 12, 1, 0), (5, 203, 13, 1, 0);
UPDATE AweKey SET KeyVal = 20 WHERE KeyNam = 'Sub';
UPDATE AweKey SET KeyVal = 10 WHERE KeyNam = 'SubUsr';

-- ------------------------------------------------------
--  Scheduler: two tasks that run a maintain target of this application (TypExe 1) on a schedule (TypLch 1)
--    FltFlagLate   every 5 minutes: flags late shipments as DELAYED, adds the tracking event and notifies
--    FltCloseRoute every hour: completes the routes of past days
--  RptTyp 1 = minutes, 2 = hours. Both targets are idempotent and take no input
-- ------------------------------------------------------
INSERT INTO AweSchTsk (Ide, Nam, Des, TypExe, CmdExe, TypLch, Act)
VALUES (1, 'Flag late shipments', 'Flags the shipments that missed their estimated arrival and notifies the dispatchers and the customers', 1, 'FltFlagLate', 1, 1);
INSERT INTO AweSchTsk (Ide, Nam, Des, TypExe, CmdExe, TypLch, Act)
VALUES (2, 'Close past routes', 'Completes the routes of past days', 1, 'FltCloseRoute', 1, 1);
INSERT INTO AweSchTskLch (Ide, IdeTsk, RptNum, RptTyp, IniDat, IniTim)
VALUES (1, 1, 5, 1, TRUNC(CURRENT_DATE) - 1 DAY, '00:00:00');
INSERT INTO AweSchTskLch (Ide, IdeTsk, RptNum, RptTyp, IniDat, IniTim)
VALUES (2, 2, 1, 2, TRUNC(CURRENT_DATE) - 1 DAY, '00:00:00');
UPDATE AweKey SET KeyVal = 3 WHERE KeyNam = 'SchTskKey';
UPDATE AweKey SET KeyVal = 3 WHERE KeyNam = 'SchTskLch';

-- AWE sequences of the tables the screens write
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('VehKey', 25);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('DrvKey', 30);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('QuoKey', 3);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('PckKey', 4);
