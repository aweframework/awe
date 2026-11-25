---
id: scheduler
title: Módulo Scheduler
sidebar_label: Módulo Scheduler
---

El módulo Scheduler añade una potente herramienta de planificación a tu aplicación

Para activar este módulo, sigue estos pasos:

- Añade las **dependencias awe scheduler** al descriptor pom.xml.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-scheduler-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

- Añade las pantallas del planificador en tu archivo `private.xml`:

```xml
<option name="scheduler" label="MENU_SCHEDULER" icon="clock-o">
  <option name="scheduler-activity" label="MENU_SCHEDULER_ACTIVITY" screen="scheduler-activity" icon="lucide:activity">
    <option name="scheduler-flow-run" screen="scheduler-flow-run" invisible="true" />
  </option>
  <option name="scheduler-task-catalog" label="MENU_SCHEDULER_TASK_CATALOG" screen="scheduler-task-catalog" icon="lucide:list-checks">
    <option name="scheduler-task" screen="scheduler-task" invisible="true" />
    <option name="scheduler-task-editor" screen="scheduler-task-editor" invisible="true" />
  </option>
  <option name="scheduler-configuration" label="MENU_SCHEDULER_CONFIGURATION" screen="scheduler-configuration" icon="lucide:settings"/>
</option>
```

