---
id: builder
title: Módulo Builder
sidebar_label: Módulo Builder
---

El módulo Builder es un conjunto de utilidades Java diseñado para cubrir algunas funcionalidades:

- Facilitar al desarrollador la tarea de enviar acciones al frontend.
- Permitir definir pantallas dinámicas en Java en lugar de archivos XML.

##  **Configuración** {#configuration}

Añade la siguiente dependencia en tu archivo `pom.xml` de maven:

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-builder-spring-boot-starter</artifactId>
</dependency>
```

¡Y ya está!

##  **Constructores de acciones de cliente** {#client-action-builders}

Los constructores de acciones de cliente ayudan al desarrollador en la tarea de enviar acciones de cliente específicas al frontend.

####  **Constructor de acción `screen`** {#screen-action-builder}

Esta acción hace que la aplicación se mueva a otra pantalla.

Uso:

```java
serviceData.addClientAction(new ScreenActionBuilder("another-screen-option").build());
```

Este ejemplo crea una acción de cliente que intentará navegar a la opción definida como `another-screen-option`.

####  **Constructor de acción `redirect`** {#redirect-action-builder}

Esta acción hace que la aplicación redirija a otra URL.

Uso:

```java
serviceData.addClientAction(new RedirectActionBuilder("http://go.to.another.url").build());
```

Este ejemplo crea una acción de cliente que intentará navegar a la URL `http://go.to.another.url`.

####  **Constructor de acción `redirect-screen`** {#redirect-screen-action-builder}

Esta acción hace que la aplicación redirija una pantalla específica a otra URL.

Uso:

```java
serviceData.addClientAction(new RedirectScreenActionBuilder("specific-screen", "http://go.to.another.url").build());
```

Este ejemplo crea una acción de cliente que intentará navegar a la URL `http://go.to.another.url` si la
pantalla actual es `specific-screen` (útil para pantallas generadas dinámicamente).

####  **Constructor de acción `fill`** {#fill-action-builder}

Esta acción envía una lista de datos a un componente:

Uso:

```java
DataList dataList = new DataList();
// ...
serviceData.addClientAction(new FillActionBuilder("my-grid", dataList).build());
```

Este ejemplo envía `dataList` al componente `"my-grid"`.

####  **Constructor de acción `select`** {#select-action-builder}

Esta acción envía una lista de valores seleccionados a un componente.

Uso:

```java
List<String> values = new ArrayList();
// ...
serviceData.addClientAction(new SelectActionBuilder("my-select", values).build());
```

Uso alternativo:

```java
serviceData.addClientAction(new SelectActionBuilder("my-select", value1, value2, ... valueN).build());
```

Este ejemplo envía una lista de valores seleccionados al componente `"my-select"`.

####  **Constructor de acción `fill-suggest`** {#fill-suggest-action-builder}

Esta acción envía una lista de valores seleccionados y etiquetas a un componente suggest.

Uso:

```java
List<Object> values = new ArrayList();
// ...
serviceData.addClientAction(new FillSuggestActionBuilder("my-suggest", values).build());
```

Este ejemplo envía una lista de valores seleccionados y etiquetas al componente `"my-suggest"`.

####  **Constructor de acción `filter`** {#filter-action-builder}

Esta acción hace que un componente se recargue a sí mismo:

Uso:

```java
serviceData.addClientAction(new FilterActionBuilder("my-grid").build());
```

Este ejemplo indica al componente `"my-grid"` que recargue sus datos.

####  **Constructor de acción `update-controller`** {#update-controller-action-builder}

Esta acción provoca un cambio de atributo dentro de un componente:

Uso:

```java
// ...
serviceData.addClientAction(new UpdateControllerActionBuilder("my-select", "optional", false).build());
```

Este ejemplo cambia el atributo `optional` del componente `"my-select"` a `false`.

####  **Constructor de acción `message`** {#message-action-builder}

Esta acción permite enviar un mensaje al usuario. El tipo de mensaje puede ser uno de `OK`, `WARNING`, `ERROR` o `INFO`.

Uso:

```java
serviceData.addClientAction(new MessageActionBuilder(AnswerType.OK, "message title", "message description").build());
```

Este ejemplo envía un mensaje al frontend.

####  **Constructor de acción `dialog`** {#dialog-action-builder}

Esta acción abre una ventana modal en la pantalla.

Uso:

```java
serviceData.addClientAction(new DialogActionBuilder("my-dialog").build());
```

