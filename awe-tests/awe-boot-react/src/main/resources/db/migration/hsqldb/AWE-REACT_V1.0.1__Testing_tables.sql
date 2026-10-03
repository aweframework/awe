--------------------------------------------------------
--  TESTING TABLES (the ones of the AngularJS test application that the React suites use)
--------------------------------------------------------

CREATE TABLE IF NOT EXISTS TST_COUNTRY
(
    ID           INTEGER
        CONSTRAINT PK_COUNTRY PRIMARY KEY NOT NULL,
    COUNTRY_NAME VARCHAR(50),
    COUNTRY_CODE VARCHAR(3)
);
CREATE UNIQUE INDEX IF NOT EXISTS IDX_COUNTRY ON TST_COUNTRY (COUNTRY_NAME);

CREATE TABLE IF NOT EXISTS TST_CUSTOMER
(
    ID               INTEGER
        CONSTRAINT PK_CUSTOMER PRIMARY KEY NOT NULL,
    CUSTOMER_NAME    VARCHAR(255),
    CUSTOMER_ADDRESS VARCHAR(255),
    COUNTRY_ID       INTEGER               NOT NULL,
    CUSTOMER_DATE    DATE                  NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS IDX_CUSTOMER ON TST_CUSTOMER (CUSTOMER_NAME);
ALTER TABLE TST_CUSTOMER
    ADD CONSTRAINT IF NOT EXISTS FK_COUNTRIES FOREIGN KEY (COUNTRY_ID) REFERENCES TST_COUNTRY (ID);

-- Insert Countries
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE) values ('1', 'Spain', 'ESP');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE) values ('2', 'France', 'FRA');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE) values ('3', 'United States', 'USA');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE) values ('4', 'Portugal', 'POR');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE) values ('5', 'Italy', 'ITA');
Insert into TST_COUNTRY (ID, COUNTRY_NAME, COUNTRY_CODE) values ('6', 'United Kingdom', 'UK');

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
