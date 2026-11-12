-- ------------------------------------------------------
--  Veterinary clinic (AWE sample application)
--  Schema, read models (views), profiles, demo users and demo data.
--  Demo credentials: see the README of the module. They are sample values, never use them in production.
-- ------------------------------------------------------

-- ------------------------------------------------------
--  Tables
--  Primary keys come from AweKey (the AWE sequences): the maintain targets use sequence="PetKey" and so on
-- ------------------------------------------------------
CREATE TABLE VetOwn (
    IdeOwn int          NOT NULL PRIMARY KEY,   -- Owner
    IdeOpe int          NULL,                   -- Login of the owner (ope), empty when the owner cannot sign in
    Nam    varchar(80)  NOT NULL,               -- Full name
    Eml    varchar(80)  NULL,                   -- Email
    Tel    varchar(20)  NULL,                   -- Phone
    Adr    varchar(120) NULL,                   -- Address
    Cit    varchar(60)  NULL,                   -- City
    RegDat date         NOT NULL,               -- Registration date
    Nte    varchar(400) NULL,                   -- Notes
    CONSTRAINT fk_VetOwn_ope FOREIGN KEY (IdeOpe) REFERENCES ope (IdeOpe),
    CONSTRAINT uk_VetOwn_ope UNIQUE (IdeOpe)
);

CREATE TABLE VetPet (
    IdePet int           NOT NULL PRIMARY KEY,  -- Pet
    IdeOwn int           NOT NULL,              -- Owner
    Nam    varchar(40)   NOT NULL,              -- Name
    Spc    varchar(10)   NOT NULL,              -- Species (enumerated VetSpecies)
    Brd    varchar(60)   NULL,                  -- Breed
    Sex    varchar(1)    NOT NULL,              -- M or F
    BrnDat date          NULL,                  -- Birth date
    Wgt    decimal(6, 2) NULL,                  -- Weight in kg
    Chp    varchar(20)   NULL,                  -- Microchip
    Act    int DEFAULT 1 NOT NULL,              -- Active (1) or not (0)
    CONSTRAINT fk_VetPet_owner FOREIGN KEY (IdeOwn) REFERENCES VetOwn (IdeOwn)
);
CREATE INDEX VetPetI1 ON VetPet (IdeOwn);

CREATE TABLE VetVet (
    IdeVet int         NOT NULL PRIMARY KEY,    -- Veterinarian
    IdeOpe int         NULL,                    -- Login of the veterinarian (ope)
    Nam    varchar(80) NOT NULL,                -- Full name
    Spe    varchar(60) NULL,                    -- Specialty
    CONSTRAINT fk_VetVet_ope FOREIGN KEY (IdeOpe) REFERENCES ope (IdeOpe)
);

CREATE TABLE VetVis (
    IdeVis int           NOT NULL PRIMARY KEY,  -- Visit
    IdePet int           NOT NULL,              -- Pet
    IdeVet int           NOT NULL,              -- Veterinarian
    VisDat date          NOT NULL,              -- Date
    VisTim time          NULL,                  -- Time
    Typ    varchar(12)   NOT NULL,              -- Type (enumerated VetVisitType)
    Dia    varchar(200)  NULL,                  -- Diagnosis
    Trt    varchar(400)  NULL,                  -- Treatment
    Fee    decimal(8, 2) NOT NULL,              -- Fee in euros
    CONSTRAINT fk_VetVis_pet FOREIGN KEY (IdePet) REFERENCES VetPet (IdePet),
    CONSTRAINT fk_VetVis_vet FOREIGN KEY (IdeVet) REFERENCES VetVet (IdeVet)
);
CREATE INDEX VetVisI1 ON VetVis (IdePet);
CREATE INDEX VetVisI2 ON VetVis (VisDat);

CREATE TABLE VetVac (
    IdeVac int         NOT NULL PRIMARY KEY,    -- Vaccination
    IdePet int         NOT NULL,                -- Pet
    IdeVet int         NULL,                    -- Veterinarian who applied it
    Vcc    varchar(20) NOT NULL,                -- Vaccine (enumerated VetVaccine)
    VacDat date        NOT NULL,                -- Applied on
    NxtDat date        NOT NULL,                -- Next dose due on
    CONSTRAINT fk_VetVac_pet FOREIGN KEY (IdePet) REFERENCES VetPet (IdePet)
);
CREATE INDEX VetVacI1 ON VetVac (IdePet);

CREATE TABLE VetApt (
    IdeApt int          NOT NULL PRIMARY KEY,   -- Appointment
    IdePet int          NOT NULL,               -- Pet
    IdeOwn int          NOT NULL,               -- Owner of the pet
    IdeVet int          NULL,                   -- Veterinarian, empty until the clinic assigns one
    AptDat date         NOT NULL,               -- Date
    AptTim time         NOT NULL,               -- Time
    Rea    varchar(12)  NOT NULL,               -- Reason (enumerated VetVisitType)
    Sta    varchar(10)  NOT NULL,               -- Status (enumerated VetAptStatus)
    Src    varchar(10)  NOT NULL,               -- Source: STAFF or OWNER
    Nte    varchar(400) NULL,                   -- Notes
    CONSTRAINT fk_VetApt_pet FOREIGN KEY (IdePet) REFERENCES VetPet (IdePet),
    CONSTRAINT fk_VetApt_owner FOREIGN KEY (IdeOwn) REFERENCES VetOwn (IdeOwn)
);
CREATE INDEX VetAptI1 ON VetApt (AptDat);
CREATE INDEX VetAptI2 ON VetApt (IdeOwn);