Este ejemplo abre la pantalla modal llamada `"my-dialog"`.

####  **Constructor de acción `confirm`** {#confirm-action-builder}

Esta acción abre una ventana modal con un botón para confirmar o cancelar la acción.

Uso:

```java
serviceData.addClientAction(new ConfirmActionBuilder("messageId").build());
```

Este ejemplo abre un diálogo modal de confirmación usando la etiqueta `message` definida como `message-id`.

Un uso alternativo de este constructor es el siguiente:

```java
serviceData.addClientAction(new ConfirmActionBuilder("message title", "message description").build());
```

En lugar de usar una etiqueta `message` definida, esta acción usa los parámetros `title` y `description` definidos en ella.

####  **Constructor de acción `get-file`** {#get-file-action-builder}

Esta acción solicita la descarga de un archivo.

Uso:

```java
FileData fileData = new FileData();
//...
serviceData.addClientAction(new DownloadActionBuilder(fileData).build());
```

Este ejemplo intenta descargar el archivo definido en el bean `fileData`.

###  **Constructores de acciones CSS** {#css-action-builders}

####  **Constructor de acción `add-class`** {#add-class-action-builder}

Esta acción busca una clase CSS en la pantalla y, si la encuentra, añade algunas clases CSS al elemento.

Uso:

```java
serviceData.addClientAction(new AddCssClassActionBuilder(".selector", "class1", "class2", "class3").build());
```

Este ejemplo busca el selector CSS `.selector` y añade las clases "class1", "class2" y "class3" al elemento si lo encuentra.

####  **Constructor de acción `remove-class`** {#remove-class-action-builder}

Esta acción busca una clase CSS en la pantalla y, si la encuentra, elimina algunas clases CSS del elemento.

Uso:

```java
serviceData.addClientAction(new RemoveCssClassActionBuilder(".selector", "class1", "class2", "class3").build());
```

Este ejemplo busca el selector CSS `.selector` y elimina las clases "class1", "class2" y "class3" del elemento si lo encuentra.

####  **Constructor de acción `toggle-class`** {#toggle-class-action-builder}

Esta acción busca un selector CSS en la pantalla y, si lo encuentra, alterna algunas clases CSS en el elemento: las clases que el elemento tiene se eliminan y las que no tiene se añaden.

Uso:

```java
serviceData.addClientAction(new ToggleCssClassActionBuilder(".selector", "class1", "class2", "class3").build());
```

Este ejemplo busca el selector CSS `.selector` y alterna las clases "class1", "class2" y "class3" en el elemento si lo encuentra.

Este ejemplo busca el selector CSS `.selector` y elimina las clases "class1", "class2" y "class3" del elemento si lo encuentra.

###  **Constructores de acciones de tabla** {#grid-action-builders}

####  **Constructor de acción `add-columns`** {#add-columns-action-builder}

Esta acción añade algunas columnas a una tabla.

Uso:

```java
List<Column> columnList = new ArrayList<>();
//...
serviceData.addClientAction(new AddColumnsActionBuilder("my-grid", columnList).build());
```

Este ejemplo añade una lista de columnas a la tabla `"my-grid"`.

####  **Constructor de acción `replace-columns`** {#replace-columns-action-builder}

Esta acción **reemplaza todas las columnas de la tabla** por la lista de columnas definida.

Uso:

```java
List<Column> columnList = new ArrayList<>();
//...
serviceData.addClientAction(new ReplaceColumnsActionBuilder("my-grid", columnList).build());
```

Este ejemplo reemplaza todas las columnas de la tabla `"my-grid"`.

####  **Constructor de acción `show-columns`** {#show-columns-action-builder}

Esta acción muestra algunas columnas de una tabla.

Uso:

```java
List<String> columnIdList = new ArrayList<>();
//...
serviceData.addClientAction(new ShowColumnsActionBuilder("my-grid", columnIdList).build());
```

Uso alternativo:

```java
serviceData.addClientAction(new ShowColumnsActionBuilder("my-grid", "column1", "column2", ... "columnN").build());
```

####  **Constructor de acción `hide-columns`** {#hide-columns-action-builder}

Esta acción oculta algunas columnas de una tabla.

Uso:

```java
List<String> columnIdList = new ArrayList<>();
//...
serviceData.addClientAction(new HideColumnsActionBuilder("my-grid", columnIdList).build());
```

Uso alternativo:

