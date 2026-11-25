---
id: widget
title: Widgets
---

:::tip Pruébalo en vivo

Prueba los visores de archivos y de PDF con controles en vivo en el [catálogo de componentes](../components/file-viewer.mdx): los widgets reales de AWE, un archivo de ejemplo y el XML que los produce. El [carrusel y el visor de ayuda](../components/widgets.mdx) también están ahí.

:::

Un **widget** incrusta un componente JavaScript personalizado en una pantalla de AWE. Es el punto de extensión para enriquecer la interfaz que AWE ofrece de serie con tus propios componentes de framework (y sus estilos), sin tener que habilitarlos uno a uno en el framework.

Los widgets se declaran con el elemento `<widget>` dentro de cualquier contenedor de pantalla (un `tag`, `window`, etc.):

```xml
<tag source="center">
  <widget type="file-manager" id="file-manager" style="expand"/>
</tag>
```

## Cómo se resuelve {#how-resolution-works}

El atributo `type` es la **clave que resuelve el componente cliente**. El backend renderiza el widget como un elemento personalizado con el nombre derivado de `type`:

```
<widget type="event-calendar" id="myCalendar"/>
  →  <awe-event-calendar event-calendar-id="myCalendar"></awe-event-calendar>
```

- **Cliente Angular (`awe-client-angular`)**: el elemento se resuelve contra el registro de directivas de AngularJS. Una directiva llamada `aweEventCalendar` (elemento `awe-event-calendar`) renderiza el widget. Las aplicaciones registran sus propias directivas en su bundle, de modo que los widgets son extensibles a nivel de aplicación sin cambios en el núcleo del framework.
- **Cliente React (`awe-react-client`)**: el `type` se resuelve contra un registro de widgets del lado del cliente. Las aplicaciones registran sus propios componentes con `registerWidget(type, component)` (exportado por `awe-react-client`), de modo que los widgets son extensibles a nivel de aplicación sin cambios en el núcleo del framework. Consulta la [guía de widgets personalizados (React)](../guides/custom-widgets-react).
- Si ningún componente coincide con `type`, el cliente muestra un marcador de posición (`The widget <type> has not been created yet.`) en lugar de fallar.

Para crear y registrar tu propio widget, consulta la [guía de widgets personalizados](../guides/custom-widgets) (Angular) o la [guía de widgets personalizados (React)](../guides/custom-widgets-react).

## Widgets incluidos {#built-in-widgets}

El cliente Angular incluye de serie estos tipos de widget:

| `type`         | Descripción                              |
| -------------- | ---------------------------------------- |
| `file-manager` | Gestor / explorador de archivos          |
| `pdf-viewer`   | Visor de documentos PDF                  |
| `log-viewer`   | Visor de logs en tiempo real             |
| `file-viewer`  | Visor de archivos de texto (ambos motores) |
| `help-viewer`  | Visor de contenido de ayuda              |
| `carousel`     | Carrusel de imágenes / contenido         |
| `month-view`   | Mes de solo lectura con las fechas de una tabla (ambos motores) |

### Cliente React {#react-client}

El cliente React (`awe-react-client`) resuelve el mismo `type` contra un registro del lado del cliente e incluye los mismos tipos de widget. Las aplicaciones añaden los suyos llamando una vez al arrancar a `registerWidget(type, component)` (exportado por `awe-react-client`); los widgets incluidos usan ese mismo mecanismo. Los atributos del nodo widget (`id`, `style`, ...) se pasan al componente como props. Consulta la [guía de widgets personalizados (React)](../guides/custom-widgets-react).

### Visor de logs {#log-viewer}

El widget `log-viewer` (cliente React) muestra un log en tiempo real: una barra con un campo de búsqueda, un botón de
filtro (solo las líneas que coinciden), el número de coincidencias y, en su extremo derecho, el interruptor **Seguir el
final** (`LOG_VIEWER_FOLLOW`). El interruptor está activo mientras el widget lee las líneas nuevas cada `autorefresh`
segundos y se desplaza a la última; al desactivarlo deja de leer. El texto del campo de búsqueda es `LOG_VIEWER_SEARCH`.

La respuesta de una lectura puede traer otras acciones de cliente además de las líneas (`log-delta`): se ejecutan sobre el
visor, en los dos motores. El servidor deja de seguir el log con una acción `update-controller` sin destino que pone
`autorefresh` a `0` (el planificador la envía cuando la ejecución del log ha terminado): el interruptor se desactiva y
las lecturas paran.

### Visor de archivos {#file-viewer}

El widget `file-viewer` muestra el texto de un archivo dentro de un `<pre>`. Funciona en el cliente Angular y en el React.
Al cargar envía los valores de la pantalla a `server-action`/`target-action` (por ejemplo `file` / `text`) y muestra la
respuesta cuando el servidor devuelve estado 201 con contenido. `autoload` lee el archivo al iniciar la pantalla y
`autorefresh` (segundos) lo vuelve a leer periódicamente. El parámetro de widget `stickBottom` (`boolean`) mantiene la
vista al final del archivo mientras el usuario está abajo (y en la primera carga).

