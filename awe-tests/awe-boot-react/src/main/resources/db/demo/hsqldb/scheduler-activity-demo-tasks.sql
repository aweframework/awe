-- Demo data of the scheduler Activity screen (awe-boot-react, hot-reload profile only): tasks.
-- Treasury back office of the redesign mock-ups, ids 9012-9018 (out of the range of the SchTsk* keys). Every task runs a
-- harmless test maintain: waitSomeSeconds (sleeps a few seconds) or deferredTestLaunch (answers accepted, finishes later).
--   9012 Importar extractos bancarios      file (SFTP sftp-bancos, every 5 min), inactive so it never polls the server
--   9013 Conciliación bancaria             child of 9012 (its dependants also run on warning)
--   9014 Actualizar posiciones de tesorería child of 9012
--   9015 Informe diario de tesorería       child of 9013 and 9014 (fan-in)
--   9016 Envío de remesas SEPA             child of 9015, deferred answer, timeout 4 h
--   9017 Cierre contable diario            scheduled, Monday to Friday at 22:30, except the dates of calendar 9001
--   9018 Purga de logs                     scheduled, every day at 03:00
--   9019 Recalcular previsiones             manual, asks for the value of its parameter when it is launched
--   9020 Confirmación de cobros del banco   webhook (its hook key is a demo value, no secret property is set)

-- Servers of the configuration screen (no credentials): the SFTP server of the bank statements, an FTP server and the
-- input folder of the application server, unused, and an old SFTP server, inactive and unused
INSERT INTO AweSchSrv (Ide, Nom, Pro, Hst, Prt, Act)
VALUES (9001, 'sftp-bancos', 'ssh', 'sftp.bancos.example', '22', 1),
       (9002, 'ftp-remesas', 'ftp', 'ftp.remesas.example', '21', 1),
       (9003, 'carpeta-entrada', 'folder', 'localhost', '0', 1),
       (9004, 'sftp-antiguo', 'ssh', 'sftp-antiguo.example', '22', 0);

-- Calendars of the configuration screen: the Spanish holidays of 2026, used by 9017; the TARGET2 closing days, unused;
-- and the Spanish holidays of 2027, ready for next year (inactive, unused). Inserted before the launches that use them
INSERT INTO AweSchCal (Ide, Nom, Des, Act)
VALUES (9001, 'Festivos ES 2026', 'Festivos nacionales de España: el cierre contable no se ejecuta estos días', 1),
       (9002, 'Cierre TARGET2 2026', 'Días sin liquidación en TARGET2 (BCE)', 1),
       (9003, 'Festivos ES 2027', 'Preparado para el año que viene', 0);

INSERT INTO AweSchCalDat (Ide, IdeCal, Nom, Dat)
VALUES (9001, 9001, 'Año Nuevo', DATE '2026-01-01'),
       (9002, 9001, 'Epifanía del Señor', DATE '2026-01-06'),
       (9003, 9001, 'Viernes Santo', DATE '2026-04-03'),
       (9004, 9001, 'Fiesta del Trabajo', DATE '2026-05-01'),
       (9005, 9001, 'Asunción de la Virgen', DATE '2026-08-15'),
       (9006, 9001, 'Fiesta Nacional de España', DATE '2026-10-12'),
       (9007, 9001, 'Todos los Santos', DATE '2026-11-01'),
       (9008, 9001, 'Día de la Constitución', DATE '2026-12-06'),
       (9009, 9001, 'Inmaculada Concepción', DATE '2026-12-08'),
       (9010, 9001, 'Navidad', DATE '2026-12-25'),
       (9011, 9002, 'Año Nuevo', DATE '2026-01-01'),
       (9012, 9002, 'Viernes Santo', DATE '2026-04-03'),
       (9013, 9002, 'Lunes de Pascua', DATE '2026-04-06'),
       (9014, 9002, 'Día del Trabajo', DATE '2026-05-01'),
       (9015, 9002, 'Navidad', DATE '2026-12-25'),
       (9016, 9002, 'San Esteban', DATE '2026-12-26'),
       (9017, 9003, 'Año Nuevo', DATE '2027-01-01'),
       (9018, 9003, 'Epifanía del Señor', DATE '2027-01-06'),
       (9019, 9003, 'Viernes Santo', DATE '2027-03-26'),
       (9020, 9003, 'Fiesta del Trabajo', DATE '2027-05-01'),
       (9021, 9003, 'Asunción de la Virgen', DATE '2027-08-15'),
       (9022, 9003, 'Fiesta Nacional de España', DATE '2027-10-12'),
       (9023, 9003, 'Todos los Santos', DATE '2027-11-01'),
       (9024, 9003, 'Día de la Constitución', DATE '2027-12-06'),
       (9025, 9003, 'Inmaculada Concepción', DATE '2027-12-08'),
       (9026, 9003, 'Navidad', DATE '2027-12-25');