CREATE TABLE VetInv (
    IdeInv int           NOT NULL PRIMARY KEY,  -- Invoice
    IdeOwn int           NOT NULL,              -- Owner
    IdeVis int           NULL,                  -- Visit that generated it
    InvDat date          NOT NULL,              -- Date
    Amt    decimal(9, 2) NOT NULL,              -- Amount in euros
    Sta    varchar(10)   NOT NULL,              -- Status (enumerated VetInvStatus)
    CONSTRAINT fk_VetInv_owner FOREIGN KEY (IdeOwn) REFERENCES VetOwn (IdeOwn)
);
CREATE INDEX VetInvI1 ON VetInv (IdeOwn);

CREATE TABLE VetInvLin (
    IdeInv int           NOT NULL,              -- Invoice
    Pos    int           NOT NULL,              -- Position of the line in the invoice
    Con    varchar(12)   NOT NULL,              -- Concept (enumerated VetInvConcept)
    Qty    int           NOT NULL,              -- Quantity
    Prc    decimal(9, 2) NOT NULL,              -- Price of one unit before tax
    Amt    decimal(9, 2) NOT NULL,              -- Amount of the line before tax (quantity by price)
    PRIMARY KEY (IdeInv, Pos),
    CONSTRAINT fk_VetInvLin_inv FOREIGN KEY (IdeInv) REFERENCES VetInv (IdeInv)
);

CREATE TABLE VetDoc (
    IdeDoc int          NOT NULL PRIMARY KEY,   -- Document
    IdePet int          NOT NULL,               -- Pet
    DocDat date         NOT NULL,               -- Date
    Typ    varchar(10)  NOT NULL,               -- Type (enumerated VetDocType)
    Des    varchar(120) NOT NULL,               -- Description
    FilNam varchar(200) NOT NULL,               -- Name of the uploaded file
    CONSTRAINT fk_VetDoc_pet FOREIGN KEY (IdePet) REFERENCES VetPet (IdePet)
);

-- Appointment requests of the public form: anybody can add one, only the staff reads them
CREATE TABLE VetReq (
    IdeReq int          NOT NULL PRIMARY KEY,   -- Request
    ReqDat timestamp    NOT NULL,               -- Received on
    OwnNam varchar(80)  NOT NULL,               -- Name of the person who asks
    Eml    varchar(80)  NOT NULL,               -- Email
    Tel    varchar(20)  NULL,                   -- Phone
    PetNam varchar(40)  NOT NULL,               -- Name of the pet
    Spc    varchar(10)  NOT NULL,               -- Species (enumerated VetSpecies)
    PrfDat date         NOT NULL,               -- Preferred date
    Rea    varchar(12)  NOT NULL,               -- Reason (enumerated VetVisitType)
    Nte    varchar(400) NULL,                   -- Notes
    Sta    varchar(10)  NOT NULL,               -- Status (enumerated VetReqStatus)
    -- The public form can only add rows, so the database checks what the form sends
    CONSTRAINT ck_VetReq_name CHECK (CHAR_LENGTH(TRIM(OwnNam)) >= 2),
    CONSTRAINT ck_VetReq_pet CHECK (CHAR_LENGTH(TRIM(PetNam)) >= 1),
    CONSTRAINT ck_VetReq_email CHECK (Eml LIKE '%_@_%._%'),
    CONSTRAINT ck_VetReq_species CHECK (Spc IN ('DOG', 'CAT', 'RABBIT', 'BIRD', 'REPTILE')),
    CONSTRAINT ck_VetReq_reason CHECK (Rea IN ('CHECKUP', 'VACCINATION', 'DENTAL', 'SURGERY', 'EMERGENCY')),
    CONSTRAINT ck_VetReq_status CHECK (Sta IN ('PENDING', 'HANDLED', 'REJECTED'))
);

-- ------------------------------------------------------
--  Read models: the lists of the screens read these views, the queries add paging, filters and security
-- ------------------------------------------------------

-- Owner and login of the owner (the base of the "my records" filter)
CREATE VIEW VetOwnUsr AS
SELECT o.IdeOwn, TRIM(u.l1_nom) AS Usr
FROM VetOwn o
         JOIN ope u ON u.IdeOpe = o.IdeOpe;

CREATE VIEW VetOwnLst AS
SELECT o.IdeOwn, o.Nam, o.Eml, o.Tel, o.Adr, o.Cit, o.RegDat, o.Nte,
       UPPER(SUBSTRING(o.Nam, 1, 1)) AS Ini,
       (SELECT COUNT(*) FROM VetPet p WHERE p.IdeOwn = o.IdeOwn AND p.Act = 1) AS Pets,
       CASE WHEN o.IdeOpe IS NULL THEN 0 ELSE 1 END AS Acc
FROM VetOwn o;