```java
serviceData.addClientAction(new HideColumnsActionBuilder("my-grid", "column1", "column2", ... "columnN").build());
```

#### **Constructor de acción `add-row`** {#add-row-action-builder}

Esta acción añade una fila a la tabla. Dependiendo de la fila seleccionada puedes decidir dónde añadir la nueva fila:

- `TOP`: Como primera línea de la tabla.
- `BOTTOM`: Como última línea de la tabla (por defecto).
- `UP`: Encima de la fila seleccionada.
- `DOWN`: Debajo de la fila seleccionada.

Uso:

```java
Map<String, Object> rowData = new HashMap<>();
//...
serviceData.addClientAction(new AddRowActionBuilder(RowPosition.TOP, "my-grid", rowData).build());
```

Uso alternativo:

```java
ObjectNode rowData = JsonNodeFactory.instance.objectNode();
//...
serviceData.addClientAction(new AddRowActionBuilder(RowPosition.DOWN, "my-grid", rowData).build());
```

#### **Constructor de acción `copy-row`** {#copy-row-action-builder}

Esta acción copia una fila en la tabla. Dependiendo de la fila seleccionada puedes decidir dónde añadir la nueva fila:

- `TOP`: Como primera línea de la tabla.
- `BOTTOM`: Como última línea de la tabla (por defecto).
- `UP`: Encima de la fila seleccionada.
- `DOWN`: Debajo de la fila seleccionada.

Uso:

```java
serviceData.addClientAction(new CopyRowActionBuilder(RowPosition.TOP, "my-grid").build());
```

También puedes definir la fila que quieres copiar:

```java
serviceData.addClientAction(new CopyRowActionBuilder(RowPosition.BOTTOM, "my-grid", "my-row").build());
```

#### **Constructor de acción `update-row`** {#update-row-action-builder}

Esta acción actualiza la fila seleccionada.

Uso:

```java
Map<String, Object> rowData = new HashMap<>();
//...
serviceData.addClientAction(new UpdateRowActionBuilder("my-grid", rowData).build());
```

Uso alternativo:

```java
ObjectNode rowData = JsonNodeFactory.instance.objectNode();
//...
serviceData.addClientAction(new UpdateRowActionBuilder("my-grid", rowData).build());
```

También puedes definir la fila que quieres actualizar:

```java
serviceData.addClientAction(new UpdateRowActionBuilder("my-grid", "my-row", rowData).build());
```

#### **Constructor de acción `delete-row`** {#delete-row-action-builder}

Esta acción elimina la fila seleccionada.

Uso:

```java
serviceData.addClientAction(new DeleteRowActionBuilder("my-grid").build());
```

También puedes definir la fila que quieres eliminar:

```java
serviceData.addClientAction(new DeleteRowActionBuilder("my-grid", "my-row").build());
```

#### **Constructor de acción `select-all-rows`** {#select-all-rows-action-builder}

Esta acción selecciona todas las filas de la tabla.

Uso:

```java
serviceData.addClientAction(new SelectAllRowsActionBuilder("my-grid").build());
```

#### **Constructor de acción `unselect-all-rows`** {#unselect-all-rows-action-builder}

Esta acción deselecciona todas las filas de la tabla.

Uso:

```java
serviceData.addClientAction(new UnselectAllRowsActionBuilder("my-grid").build());
```


####  **Constructor de acción `update-cell`** {#update-cell-action-builder}

Esta acción actualiza los datos de una celda de la tabla.

Uso:

```java
ComponentAddress address = new ComponentAddress("view", "component", "row", "column");
CellData cellData = new CellData(value);
//...
serviceData.addClientAction(new UpdateCellActionBuilder("my-grid", cellData).build());
```

Este ejemplo cambia la celda `column` de la fila `row` de la tabla `component` con el valor `cellData`.

Este constructor también permite definir los datos como `JsonNode` en lugar de `CellData`:

```java
ComponentAddress address = new ComponentAddress("view", "component", "row", "column");
ObjectNode nodeData = JsonNodeFactory.instance.objectNode();
nodeData.put("icon", "fa-empire");
nodeData.put("style", "text-danger");
nodeData.put("value", "fa-empire");
//...
serviceData.addClientAction(new UpdateCellActionBuilder(address, nodeData).build());
```

###  **Constructores de acciones de gráfico** {#chart-action-builders}

####  **Constructor de acción `add-chart-series`** {#add-chart-series-action-builder}

Esta acción permite añadir series a un gráfico

Uso:

