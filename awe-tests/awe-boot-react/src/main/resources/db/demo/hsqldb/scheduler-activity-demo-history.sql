-- Demo data of the scheduler Activity screen (awe-boot-react, hot-reload profile only): executions of the last 7 days.
-- Every date is relative to the moment the script runs (ago = minutes before now, dur = seconds, -1 = still open), so
-- the 24 h counters and the date filters always show data. Statuses: 0 OK, 1 error, 2 warning, 3 stopped, 4 running,
-- 5 queued, 6 interrupted, 7 info. Expected counters right after the load: running 1, waiting for confirmation 1,
-- queued 2, errors in the last 24 h 2 (9013 #5 and 9017 #6).
-- Flow runs (root 9012, launched by a file):
--   ...4812 now:        9012 OK -> 9013 warning + 9014 OK -> 9015 OK -> 9016 running, waiting for the bank (deferred)
--   ...4807 yesterday:  9012 OK -> 9013 error + 9014 OK -> 9015 and 9016 skipped
--   ...4795 3 days ago: every task OK

INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, EndDat, ExeTim, Sta, LchBy, Des, IdeRun)
-- RTRIM: HSQLDB types the text literals of a VALUES list as CHARACTER of the longest one (blank padded)
SELECT v.tsk, RTRIM(v.grp), v.exe,
       TIMESTAMPADD(SQL_TSI_MINUTE, -v.ago, LOCALTIMESTAMP),
       CASE WHEN v.dur < 0 THEN NULL
            ELSE TIMESTAMPADD(SQL_TSI_SECOND, v.dur, TIMESTAMPADD(SQL_TSI_MINUTE, -v.ago, LOCALTIMESTAMP)) END,
       CASE WHEN v.dur < 0 THEN NULL ELSE v.dur * 1000 END,
       v.sta, RTRIM(v.lch), NULLIF(RTRIM(v.des), ''), NULLIF(RTRIM(v.rid), '')