### Vista de mes {#month-view}

El widget `month-view` dibuja un mes de solo lectura con las fechas de una tabla de la misma pantalla tachadas, y la tira
de los doce meses del año con el número de fechas de cada uno. Funciona en el cliente Angular y en el React. No hace
ninguna llamada al servidor: lee las filas actuales de la tabla en el cliente, incluidas las filas añadidas o cambiadas
y aún sin guardar (las filas borradas y sin guardar se dejan fuera), y se vuelve a dibujar cuando cambian.

```xml
<grid id="GrdCalDat" multioperation="true" ...>
  <column name="Dat" label="PARAMETER_DATE" component="date" />
  <column name="Nam" label="PARAMETER_NAME" component="text" />
</grid>
<widget type="month-view" id="CalMth">
  <widget-parameter type="string" name="grid" value="GrdCalDat" />
  <widget-parameter type="string" name="dateColumn" value="Dat" />
  <widget-parameter type="string" name="nameColumn" value="Nam" />
</widget>
```

| Parámetro    | Obligatorio | Descripción                                                                              |
| ------------ | ----------- | ---------------------------------------------------------------------------------------- |
| `grid`       | Sí          | Id de la tabla de la misma pantalla cuyas filas dan las fechas.                          |
| `dateColumn` | Sí          | Columna de la tabla con la fecha (`dd/mm/yyyy`, o `yyyy-mm-dd`).                         |
| `nameColumn` | No          | Columna con el nombre de cada fecha: el tooltip y parte del nombre accesible del día.    |
| `legend`     | No          | Clave de locale de la leyenda bajo el mes (`MONTH_VIEW_LEGEND` por defecto).             |

- Abre en el mes de la próxima fecha a partir de hoy (incluido hoy) o, si no la hay, en el mes de hoy, y sigue las filas
  de la tabla hasta que el usuario pasa a otro mes con los botones de mes anterior y siguiente (`MONTH_VIEW_PREVIOUS`,
  `MONTH_VIEW_NEXT`) o con un mes de la tira (`MONTH_VIEW_STRIP`, muestra ese mes del año mostrado).
- La semana empieza en lunes; los nombres de los meses y de los días siguen el idioma de la aplicación.
- Una fecha de la tabla es un día tachado. Su estado se indica con la línea y con su nombre accesible y su tooltip, no solo
  con el color: `6, excluido: Día de la Constitución`, con `(sin guardar)` para una fila aún sin guardar
  (`MONTH_VIEW_EXCLUDED`, `MONTH_VIEW_UNSAVED`). Cada mes de la tira es un botón conmutable cuyo nombre lleva su número de
  fechas (`MONTH_VIEW_MONTH_DATES`).

## Atributos {#attributes}

| Atributo        | Obligatorio | Descripción                                                                                       |
| --------------- | ----------- | ------------------------------------------------------------------------------------------------- |
| `id`            | Sí          | Identificador único del widget (se usa como id del componente en el cliente).                     |
| `type`          | No          | Clave del componente que selecciona el componente cliente (ver [resolución](#how-resolution-works)). |
| `component`     | No          | Nombre del componente Javascript (avanzado/heredado).                                             |
| `style`         | No          | Clases CSS aplicadas al contenedor del widget (p. ej. `expand` para ocupar el espacio disponible). |
| `visible`       | No          | Si el widget es visible (`true` / `false`).                                                       |
| `help`          | No          | Clave de locale del texto de ayuda del widget.                                                    |
| `help-image`    | No          | Ruta de la imagen de ayuda.                                                                       |
| `server-action` | No          | Acción de servidor para obtener los datos del widget.                                             |
| `target-action` | No          | Acción destino / consulta ejecutada para alimentar el widget.                                     |
| `initial-load`  | No          | Acción de carga inicial ejecutada al arrancar la pantalla.                                        |

## Parámetros del widget {#widget-parameters}

Usa `<widget-parameter>` para pasar valores de configuración al componente cliente. Los parámetros llegan al componente a través de su controlador (`component.controller.parameters` en el cliente Angular):

```xml
<widget type="event-calendar" id="myCalendar" style="expand">
  <widget-parameter type="string" name="initialView" value="dayGridMonth"/>
  <widget-parameter type="boolean" name="editable" value="true"/>
</widget>
```

| Atributo  | Obligatorio | Descripción                                                                                                    |
| --------- | ----------- | -------------------------------------------------------------------------------------------------------------- |
| `type`    | Sí          | Tipo del parámetro: `string`, `label`, `boolean`, `integer`, `long`, `float`, `double`, `array`, `object`, `null`. |
| `value`   | Sí          | Valor del parámetro (interpretado según `type`).                                                               |
| `name`    | No          | Nombre del parámetro (clave) tal como lo recibe el componente.                                                 |

Los parámetros de tipo `array` y `object` pueden anidar más elementos `<widget-parameter>`.