```java
List<ChartSerie> serieList = new ArrayList<>();
//...
serviceData.addClientAction(new AddChartSeriesActionBuilder("my-chart", serieList).build());
```

Uso alternativo:

```java
serviceData.addClientAction(new AddChartSeriesActionBuilder("my-chart", serie1, serie2, ... serieN).build());
```

Este ejemplo añade una lista de series al gráfico `"my-chart"`.

####  **Constructor de acción `replace-chart-series`** {#replace-chart-series-action-builder}

Esta acción permite **reemplazar todas las series** de un gráfico

Uso:

```java
List<ChartSerie> serieList = new ArrayList<>();
//...
serviceData.addClientAction(new ReplaceChartSeriesActionBuilder("my-chart", serieList).build());
```

Uso alternativo:

```java
serviceData.addClientAction(new ReplaceChartSeriesActionBuilder("my-chart", serie1, serie2, ... serieN).build());
```

Este ejemplo reemplaza todas las series del gráfico `"my-chart"`.

####  **Constructor de acción `remove-chart-series`** {#remove-chart-series-action-builder}

Esta acción permite eliminar algunas series de un gráfico.

Uso:

```java
List<ChartSerie> serieList = new ArrayList<>();
//...
serviceData.addClientAction(new RemoveChartSeriesActionBuilder("my-chart", serieList).build());
```

Uso alternativo:

```java
serviceData.addClientAction(new RemoveChartSeriesActionBuilder("my-chart", serie1, serie2, ... serieN).build());
```


Este ejemplo elimina todas las series definidas en serieList del gráfico `"my-chart"`.

####  **Constructor de acción `add-points`** {#add-points-action-builder}

Esta acción envía una lista de datos de series a un gráfico:

Uso:

```java
DataList dataList = new DataList();
// ...
serviceData.addClientAction(new AddPointsActionBuilder("my-chart", dataList).build());
```

Este ejemplo envía los puntos de `dataList` al componente `"my-chart"`.
 
> **Nota:** El nombre de cada columna del datalist debe coincidir con el id de una serie del gráfico.

##  **Constructores de pantallas** {#screen-builders}

Este conjunto de constructores está diseñado para generar pantallas definidas en Java en lugar de definirlas en formato XML.

### **Constructor de pantallas** {#screen-builder}

El constructor de pantallas es el constructor principal de este conjunto. Permite al desarrollador generar dinámicamente una
estructura de pantalla XML:

```java
ScreenBuilder builder = new ScreenBuilder()
  .setId(UUID.randomUUID().toString())
  .setTemplate("window")
  .addTag(new TagBuilder()
    .setSource("center")
    .setLabel("LABEL")
    .setStyle("expand")
    .setType("div")
    .addChart(new ChartBuilder()
      .setStockChart(true)
      .setAutoload(true)
      .setId("chart1")
      .setAutorefresh(5)
      .setEnableDataLabels(true)
      .setFormatDataLabels("formatDataLabels")
      .setIconLoading(IconLoading.CIRCLEBAR)
      .setStacking(Stacking.PERCENT)
      .setInverted(true)
      .setMax(45)
      .setTheme("chartTheme")
      .setVisible(false)
      .setSubtitle("SUBTITLE")
      .setChartType(ChartType.BUBBLE)
      .setZoomType(ChartAxis.Y_AXIS)
      .setChartLegend(new ChartLegendBuilder()
        .setChartLayout(ChartLayout.HORIZONTAL)
        .setAlign(Align.CENTER)
        .setEnabled(true)
        .setFloating(true)
        .setBorderWidth(2))
      .setChartTooltip(new ChartTooltipBuilder()
        .setCrosshairs(ChartAxis.ALL)
        .setEnabled(true)
        .setNumberDecimals(4)
        .setPointFormat("pointFormat")
        .setPrefix("pre")
        .setSuffix("post")
        .setDateFormat("yyyymmdd")
        .setShared(true))
      .addChartParameter(new ChartParameterBuilder()
        .setDataType(DataType.DOUBLE)
        .setName("parameterName")
        .setValue("0.1213"))
      .addChartSerieList(new ChartSerieBuilder()
        .setDrillDown(true)
        .setColor("red")
        .setXAxis("xAxis")
        .setYAxis("yAxis")
        .setXValue("x")
        .setYValue("y")
        .setZValue("z")
        .setDrillDownSerie("drilldownSerie"))
      .addContextButton(new ContextButtonBuilder()
        .setButtonType(ButtonType.BUTTON)
        .setValue("value")
        .setIcon("icon")
        .setSize("sm")
        .setLabel("LABEL"))
      .addContextSeparator(new ContextSeparatorBuilder())
      .addDependency(new DependencyBuilder()
        .setFormule("formule")
        .setInitial(true)
        .setInvert(true)
        .setLabel("LABEL")
        .setServerAction(ServerAction.CONTROL)
        .setSourceType(SourceType.QUERY)
        .setTargetType(TargetType.ATTRIBUTE)
        .setType(DependencyType.AND)
        .setValue("value")
        .addDependencyAction((DependencyActionBuilder) new DependencyActionBuilder()
          .setServerAction(ServerAction.GET_SERVER_FILE)
          .setTargetAction("TargetAction")
          .setTarget("target")
          .setAsynchronous(true)
          .setContext("context")
          .setType(Action.ADD_ROW)
          .setSilent(true)
          .setValue("value"))
        .addDependencyElement(new DependencyElementBuilder()
          .setAlias("alias")
          .setId("id")
          .setCancel(false)
          .setView(com.almis.awe.builder.enumerates.View.REPORT)
          .setAttribute(Attribute.CURRENT_ROW_VALUE)
          .setColumn("column")
          .setCondition(Condition.EQUALS)
          .setAttribute2(Attribute.EDITABLE)
          .setColumn2("column2")
          .setEvent(Event.AFTER_ADD_ROW)
          .setId2("id2")))
      .addAxis(new AxisBuilder()
        .setFormatterFunction(FormatterFunction.FORMAT_CURRENCY_MAGNITUDE)
        .setType(AxisDataType.CATEGORY))
      .addAxis(new AxisBuilder()
        .setAllowDecimal(true))
      .setChartType(ChartType.BUBBLE)));

Screen screen = builder.build();
```