FROM (VALUES
  -- 9012 Importar extractos bancarios (file)
  (9012, 'FILE_TRACKING_TASK', 1, 8700, 68, 0, '/in/n43/extracto_bbva.n43', '248 movimientos importados', ''),
  (9012, 'FILE_TRACKING_TASK', 2, 7260, 75, 0, '/in/n43/extracto_santander.n43', '263 movimientos importados', ''),
  (9012, 'FILE_TRACKING_TASK', 3, 5850, 66, 0, '/in/n43/extracto_caixabank.n43', '231 movimientos importados', ''),
  (9012, 'FILE_TRACKING_TASK', 4, 4400, 70, 0, '/in/n43/extracto_bbva.n43', '252 movimientos importados',
   '00000000-0000-4000-8000-000000004795'),
  (9012, 'FILE_TRACKING_TASK', 5, 3000, 71, 0, '/in/n43/extracto_sabadell.n43', '240 movimientos importados', ''),
  (9012, 'FILE_TRACKING_TASK', 6, 1600, 65, 0, '/in/n43/extracto_santander.n43', '237 movimientos importados',
   '00000000-0000-4000-8000-000000004807'),
  (9012, 'FILE_TRACKING_TASK', 7, 190, 72, 0, '/in/n43/extracto_bbva.n43', '251 movimientos importados',
   '00000000-0000-4000-8000-000000004812'),
  -- 9013 Conciliación bancaria
  (9013, 'MANUAL_TASK', 1, 6000, 200, 0, 'pvidal', 'Conciliación completa: 512 partidas casadas', ''),
  (9013, 'DEPENDANT_TASK', 2, 4398, 180, 0, '#9012', 'Conciliación completa: 252 partidas casadas',
   '00000000-0000-4000-8000-000000004795'),
  (9013, 'MANUAL_TASK', 3, 2950, 210, 0, 'pvidal', 'Conciliación completa: 498 partidas casadas', ''),
  (9013, 'DEPENDANT_TASK', 4, 1598, 95, 1, '#9012',
   'El saldo inicial del extracto no cuadra con el saldo final del día anterior', '00000000-0000-4000-8000-000000004807'),
  (9013, 'MANUAL_TASK', 5, 300, 40, 1, 'mgarcia',
   'No se pudo conectar con el servicio de conciliación (tiempo de espera de 30 s agotado)', ''),
  (9013, 'DEPENDANT_TASK', 6, 188, 220, 2, '#9012', 'Conciliación con 3 partidas pendientes de casar',
   '00000000-0000-4000-8000-000000004812'),
  -- 9014 Actualizar posiciones de tesorería (6 running since a minute ago against an average of ~6.5 min, so its
  -- progress shows ~15 % and grows; 7 queued behind it). #4 stays short: its flow run ended 2 min after it started
  (9014, 'MANUAL_TASK', 1, 5800, 480, 0, 'mgarcia', 'Posiciones actualizadas: 6 bancos, 3 divisas', ''),
  (9014, 'DEPENDANT_TASK', 2, 4398, 450, 0, '#9012', 'Posiciones actualizadas: 6 bancos, 3 divisas',
   '00000000-0000-4000-8000-000000004795'),
  (9014, 'MANUAL_TASK', 3, 2500, 470, 0, 'pvidal', 'Posiciones actualizadas: 6 bancos, 3 divisas', ''),
  (9014, 'DEPENDANT_TASK', 4, 1598, 118, 0, '#9012', 'Posiciones actualizadas: 6 bancos, 3 divisas',
   '00000000-0000-4000-8000-000000004807'),
  (9014, 'DEPENDANT_TASK', 5, 188, 460, 0, '#9012', 'Posiciones actualizadas: 6 bancos, 3 divisas',
   '00000000-0000-4000-8000-000000004812'),
  (9014, 'MANUAL_TASK', 6, 1, -1, 4, 'mgarcia', '', ''),
  (9014, 'MANUAL_TASK', 7, 1, -1, 5, 'mgarcia', '', ''),
  -- 9015 Informe diario de tesorería
  (9015, 'MANUAL_TASK', 1, 7000, 20, 3, 'pvidal', 'Detenida por el usuario', ''),
  (9015, 'DEPENDANT_TASK', 2, 4392, 45, 0, '#9013', 'Informe enviado a dirección financiera',
   '00000000-0000-4000-8000-000000004795'),
  (9015, 'MANUAL_TASK', 3, 2900, 52, 0, 'pvidal', 'Informe enviado a dirección financiera', ''),
  (9015, 'MANUAL_TASK', 4, 1000, 47, 0, 'pvidal', 'Informe enviado a dirección financiera',
   '00000000-0000-4000-8000-000000009154'),
  (9015, 'DEPENDANT_TASK', 5, 184, 48, 0, '#9013', 'Informe enviado a dirección financiera',
   '00000000-0000-4000-8000-000000004812'),
  (9015, 'MANUAL_TASK', 6, 0, -1, 5, 'pvidal', '', ''),
  -- 9016 Envío de remesas SEPA (3 is waiting for the confirmation of the bank: AweSchExeExt below)
  (9016, 'DEPENDANT_TASK', 1, 4390, 2400, 0, '#9015', 'Remesa confirmada por el banco (ref. SEPA-REM-004795)',
   '00000000-0000-4000-8000-000000004795'),
  (9016, 'MANUAL_TASK', 2, 2860, 14400, 6, 'pvidal', 'Plazo de confirmación vencido (4 h) sin respuesta del banco', ''),
  (9016, 'DEPENDANT_TASK', 3, 182, -1, 4, '#9015', 'Remesa enviada, esperando la confirmación del banco',
   '00000000-0000-4000-8000-000000004812'),
  -- 9017 Cierre contable diario (scheduled)
  (9017, 'SCHEDULED_TASK', 1, 8800, 340, 0, 'scheduler', 'Día contable cerrado', ''),
  (9017, 'SCHEDULED_TASK', 2, 7360, 355, 0, 'scheduler', 'Día contable cerrado', ''),
  (9017, 'SCHEDULED_TASK', 3, 5920, 360, 2, 'scheduler', 'Día cerrado con 2 asientos descuadrados enviados a revisión', ''),
  (9017, 'SCHEDULED_TASK', 4, 4480, 348, 0, 'scheduler', 'Día contable cerrado', ''),
  (9017, 'SCHEDULED_TASK', 5, 3040, 120, 6, 'scheduler', 'Interrumpida al reiniciar el servidor', ''),
  (9017, 'SCHEDULED_TASK', 6, 1180, 95, 1, 'scheduler', 'El periodo contable está bloqueado por otro proceso',
   '00000000-0000-4000-8000-000000009176'),
  (9017, 'MANUAL_TASK', 7, 1100, 362, 0, 'pvidal', 'Día contable cerrado (relanzado tras liberar el bloqueo)', ''),
  -- 9018 Purga de logs (scheduled)
  (9018, 'SCHEDULED_TASK', 1, 9450, 42, 0, 'scheduler', '1.180 ficheros purgados', ''),
  (9018, 'SCHEDULED_TASK', 2, 8010, 40, 0, 'scheduler', '1.096 ficheros purgados', ''),
  (9018, 'SCHEDULED_TASK', 3, 6570, 3, 7, 'scheduler', 'No hay ficheros que purgar', ''),
  (9018, 'SCHEDULED_TASK', 4, 5130, 44, 0, 'scheduler', '1.215 ficheros purgados', ''),
  (9018, 'SCHEDULED_TASK', 5, 3690, 46, 2, 'scheduler', '1.204 ficheros purgados, 3 no se pudieron borrar (en uso)', ''),
  (9018, 'SCHEDULED_TASK', 6, 2250, 41, 0, 'scheduler', '1.143 ficheros purgados', ''),
  (9018, 'SCHEDULED_TASK', 7, 810, 39, 0, 'scheduler', '1.122 ficheros purgados',
   '00000000-0000-4000-8000-000000009187'),
  (9018, 'MANUAL_TASK', 8, 700, 12, 3, 'mgarcia', 'Detenida por el usuario', ''),
  -- 9019 Recalcular previsiones (manual)
  (9019, 'MANUAL_TASK', 1, 8000, 95, 0, 'mgarcia', 'Previsión recalculada: escenario base', ''),
  (9019, 'MANUAL_TASK', 2, 4300, 101, 0, 'pvidal', 'Previsión recalculada: escenario prudente', ''),
  (9019, 'MANUAL_TASK', 3, 2000, 98, 2, 'mgarcia', 'Previsión recalculada sin los cobros de 2 clientes', ''),
  -- 9020 Confirmación de cobros del banco (webhook; the error is older than 24 h, the counters stay as above)
  (9020, 'WEBHOOK_TASK', 1, 9000, 8, 0, 'webhook', '12 recibos marcados como cobrados', ''),
  (9020, 'WEBHOOK_TASK', 2, 7600, 9, 0, 'webhook', '7 recibos marcados como cobrados', ''),
  (9020, 'WEBHOOK_TASK', 3, 6100, 7, 1, 'webhook', 'El aviso del banco no trae la referencia de la remesa', ''),
  (9020, 'WEBHOOK_TASK', 4, 4500, 8, 0, 'webhook', '15 recibos marcados como cobrados', ''),
  (9020, 'WEBHOOK_TASK', 5, 1400, 9, 0, 'webhook', '9 recibos marcados como cobrados', '')
) AS v(tsk, grp, exe, ago, dur, sta, lch, des, rid);