INSERT INTO AweSchTsk (Ide, Nam, Des, NumStoExe, TimOutExe, TypExe, IdeSrvExe, CmdExe, TypLch, LchDepErr, LchDepWrn,
                       LchSetWrn, RepTyp, Act)
VALUES (9012, 'Importar extractos bancarios', 'Importa los extractos N43 que dejan los bancos en el SFTP', 50, NULL, 1,
        NULL, 'waitSomeSeconds', 2, 0, 0, 0, 0, 0),
       (9013, 'Conciliación bancaria', 'Casa los movimientos importados con las partidas contables', 50, NULL, 1, NULL,
        'waitSomeSeconds', 0, 0, 1, 0, 0, 1),
       (9014, 'Actualizar posiciones de tesorería', 'Recalcula la posición de tesorería por banco y divisa', 50, NULL, 1,
        NULL, 'waitSomeSeconds', 0, 0, 0, 0, 0, 1),
       (9015, 'Informe diario de tesorería', 'Genera el informe diario de tesorería para dirección financiera', 50, NULL,
        1, NULL, 'waitSomeSeconds', 0, 0, 0, 0, 0, 1),
       (9016, 'Envío de remesas SEPA', 'Envía las remesas SEPA al banco; termina cuando el banco confirma la recepción',
        50, 14400, 1, NULL, 'deferredTestLaunch', 0, 0, 0, 0, 0, 1),
       (9017, 'Cierre contable diario', 'Cierra el día contable y bloquea los asientos del periodo', 50, NULL, 1, NULL,
        'waitSomeSeconds', 1, 0, 0, 0, 0, 1),
       (9018, 'Purga de logs', 'Borra los ficheros de log de más de 30 días', 50, NULL, 1, NULL, 'waitSomeSeconds', 1, 0,
        0, 0, 0, 1),
       (9019, 'Recalcular previsiones', 'Recalcula la previsión de tesorería a 13 semanas con el escenario indicado', 50,
        NULL, 1, NULL, 'waitSomeSeconds', 0, 0, 0, 0, 0, 1),
       (9020, 'Confirmación de cobros del banco', 'Marca como cobrados los recibos que el banco confirma por webhook', 50,
        NULL, 1, NULL, 'waitSomeSeconds', 4, 0, 0, 0, 0, 1);

-- Launch: file check every 5 minutes (RptTyp 1 = minutes) and two custom schedules (RptTyp 7 = custom), the closing
-- with the calendar of the Spanish holidays
-- WeekDays as the task editor stores them (Monday=1 ... Saturday=6, Sunday=0): the cron converts them for Quartz
INSERT INTO AweSchTskLch (Ide, IdeTsk, RptNum, RptTyp, IdSrv, Pth, Pat, WeekDays, ExeHrs, ExeMin, ExeSec, IdeCal)
VALUES (9012, 9012, 5, 1, 9001, '/in/n43', '*.n43', NULL, NULL, NULL, NULL, NULL),
       (9013, 9013, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
       (9014, 9014, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
       (9015, 9015, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
       (9016, 9016, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
       (9017, 9017, NULL, 7, NULL, NULL, NULL, '1,2,3,4,5', '22', '30', '0', 9001),
       (9018, 9018, NULL, 7, NULL, NULL, NULL, NULL, '3', '0', '0', NULL),
       (9019, 9019, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
       (9020, 9020, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- Parameter of waitSomeSeconds (Src 0 = value)
INSERT INTO AweSchTskPar (Ide, IdeTsk, Nam, Val, Src, Typ)
VALUES (9012, 9012, 'secondsToWait', '5', 0, 'INTEGER'),
       (9013, 9013, 'secondsToWait', '5', 0, 'INTEGER'),
       (9014, 9014, 'secondsToWait', '5', 0, 'INTEGER'),
       (9015, 9015, 'secondsToWait', '5', 0, 'INTEGER'),
       (9017, 9017, 'secondsToWait', '5', 0, 'INTEGER'),
       (9018, 9018, 'secondsToWait', '5', 0, 'INTEGER'),
       (9020, 9020, 'secondsToWait', '5', 0, 'INTEGER');

-- Parameter of 9019 asked on launch (Src 1 = variable): its Run opens the launch dialog
INSERT INTO AweSchTskPar (Ide, IdeTsk, Nam, Val, Src, Typ)
VALUES (9019, 9019, 'secondsToWait', '5', 1, 'INTEGER');

-- Hook of the webhook task 9020
INSERT INTO AweSchTskHok (IdeTsk, HokKey, Act, SecPrp, TypFlt)
VALUES (9020, 'demo-bank-collections', 1, 'awe-react.demo.webhook-secret', NULL);

-- Dependencies: IdeTsk is the child, IdePrn the parent
INSERT INTO AweSchTskDpn (IdeTsk, IdePrn, IsBlk, DpnOrd)
VALUES (9013, 9012, NULL, 1),
       (9014, 9012, NULL, 2),
       (9015, 9013, NULL, 3),
       (9015, 9014, NULL, 4),
       (9016, 9015, NULL, 5);
