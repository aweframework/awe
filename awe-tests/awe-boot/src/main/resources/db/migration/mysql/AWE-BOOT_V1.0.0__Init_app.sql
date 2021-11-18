-- Profiles
Insert into AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) values ('2','GNR','General','1',null,'general','1');
Insert into AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) values ('3','OPE','Operator','1',null,'operator','1');
Insert into AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) values ('4','TST','Test','1',null,'test','1');

-- Update ProKey
UPDATE AweKey SET KeyVal = '5' where KeyNam = 'ProKey';

-- Insert AweMod
Insert into AweMod (IdeMod, Nam, ScrIni, IdeThm, Act, Ord) values (1,	'Test',	'Dbs',	4,	1, 2);
Insert into AweMod (IdeMod, Nam, ScrIni, IdeThm, Act, Ord) values (2,	'Base',	'Sit',	2,	1, 1);

-- Update ModKey
UPDATE AweKey SET KeyVal = '3' where KeyNam = 'ModKey';

-- Insert AweModPro
insert into AweModPro (IdeModPro, IdeMod, IdePro, Ord)
values (1, 1, 1, null);
insert into AweModPro (IdeModPro, IdeMod, IdePro, Ord)
values (2, 2, 1, null);
insert into AweModPro (IdeModPro, IdeMod, IdePro, Ord)
values (3, 2, 2, null);
insert into AweModPro (IdeModPro, IdeMod, IdePro, Ord)
values (4, 2, 3, null);

-- Update ModProKey
UPDATE AweKey
SET KeyVal = '5'
where KeyNam = 'ModProKey';

-- ------------------------------------------------------
--  TESTING TABLES
-- ------------------------------------------------------

CREATE TABLE IF NOT EXISTS DummyClobTestTable
(
    id       INTEGER AUTO_INCREMENT PRIMARY KEY,
    textFile LONGTEXT -- CLOB TYPE
);

CREATE TABLE IF NOT EXISTS TST_COUNTRY
(
    ID INTEGER PRIMARY KEY NOT NULL,
    COUNTRY_NAME VARCHAR(50),
    COUNTRY_CODE VARCHAR(3)
);
CREATE UNIQUE INDEX IDX_COUNTRY ON TST_COUNTRY (COUNTRY_NAME);


CREATE TABLE IF NOT EXISTS TST_CUSTOMER
(
    ID INTEGER PRIMARY KEY NOT NULL,
    CUSTOMER_NAME    VARCHAR(255),
    CUSTOMER_ADDRESS VARCHAR(255),
    COUNTRY_ID       INTEGER               NOT NULL,
    CUSTOMER_DATE    DATE                  NOT NULL
);
CREATE UNIQUE INDEX IDX_CUSTOMER ON TST_CUSTOMER (CUSTOMER_NAME);
ALTER TABLE TST_CUSTOMER ADD CONSTRAINT FK_COUNTRIES FOREIGN KEY (COUNTRY_ID) REFERENCES TST_COUNTRY (ID);

-- Insert Countries
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE)
values ('1', 'Spain', 'ESP');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE)
values ('2', 'France', 'FRA');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE)
values ('3', 'United States', 'USA');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE)
values ('4', 'Portugal', 'POR');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE)
values ('5', 'Italy', 'ITA');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE)
values ('6', 'United Kingdom', 'UK');

UPDATE AweKey SET KeyVal = '7' where KeyNam = 'CountryKey';

-- Insert Customers
Insert into TST_CUSTOMER (ID, CUSTOMER_NAME, CUSTOMER_ADDRESS, COUNTRY_ID, CUSTOMER_DATE)
values ('1', 'Customer1', 'Liverpool Street 4, London', '6', '2021-02-10 00:00:00');
Insert into TST_CUSTOMER (ID, CUSTOMER_NAME, CUSTOMER_ADDRESS, COUNTRY_ID, CUSTOMER_DATE)
values ('2', 'Customer2', 'Gran Vía 1, Madrid', '1', '2020-08-14 00:00:00');
Insert into TST_CUSTOMER (ID, CUSTOMER_NAME, CUSTOMER_ADDRESS, COUNTRY_ID, CUSTOMER_DATE)
values ('3', 'Customer3', 'Santa Clara 34, Zamora', '1', '2021-06-05 00:00:00');
Insert into TST_CUSTOMER (ID, CUSTOMER_NAME, CUSTOMER_ADDRESS, COUNTRY_ID, CUSTOMER_DATE)
values ('4', 'Customer4', '5th Ave 112, New York', '3', '2021-01-18 00:00:00');
Insert into TST_CUSTOMER (ID, CUSTOMER_NAME, CUSTOMER_ADDRESS, COUNTRY_ID, CUSTOMER_DATE)
values ('5', 'Customer5', 'Via del Corso 29, Rome', '5', '2020-06-28 00:00:00');

UPDATE AweKey SET KeyVal = '6' where KeyNam = 'CustomerKey';