-- Deferred answer of 9016 #3: accepted 50 s after its start, due tomorrow so the deadline watcher leaves it waiting
INSERT INTO AweSchExeExt (IdeTsk, ExeTsk, ExtRef, DfrDat, DueDat)
SELECT IdeTsk, ExeTsk, 'SEPA-REM-004812', TIMESTAMPADD(SQL_TSI_SECOND, 50, IniDat),
       TIMESTAMPADD(SQL_TSI_DAY, 1, LOCALTIMESTAMP)
FROM AweSchExe
WHERE IdeTsk = 9016 AND ExeTsk = 3;

-- Flow runs (end = -1: still running). Every launch has its run: the flows of 9012 and three single executions
-- (9015 #4 manual, 9017 #6 and 9018 #7 scheduled), so their side panel shows the parameters of their launch
INSERT INTO AweSchRun (IdeRun, IdeTskRoo, ExeTskRoo, LchBy, IniDat, EndDat, Sta)
SELECT RTRIM(v.rid), v.roo, v.exe, RTRIM(v.lch), TIMESTAMPADD(SQL_TSI_MINUTE, -v.ago, LOCALTIMESTAMP),
       CASE WHEN v.fin < 0 THEN NULL ELSE TIMESTAMPADD(SQL_TSI_MINUTE, -v.fin, LOCALTIMESTAMP) END, v.sta
FROM (VALUES
  ('00000000-0000-4000-8000-000000004812', 9012, 7, '/in/n43/extracto_bbva.n43', 190, -1, 4),
  ('00000000-0000-4000-8000-000000004807', 9012, 6, '/in/n43/extracto_santander.n43', 1600, 1596, 1),
  ('00000000-0000-4000-8000-000000004795', 9012, 4, '/in/n43/extracto_bbva.n43', 4400, 4350, 0),
  ('00000000-0000-4000-8000-000000009154', 9015, 4, 'pvidal', 1000, 999, 0),
  ('00000000-0000-4000-8000-000000009176', 9017, 6, 'scheduler', 1180, 1178, 1),
  ('00000000-0000-4000-8000-000000009187', 9018, 7, 'scheduler', 810, 809, 0)
) AS v(rid, roo, exe, lch, ago, fin, sta);

-- Launch parameters of each run: the operator values as they are (the dependencies of the run receive them) and the
-- resolved VALUE and VARIABLE parameters of the root task with the "=" prefix (only shown in the side panel)
INSERT INTO AweSchRunPar (IdeRun, Nam, Val)
VALUES ('00000000-0000-4000-8000-000000004812', 'fichero', 'extracto_bbva.n43'),
       ('00000000-0000-4000-8000-000000004812', '=secondsToWait', '5'),
       ('00000000-0000-4000-8000-000000004807', 'fichero', 'extracto_santander.n43'),
       ('00000000-0000-4000-8000-000000004807', '=secondsToWait', '5'),
       ('00000000-0000-4000-8000-000000004795', 'fichero', 'extracto_bbva.n43'),
       ('00000000-0000-4000-8000-000000004795', '=secondsToWait', '5'),
       ('00000000-0000-4000-8000-000000009154', 'secondsToWait', '2'),
       ('00000000-0000-4000-8000-000000009176', '=secondsToWait', '5'),
       ('00000000-0000-4000-8000-000000009187', '=secondsToWait', '5');

-- Join state of the children of each run (the root has no row); exe = -1: not launched
INSERT INTO AweSchRunTsk (IdeRun, IdeTsk, Sta, ExeTsk, UpdDat)
SELECT RTRIM(v.rid), v.tsk, RTRIM(v.sta), NULLIF(v.exe, -1), TIMESTAMPADD(SQL_TSI_MINUTE, -v.ago, LOCALTIMESTAMP)
FROM (VALUES
  ('00000000-0000-4000-8000-000000004812', 9013, 'LAUNCHED', 6, 188),
  ('00000000-0000-4000-8000-000000004812', 9014, 'LAUNCHED', 5, 188),
  ('00000000-0000-4000-8000-000000004812', 9015, 'LAUNCHED', 5, 184),
  ('00000000-0000-4000-8000-000000004812', 9016, 'LAUNCHED', 3, 182),
  ('00000000-0000-4000-8000-000000004807', 9013, 'LAUNCHED', 4, 1598),
  ('00000000-0000-4000-8000-000000004807', 9014, 'LAUNCHED', 4, 1598),
  ('00000000-0000-4000-8000-000000004807', 9015, 'SKIPPED', -1, 1596),
  ('00000000-0000-4000-8000-000000004807', 9016, 'SKIPPED', -1, 1596),
  ('00000000-0000-4000-8000-000000004795', 9013, 'LAUNCHED', 2, 4398),
  ('00000000-0000-4000-8000-000000004795', 9014, 'LAUNCHED', 2, 4398),
  ('00000000-0000-4000-8000-000000004795', 9015, 'LAUNCHED', 2, 4392),
  ('00000000-0000-4000-8000-000000004795', 9016, 'LAUNCHED', 1, 4390)
) AS v(rid, tsk, sta, exe, ago);

-- Execution log lines (database store, scheduler origin, head section), rendered like the file pattern
-- "%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m"; sec = seconds after the start of the execution
INSERT INTO AweSchExeLog (IdeTsk, ExeTsk, Src, Sec, Slt, LinNum, LinTxt, LogDat)
SELECT v.tsk, v.exe, 'S', 'H', v.lin, v.lin,
       TO_CHAR(TIMESTAMPADD(SQL_TSI_SECOND, v.sec, e.IniDat), 'YYYY-MM-DD HH24:MI:SS') || '.000 -' || v.lvl || ' : '
         || RTRIM(v.msg),
       TIMESTAMPADD(SQL_TSI_SECOND, v.sec, e.IniDat)
FROM (VALUES
  (9012, 7, 0, 0, ' INFO', '[FILE_TRACKING_TASK.9012] Maintain job started: waitSomeSeconds'),
  (9012, 7, 1, 1, ' INFO', 'Fichero detectado: /in/n43/extracto_bbva.n43 (48 KB)'),
  (9012, 7, 2, 9, ' INFO', 'Leyendo registros N43 de 2 cuentas'),
  (9012, 7, 3, 38, ' INFO', '251 movimientos leídos, 0 duplicados'),
  (9012, 7, 4, 72, ' INFO', '251 movimientos importados'),
  (9013, 6, 0, 0, ' INFO', '[DEPENDANT_TASK.9013] Maintain job started: waitSomeSeconds'),
  (9013, 6, 1, 5, ' INFO', 'Partidas a conciliar: 251 movimientos bancarios, 248 apuntes contables'),
  (9013, 6, 2, 140, ' INFO', '248 partidas casadas automáticamente'),
  (9013, 6, 3, 215, ' WARN', '3 movimientos sin apunte contable: 2 comisiones y 1 transferencia recibida'),
  (9013, 6, 4, 220, ' WARN', 'Conciliación terminada con avisos: 3 partidas pendientes de casar'),
  (9013, 4, 0, 0, ' INFO', '[DEPENDANT_TASK.9013] Maintain job started: waitSomeSeconds'),
  (9013, 4, 1, 4, ' INFO', 'Partidas a conciliar: 237 movimientos bancarios'),
  (9013, 4, 2, 93, 'ERROR',
   'Saldo inicial 1.204.331,18 EUR distinto del saldo final del día anterior 1.198.910,02 EUR'),
  (9013, 4, 3, 95, 'ERROR', '[SCHEDULER] Error on task execution: DEPENDANT_TASK.9013'),
  (9014, 6, 0, 0, ' INFO', '[MANUAL_TASK.9014] Maintain job started: waitSomeSeconds'),
  (9014, 6, 1, 15, ' INFO', 'Recalculando posiciones de 6 bancos en 3 divisas'),
  (9014, 6, 2, 45, ' INFO', 'BBVA, Santander y CaixaBank actualizados, quedan 3 bancos'),
  (9016, 3, 0, 0, ' INFO', '[DEPENDANT_TASK.9016] Maintain job started: deferredTestLaunch'),
  (9016, 3, 1, 20, ' INFO', 'Remesa SEPA-REM-004812 generada: 37 adeudos, 182.450,00 EUR'),
  (9016, 3, 2, 48, ' INFO', 'Remesa enviada al banco, pendiente de confirmación'),
  (9016, 3, 3, 50, ' INFO', '[SCHEDULER] Execution 9016-3 accepted, it finishes later'),
  (9017, 6, 0, 0, ' INFO', '[SCHEDULED_TASK.9017] Maintain job started: waitSomeSeconds'),
  (9017, 6, 1, 30, ' INFO', 'Comprobando los asientos pendientes del día'),
  (9017, 6, 2, 94, 'ERROR', 'El periodo contable está bloqueado por otro proceso'),
  (9017, 6, 3, 95, 'ERROR', '[SCHEDULER] Error on task execution: SCHEDULED_TASK.9017')
) AS v(tsk, exe, lin, sec, lvl, msg)
JOIN AweSchExe e ON e.IdeTsk = v.tsk AND e.ExeTsk = v.exe;