Para usar las pantallas dinámicas, ahora puedes añadir una opción en cualquiera de los menús (público o privado)
con la siguiente estructura:

```xml
<option name="dynamic-screen-test" dynamic-screen="true" dynamic-screen-service="serviceWhichReturnsAnScreen"
        label="DYNAMIC_WINDOW_TEST" .../>
```

Esto llamará directamente al servicio...

```xml
<service id="serviceWhichReturnsAnScreen">
  <java classname="com.almis.awe.service.DummyService" method="serviceWhichReturnsAnScreen"/>
</service>
```

Y el servicio que devuelve la pantalla debería ser así:

```java
public ServiceData serviceWhichReturnsAnScreen() throws AWException {
    return new ServiceData().setData(new ScreenBuilder().setId("...")...
  .build())
}
```

### **Constructores de componentes** {#component-builders}

Para cada componente diseñado en AWE tienes un `builder` para definir su comportamiento:

- `TagBuilder`: Construye un componente `<tag>`.
- `WindowBuilder`: Construye un componente `<window>`.
- `DialogBuilder`: Construye un componente `<dialog>`.
- `IncludeBuilder`: Construye un componente `<include>`.
- `MenuContainerBuilder`: Construye un componente `<menu-container>`.
- `MessageBuilder`: Construye un componente `<message>`.
- `PivotTableBuilder`: Construye un componente `<pivot-table>`.
- `ResizableBuilder`: Construye un componente `<resizable>`.
- `TagListBuilder`: Construye un componente `<tag-list>`.
- `ViewBuilder`: Construye un componente `<view>`.
- `WidgetBuilder` and `WidgetParameterBuilder`: Construye un componente `<widget>`.
- `InfoBuilder`: Construye un componente `<info>`.
- `TabBuilder` and `TabContainerBuilder`: Construye un componente `<tab>`.
- `WizardBuilder` and `WizardPanelBuilder`: Construye un componente `<wizard>`.
- `ButtonBuilder` and `ButtonActionBuilders`: Construye componentes `<button>`.
- `ImageBuilder`: Construye un componente `<img>`.
- `ContextButtonBuilder` and `ContextSeparatorBuilder`: Construye componentes `<context-button>`.
- `DependencyBuilder`, `DependencyActionBuilder` and `DependencyElementBuilder`: Construye componentes `<dependency>`.

### **Constructores de criterios** {#criteria-builders}

Hay un constructor para cada tipo de criterio definido en AWE:

