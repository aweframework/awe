-- Test data for the screens of the test application that read sites, site/module/database relations and the
-- database history: the SQL and service totalization grids of the matrix test (QrySitModDbsOrd) and the chart examples
-- (the HISdat dates of HISAweDbs are the points of the charts). Ids reference the modules and databases of V1.0.0

-- Insert AweDbs: the MySQL migration V1.0.0 has no databases (the HSQLDB one has these three)
Insert into AweDbs (IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values (1, 'awedb1', 'AWE DB 1', 'J', 'mysql', 'com.mysql.cj.jdbc.Driver', 'awe', null, 'Des', 'jdbc:mysql://localhost:3306/awe', 1);
Insert into AweDbs (IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values (2, 'awedb2', 'AWE DB 2', 'J', 'mysql', 'com.mysql.cj.jdbc.Driver', 'awe', null, 'Des', 'jdbc:mysql://localhost:3306/awe', 1);
Insert into AweDbs (IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values (3, 'awedb3', 'AWE DB 3', 'J', 'mysql', 'com.mysql.cj.jdbc.Driver', 'awe', null, 'Des', 'jdbc:mysql://localhost:3306/awe', 1);

-- Update DbsKey
UPDATE AweKey SET KeyVal = '4' where KeyNam = 'DbsKey';

-- Insert AweSit
Insert into AweSit (IdeSit, Nam, Ord, Act) values (10, 'Madrid', 2, 1);
Insert into AweSit (IdeSit, Nam, Ord, Act) values (17, 'Onate', 1, 1);

-- Update SitKey
UPDATE AweKey SET KeyVal = '18' where KeyNam = 'SitKey';

-- Insert AweSitModDbs: the totalization grid sorts by database and subtotals by module, so each module keeps its own
-- databases (Base: awedb1 and awedb2, Test: awedb3)
Insert into AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) values (100, 17, 2, 1, 1);
Insert into AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) values (101, 17, 2, 2, 2);
Insert into AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) values (102, 17, 1, 3, 3);
Insert into AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) values (103, 10, 2, 1, 4);
Insert into AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) values (104, 10, 2, 2, 5);
Insert into AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) values (105, 10, 1, 3, 6);

-- Update SitModDbsKey
UPDATE AweKey SET KeyVal = '106' where KeyNam = 'SitModDbsKey';

-- Insert HISAweDbs: one change a month over two years
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-01-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-02-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-03-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-04-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-05-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-06-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-07-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-08-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-09-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-10-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-11-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2024-12-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-01-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-02-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-03-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-04-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-05-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-06-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-07-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-08-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-09-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-10-15', 'U', 1, 'awedb1', 'AWE DB 1', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-11-15', 'U', 2, 'awedb2', 'AWE DB 2', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
Insert into HISAweDbs (HISope, HISdat, HISact, IdeDbs, Als, Des, Dct, Dbt, Drv, DbsUsr, DbsPwd, Typ, Dbc, Act) values ('test', DATE '2025-12-15', 'U', 3, 'awedb3', 'AWE DB 3', 'J', 'hsql', 'org.hsqldb.jdbc.JDBCDriver', 'sa', null, 'Des', 'jdbc:hsqldb:file:target/db/awe-angular-redux', 1);