| Opción de menú | Pantalla | Para qué sirve |
|----------------|----------|----------------|
| **Actividad** | [`scheduler-activity`](#activity-screen) | Lo que se ejecuta, espera o ha fallado en todas las tareas, y el menú del motor |
| **Tareas** | [`scheduler-task-catalog`](#tasks-catalog) | Todas las tareas, con **Nueva tarea** |
| **Configuración** | [`scheduler-configuration`](#configuration-screen) | Servidores, calendarios y el motor, una pestaña cada uno |
| (invisibles) | [`scheduler-task`](#task-screen), [`scheduler-task-editor`](#task-editor), [`scheduler-flow-run`](#flow-run-page) | La ficha de tarea, el editor de tareas y la página del flujo, que se abren desde las pantallas anteriores |

Las opciones invisibles dan a sus pantallas una ruta y una miga de pan: la ficha y el editor vuelven a **Tareas**, y la
página del flujo a **Actividad**. Una aplicación que se actualiza desde las pantallas antiguas del planificador sigue la
entrada [Rediseño del planificador](guides/v5-migration.md#scheduler-redesign) de la guía de migración.

- Configura el valor de la propiedad para añadir `awe-scheduler` a la lista de módulos.

```properties
awe.application.module-list = APP, ..., awe-scheduler, ..., awe
```

- Da a los perfiles que usan las pantallas del planificador los permisos `scheduler.read`, `scheduler.update` y `scheduler.execute` (solo los tiene el perfil de administrador). Consulta [Permisos](security/permissions.md#catalog-resources).

- Por último, si estás usando `flyway`, añade las tablas del planificador al módulo de migración:

```properties
awe.database.migration-modules=AWE,...,SCHEDULER,...
```

## Ejecuciones de flujo {#flow-runs}

Toda tarea lanzada por su planificación, de forma manual, por un fichero o por un webhook inicia una ejecución de flujo que agrupa las ejecuciones de sus dependencias, con un estado final y parámetros opcionales. Una tarea con varios padres se lanza una sola vez, cuando han terminado todos sus padres de la ejecución de flujo.

La ejecución de flujo guarda los parámetros de su lanzamiento en `AweSchRunPar`: primero los valores que el operador indicó al lanzarla a mano, tal cual, que recibe cada dependencia del flujo; después los demás parámetros `VALUE` y `VARIABLE` de la tarea raíz con su valor resuelto y el prefijo reservado `=` en su nombre (`=secondsToWait`). Las filas con prefijo solo se muestran (el panel lateral de **Actividad**): las dependencias no las reciben, así que un flujo recibe los mismos valores que antes de guardarlas. Un parámetro `PROPERTY` solo se guarda como la clave que envía un lanzamiento manual, que no puede cambiar (su valor resuelto es configuración y nunca se escribe), y un parámetro cuyo nombre tiene 40 caracteres (sin sitio para el prefijo) no se guarda como resuelto. Por eso el nombre de un parámetro de tarea no puede empezar por `=`: la pantalla de la tarea lo rechaza.

La [página del flujo](#flow-run-page) (`scheduler-flow-run`, permiso `scheduler.read`) muestra una ejecución de flujo con sus tareas, sus ejecuciones y sus tareas omitidas; la [pantalla de actividad](#activity-screen) lista las ejecuciones de flujo en su vista **Agrupar por flujo**. Consulta **[Dependencias entre tareas](guides/dependencies-guide.md#flow-runs-and-fan-in)**.

El widget `flow-graph` dibuja las dependencias de las tareas (las filas de `getFlowGraph`) como un grafo de izquierda a
derecha. Cada
nodo muestra la tarea, su tipo de lanzamiento y si está activa; cada arista muestra cuándo lanza el padre a sus hijos
(siempre con OK, también con aviso o con error). En una ejecución, cada nodo muestra su estado: terminada con su estado,
en curso, en espera, omitida o no alcanzada. Al pulsar un nodo se abre la tarea. Una tarea inactiva se ve atenuada, con
una etiqueta **Inactiva**. Los dos motores dibujan el grafo. Con el parámetro `mode` = `task` del widget, el mismo widget
dibuja el grafo de un nivel de la [ficha de tarea](#task-screen): las tareas tras las que se lanza, la tarea y las tareas
que lanza, en tres columnas con título. Cada arista muestra su condición como iconos (✓ OK, ⚠ aviso, ✕ error: los
finales del padre que lanzan al hijo), con su texto completo como tooltip y nombre accesible, y una leyenda bajo el grafo
los explica. Cada tarea muestra el estado de su última ejecución con cuándo empezó (`OK · hoy 08:00`) y una planificada
su próxima ejecución (`Próxima: mañana 03:00`); un hijo que además espera a otras tareas las lista, atenuadas, a su
derecha (**También espera a**). Con el parámetro `target` las tareas tras las que se lanza y las que lanza son botones
(`Abrir <tarea>`, Intro o Espacio) que abren la pantalla `target`, y la propia tarea no: la tarea del nodo pasa al valor
seleccionado del criterio que nombra el parámetro `criterion` (la ficha lee `IdeTsk.selected`) y la pantalla se abre de
nuevo. Sin `target` sus nodos no son botones. Con el parámetro `side` (`parents` o `children`, el
[editor de tareas](#task-editor)) solo se dibuja ese lado, en dos columnas: las tareas tras las que se lanza y la tarea,
o la tarea y las tareas que lanza; sin él, las tres columnas.

Con el parámetro `mode` = `run` del widget, el mismo widget dibuja una ejecución de flujo a partir de las filas de
`getFlowRunGraph` (una fila por tarea del flujo y padre), en los dos motores:

```xml
<widget type="flow-graph" id="FlwRunGph" server-action="data" target-action="getFlowRunGraph" initial-load="query">
  <widget-parameter type="string" name="mode" value="run" />
</widget>
```

Las acciones de un nodo seleccionado son las dependencias de la página sobre su evento click
(`<dependency-element id="FlwRunGph" event="click" />`).

- Cada nodo muestra el estado de la tarea en el flujo con su icono y su texto (el recuadro es punteado mientras está
  **Pendiente** y discontinuo cuando está **Omitida** o **Esperando confirmación**), su ejecución, su duración con sus
  horas (hasta **Ahora** mientras se ejecuta), el vencimiento de una diferida (`ExeDueLbl`) y, en una unión, el padre que
  la cerró (**Unido por …**, `joinedByText`) con su espera (`joinWaitText`). Una tarea que no se alcanzó en un flujo
  terminado se omite.
- Cada arista muestra la condición de su padre (`conditionLabel`: «OK», «OK o Aviso», «OK o Error», «OK, Aviso o
  Error») y su estado en el flujo: cumplida, no cumplida (discontinua, tono de error), cortada (discontinua, atenuada) o
  pendiente (punteada); la arista que cerró una unión es más gruesa. Una leyenda bajo el grafo explica los estados y las
  aristas.
- Un nodo con ejecución es un botón conmutador (`aria-pressed`): un clic, Intro o Espacio lo selecciona, lanza el evento
  `click` del widget (las dependencias sobre él lanzan sus acciones, como hace el panel lateral de Actividad) y el widget
  envía `<id>.selected` (la tarea) y cada columna de la fila del nodo como `<column>.selected`, como la fila seleccionada
  de un grid (`IdeTsk.selected`, `ExeTsk.selected`). Con un `target` además se abre la pantalla. Una tarea pendiente u
  omitida no tiene ejecución: sigue siendo enfocable pero no abre nada. El nodo seleccionado se mantiene a la vista y se
  anuncia a los lectores de pantalla; Escape borra la selección.
- Una pequeña barra de herramientas aleja, acerca (del 50 % al 200 %, un cuarto en cada paso) y ajusta el grafo a su
  ancho.

El parámetro `view` del widget dibuja la misma ejecución en otra vista (un parámetro estático: pon un widget por vista,
o cámbialo en la pantalla): `graph` (el valor por defecto, también para cualquier otro valor), `timeline` o `list`.

```xml
<widget type="flow-graph" id="FlwRunTim" server-action="data" target-action="getFlowRunGraph" initial-load="query">
  <widget-parameter type="string" name="mode" value="run" />
  <widget-parameter type="string" name="view" value="timeline" />
</widget>
```

- **Cronograma** (`timeline`): una tabla con una fila por tarea, en el orden de la lista: su nombre, su estado (icono y
  texto), su ejecución y una nota con su vencimiento o la unión que cerró; a la derecha sus barras sobre un eje de tiempo
  con sus marcas. Cada barra tiene el tono de su estado y su duración; una ejecución diferida llega hasta el momento en
  que empezó a esperar y después una barra discontinua queda abierta hasta **Ahora** (una línea cruza las filas mientras
  hay una barra abierta), con su vencimiento marcado. La espera en una unión es una barra rayada antes de la tarea. Cada
  hueco inactivo de más de un cuarto del flujo se comprime (rayado, `≈` en el eje) para que minutos de trabajo y horas de
  espera quepan en una fila.
- **Lista** (`list`): una lista ordenada anidada desde la tarea raíz, siguiendo las dependencias: cada tarea con su
  estado, ejecución, duración y horas, su vencimiento, la unión que cerró con su espera, por qué se omitió y la condición
  de su arista (`Después de <padre> si termina OK…`). Una unión aparece una vez, bajo el padre que la cerró; bajo sus
  otros padres una entrada `También alimenta a <tarea>` apunta a ella.
- Las barras del cronograma y las tareas de la lista seleccionan su tarea exactamente como un nodo del grafo: botones
  conmutadores (`aria-pressed`, Intro o Espacio), el mismo evento `click` y los mismos valores `<id>.selected` y
  `<column>.selected`; una tarea sin ejecución no abre nada, Escape borra la selección y la tarea seleccionada se mantiene
  a la vista y se anuncia. Las dos vistas mantienen la leyenda de los estados (el cronograma añade la espera de la unión);
  la barra de zoom solo está en el grafo.

Si usas `flyway`, las tablas de las ejecuciones de flujo (`AweSchRun`, `AweSchRunPar`, `AweSchRunTsk` y la columna `IdeRun` de `AweSchExe`) se incluyen en el módulo de migración `SCHEDULER`, versión `1.0.9`.

## Pantalla de actividad {#activity-screen}

La pantalla **Actividad** (`scheduler-activity`, permiso `scheduler.read`; sus acciones necesitan `scheduler.execute`) muestra lo que se está ejecutando, lo que espera confirmación y lo que ha fallado, en todas las tareas. Su cabecera muestra el estado del motor (en marcha, en pausa o parado) y el menú **Motor** (pausar, reanudar, reiniciar y limpiar y parar, los dos últimos con confirmación). Los contadores (en ejecución, esperando confirmación, en cola, con error en las últimas 24 horas y próximas en las siguientes 24 horas) filtran la lista, salvo el de próximas, que solo informa. La lista de ejecuciones se filtra por tarea, estado, origen y fecha de inicio; la tarea y el flujo de una fila abren sus pantallas, y el botón de detalle de una fila abre un panel lateral con los enlaces a la tarea y al flujo, el progreso de la ejecución (su tiempo transcurrido, o su tiempo de ejecución al terminar, frente a la media de su tarea), los datos de la ejecución (una raya para el valor que aún no tiene), la hora de vencimiento de una ejecución diferida, los parámetros guardados con su flujo, el log en directo y las acciones de detener y relanzar. Lee las [consultas de actividad](#activity-queries).

## Consultas de actividad {#activity-queries}

Los datos de actividad de todas las tareas se leen con estas consultas (permiso `scheduler.read`, solo lectura). Con `awe.scheduler.remote-enabled=true` las responde la instancia del planificador (`POST /scheduler/api/v1/executions`, `POST /scheduler/api/v1/executions/runs` y `GET /scheduler/api/v1/executions/counters`):

| Consulta | Devuelve |
|----------|----------|
| `getActivityExecutionList` | Una página de las ejecuciones de todas las tareas, de la más reciente a la más antigua salvo que el grid la ordene, ordenada y paginada en la base de datos (con el `sort`, `page` y `max` que envía el grid). Filtros opcionales: `CrtExeTsk` (texto del nombre de la tarea), `CrtExeTskIde` (una tarea, el historial de la [ficha de tarea](#task-screen)), `CrtExeSta` (uno o varios estados), `CrtExeOri` (uno o varios orígenes del enumerado `ExeOri`: `scheduled`, `manual`, `file`, `dependency`, `webhook`), `CrtExeDfr` (`1` ejecuciones en curso que esperan la confirmación de una respuesta diferida, `0` ejecuciones en curso que no la esperan), `CrtExeWin.from` / `CrtExeWin.to` (primer y último día de inicio, incluidos, `dd/MM/yyyy`: el rango personalizado del filtro rápido de rango de fechas `CrtExeWin`) y `CrtExeWin` (iniciadas en las últimas horas). Un valor de filtro no válido muestra el error *Filtro no válido* en lugar de una lista más amplia, igual que un tamaño de página (`max`) de `0` o mayor que `500` (sin él, `50` filas) o una columna o dirección de `sort` que no se acepta (consulta en la [guía del planificador](guides/scheduler-guide.md#activity-of-all-tasks) los valores válidos y la API). Cada fila tiene las columnas de la lista de ejecuciones de una tarea (estado, progreso con el tiempo medio de su propia tarea, tiempo, lanzada por, localizador del log) y además la tarea (`IdeTsk`, `TskNam`), la ejecución de flujo (`ExeRun`, y solo su etiqueta en `ExeRunLbl`, que muestra el panel lateral), el código de origen (`ExeOri`) y, mientras espera una confirmación diferida, `ExeDfr` = `1` y su vencimiento (`ExeDue`, `ExeDueTim` como `HH:mm`). La base de datos ordena todas las ejecuciones antes de tomar la página; las columnas ordenables, y el campo que envía cada una como id de ordenación (decláralo como `sort-field`, para que el motor, al volver a ordenar la página, mantenga el orden de la base de datos), son el inicio `ExeLstTim`, la tarea `TskNam`, el estado `Sta`, la duración `ExeEla` (segundos transcurridos, hasta ahora mientras la ejecución no ha terminado), quién la lanzó `ExeLchBy` y el origen `TypLch` (también se aceptan los nombres de columna `ExeStaTxt`, `ExePrg`, `ExeTim` y `ExeOriTxt`); declara cualquier otra columna `sortable="false"` |
| `getActivityRunList` | Las ejecuciones de `getActivityExecutionList` agrupadas por ejecución de flujo, para un tree grid (`tree-id="id"`, `tree-parent="RunPar"`, `tree-leaf="RunLeaf"`). Las filas de primer nivel son los flujos y las ejecuciones sin flujo (lanzadas antes de que se guardaran los flujos), de la más reciente a la más antigua; la página, la ordenación y los filtros (los de `getActivityExecutionList`, leídos de la barra de herramientas de la vista por flujo: `CrtRunTsk`, `CrtRunQck`, `CrtRunOri`, `CrtRunWin`, `CrtRunWin.from` y `CrtRunWin.to`) se aplican a ellas: **un flujo aparece si al menos una de sus ejecuciones cumple los filtros**, y entonces trae todas las ejecuciones del flujo, no solo las que los cumplen. Sin `max`, 50 filas de primer nivel. Los campos ordenables son los de `getActivityExecutionList`: un flujo se ordena por su inicio, estado, lanzador y tiempo transcurrido guardados, por el nombre de su tarea raíz y por el origen de su ejecución raíz. Una fila de flujo tiene `id` = el id del flujo, `RunPar` vacío, `RunLeaf` = `0`, su raíz como `TskNam` (`<tarea raíz> #<ejecución raíz>`) y como `ExeRun`/`ExeRunLbl`, su estado agregado en `Sta`, `ExeStaTxt`, `ExeStaIcn` y `ExeStaTon` (en ejecución si una ejecución está en curso; si no, en cola si una está en cola; si no, error si una terminó con error, detenida o interrumpida; si no, aviso si una terminó con aviso o el flujo omitió una tarea; si no, OK), su inicio (`ExeLstTim`, el primer inicio de sus ejecuciones), su duración (`ExeTim`, `ExeEla`: hasta el último fin, o hasta ahora mientras una ejecución no ha terminado), el origen y el lanzador de su ejecución raíz y su número de ejecuciones (`RunExeCnt`, y `RunExeTxt`, «5 ejecuciones · 2 omitidas», también en `ExeDueLbl`, la línea bajo su estado), y un botón de log oculto (`ExecutionLogPath` con el estilo `hidden`). Le siguen sus hijas, la primera iniciada primero: cada ejecución con los campos de `getActivityExecutionList` (`id` = `idTarea-idEjecución`, `RunPar` = el id del flujo, `RunLeaf` = `1`) y después cada tarea que omitió el flujo (`id` = `idFlujo/idTarea`, estado **Omitida**, sin ejecución ni log). Una ejecución sin flujo es una hoja de primer nivel con los campos de `getActivityExecutionList` |
| `getActivityCounters` | Una fila con `running`, `waiting` (esperando la confirmación de una respuesta diferida), `queued`, `errorsLast24h` y `upcomingNext24h` (disparos de los triggers visibles y que pueden dispararse (no pausados, en error ni completados) en las próximas 24 horas, con sus calendarios, como máximo 1440 contados por trigger y como máximo 8640 saltados porque su calendario los excluye; vacío si falla la parte de Quartz y los demás contadores se devuelven igual). Cada contador de ejecuciones coincide con las filas de `getActivityExecutionList` con el mismo filtro |
| `getActivityExecutionProgress` | Progreso de la ejecución seleccionada (`IdeTsk.selected`, `ExeTsk.selected`) como los campos de una [tarjeta](api/card.md): `title`, `value` (el tiempo transcurrido mientras se ejecuta, si no su tiempo de ejecución), `unit` (`s`, `min` o `h`, según lo que necesite el mayor entre el tiempo y la media), `target` (el tiempo medio de su tarea, ninguno sin ejecuciones correctas), `numberDecimals` y `description`. Ninguna fila sin ejecución |
| `getActivityExecutionParameters` | Parámetros del lanzamiento del flujo de la ejecución seleccionada (`ExeRun.selected`), del servicio `getActivityExecutionParameters`: `ParNam` y `ParVal` (una raya sin valor), ordenados por nombre. Son los valores del operador de un lanzamiento manual y los parámetros `VALUE` y `VARIABLE` resueltos de la tarea raíz, sin su prefijo `=` (gana el valor del operador si ambos tienen el mismo nombre; consulta [Ejecuciones de flujo](#flow-runs)). Una dependencia muestra los parámetros del lanzamiento de su flujo; una ejecución lanzada antes de que se guardaran no muestra ninguno. Los valores no se enmascaran (los parámetros de una tarea no tienen marca de secreto; los valores `PROPERTY` nunca se guardan) |

`getSchedulerMetadata` devuelve también `schedulerStatusCode` (`started`, `standby` o `stopped`), el código independiente del idioma de `schedulerStatus`.

## Catálogo de tareas {#tasks-catalog}

La pantalla **Tareas** (`scheduler-task-catalog`, permiso `scheduler.read`; ejecutar una tarea necesita `scheduler.execute`, y crearla, editarla, activarla, desactivarla y borrarla `scheduler.update`) lista todas las tareas con lo que ejecutan, cuándo se lanzan y cómo han ido sus últimas ejecuciones. El botón **Nueva tarea** abre el editor de tareas. La barra de herramientas del grid tiene un buscador (el nombre, la descripción o lo que ejecuta la tarea), el filtro de lanzamiento (manual, planificado, por fichero, dependencia o webhook), el filtro de activa con el número de tareas de cada opción y el filtro de último estado (OK, aviso, error, en ejecución, esperando confirmación, en cola o sin ejecuciones). Sus columnas son:

- **Último estado**: la píldora de estado de la [pantalla de actividad](#activity-screen) de la última ejecución, con su inicio debajo (hoy, ayer, mañana o la fecha).
- **Tarea**: un enlace a la [ficha de tarea](#task-screen), con su descripción como título.
- **Cuándo**: un resumen en lenguaje natural de cómo se lanza la tarea, derivado de su tipo de lanzamiento, su repetición, su calendario, el servidor, la carpeta y el patrón de ficheros que revisa y las tareas tras las que se ejecuta (sin campo propio). Por ejemplo *Días laborables a las 22:30* con *calendario Festivos* debajo, *Al llegar un fichero en sftp-bancos:/in* con *\*.n43 · revisa cada 5 min*, o *Después de Importar extractos* con *si termina en OK o Aviso*. Una tarea manual con padres es una **dependencia**: se ejecuta tras ellos y, con varios padres, cuando terminan todos.
- **Historial**: una [tira de estados](api/grids.md) de las 20 últimas ejecuciones guardadas (las que conserva la tarea), de la más antigua a la más reciente; el tooltip de cada barra indica la ejecución, su inicio, su duración y su flujo.
- **Próxima ejecución**: el próximo disparo de una tarea planificada o por fichero, o lo que lanza la tarea (sus padres, un webhook, un usuario), o que está inactiva. No es ordenable.
- Tiempo **Medio** de sus ejecuciones correctas y estado **Activa** (una píldora).

El menú de fila (botón derecho, o el botón «⋯» *Más acciones* al final de cada fila, también en los *grids* de ejecuciones de Actividad y en el historial de la ficha de la tarea) abre la pantalla de la tarea (*Ver detalle*, como el enlace de la tarea), ejecuta la tarea (*Ejecutar…* abre el [diálogo de ejecución](#run-dialog) si tiene parámetros, *Ejecutar ahora* lanza al momento una tarea sin parámetros), abre el editor (*Editar* y *Duplicar*), la activa o desactiva y la borra tras una confirmación (los mantenimientos `LchTsk`, `ActSchTsk`, `DeaSchTsk` y `DeleteSchedulerTask`). El motor no tiene una celda interruptor editable ligada a un mantenimiento, así que el estado activo es una píldora y lo cambia el menú de fila.

El catálogo se lee con estas consultas (permiso `scheduler.read`, solo lectura). Con `awe.scheduler.remote-enabled=true` las responde la instancia del planificador (`POST /scheduler/api/v1/tasks/catalog` y `POST /scheduler/api/v1/tasks/catalog/active-counts`, con el token bearer; su cuerpo es el `TaskCatalogCriteria` con los campos `search`, `launchType`, `active`, `lastStatus`, `page`, `max` y `sort`, y un valor no válido responde `400` con `{"type": "invalid-request"}`):

| Consulta | Devuelve |
|----------|----------|
| `getTaskCatalog` | Una página de las tareas, por nombre salvo que el grid la ordene. El servicio lee todas las tareas una vez, las filtra, ordena y pagina, y lee los próximos disparos, el historial y los parámetros solo de las tareas de la página (cada uno con una consulta para toda la página; la base de datos se queda con las 20 últimas ejecuciones de cada tarea, así que un historial largo nunca trae más filas de las que muestra la tira). Filtros opcionales: `CrtTskSch` (texto del nombre, la descripción o lo que ejecuta la tarea, sin distinguir mayúsculas), `CrtTskLch` (`manual`, `scheduled`, `file`, `dependency` o `webhook`), `CrtTskAct` (`1` activas, `0` inactivas) y `CrtTskSta` (`ok`, `warning`, `error` —también detenidas e interrumpidas—, `running`, `waiting`, `queued` o `none`). Un valor no válido muestra el error *Filtro no válido*, igual que un tamaño de página (`max`) de `0` o mayor que `500` (sin él, `50` filas) o una columna o dirección de `sort` no aceptada. Cada fila tiene la tarea (`IdeTsk`, `Nam`, `Des`, `CmdExe`, `Act`, `TypLch`, `Sta` = estado de su última ejecución), el enlace `TskNam` (`value`, `label` y `title` = su descripción), la píldora del último estado (`TskStaTxt`, `TskStaIcn`, `TskStaTon`, `TskStaSub`), el resumen «Cuándo» (`TskWhnTxt`, `TskWhnIcn`, `TskWhnSub`), el historial (`TskHis`, un array JSON de `{"tone", "label", "title"}`), la próxima ejecución (`TskNxtTxt`, `TskNxtSub`), la media (`TskAvg`), la píldora de activa (`TskActTxt`, `TskActIcn`, `TskActTon`) y `TskVar` (`1` si la tarea tiene parámetros, por lo que su lanzamiento pide sus valores). Los campos ordenables (decláralos como `sort-field`) son `Nam`, `TskLstDat` (inicio de la última ejecución), `TskKnd` (tipo de lanzamiento), `TskAvgMs` (tiempo medio) y `Act`; también se aceptan los nombres de columna `TskNam`, `TskStaTxt`, `TskWhnTxt`, `TskAvg` y `TskActTxt`. La próxima ejecución no es ordenable: mezcla disparos con lo que lanza la tarea («Cuando termine X», un fichero), y ordenarla pediría al planificador el próximo disparo de cada tarea |
| `getTaskCatalogActiveCounts` | Las opciones del filtro de activa (todas, activas, inactivas) con `value`, `label`, `count` y `tone`, contadas con los filtros de búsqueda, lanzamiento y último estado |
| `getTaskCatalogLaunchTypes`, `getTaskCatalogLastStatuses` | Las opciones de los filtros de lanzamiento y último estado (enumerados `TskCatLch` y `TskCatSta`) |

## Ficha de tarea {#task-screen}

La ficha de tarea (`scheduler-task`, permiso `scheduler.read`; ejecutar la tarea necesita `scheduler.execute`, y
editarla, activarla, desactivarla y borrarla `scheduler.update`) se abre desde la tarea de una fila del
[catálogo de tareas](#tasks-catalog) y desde todos los enlaces de tarea de la [pantalla de actividad](#activity-screen),
con la tarea como `IdeTsk.selected`. Decláralo como opción invisible del catálogo (consulta el menú de arriba), para que
sus migas de pan vuelvan a **Tareas**, como el botón **Volver a Tareas**, la primera acción de su cabecera. La pestaña del navegador guarda la tarea (`keep-on-reload`, consulta
[criterios](api/criteria.md)), así que recargar el navegador abre la misma tarea. Su cabecera muestra el nombre, el
estado de la última ejecución, la descripción y los datos de la tarea como píldoras: su id, qué ejecuta (el nombre del
mantenimiento o del comando en la fuente monoespaciada), cuándo se lanza (el resumen «Cuándo» del catálogo), las
ejecuciones que guarda, su tiempo máximo y su informe. **Editar** abre el editor de tareas; **Ejecutar…** abre el
[diálogo de ejecución](#run-dialog) si la tarea tiene parámetros, y **Ejecutar ahora** lanza al momento una tarea sin
parámetros (no hay nada que preguntar); el botón de tres puntos de la derecha (**Más acciones**) activa,
desactiva o borra la tarea (tras una confirmación, y vuelve al catálogo). Sus pestañas son:

- **Resumen**, sobre las ejecuciones guardadas de la tarea: la tasa de éxito (el porcentaje de ejecuciones OK entre las
  que terminaron, como una barra apilada por estado con el número de cada estado debajo), la duración media de sus
  ejecuciones OK con el tiempo máximo permitido y las duraciones de sus últimas ejecuciones como un gráfico de área, la próxima ejecución (su fecha cuando el planificador dispara la tarea, si
  no lo que la lanza), la tira de estados de sus 20 últimas ejecuciones a lo ancho de su tarjeta, con el número de cada estado y **Ver
  historial**, y su última ejecución
  (estado, resultado, ejecución, flujo, origen, quién la lanzó, inicio y duración) con **Ver log**, que abre el detalle de
  la ejecución más reciente del historial. Las acciones de las últimas ejecuciones y de la última ejecución quedan en la
  misma línea.
- **Historial**: las ejecuciones de la tarea como las lista la pantalla de actividad (`getActivityExecutionList` con
  `CrtExeTskIde`), con sus filtros de estado, origen y fecha y el mismo panel lateral (progreso, parámetros, log en vivo
  y parar).
- **Dependencias**: la posición de la tarea en el flujo de ejecución, como un grafo de flujo de un nivel (solo de
  consulta: las condiciones de cada dependencia se configuran en la tarea que se lanza después, que edita **Cambiar de
  qué depende**) en tres columnas, de izquierda a derecha **Se lanza después de**, **Esta tarea** y **Lanza después** (una vacía
  dice **Ninguna**), con el nombre completo de cada tarea en hasta dos líneas, el estado de su última ejecución con cuándo
  empezó y, en una tarea planificada, su próxima ejecución; la condición de cada arista como iconos con una leyenda; a la
  derecha de un hijo, las otras tareas a las que también espera. Una tarea tras la que se lanza o que lanza abre su
  propia ficha (la pestaña del navegador la conserva, así que al recargar se vuelve a abrir). **Cambiar de qué depende**
  abre el editor de tareas.
- **Configuración**: un resumen de solo lectura de qué ejecuta la tarea, dónde, qué guarda, su tiempo máximo, cuándo se
  lanza, tras qué tareas, qué lanza después, sus parámetros y su informe, con **Editar**.

Lee estas consultas (permiso `scheduler.read`, solo lectura):

| Consulta | Devuelve |
|----------|----------|
| `getTaskPage` | Una fila para la tarea de `IdeTsk.selected` (ninguna si no existe), el target de la pantalla: `TskAct`, `TskVar` (`1` si su lanzamiento pide los valores de sus parámetros), la cabecera (`TskPagTit`, `TskPagSub`, `TskPagChp`, `TskPagChpTon`, `TskPagIcn` = el icono de su lanzamiento, `TskPagFct` = sus datos como un array JSON de píldoras `{"label", "icon", "code"}`), el resumen (`TskOkVal`, `TskOkUni`, `TskOkDes`, `TskAvgVal`, `TskAvgDes`, `TskAvgSpk`, `TskNxtVal`, `TskNxtDes`, `TskHis`, `TskHisTxt`), la última ejecución (`TskLstHas`, `TskLstSta`, `TskLstTon`, `TskLstIcn`, `TskLstDes`, `TskLstExe`, `TskLstRun`, `TskLstOri`, `TskLstBy`, `TskLstIni`, `TskLstDur`) y la configuración (`TskCfgRun`, `TskCfgWhr`, `TskCfgWhn`, `TskCfgPrn`, `TskCfgChl`, `TskCfgSto`, `TskCfgTim`, `TskCfgRpt`). Un valor que la tarea no tiene es un guion |
| `getTaskPageHeader`, `getTaskPageSuccess`, `getTaskPageAverage`, `getTaskPageNext`, `getTaskPageHistoryCount`, `getTaskPageLast` | Los campos de `getTaskPage` que muestra cada [tarjeta](api/card.md) de la pantalla, con los nombres de los atributos de la tarjeta (`title`, `subtitle`, `chip`, `chipTone`, `pills`, `value`, `unit`, `description`, `stack`, `sparkline`, `strip`) |
| `getFlowGraph` | Las filas del grafo de flujo, ahora con `lastStatus` (el estado de la última ejecución de cada tarea) |
| `getTaskFlowGraph` | Las filas de `getFlowGraph` de la tarea (la pestaña **Dependencias**), donde la tarea, las tareas tras las que se lanza y las que lanza tienen además `lastWhen` (cuándo empezó su última ejecución, con las palabras relativas del catálogo: `hoy 08:00`, `23/03/2019 08:00`) y `nextRun` (`Próxima: mañana 03:00` en una tarea planificada activa que dispara el planificador); vacíos en las demás filas |

### Diálogo de ejecución {#run-dialog}

**Ejecutar…** (la ficha de la tarea y el menú de fila del catálogo) abre el
diálogo de ejecución (`scheduler-launch-variables`): un lanzamiento manual en el que el operador puede cambiar el valor
de los parámetros de origen **Valor** y **Variable** de la tarea, solo para esta ejecución (la tarea no cambia). Su grid
(`taskVariableParameters`, permiso `scheduler.read`) lista cada parámetro con su tipo bajo el nombre, su origen como
píldora (**Valor**, **Variable** o **Propiedad**) y su valor guardado, editable salvo en una **Propiedad**. **Restablecer valores** vuelve a cargar
los valores guardados, **Cancelar** lo cierra y **Ejecutar** valida los valores (`VldTskPar`) y lanza la tarea
(`LchTskVar`), ambos con el permiso `scheduler.execute` (`scheduler.update` no lo permite). Una nota bajo el grid
(`getTaskLaunchNote`) indica que el lanzamiento se registra con origen **Manual**, lanzado por el usuario de la sesión.

- Un parámetro de origen **Propiedad** muestra la clave de la propiedad, de solo lectura: se resuelve al ejecutar y una
  clave que no se resuelve rechaza el lanzamiento. Un lanzamiento no puede cambiar la clave: una clave distinta se
  rechaza con *La clave de una propiedad no se puede cambiar al lanzar* y no se ejecuta nada (también por la API y en un
  planificador remoto). El valor resuelto es configuración y nunca llega al navegador.
- Un valor que cambia el guardado debe ser un valor del tipo del parámetro, sin los espacios que lo rodean (se quitan,
  también del valor que se ejecuta): un número entero (`INTEGER`, `LONG`), un número decimal con punto (`FLOAT`,
  `DOUBLE`: `3`, `0.5` o `.5`), una fecha `dd/MM/yyyy` o `yyyy-MM-dd` (`DATE`) o una hora `HH:mm` o `HH:mm:ss` (`TIME`);
  solo los tipos de texto (`STRING`, `OBJECT`) admiten un valor vacío y conservan sus espacios. Si no, el lanzamiento se
  rechaza con *Valor de parámetro no válido* y no se ejecuta nada. El propio lanzamiento también lo comprueba, así que
  la API del planificador (`POST /scheduler/api/v1/task/{taskId}/execute`, ver
  [Seguridad de la API del planificador](#scheduler-api-security)) y un planificador remoto rechazan los mismos valores. Un valor
  guardado sin cambios no se vuelve a comprobar.
- Los valores llegan a la ejecución y se guardan como parámetros de su ejecución de flujo (`AweSchRunPar`, ver
  [Ejecuciones de flujo](#flow-runs)), así que el panel lateral de **Actividad** los muestra. Los demás lanzamientos
  (una planificación, un fichero, un webhook, una dependencia) solo cambian los parámetros de origen **Variable**.

## Editor de tareas {#task-editor}

El editor de tareas (`scheduler-task-editor`, permiso `scheduler.update` para leer la tarea y guardarla) es una sola
pantalla para el alta, la edición y la copia de una tarea; decláralo como opción invisible del catálogo, como la ficha.
El catálogo lo abre con **Nueva tarea** y con **Editar** y **Duplicar** de su menú de fila; la ficha con **Editar**,
**Duplicar** (su menú **Más acciones**), **Editar** de su pestaña Configuración y **Cambiar de qué depende**, que lo abre
en **Cuándo**. Quien lo abre envía el modo (`TskEdtMod`: `new`, `edit` o `copy`) y la tarea (`IdeTsk.selected`), y la
pestaña del navegador conserva ambos, así que al recargar se abre la misma tarea en el mismo modo. Su cabecera dice el
modo (**Nueva tarea**, **Editar tarea #N**, **Duplicar tarea**) y si la tarea está activa; sus secciones, una tarjeta
cada una, son **Qué ejecuta** (nombre, descripción y el tipo de ejecución como [tarjetas de
opción](api/criteria.md#choice-cards)), **Parámetros**, **Cuándo** (tarjetas de opción: manual, programada, al llegar un
fichero, después de otras tareas o webhook), **Dependencias** (las tareas que se lanzan después de ella, solo lectura, cada una un enlace a
su ficha), **Comportamiento** (tiempo máximo, ejecuciones conservadas, activa y los tres interruptores de las tareas que
se lanzan después) y **Notificaciones**; una navegación a la izquierda lleva a cada sección y un resumen a la derecha
sigue los cambios. **Después de otras tareas** elige los padres en una lista agrupada en *Elegidas*, *Disponibles* y
*No se pueden elegir* (las que ya se lanzan después de ella, con el motivo) y los dibuja uniéndose en la tarea con la
condición de cada uno (solo lectura: los interruptores del padre; un padre abre su ficha). Salir de esa opción con
padres elegidos pregunta antes: cambiar los quita, seguir vuelve a ella. La navegación
marca la sección a la vista mientras el formulario se desplaza. El pie se ve siempre bajo el formulario con **Cancelar**
(vuelve de donde se vino) y **Guardar**, cuya validación lleva el foco al primer campo con error y que abre la ficha de
la tarea guardada (una nueva en el alta y en la copia) con un mensaje de éxito.

Una tarea programada elige su **Repetición** entre preajustes, cada uno solo con sus campos: *Cada N minutos* y *Cada N
horas* (N), *Diaria* y *Días laborables* (hora), *Semanal* (días de la semana y hora), *Mensual* (día del mes y hora),
*Una vez* (fecha y hora) y *Personalizada*, que muestra los campos guardados de las pantallas antiguas agrupados en *Qué
días* (años, meses, semanas, días de la semana, días) y *A qué hora* (horas, minutos, segundos) con lo que significan.
Un preajuste es solo una forma de rellenar esos campos guardados (sin datos nuevos): cada N minutos u horas es la
repetición de esa unidad, los demás la repetición personalizada de las listas a esa hora (los días laborables son de
lunes a viernes), y una vez la fecha y la hora de un lanzamiento. Al cambiar de preajuste sus campos guardados se
seleccionan en *Personalizada* (`SyncTaskEditorSchedule`), y al guardar el servidor vuelve a guardar los campos del
preajuste. Una tarea se abre en el preajuste que coincide con sus campos guardados, si no en *Personalizada* (también con
una hora con segundos: los preajustes usan horas y minutos). Los preajustes son chips; los días de *Diaria*, *Días
laborables* y *Semanal* son conmutadores («L M X J V S D»), y al cambiar los días de *Diaria* o *Días laborables* el
preajuste pasa a *Semanal*. *Personalizada* ofrece volver al preajuste que coincide con sus campos. Debajo, el
calendario de festivos y el rango (desde y hasta, cada uno una fecha y su hora; ambos opcionales, y cada hora solo se
puede rellenar cuando lo está su fecha, y se vacía con ella), y una tarjeta de vista previa calculada en el servidor con
los mismos campos según cambian: la repetición en una frase con sus partes clave en negrita, sus próximas cinco
ejecuciones como recuadros (el día y la fecha, la hora, y *hoy*, *mañana* o los días o el festivo que se salta), el
primer festivo de su calendario que se salta (o, cuando su *hasta* ya ha pasado, que no volverá a ejecutarse) y cómo se
guarda (los campos guardados, plegados). Las fechas, horas y números ocupan una columna ajustada a su valor, así que se
colocan uno tras otro. Una tarea de fichero muestra cada cuánto
revisa su carpeta, dónde busca, el calendario y el rango; una de webhook, su gancho.

Usa estos targets:

- `getTaskEditor` (permiso `scheduler.update`), el target de la pantalla, con `TskEdtMod` e `IdeTsk.selected`: los
  campos de `taskData` con los padres (`TskPrn`, edición), la copia (copia) o ninguno (alta), con `TskEdtMod` (`new`
  cuando la tarea no existe) y `TskWhn` (el tipo de lanzamiento, o `3` para una tarea manual con padres).
  `getTaskEditorHeader` da su tarjeta de cabecera (`title`, `subtitle`, `chip`, `chipTone`).
- `getTaskEditorParentOptions` (permiso `scheduler.read`): las filas de `getTaskEditorParents` para la tarea del modo
  con los campos del select (`group`, `disabled`, `description`, ver [criterios](api/criteria.md)).
- `getTaskEditorParameters` (permiso `scheduler.update`): los parámetros de la tarea (ninguno en el alta, sin
  identificadores en la copia).
- `getTaskEditorWhenOptions` y `getTaskEditorExecutionOptions` (permiso `scheduler.read`): las opciones de las tarjetas
  de **Cuándo** y del tipo de ejecución (`value`, `label`, `icon`, `description`).
- `getTaskEditorSchedule` (permiso `scheduler.read`): la vista previa de una tarea programada con los campos de la
  pantalla (el preajuste `SchPre` y sus campos `SchEvy`, `SchTim`, `SchWkd`, `SchMdy`, y los campos guardados): la
  repetición en una frase con sus partes clave en negrita (`title`, segmentos), sus próximas cinco ejecuciones (`tiles`,
  `tilesLabel`), el primer festivo que se salta o por qué no hay ejecuciones (`description`) y cómo se guarda
  (`details`, `detailsLabel`); un preajuste con un campo sin rellenar lo pide. `getTaskEditor` trae el preajuste de la
  tarea y sus campos (con `SchWkdPre`, el preajuste para el que se rellenaron sus días).
- `getTaskEditorWeekDayOptions` (permiso `scheduler.read`): los conmutadores de los días (`value`, `label` su inicial,
  `title` su nombre completo).
- `SyncTaskEditorSchedule` (permiso `scheduler.read`, no guarda nada): los campos guardados del preajuste como acciones
  select de sus criterios; también rellena los días de *Diaria* y *Días laborables*, y elige *Semanal* cuando cambian.
- `getTaskEditorScheduleMatch` y `BackToTaskEditorSchedulePreset` (permiso `scheduler.read`, no guardan nada): el
  preajuste que coincide con la repetición personalizada, y la vuelta a él (el preajuste y sus campos como acciones
  select).
- `getTaskEditorParentGraph` y `getTaskEditorChildGraph` (permiso `scheduler.read`): las filas del grafo de flujo en
  modo tarea para los padres elegidos (`TskPrn`, aún sin guardar) y para las tareas que se lanzan después de la tarea
  tal como están guardadas.
- Los dos guardados terminan con `getTaskEditorSaved`, que responde con la tarea guardada como `IdeTsk.selected`.

- `NewSchedulerTaskEditor` y `UpdateSchedulerTaskEditor` (permiso `scheduler.update`) guardan la tarea (su configuración, lanzamiento, parámetros, informe y gancho de webhook; una tarea programada con un preajuste distinto de *Personalizada* guarda los campos del preajuste en su fila de lanzamiento, `applyTaskEditorSchedule`), pero las dependencias se editan **desde la hija**: `TskPrn` es la lista de las tareas tras las que se lanza, y sus propias filas de `AweSchTskDpn` (`IdeTsk` = la tarea) se sustituyen por ellas. Las filas de las demás tareas se mantienen, y `DpnOrd` queda vacío (ordena las hijas de un padre en sus flujos). *Después de otras tareas* se guarda como lanzamiento manual (`TypLch` = `0`) con padres. Un padre que es la propia tarea o que ya se lanza después de ella, directamente o a través de otras tareas, se rechaza con el mensaje *Dependencia circular* antes de cambiar nada. Tras guardar, la tarea toma su nuevo lanzamiento en el planificador sin purgar sus ejecuciones, sus logs ni los ficheros que ha visto (un cambio de tipo de lanzamiento añade o quita su disparador; `POST /scheduler/api/v1/task/{taskId}/refresh` con `awe.scheduler.remote-enabled=true`). El nuevo disparador se construye y se comprueba antes de quitar el anterior, así que una tarea cuyo calendario no existe mantiene su disparador anterior y el guardado muestra el error; solo se tocan los grupos de tareas programadas y de fichero. Un disparador nunca queda activo cuando no debe lanzarse: una tarea inactiva, y una tarea cuyo disparador no se lanza ya después de ahora, se quedan sin disparador (no se añade ninguno pausado), y al activar la tarea (`ActSchTsk`) se añade su disparador a partir de la tarea guardada; el catálogo y la ficha de la tarea la muestran inactiva, sin próxima ejecución. Si el nuevo disparador no se puede añadir vuelve el anterior, reconstruido para empezar en su anterior próximo lanzamiento (o en su primer lanzamiento después de ahora si ese ya ha pasado), así que no se lanza ningún disparo pasado como fallo de disparo; un disparador anterior que estaba pausado no vuelve, y la tarea se queda sin disparador hasta que se activa. Sus padres no se tocan en el planificador: una tarea que termina lee de la base de datos las tareas que se lanzan después de ella.
- `getTaskEditorParents` (permiso `scheduler.read`): las tareas tras las que se puede lanzar, todas menos ella misma (`taskId`; ninguna en una tarea nueva, y `copyTaskId` da los padres de una tarea duplicada). Cada fila tiene `value`, `label`, `Act`, la condición del padre `PrnCnd` (*Lanza en OK*, *OK o aviso*, *OK o error*, *OK, aviso o error*: los interruptores del padre, iguales para todas las tareas que se lanzan después de él), su resumen «Cuándo» (`PrnWhnTxt`, `PrnWhnIcn`, `PrnWhnSub`), su grupo `PrnGrp` (`chosen` primero, `available` y luego `blocked`, cada grupo por nombre), `PrnSel`, y `PrnDis` con el motivo `PrnDisTxt` (*Dependencia circular: ya se lanza después de esta tarea*) en las tareas que se lanzan después de ella. Una tarea elegida que ya se lanza después de ella (un ciclo guardado antes del editor) sigue elegida con `PrnDis` = `0` y el motivo, para poder quitarla pero no volver a añadirla. Las filas no llevan nada restringido a los editores.
- `getTaskEditorCopy` (permiso `scheduler.update`, porque copiar una tarea es editarla: la copia lleva dónde se ejecuta, sus ficheros y quién recibe su informe): los campos de `taskData` de una tarea (`taskId`) para duplicarla, con el nombre *nombre (copia)* en el idioma del usuario (el nombre se recorta para que el total quepa en los 40 caracteres del nombre de una tarea), inactiva, sin identificador ni gancho de webhook (su clave no se puede repetir), y con sus padres (`TskPrn`). Las tareas que se lanzan después de ella no se copian.

## Pantalla de configuración {#configuration-screen}

La pantalla **Configuración** (`scheduler-configuration`, permiso `scheduler.read`; cambiar un servidor o un calendario
necesita `scheduler.update`, y manejar el motor `scheduler.execute`) tiene tres pestañas: **Servidores**,
**Calendarios** y **Motor**. El título de cada pestaña dice cuántos servidores o calendarios hay, o el estado del motor.
La pestaña forma parte de la ruta: otra pantalla abre una pestaña enviando `CfgTab` (`CfgSrv`, `CfgCal` o `CfgEng`); al
recargar el navegador se abre la primera.

| Pestaña | Qué muestra | Acciones |
|---------|-------------|----------|
| **Servidores** | Los servidores externos de las tareas de fichero y SSH: nombre, host, puerto, tipo de conexión, estado activo, **En uso por** (un enlace al catálogo de tareas filtrado por el servidor) y **Última prueba** (la última prueba de conexión de la sesión, nunca se guarda). Búsqueda por nombre o host y el filtro de activos con sus recuentos | **Nuevo servidor** y **Probar todos** (todos los servidores activos); el menú de fila edita, prueba, activa, desactiva (con confirmación) y borra. Un servidor en uso no se puede borrar, tampoco en el servidor. El panel lateral edita la conexión y la autenticación: los secretos guardados nunca se devuelven (**Cambiar**, **Reemplazar**, **Quitar**), y **Probar conexión** prueba los valores del formulario sin guardarlos |
| **Calendarios** | Los calendarios de festivos: nombre, descripción, fechas (cuántas y la próxima), estado activo y **En uso por**. Búsqueda y filtro de activos | **Nuevo calendario**; el panel lateral edita el nombre, la descripción, el estado activo y los días en los que no se ejecutan las tareas (**Añadir fecha**, **Importar fichero** con un fichero `.ics` o `.csv`), con una vista mensual de solo lectura. Al guardar, el motor vuelve a cargar el calendario. Borrar un calendario en uso lista las tareas y fechas afectadas y lo quita de ellas |
| **Motor** | La tarjeta del motor (estado, instancia, versión, almacén de trabajos, hilos en uso, en marcha desde, trabajos ejecutados y los detalles técnicos para soporte) y las programaciones del motor (tarea, trigger, calendario, ejecución anterior y próxima, estado del trigger), filtradas por todas, pausadas o con error | **Pausar…**, **Reanudar**, **Reiniciar…** y **Detener y vaciar…**, cada una con confirmación; detener y vaciar pide al operador que confirme que ninguna tarea se ejecutará hasta reanudar el motor. Cuando espera una ejecución diferida, una nota enlaza a **Actividad** |

Lee las consultas `getConfiguration*` (permiso `scheduler.read`) y guarda con los targets de servidores
(`NewSchedulerServer`, `UpdSchSrv`, `ActSchSrv`, `DeaSchSrv`, `DeleteConfigurationServer`, `TstSchSrvCnx`,
`TstSchSrvCnxSel`, `TstAllSchSrvCnx`) y de calendarios (`NewConfigurationCalendar`, `UpdConfigurationCalendar`,
`ActConfigurationCalendar`, `DeaConfigurationCalendar`, `DeleteConfigurationCalendar`,
`ImportConfigurationCalendarDates`), todos con el permiso `scheduler.update`. Las acciones del motor usan
`stopScheduler`, `startScheduler`, `restartScheduler` y `clearAndStop` (permiso `scheduler.execute`).

## Página del flujo {#flow-run-page}

La página del flujo (`scheduler-flow-run`, permiso `scheduler.read`; detener una ejecución necesita `scheduler.execute`)
muestra una [ejecución de flujo](#flow-runs): qué ha pasado, por qué y qué queda. Todo enlace a un flujo la abre: la
columna de flujo y el panel lateral de la [pantalla de actividad](#activity-screen), y el historial, la última ejecución
y el panel lateral de la [ficha de tarea](#task-screen). El enlace envía el flujo como `ExeRun.selected`, y la pestaña del
navegador lo conserva, así que al recargar se abre el mismo flujo. Decláralo como opción invisible de **Actividad** (ver
el menú anterior), para que su miga de pan vuelva a **Actividad**.

- **Cabecera**: el flujo (su tarea y ejecución raíz) como título, su estado final, quién lo lanzó, su inicio, fin y
  duración, cuántas de sus tareas han terminado, la tarea a la que apunta el estado final y los parámetros de su
  lanzamiento. El botón de información junto al estado explica su regla: **Error** si una tarea terminó con error,
  detenida o interrumpida; si no, **Aviso** si una terminó con aviso o se omitió; si no, **OK**.
- **Vistas**: **Grafo**, **Cronograma** y **Lista**, un widget `flow-graph` en modo `run` cada una (ver
  [Ejecuciones de flujo](#flow-runs)), elegidas con el control segmentado de la tarjeta.
- **Panel de la ejecución**: una tarea con ejecución (un nodo, una barra o una entrada de la lista) abre un panel lateral
  con su estado, su tarea (un enlace a su ficha), sus horas, quién la lanzó, su log en directo y la acción de detener.

Lee `getFlowRunPageHeader`, `getFlowRunPageRule` y `getFlowRunSummary` (el flujo, a partir de `IdeRun`),
`getFlowRunGraph` (sus tareas) y `getFlowRunExecutionStatus` (el estado de la ejecución seleccionada).

## Lanzamiento por webhook {#webhook-launch}

Una tarea con el tipo de lanzamiento **Webhook** se lanza cuando un `POST` firmado llega a la URL de su hook (consulta la **[guía del planificador](guides/scheduler-guide.md#4-webhook)**). Los webhooks se reciben, se almacenan y se responden de inmediato, y se procesan fuera de la petición. Se configuran con las siguientes propiedades:

| Propiedad | Descripción | Valor por defecto |
|-----------|-------------|-------------------|
| `awe.scheduler.webhooks.max-body-size` | Tamaño máximo del cuerpo de un webhook. Uno mayor se responde con `413` | `256KB` |
| `awe.scheduler.webhooks.tolerance` | Diferencia máxima entre el `webhook-timestamp` de un webhook y la hora actual. Un webhook fuera de ella se responde con `401` | `5m` |
| `awe.scheduler.webhooks.inbox-retention` | Tiempo que los webhooks procesados, ignorados y fallidos definitivamente permanecen en la bandeja antes de purgarse | `30d` |
| `awe.scheduler.webhooks.process-interval` | Intervalo entre las comprobaciones de la bandeja para buscar webhooks que lanzar | `10s` |

Una duración sin unidad se lee en la unidad de su valor por defecto (minutos para `tolerance`, días para `inbox-retention`, segundos para `process-interval`).

```properties
awe.scheduler.webhooks.max-body-size=256KB
awe.scheduler.webhooks.tolerance=5m
awe.scheduler.webhooks.inbox-retention=30d
awe.scheduler.webhooks.process-interval=10s
```

### Seguridad {#security}

La ruta de los webhooks (`/scheduler/api/v1/webhooks/**`) no necesita token bearer: tiene su propia cadena de seguridad y **la firma es la autenticación**. Una petición solo se acepta cuando su firma de [Standard Webhooks](https://www.standardwebhooks.com/) es válida para el secreto del hook y su marca de tiempo está dentro de la tolerancia. El resto de la API del planificador sigue exigiendo el token bearer (consulta [Seguridad de la API del planificador](#scheduler-api-security)).

El secreto nunca se guarda en la base de datos ni se muestra en pantalla: el hook contiene el **nombre de una propiedad de la aplicación** y la propiedad contiene el secreto. Puede contener varios secretos separados por espacios o comas, y se acepta un webhook firmado con cualquiera de ellos, de modo que un secreto se puede rotar sin interrupción: añade el nuevo, cambia el emisor y después elimina el antiguo.

```properties
fit.webhook.secret=whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=
# Durante una rotación se aceptan ambos secretos
fit.webhook.secret=whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8= whsec_BAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=
```

### Migración {#migration}

Si usas `flyway`, las tablas del lanzamiento por webhook (`AweSchTskHok`, `AweSchHokMsg` y la columna `WhkPtr` de `AweSchTskPar`) se incluyen en el módulo de migración `SCHEDULER`, versión `1.0.10`.

## Ejecución remota de comandos por SSH {#ssh-remote-command-execution}

Las tareas de comando pueden ejecutarse en un host remoto mediante SSH (consulta la **[guía del planificador](guides/scheduler-guide.md#command-execution-local-and-remote)**). Los clientes SSH usados para la ejecución remota y para la comprobación de archivos por SFTP se configuran con las siguientes propiedades:

| Propiedad | Descripción | Valor por defecto |
|----------|-------------|---------|
| `awe.scheduler.ssh-host-key-policy` | Política de verificación de claves de host. Consulta los valores más abajo. | `ACCEPT_ON_FIRST_USE` |
| `awe.scheduler.ssh-known-hosts-path` | Ruta al archivo `known_hosts` usado para persistir y leer las claves de host de confianza | `${user.home}/.ssh/known_hosts` |
| `awe.scheduler.ssh-connect-timeout` | Tiempo de espera de conexión y autenticación SSH, en segundos | `30s` |
| `awe.scheduler.connection-test-timeout` | Tiempo de espera de **Probar conexión** y **Probar todos** en la pestaña **Servidores** de la [pantalla de configuración](#configuration-screen) (sesión SSH e intercambios de conexión/datos FTP), en segundos | `10s` |

La política de claves de host admite tres valores:

| Valor | Comportamiento |
|-------|-----------|
| `ACCEPT_ON_FIRST_USE` | Confianza en el primer uso: un host que aún no está en `known_hosts` se acepta y se almacena en su primera conexión, y después se valida contra la clave almacenada. |
| `STRICT` | Solo se aceptan los hosts que ya están en `known_hosts`; los hosts desconocidos se rechazan. |
| `ACCEPT_ALL` | Se acepta cualquier clave de host sin verificación. Inseguro: solo para desarrollo y pruebas. |

```properties
awe.scheduler.ssh-host-key-policy=ACCEPT_ON_FIRST_USE
awe.scheduler.ssh-known-hosts-path=/opt/awe/.ssh/known_hosts
awe.scheduler.ssh-connect-timeout=30s
```

> **Nota:** Para uso en producción prefiere `STRICT` con un `known_hosts` preprovisionado, o el valor por defecto `ACCEPT_ON_FIRST_USE`. Evita `ACCEPT_ALL` fuera del desarrollo.


## Ejecuciones diferidas {#deferred-executions}

Una tarea maintain puede responder que ha aceptado el trabajo y que finaliza más tarde (consulta la **[guía del planificador](guides/scheduler-guide.md#deferred-executions)**). El plazo de esas ejecuciones se configura con las siguientes propiedades:

| Propiedad | Descripción | Por defecto |
|-----------|-------------|-------------|
| `awe.scheduler.deferred.default-timeout` | Tiempo que una ejecución diferida puede seguir en marcha cuando su tarea no tiene timeout. Pasado ese tiempo la ejecución se interrumpe | `24h` |
| `awe.scheduler.deferred.timeout-check-interval` | Intervalo entre las comprobaciones de las ejecuciones diferidas que han superado su plazo | `60s` |

Una duración sin unidad se lee en la unidad de su valor por defecto (horas para la primera propiedad, segundos para la segunda).

```properties
awe.scheduler.deferred.default-timeout=12h
awe.scheduler.deferred.timeout-check-interval=30s
```

## Seguridad de la API del planificador {#scheduler-api-security}

La API del planificador (`/scheduler/api/**`) exige el token bearer de AWE, validado como lo valida la [API REST](rest-module.md) (`local-jwt` u `oauth2-resource-server`, según `awe.rest.api.auth.mode`). Todas las rutas de la API lo necesitan, incluida la que completa una ejecución diferida.

La validación la hace el [starter de AWE REST](rest-module.md). El planificador no depende de él, así que la aplicación que aloja la API del planificador debe incluirlo. Sin él ninguna petición puede autenticarse: todas reciben `401` y al arrancar se escribe una advertencia en el log.

:::warning Cambio incompatible para las aplicaciones que usan un planificador externo
Antes, `/scheduler/api/**` estaba abierta. Una aplicación con el planificador embebido no se ve afectada, porque no llama a esa API. Una aplicación con `awe.scheduler.remote-enabled=true` ahora debe autenticarse contra la instancia del planificador, y la instancia del planificador debe incluir el starter de AWE REST. Configura en la aplicación principal un usuario de la instancia del planificador, o sus llamadas al planificador recibirán `401`:

| Propiedad | Descripción | Por defecto |
|-----------|-------------|-------------|
| `awe.scheduler.remote-scheduler-user` | Usuario de la instancia del planificador. La aplicación principal inicia sesión con él en `/api/authenticate` de la instancia del planificador (la `awe.scheduler.remote-scheduler-url` sin su terminación `/scheduler/api/v1`) y envía el token que obtiene. El token se reutiliza hasta 30 segundos antes de que caduque. Si está vacío, las llamadas se envían sin credenciales y se escribe una advertencia en el log | vacío |
| `awe.scheduler.remote-scheduler-password` | Contraseña de ese usuario | vacío |

```properties
awe.scheduler.remote-enabled=true
awe.scheduler.remote-scheduler-url=https://scheduler.example.com/scheduler/api/v1
awe.scheduler.remote-scheduler-user=scheduler-client
awe.scheduler.remote-scheduler-password=ENC(yourEncryptedPassword)
```
:::

:::note Limitación: el modo OAuth2 resource server de la instancia del planificador
Cuando la instancia del planificador usa `awe.rest.api.auth.mode=oauth2-resource-server`, su endpoint `/api/authenticate` está deshabilitado, así que el inicio de sesión de la aplicación principal no puede obtener un token y sus llamadas se rechazan. La API del planificador acepta los tokens del proveedor de identidad, pero el cliente del planificador remoto no los obtiene: usa el modo `local-jwt` en la instancia del planificador cuando la llama otra aplicación AWE.
:::

## Notas de actualización {#upgrade-notes}

- **Pantallas del planificador**: se han eliminado las pantallas antiguas del planificador y sus opciones de menú.
  Sustitúyelas en tus menús, perfiles y restricciones por **Actividad**, **Tareas** y **Configuración**; la tabla de
  opciones antiguas y nuevas está en la entrada [Rediseño del planificador](guides/v5-migration.md#scheduler-redesign)
  de la guía de migración.
- **Días de la semana**: las tareas semanales se lanzan ahora el día configurado (se lanzaban un día antes). El editor de tareas guarda el lunes como `1` hasta el sábado como `6` y el domingo como `0`, y el cron los convierte ahora a los días de Quartz (domingo `1` a sábado `7`); las tareas guardadas no cambian. El resumen «Cuándo» del [Catálogo de tareas](#tasks-catalog) lee los días guardados de la misma forma. Con `awe.scheduler.remote-enabled=true`, actualiza a la vez la aplicación y la instancia del planificador: la instancia construye el cron y la aplicación muestra el resumen «Cuándo», así que con solo una de ellas actualizada difieren en un día.