CREATE VIEW VetPetLst AS
SELECT p.IdePet, p.IdeOwn, p.Nam, p.Spc, p.Brd, p.Sex, p.BrnDat, p.Wgt, p.Chp, p.Act,
       UPPER(SUBSTRING(p.Nam, 1, 1)) AS Ini,
       o.Nam AS OwnNam,
       DATEDIFF('yy', p.BrnDat, TRUNC(CURRENT_DATE)) AS Age,
       (SELECT MIN(v.NxtDat) FROM VetVac v WHERE v.IdePet = p.IdePet AND v.NxtDat >= TRUNC(CURRENT_DATE)) AS NxtVac
FROM VetPet p
         JOIN VetOwn o ON o.IdeOwn = p.IdeOwn;

CREATE VIEW VetVisLst AS
SELECT v.IdeVis, v.IdePet, p.IdeOwn, p.Nam AS PetNam, o.Nam AS OwnNam, v.IdeVet, t.Nam AS VetNam,
       v.VisDat, v.VisTim, v.Typ, v.Dia, v.Trt, v.Fee
FROM VetVis v
         JOIN VetPet p ON p.IdePet = v.IdePet
         JOIN VetOwn o ON o.IdeOwn = p.IdeOwn
         JOIN VetVet t ON t.IdeVet = v.IdeVet;

CREATE VIEW VetAptLst AS
SELECT a.IdeApt, a.IdePet, a.IdeOwn, p.Nam AS PetNam, o.Nam AS OwnNam, a.IdeVet, t.Nam AS VetNam,
       a.AptDat, a.AptTim, a.Rea, a.Sta, a.Src, a.Nte,
       -- The note an owner may read: only the one the owner wrote. The notes of the staff stay internal
       CASE WHEN a.Src = 'OWNER' THEN a.Nte END AS PubNte,
       CASE a.Sta WHEN 'CONFIRMED' THEN 'success' WHEN 'PENDING' THEN 'warning' WHEN 'CANCELLED' THEN 'danger'
                  ELSE 'neutral' END AS Ton
FROM VetApt a
         JOIN VetPet p ON p.IdePet = a.IdePet
         JOIN VetOwn o ON o.IdeOwn = a.IdeOwn
         LEFT JOIN VetVet t ON t.IdeVet = a.IdeVet;

CREATE VIEW VetVacLst AS
SELECT v.IdeVac, v.IdePet, p.IdeOwn, p.Nam AS PetNam, o.Nam AS OwnNam, v.Vcc, v.VacDat, v.NxtDat,
       CASE WHEN v.NxtDat < TRUNC(CURRENT_DATE) THEN 'OVERDUE'
            WHEN v.NxtDat < TRUNC(CURRENT_DATE) + 30 DAY THEN 'DUE_SOON'
            ELSE 'UP_TO_DATE' END AS Sta,
       CASE WHEN v.NxtDat < TRUNC(CURRENT_DATE) THEN 'danger'
            WHEN v.NxtDat < TRUNC(CURRENT_DATE) + 30 DAY THEN 'warning'
            ELSE 'success' END AS Ton
FROM VetVac v
         JOIN VetPet p ON p.IdePet = v.IdePet
         JOIN VetOwn o ON o.IdeOwn = p.IdeOwn;

CREATE VIEW VetInvLst AS
SELECT i.IdeInv, i.IdeOwn, o.Nam AS OwnNam, i.IdeVis, p.Nam AS PetNam, v.Typ AS VisTyp, i.InvDat, i.Amt, i.Sta,
       'HP-' || LPAD(CAST(i.IdeInv AS VARCHAR(6)), 6, '0') AS InvNum,
       CAST(ROUND(i.Amt / 1.21, 2) AS decimal(9, 2)) AS Sub, CAST(i.Amt - ROUND(i.Amt / 1.21, 2) AS decimal(9, 2)) AS Tax,
       CASE i.Sta WHEN 'PAID' THEN 'success' WHEN 'PENDING' THEN 'warning' ELSE 'danger' END AS Ton
FROM VetInv i
         JOIN VetOwn o ON o.IdeOwn = i.IdeOwn
         LEFT JOIN VetVis v ON v.IdeVis = i.IdeVis
         LEFT JOIN VetPet p ON p.IdePet = v.IdePet;

-- Invoice document: the owner (to bill him), the amounts before tax and the tax. VetInv.Amt is the total with 21% VAT
CREATE VIEW VetInvDoc AS
SELECT i.IdeInv, i.IdeOwn, 'HP-' || LPAD(CAST(i.IdeInv AS VARCHAR(6)), 6, '0') AS InvNum, o.Nam AS OwnNam, o.Adr AS OwnAdr,
       o.Cit AS OwnCit, o.Eml AS OwnEml, p.Nam AS PetNam, i.InvDat, TO_CHAR(i.InvDat, 'YYYY-MM-DD') AS InvIso, i.Sta,
       CAST(ROUND(i.Amt / 1.21, 2) AS decimal(9, 2)) AS Sub,
       CAST(i.Amt - ROUND(i.Amt / 1.21, 2) AS decimal(9, 2)) AS Tax, i.Amt
FROM VetInv i
         JOIN VetOwn o ON o.IdeOwn = i.IdeOwn
         LEFT JOIN VetVis v ON v.IdeVis = i.IdeVis
         LEFT JOIN VetPet p ON p.IdePet = v.IdePet;

-- Lines of the invoices (with the owner, so the owner area can filter them). An invoice without lines, like the ones
-- the application creates with a visit, shows one line with its amount before tax
CREATE VIEW VetInvLinDoc AS
SELECT i.IdeInv, i.IdeOwn, l.Pos, l.Con, l.Qty, l.Prc, l.Amt
FROM VetInvLin l
         JOIN VetInv i ON i.IdeInv = l.IdeInv