- `ButtonCheckboxCriteriaBuilder`: Construye un componente `<criteria>` con el componente `button-checkbox`.
- `ButtonRadioCriteriaBuilder`: Construye un componente `<criteria>` con el componente `button-radio`.
- `CalendarCriteriaBuilder`: Construye un componente `<criteria>` con el componente `date`.
- `CheckboxCriteriaBuilder`: Construye un componente `<criteria>` con el componente `checkbox`.
- `ColorpickerCriteriaBuilder`: Construye un componente `<criteria>` con el componente `color`.
- `FilteredCalendarCriteriaBuilder`: Construye un componente `<criteria>` con el componente `filtered-calendar`.
- `HiddenCriteriaBuilder`: Construye un componente `<criteria>` con el componente `hidden`.
- `MarkdownCriteriaBuilder`: Construye un componente `<criteria>` con el componente `markdown-editor`.
- `NumericCriteriaBuilder`: Construye un componente `<criteria>` con el componente `numeric`.
- `PasswordCriteriaBuilder`: Construye un componente `<criteria>` con el componente `password`.
- `RadioCriteriaBuilder`: Construye un componente `<criteria>` con el componente `radio`.
- `SelectCriteriaBuilder`: Construye un componente `<criteria>` con el componente `select`.
- `SelectMultipleCriteriaBuilder`: Construye un componente `<criteria>` con el componente `select-multiple`.
- `SuggestCriteriaBuilder`: Construye un componente `<criteria>` con el componente `suggest`.
- `SuggestMultipleCriteriaBuilder`: Construye un componente `<criteria>` con el componente `suggest-multiple`.
- `TextareaCriteriaBuilder`: Construye un componente `<criteria>` con el componente `textarea`.
- `TextCriteriaBuilder`: Construye un componente `<criteria>` con el componente `text`.
- `TextViewCriteriaBuilder`: Construye un componente `<criteria>` con el componente `text-view`.
- `TimeCriteriaBuilder`: Construye un componente `<criteria>` con el componente `time`.
- `UploaderCriteriaBuilder`: Construye un componente `<criteria>` con el componente `uploader`.

### **Constructores de tablas y columnas** {#grid-and-column-builders}

Hay un par de constructores para los elementos de la tabla:

- `GridBuilder`: Construye un componente `<grid>`.
- `GroupHeaderBuilder`: Construye un componente `<group-header>`.

Y un constructor para cada tipo de columna:

- `CalendarColumnBuilder`: Construye una columna con el componente `date`.
- `CheckboxColumnBuilder`: Construye una columna con el componente `checkbox`.
- `ColorpickerColumnBuilder`: Construye una columna con el componente `color`.
- `FilteredCalendarColumnBuilder`: Construye una columna con el componente `filtered-calendar`.
- `MarkdownColumnBuilder`: Construye una columna con el componente `markdown-editor`.
- `NumericColumnBuilder`: Construye una columna con el componente `numeric`.
- `PasswordColumnBuilder`: Construye una columna con el componente `password`.
- `SelectColumnBuilder`: Construye una columna con el componente `select`.
- `SelectMultipleColumnBuilder`: Construye una columna con el componente `select-multiple`.
- `SuggestColumnBuilder`: Construye una columna con el componente `suggest`.
- `SuggestMultipleColumnBuilder`: Construye una columna con el componente `suggest-multiple`.
- `TextareaColumnBuilder`: Construye una columna con el componente `textarea`.
- `TextColumnBuilder`: Construye una columna con el componente `text`.
- `TextViewColumnBuilder`: Construye una columna con el componente `text-view`.
- `TimeColumnBuilder`: Construye una columna con el componente `time`.
- `UploaderColumnBuilder`: Construye una columna con el componente `uploader`.
- `IconColumnBuilder`: Construye una columna con el componente `icon`.
- `ImageColumnBuilder`: Construye una columna con el componente `image`.
- `ProgressColumnBuilder`: Construye una columna con el componente `progress`.

### **Constructores de gráficos** {#chart-builders}

También hay una lista de constructores para los componentes de gráfico:

- `AxisBuilder`: Construye los datos de los ejes del gráfico.
- `ChartBuilder`: Construye un componente de gráfico.
- `ChartLegendBuilder`: Construye los datos de la leyenda del gráfico.
- `ChartParameterBuilder`: Construye los datos de los parámetros del gráfico.
- `ChartSerieBuilder`: Construye los datos de las series del gráfico.
- `ChartTooltipBuilder`: Construye los datos del tooltip del gráfico.