UNION ALL
SELECT i.IdeInv, i.IdeOwn, 1, 'VISIT', 1, CAST(ROUND(i.Amt / 1.21, 2) AS decimal(9, 2)),
       CAST(ROUND(i.Amt / 1.21, 2) AS decimal(9, 2))
FROM VetInv i
WHERE NOT EXISTS (SELECT 1 FROM VetInvLin l WHERE l.IdeInv = i.IdeInv);

CREATE VIEW VetDocLst AS
SELECT d.IdeDoc, d.IdePet, p.IdeOwn, p.Nam AS PetNam, d.DocDat, d.Typ, d.Des, d.FilNam
FROM VetDoc d
         JOIN VetPet p ON p.IdePet = d.IdePet;

-- Dashboard: visits and revenue per month (last 12 months), pets per species and the key figures of the cards.
-- The key figures compare the last 30 days with the 30 days before
-- Key figures of the home of a pet owner (one row per owner)
CREATE VIEW VetOwnKpi AS
SELECT o.IdeOwn,
       (SELECT COUNT(*) FROM VetPet p WHERE p.IdeOwn = o.IdeOwn AND p.Act = 1) AS Pets,
       (SELECT MIN(a.AptDat) FROM VetApt a
        WHERE a.IdeOwn = o.IdeOwn AND a.AptDat >= TRUNC(CURRENT_DATE) AND a.Sta IN ('PENDING', 'CONFIRMED')) AS NxtApt,
       (SELECT COALESCE(SUM(i.Amt), 0) FROM VetInv i WHERE i.IdeOwn = o.IdeOwn AND i.Sta <> 'PAID') AS InvDue,
       (SELECT COUNT(*) FROM VetVac v JOIN VetPet p ON p.IdePet = v.IdePet
        WHERE p.IdeOwn = o.IdeOwn AND v.NxtDat < TRUNC(CURRENT_DATE) + 30 DAY) AS VacDue
FROM VetOwn o;

CREATE VIEW VetVisMon AS
SELECT TO_CHAR(VisDat, 'YYYY-MM') AS Mon, COUNT(*) AS Vis, SUM(Fee) AS Rev
FROM VetVis
WHERE VisDat >= TRUNC(CURRENT_DATE) - 365 DAY
GROUP BY TO_CHAR(VisDat, 'YYYY-MM');

CREATE VIEW VetSpcCnt AS
SELECT Spc, COUNT(*) AS Cnt
FROM VetPet
WHERE Act = 1
GROUP BY Spc;

CREATE VIEW VetKpi AS
SELECT k.VisCur, k.RevCur, k.PetAct, k.ReqPen, k.VacOvd,
       CASE WHEN k.VisPrv = 0 THEN 0 ELSE ROUND((k.VisCur - k.VisPrv) * 100.0 / k.VisPrv, 1) END AS VisDel,
       CASE WHEN k.RevPrv = 0 THEN 0 ELSE ROUND((k.RevCur - k.RevPrv) * 100.0 / k.RevPrv, 1) END AS RevDel
FROM (SELECT (SELECT COUNT(*) FROM VetVis WHERE VisDat >= TRUNC(CURRENT_DATE) - 30 DAY) AS VisCur,
             (SELECT COUNT(*) FROM VetVis WHERE VisDat >= TRUNC(CURRENT_DATE) - 60 DAY
                                            AND VisDat < TRUNC(CURRENT_DATE) - 30 DAY) AS VisPrv,
             (SELECT COALESCE(SUM(Fee), 0) FROM VetVis WHERE VisDat >= TRUNC(CURRENT_DATE) - 30 DAY) AS RevCur,
             (SELECT COALESCE(SUM(Fee), 0) FROM VetVis WHERE VisDat >= TRUNC(CURRENT_DATE) - 60 DAY
                                                         AND VisDat < TRUNC(CURRENT_DATE) - 30 DAY) AS RevPrv,
             (SELECT COUNT(*) FROM VetPet WHERE Act = 1) AS PetAct,
             (SELECT COUNT(*) FROM VetReq WHERE Sta = 'PENDING') AS ReqPen,
             (SELECT COUNT(*) FROM VetVac WHERE NxtDat < TRUNC(CURRENT_DATE)) AS VacOvd
      FROM DUAL) k;

-- ------------------------------------------------------
--  Profiles. The restriction file (Res) is application/vetclinic/profile/<Res>.xml
--    vet   staff: manages the whole clinic
--    owner pet owners: see and request only their own records
-- ------------------------------------------------------
INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act)
VALUES (2, 'VET', 'Veterinary staff', 11, 'vet-dashboard', 'vet', 1);
INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act)
VALUES (3, 'OWN', 'Pet owner', 11, 'vet-my-home', 'owner', 1);
UPDATE AweKey SET KeyVal = 3 WHERE KeyNam = 'ProKey';

-- ------------------------------------------------------
--  Demo users (passwords are BCrypt hashes, see the README)
--    staff  : dr.garcia, dr.lopez          password vetdemo
--    owners : ana, luis, marie             password petdemo
-- ------------------------------------------------------
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (101, 'dr.garcia', '{bcrypt}$2y$10$qy419q8CvMLJCHoWZTAB/usQHk8ULCQrFbt9VlUxQZiQCmMnkLhGO', 0, 1, 'en-GB',
        'garcia@vetclinic.example', 'Dr. Elena Garcia', 2, 11, 'vet-dashboard', 'vet', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (102, 'dr.lopez', '{bcrypt}$2y$10$qy419q8CvMLJCHoWZTAB/usQHk8ULCQrFbt9VlUxQZiQCmMnkLhGO', 0, 1, 'es-ES',
        'lopez@vetclinic.example', 'Dr. Marcos Lopez', 2, 11, 'vet-dashboard', 'vet', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (201, 'ana', '{bcrypt}$2y$10$cJjYesqTk98yBsnrOFbBp.tkY.8ZDbbOTRpaciANHhnn.gDLI2CcS', 0, 1, 'es-ES',
        'ana.ruiz@mail.example', 'Ana Ruiz', 3, 11, 'vet-my-home', 'owner', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (202, 'luis', '{bcrypt}$2y$10$cJjYesqTk98yBsnrOFbBp.tkY.8ZDbbOTRpaciANHhnn.gDLI2CcS', 0, 1, 'en-GB',
        'luis.mora@mail.example', 'Luis Mora', 3, 11, 'vet-my-home', 'owner', 0);
INSERT INTO ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_lan, EmlAdr, OpeNam, IdePro, IdeThm, ScrIni, Res, PwdLck)
VALUES (203, 'marie', '{bcrypt}$2y$10$cJjYesqTk98yBsnrOFbBp.tkY.8ZDbbOTRpaciANHhnn.gDLI2CcS', 0, 1, 'fr-FR',
        'marie.dubois@mail.example', 'Marie Dubois', 3, 11, 'vet-my-home', 'owner', 0);
UPDATE AweKey SET KeyVal = 300 WHERE KeyNam = 'OpeKey';

-- ------------------------------------------------------
--  Demo data
-- ------------------------------------------------------
INSERT INTO VetVet (IdeVet, IdeOpe, Nam, Spe) VALUES (1, 101, 'Dr. Elena Garcia', 'Internal medicine');
INSERT INTO VetVet (IdeVet, IdeOpe, Nam, Spe) VALUES (2, 102, 'Dr. Marcos Lopez', 'Surgery');
INSERT INTO VetVet (IdeVet, IdeOpe, Nam, Spe) VALUES (3, NULL, 'Dr. Claire Martin', 'Exotic animals');

-- Owners 1 to 3 can sign in (ana, luis, marie)
INSERT INTO VetOwn (IdeOwn, IdeOpe, Nam, Eml, Tel, Adr, Cit, RegDat, Nte) VALUES
    (1, 201, 'Ana Ruiz', 'ana.ruiz@mail.example', '600 111 201', 'Calle Mayor 12', 'Madrid', DATE '2021-02-11', 'Prefers morning appointments'),
    (2, 202, 'Luis Mora', 'luis.mora@mail.example', '600 111 202', 'Avenida del Mar 5', 'Valencia', DATE '2020-06-03', NULL),
    (3, 203, 'Marie Dubois', 'marie.dubois@mail.example', '600 111 203', 'Rue de la Paix 8', 'Lyon', DATE '2022-09-19', 'Speaks French and Spanish'),
    (4, NULL, 'Carlos Navarro', 'carlos.navarro@mail.example', '600 111 204', 'Calle Sol 3', 'Madrid', DATE '2019-01-21', NULL),
    (5, NULL, 'Laura Gil', 'laura.gil@mail.example', '600 111 205', 'Plaza Nueva 9', 'Sevilla', DATE '2023-03-02', NULL),
    (6, NULL, 'Pedro Santos', 'pedro.santos@mail.example', '600 111 206', 'Calle Larga 41', 'Madrid', DATE '2018-11-30', 'Two cats and a dog'),
    (7, NULL, 'Sofia Rossi', 'sofia.rossi@mail.example', '600 111 207', 'Via Roma 17', 'Barcelona', DATE '2022-04-14', NULL),
    (8, NULL, 'Javier Ortega', 'javier.ortega@mail.example', '600 111 208', 'Calle Luna 22', 'Madrid', DATE '2021-12-05', NULL),
    (9, NULL, 'Elisa Moreno', 'elisa.moreno@mail.example', '600 111 209', 'Paseo Verde 2', 'Bilbao', DATE '2024-01-09', NULL),
    (10, NULL, 'Hugo Castro', 'hugo.castro@mail.example', '600 111 210', 'Calle Rio 30', 'Madrid', DATE '2020-10-27', NULL),
    (11, NULL, 'Nuria Vidal', 'nuria.vidal@mail.example', '600 111 211', 'Calle Alta 7', 'Valencia', DATE '2023-07-18', NULL),
    (12, NULL, 'Tomas Ibanez', 'tomas.ibanez@mail.example', '600 111 212', 'Avenida Sur 66', 'Madrid', DATE '2019-05-23', 'Breeder of rabbits'),
    (13, NULL, 'Irene Soler', 'irene.soler@mail.example', '600 111 213', 'Calle Norte 14', 'Sevilla', DATE '2024-02-12', NULL),
    (14, NULL, 'David Prieto', 'david.prieto@mail.example', '600 111 214', 'Calle Mar 90', 'Madrid', DATE '2022-08-08', NULL),
    (15, NULL, 'Clara Benitez', 'clara.benitez@mail.example', '600 111 215', 'Plaza Mayor 1', 'Madrid', DATE '2025-01-15', NULL);

INSERT INTO VetPet (IdePet, IdeOwn, Nam, Spc, Brd, Sex, BrnDat, Wgt, Chp) VALUES
    (1, 1, 'Luna', 'DOG', 'Labrador Retriever', 'F', DATE '2019-03-14', 28.40, '941000001000001'),
    (2, 1, 'Mishi', 'CAT', 'European Shorthair', 'F', DATE '2020-07-02', 4.10, '941000001000002'),
    (3, 2, 'Rex', 'DOG', 'German Shepherd', 'M', DATE '2018-11-21', 34.60, '941000001000003'),
    (4, 2, 'Kiwi', 'BIRD', 'Cockatiel', 'M', DATE '2022-04-10', 0.09, NULL),
    (5, 3, 'Noisette', 'RABBIT', 'Mini Lop', 'F', DATE '2023-05-30', 1.70, NULL),
    (6, 3, 'Felix', 'CAT', 'Maine Coon', 'M', DATE '2021-01-17', 6.80, '941000001000006'),
    (7, 3, 'Olive', 'DOG', 'French Bulldog', 'F', DATE '2022-09-09', 11.20, '941000001000007'),
    (8, 4, 'Thor', 'DOG', 'Boxer', 'M', DATE '2017-12-01', 31.00, '941000001000008'),
    (9, 5, 'Nala', 'CAT', 'Siamese', 'F', DATE '2021-06-23', 4.40, '941000001000009'),
    (10, 6, 'Simba', 'CAT', 'Persian', 'M', DATE '2019-08-15', 5.20, '941000001000010'),
    (11, 6, 'Cleo', 'CAT', 'Bengal', 'F', DATE '2020-02-02', 4.00, '941000001000011'),
    (12, 6, 'Max', 'DOG', 'Beagle', 'M', DATE '2018-05-19', 14.30, '941000001000012'),
    (13, 7, 'Bella', 'DOG', 'Golden Retriever', 'F', DATE '2020-10-10', 27.50, '941000001000013'),
    (14, 8, 'Coco', 'BIRD', 'African Grey Parrot', 'F', DATE '2015-03-03', 0.45, NULL),
    (15, 8, 'Rocky', 'DOG', 'Staffordshire Terrier', 'M', DATE '2019-12-12', 17.90, '941000001000015'),
    (16, 9, 'Misty', 'CAT', 'Ragdoll', 'F', DATE '2023-01-20', 4.90, '941000001000016'),
    (17, 10, 'Duke', 'DOG', 'Rottweiler', 'M', DATE '2016-07-07', 42.00, '941000001000017'),
    (18, 10, 'Lola', 'DOG', 'Dachshund', 'F', DATE '2021-04-04', 8.30, '941000001000018'),
    (19, 11, 'Pipo', 'RABBIT', 'Dwarf', 'M', DATE '2022-11-11', 1.40, NULL),
    (20, 11, 'Gala', 'CAT', 'European Shorthair', 'F', DATE '2018-09-29', 4.60, '941000001000020'),
    (21, 12, 'Snow', 'RABBIT', 'Angora', 'F', DATE '2023-08-08', 2.10, NULL),
    (22, 12, 'Cocoa', 'RABBIT', 'Rex', 'M', DATE '2023-08-08', 2.30, NULL),
    (23, 12, 'Ziggy', 'REPTILE', 'Bearded Dragon', 'M', DATE '2021-02-14', 0.55, NULL),
    (24, 13, 'Daisy', 'DOG', 'Cavalier King Charles', 'F', DATE '2022-06-06', 8.70, '941000001000024'),
    (25, 14, 'Oscar', 'CAT', 'British Shorthair', 'M', DATE '2017-10-31', 6.10, '941000001000025'),
    (26, 14, 'Milo', 'DOG', 'Jack Russell', 'M', DATE '2020-03-23', 7.80, '941000001000026'),
    (27, 14, 'Rio', 'REPTILE', 'Corn Snake', 'M', DATE '2022-07-01', 0.60, NULL),
    (28, 15, 'Maya', 'DOG', 'Border Collie', 'F', DATE '2024-05-05', 19.40, '941000001000028'),
    (29, 15, 'Tigre', 'CAT', 'Tabby', 'M', DATE '2024-05-05', 3.90, '941000001000029'),
    (30, 7, 'Mia', 'CAT', 'Norwegian Forest', 'F', DATE '2021-12-24', 5.30, '941000001000030');

-- 160 visits spread over the last 12 months: pet, veterinarian, type, date and time follow the visit number
INSERT INTO VetVis (IdeVis, IdePet, IdeVet, VisDat, VisTim, Typ, Dia, Trt, Fee)
SELECT n,
       1 + MOD(n * 7, 30),
       1 + MOD(n, 3),
       TRUNC(CURRENT_DATE) - (n * 2 + MOD(n, 3)) DAY,
       TIME '09:00:00' + (MOD(n * 37, 480)) MINUTE,
       CASE MOD(n, 10) WHEN 0 THEN 'SURGERY' WHEN 1 THEN 'DENTAL' WHEN 2 THEN 'VACCINATION' WHEN 3 THEN 'CHECKUP'
                       WHEN 4 THEN 'VACCINATION' WHEN 5 THEN 'CHECKUP' WHEN 6 THEN 'EMERGENCY' WHEN 7 THEN 'CHECKUP'
                       WHEN 8 THEN 'VACCINATION' ELSE 'CHECKUP' END,
       CASE MOD(n, 10) WHEN 0 THEN 'Tumour removal, good recovery' WHEN 1 THEN 'Tartar and gingivitis'
                       WHEN 2 THEN 'Healthy, vaccine applied' WHEN 3 THEN 'Healthy, weight in range'
                       WHEN 4 THEN 'Healthy, vaccine applied' WHEN 5 THEN 'Mild skin allergy'
                       WHEN 6 THEN 'Ingested a foreign object' WHEN 7 THEN 'Ear infection'
                       WHEN 8 THEN 'Healthy, booster applied' ELSE 'Healthy, annual check' END,
       CASE MOD(n, 10) WHEN 0 THEN 'Antibiotics for 7 days and rest' WHEN 1 THEN 'Dental cleaning under sedation'
                       WHEN 2 THEN 'Next dose in 12 months' WHEN 3 THEN 'None'
                       WHEN 4 THEN 'Next dose in 12 months' WHEN 5 THEN 'Antihistamine and special diet'
                       WHEN 6 THEN 'Endoscopy and observation 24 h' WHEN 7 THEN 'Ear drops twice a day for 10 days'
                       WHEN 8 THEN 'Next dose in 12 months' ELSE 'Deworming' END,
       CASE MOD(n, 10) WHEN 0 THEN 380 WHEN 1 THEN 120 WHEN 2 THEN 38 WHEN 3 THEN 45 WHEN 4 THEN 38 WHEN 5 THEN 52
                       WHEN 6 THEN 150 WHEN 7 THEN 48 WHEN 8 THEN 42 ELSE 45 END + MOD(n, 7) * 3
FROM UNNEST(SEQUENCE_ARRAY(1, 160, 1)) AS t (n);

-- Vaccinations: two per pet. The first one is a year old or more for some pets, so some are overdue
INSERT INTO VetVac (IdeVac, IdePet, IdeVet, Vcc, VacDat, NxtDat)
SELECT p.IdePet * 2 - 1, p.IdePet, 1 + MOD(p.IdePet, 3),
       CASE p.Spc WHEN 'DOG' THEN 'RABIES' WHEN 'CAT' THEN 'FELINE_LEUKEMIA' WHEN 'RABBIT' THEN 'MYXOMATOSIS'
                  WHEN 'BIRD' THEN 'POLYVALENT' ELSE 'POLYVALENT' END,
       TRUNC(CURRENT_DATE) - (120 + MOD(p.IdePet * 53, 420)) DAY,
       TRUNC(CURRENT_DATE) - (120 + MOD(p.IdePet * 53, 420)) DAY + 365 DAY
FROM VetPet p;
INSERT INTO VetVac (IdeVac, IdePet, IdeVet, Vcc, VacDat, NxtDat)
SELECT p.IdePet * 2, p.IdePet, 1 + MOD(p.IdePet + 1, 3),
       CASE p.Spc WHEN 'DOG' THEN 'KENNEL_COUGH' WHEN 'CAT' THEN 'POLYVALENT' WHEN 'RABBIT' THEN 'POLYVALENT'
                  ELSE 'RABIES' END,
       TRUNC(CURRENT_DATE) - (40 + MOD(p.IdePet * 29, 300)) DAY,
       TRUNC(CURRENT_DATE) - (40 + MOD(p.IdePet * 29, 300)) DAY + 365 DAY
FROM VetPet p;

-- Appointments from 12 days ago to 23 days ahead
INSERT INTO VetApt (IdeApt, IdePet, IdeOwn, IdeVet, AptDat, AptTim, Rea, Sta, Src, Nte)
SELECT n, p.IdePet, p.IdeOwn, 1 + MOD(n, 3),
       TRUNC(CURRENT_DATE) + (n - 12) DAY,
       TIME '09:00:00' + (MOD(n, 10) * 30) MINUTE,
       CASE MOD(n, 5) WHEN 0 THEN 'VACCINATION' WHEN 1 THEN 'CHECKUP' WHEN 2 THEN 'DENTAL' WHEN 3 THEN 'CHECKUP'
                      ELSE 'SURGERY' END,
       CASE WHEN n < 12 THEN CASE WHEN MOD(n, 6) = 0 THEN 'CANCELLED' ELSE 'DONE' END
            WHEN MOD(n, 3) = 0 THEN 'PENDING' ELSE 'CONFIRMED' END,
       CASE WHEN MOD(n, 4) = 0 THEN 'OWNER' ELSE 'STAFF' END,
       CASE WHEN MOD(n, 4) = 0 THEN 'Requested from the owner area' ELSE NULL END
FROM UNNEST(SEQUENCE_ARRAY(1, 35, 1)) AS t (n)
         JOIN VetPet p ON p.IdePet = 1 + MOD(n * 7, 30);

-- One invoice per visit; the latest unpaid ones are pending, the older ones overdue
INSERT INTO VetInv (IdeInv, IdeOwn, IdeVis, InvDat, Amt, Sta)
SELECT v.IdeVis, p.IdeOwn, v.IdeVis, v.VisDat, v.Fee,
       CASE WHEN v.IdeVis > 10 THEN 'PAID' WHEN v.VisDat > TRUNC(CURRENT_DATE) - 14 DAY THEN 'PENDING' ELSE 'OVERDUE' END
FROM VetVis v
         JOIN VetPet p ON p.IdePet = v.IdePet;

-- Three lines per invoice that add up to the amount before tax: the visit, the materials and the clinic fee
INSERT INTO VetInvLin (IdeInv, Pos, Con, Qty, Prc, Amt)
SELECT i.IdeInv, 1, v.Typ, 1, ROUND(ROUND(i.Amt / 1.21, 2) * 0.6, 2), ROUND(ROUND(i.Amt / 1.21, 2) * 0.6, 2)
FROM VetInv i
         JOIN VetVis v ON v.IdeVis = i.IdeVis;
INSERT INTO VetInvLin (IdeInv, Pos, Con, Qty, Prc, Amt)
SELECT i.IdeInv, 2, 'MATERIALS', 1 + MOD(i.IdeInv, 3), ROUND(ROUND(i.Amt / 1.21, 2) * 0.25 / (1 + MOD(i.IdeInv, 3)), 2),
       (1 + MOD(i.IdeInv, 3)) * ROUND(ROUND(i.Amt / 1.21, 2) * 0.25 / (1 + MOD(i.IdeInv, 3)), 2)
FROM VetInv i;
INSERT INTO VetInvLin (IdeInv, Pos, Con, Qty, Prc, Amt)
SELECT i.IdeInv, 3, 'CLINIC', 1, ROUND(i.Amt / 1.21, 2) - (SELECT SUM(l.Amt) FROM VetInvLin l WHERE l.IdeInv = i.IdeInv),
       ROUND(i.Amt / 1.21, 2) - (SELECT SUM(l.Amt) FROM VetInvLin l WHERE l.IdeInv = i.IdeInv)
FROM VetInv i;

INSERT INTO VetDoc (IdeDoc, IdePet, DocDat, Typ, Des, FilNam) VALUES
    (1, 3, TRUNC(CURRENT_DATE) - 40 DAY, 'XRAY', 'Hip X-ray, right side', 'rex-hip-xray.png'),
    (2, 3, TRUNC(CURRENT_DATE) - 38 DAY, 'REPORT', 'Orthopaedic report', 'rex-orthopaedic-report.pdf'),
    (3, 1, TRUNC(CURRENT_DATE) - 90 DAY, 'LAB', 'Blood test, annual', 'luna-blood-test.pdf'),
    (4, 8, TRUNC(CURRENT_DATE) - 15 DAY, 'XRAY', 'Chest X-ray', 'thor-chest-xray.png'),
    (5, 17, TRUNC(CURRENT_DATE) - 60 DAY, 'LAB', 'Kidney profile', 'duke-kidney-profile.pdf'),
    (6, 6, TRUNC(CURRENT_DATE) - 22 DAY, 'REPORT', 'Dental cleaning report', 'felix-dental-report.pdf'),
    (7, 23, TRUNC(CURRENT_DATE) - 8 DAY, 'OTHER', 'Terrarium setup advice', 'ziggy-terrarium.pdf');

INSERT INTO VetReq (IdeReq, ReqDat, OwnNam, Eml, Tel, PetNam, Spc, PrfDat, Rea, Nte, Sta) VALUES
    (1, CURRENT_TIMESTAMP - 5 HOUR, 'Patricia Vega', 'patricia.vega@mail.example', '611 222 301', 'Boli', 'DOG', TRUNC(CURRENT_DATE) + 3 DAY, 'CHECKUP', 'First visit, we just adopted him', 'PENDING'),
    (2, CURRENT_TIMESTAMP - 1 DAY, 'Andres Cruz', 'andres.cruz@mail.example', '611 222 302', 'Misu', 'CAT', TRUNC(CURRENT_DATE) + 5 DAY, 'VACCINATION', NULL, 'PENDING'),
    (3, CURRENT_TIMESTAMP - 2 DAY, 'Lucia Ferrer', 'lucia.ferrer@mail.example', NULL, 'Pepe', 'BIRD', TRUNC(CURRENT_DATE) + 7 DAY, 'CHECKUP', 'He stopped singing', 'PENDING'),
    (4, CURRENT_TIMESTAMP - 6 DAY, 'Mario Diaz', 'mario.diaz@mail.example', '611 222 304', 'Toby', 'DOG', TRUNC(CURRENT_DATE) - 1 DAY, 'EMERGENCY', 'Limping since yesterday', 'HANDLED'),
    (5, CURRENT_TIMESTAMP - 9 DAY, 'Eva Marin', 'eva.marin@mail.example', '611 222 305', 'Greta', 'REPTILE', TRUNC(CURRENT_DATE) - 3 DAY, 'CHECKUP', NULL, 'REJECTED');

-- AWE sequences: next key = last seeded key
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('OwnKey', 15);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('PetKey', 30);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('VisKey', 160);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('VacKey', 60);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('AptKey', 35);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('InvKey', 160);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('DocKey', 7);
INSERT INTO AweKey (KeyNam, KeyVal) VALUES ('ReqKey', 5);
