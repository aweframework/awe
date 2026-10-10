---
id: datalist-java
title: DataList en Java
sidebar_label: DataList en Java
---

`DataList` es la carga útil tabular estándar de AWE para el intercambio de datos del lado Java. Normalmente te lo encontrarás cuando una consulta devuelve filas, cuando un servicio prepara datos de un grid, o cuando AWE enlaza datos de petición basados en filas a objetos Java.

## Camino rápido {#quick-path}

1. Empieza con `DataListUtil` cuando necesites construir filas, leer celdas, añadir columnas o convertir beans.
2. Usa `DataListService` cuando quieras explícitamente una ruta de conversión gestionada por Spring a través de `ConversionService`.
3. Mantén `DataList` como el contenedor: las filas viven ahí, mientras que la mayoría de las operaciones auxiliares prácticas viven en la capa de utilidades.

## Qué es DataList en AWE {#what-datalist-is-in-awe}

`DataList` es el DTO del framework que representa datos tabulares en Java:

- Una fila es un `Map<String, CellData>`
- Una tabla es una `List<Map<String, CellData>>`
- Metadatos como `records`, `page` y `total` viajan con la carga útil

En la práctica, AWE lo usa como formato común entre:

- resultados de consultas
- salidas de servicios
- acciones de cliente orientadas a grids
- cargas útiles de parámetros que vienen de las pantallas o del estado de UI almacenado

Si estás rellenando un grid, leyendo la salida de una consulta o transformando datos de petición basados en filas en beans, ya estás trabajando en la capa `DataList`.

## DataListUtil vs DataListService {#datalistutil-vs-datalistservice}

Esta distinción es importante.

| API | Cómo se usa | Ideal para | Papel actual en el framework |
|-----|-------------|------------|------------------------------|
| `DataListUtil` | Métodos de utilidad estáticos | Creación, lectura, actualización, filtrado, ordenación y conversión a bean de `DataList` en el día a día | La API de utilidades de bajo nivel más visible en el código actual |
| `DataListService` | Bean inyectado por Spring | Escenarios de conversión que deben pasar por el `ConversionService` de Spring | Bean de conversión gestionado por Spring disponible |

### La regla general {#the-rule-of-thumb}

Empieza con `DataListUtil` cuando tu tarea sea manipular filas y columnas, hacer una conversión sencilla a bean u otro trabajo de bajo nivel con `DataList`.

En el árbol de código actual, también es la utilidad que usan los helpers del framework para enlazar parámetros a beans y muchas operaciones `DataList` del lado Java.

Usa `DataListService` cuando tu lógica de conversión necesite el comportamiento de conversión de tipos gestionado por Spring, formateadores personalizados o una API de servicio inyectada en tu propia clase de servicio.

## Modelo mental básico {#core-mental-model}

Piensa en la capa como tres partes:

| Parte | Responsabilidad |
|-------|-----------------|
| `DataList` | Posee las filas y los metadatos |
| `CellData` | Envuelve los valores de cada celda |
| `DataListUtil` / `DataListService` | Leer, construir, transformar y convertir |

`DataList` incluye por sí mismo algunas operaciones directas de fila como `addRow`, `updateRow` y `deleteRow`. La mayor parte del trabajo de más alto nivel sigue haciéndose mediante `DataListUtil`.

## Crear valores DataList {#creating-datalist-values}

### Crear un DataList a partir de columnas {#create-a-datalist-from-columns}

Es un patrón habitual cuando un servicio ya tiene listas paralelas y necesita rellenar un grid o devolver datos tabulares.

```java
public DataList buildStatusData(List<String> ids, List<String> labels, List<Integer> priorities) {
  DataList dataList = new DataList();

  DataListUtil.addColumn(dataList, "id", ids);
  DataListUtil.addColumn(dataList, "label", labels);
  DataListUtil.addColumn(dataList, "priority", priorities);

  return dataList;
}
```

### Crear un DataList a partir de beans {#create-a-datalist-from-beans}

A menudo es la opción más limpia cuando los datos de origen ya existen como DTOs de Java.

```java
@Data
@Accessors(chain = true)
public class CustomerRow {
  private String id;
  private String name;
  private Boolean active;
}

public DataList buildCustomerData() {
  List<CustomerRow> rows = List.of(
    new CustomerRow().setId("CUS-1").setName("Acme").setActive(true),
    new CustomerRow().setId("CUS-2").setName("Globex").setActive(false)
  );

  return DataListUtil.fromBeanList(rows);
}
```

### Crear una carga útil de una fila {#create-a-one-row-payload}

Útil para respuestas de servicio compactas o acciones de cliente de un único registro.

```java
public DataList buildSummaryRow(String code, String message) {
  DataList dataList = new DataList();
  DataListUtil.addColumnWithOneRow(dataList, "code", code);
  DataListUtil.addColumnWithOneRow(dataList, "message", message);
  return dataList;
}
```

## Leer filas y columnas de un DataList {#reading-datalist-rows-and-columns}

### Leer una celda {#read-one-cell}

```java
public String firstCustomerName(DataList dataList) {
  CellData cell = DataListUtil.getCellData(dataList, 0, "name");
  return cell != null ? cell.getStringValue() : "";
}
```

### Leer una fila {#read-one-row}

```java
public Map<String, CellData> firstRow(DataList dataList) {
  return DataListUtil.getRow(dataList, 0);
}
```

### Leer una columna completa {#read-one-full-column}

```java
public List<CellData> readPriorityColumn(DataList dataList) {
  return DataListUtil.getColumn(dataList, "priority");
}
```

### Leer metadatos {#read-metadata}

```java
long records = dataList.getRecords();
long page = dataList.getPage();
long totalPages = dataList.getTotal();
```

## Actualizar filas y columnas de un DataList {#updating-datalist-rows-and-columns}

### Añadir una nueva columna a filas existentes {#add-a-new-column-to-existing-rows}

```java
public void addStatusColumn(DataList dataList) {
  DataListUtil.addColumn(dataList, "status", "PENDING");
}
```

Esto añade el mismo valor por defecto a todas las filas existentes.

### Copiar o renombrar columnas {#copy-or-rename-columns}

```java
public void normalizeLabels(DataList dataList, DataList source) {
  DataListUtil.copyColumn(dataList, "displayLabel", source, "label");
  DataListUtil.renameColumn(dataList, "displayLabel", "screenLabel");
}
```

### Actualizar una fila {#update-a-row}

```java
public void markRowAsProcessed(DataList dataList, int rowIndex) {
  Map<String, CellData> row = new HashMap<>(DataListUtil.getRow(dataList, rowIndex));
  row.put("status", new CellData("PROCESSED"));
  dataList.updateRow(row, rowIndex);
}
```

### Eliminar una fila {#delete-a-row}

```java
public void deleteFirstRow(DataList dataList) {
  dataList.deleteRow(0);
}
```

## Casos de uso de conversión a bean {#bean-conversion-use-cases}

### Convertir la salida de una consulta en beans tipados con DataListUtil {#convert-query-output-into-typed-beans-with-datalistutil}

Es el patrón de lectura más habitual.

```java
@Data
public class Favourite {
  private String option;
  private String label;
}

public List<Favourite> loadFavourites(ServiceData serviceData) {
  return DataListUtil.asBeanList(serviceData.getDataList(), Favourite.class);
}
```

Es la ruta de conversión más sencilla y coincide con muchos ejemplos que ya existen en el código.

### Convertir con DataListService cuando quieras conversión de Spring en tu propio código {#convert-with-datalistservice-when-you-want-spring-conversion-in-your-own-code}

`DataListService` está registrado como bean de Spring y delega la asignación de propiedades en el `ConversionService` de Spring.

```java
@Service
@RequiredArgsConstructor
public class CustomerImportService {

  private final DataListService dataListService;

  public List<CustomerFilter> readFilters(DataList dataList) throws AWException {
    return dataListService.asBeanList(dataList, CustomerFilter.class);
  }
}
```

Elige esta ruta cuando los campos del bean dependan de conversores o formateadores configurados.

### Convertir beans de nuevo en DataList {#convert-beans-back-into-datalist}

Es útil cuando un servicio recibe objetos de dominio pero debe devolver datos listos para un grid.

```java
public ServiceData fillCustomers(List<CustomerRow> customers) {
  DataList dataList = DataListUtil.fromBeanList(customers);

  return new ServiceData()
    .setDataList(dataList);
}
```

## Cómo se relaciona con el enlace de parámetros de servicio {#how-this-relates-to-service-parameter-binding}

Si tu objetivo principal es declarar parámetros de tipo bean en `Services.xml`, lee [Definición de servicios](service-definition.md) para el contrato XML completo y ejemplos de principio a fin.

El vínculo importante con esta página es más simple:

- el enlace de parámetros de servicio suele partir de cargas útiles basadas en filas que acaban como `DataList`
- los helpers actuales del framework de parámetros a bean suelen usar `DataListUtil.getParameterBeanValue(...)` y `DataListUtil.getParameterBeanListValue(...)`
- `DataListService` es útil cuando quieres una API inyectada, orientada a la conversión de Spring, en tu propio código de servicio

Por eso esta guía se centra en trabajar con `DataList` en sí, mientras que los detalles completos de la declaración de servicios quedan en la referencia de service-definition.

## Operaciones de utilidad habituales que también puedes necesitar {#common-utility-operations-you-may-also-need}

### Filtrar filas {#filter-rows}

```java
DataListUtil.filter(dataList, "status", "ACTIVE");
```

### Ordenar filas {#sort-rows}

```java
DataListUtil.sort(dataList, "name", "ASC");
```

### Combinar varios DataList por clave {#merge-multiple-datalists-by-key}

```java
DataList merged = DataListUtil.mergeByKey(
  List.of("id"),
  baseCustomerData,
  enrichedCustomerData,
  auditCustomerData
);
```

Esto conserva la primera fila vista para cada clave y rellena las columnas que faltan con las de las fuentes posteriores.

## Errores comunes y advertencias {#common-mistakes-and-caveats}

### 1. Recurrir a DataListService antes de necesitar la conversión de Spring {#1-reaching-for-datalistservice-before-you-need-spring-conversion}

No empieces por ahí a menos que realmente necesites el comportamiento de conversión de Spring. Muchas rutas auxiliares actuales de AWE usan `DataListUtil` directamente.

### 2. Olvidar que las filas están basadas en mapas {#2-forgetting-that-rows-are-map-based}

Los nombres de columna deben coincidir exactamente con los nombres de campo del bean esperado o con lo que espere el consumidor. Una discrepancia como `user_id` frente a `userId` puede producir conversiones incompletas sin avisar.

### 3. Asumir que todas las celdas existen {#3-assuming-every-cell-exists}

`DataListUtil.getCellData(...)` puede devolver `null`. Las comprobaciones defensivas son importantes cuando los datos vienen de campos opcionales de una consulta o de filas construidas parcialmente.

### 4. Construir listas de columnas desiguales sin pensar en la forma de las filas {#4-building-uneven-column-lists-without-thinking-about-row-shape}

`addColumn(dataList, name, values)` hace crecer las filas según sea necesario. Si una columna tiene tres valores y otra uno, las lecturas posteriores pueden ver filas parcialmente rellenas.

### 5. Esperar que la conversión a bean arregle problemas de nomenclatura o de semántica {#5-expecting-bean-conversion-to-fix-naming-or-semantic-problems}

La conversión ayuda con los tipos, no con una mala estructura. Si los nombres de columna de origen no representan correctamente el bean de destino, arregla primero la carga útil.

### 6. Sobrescribir filas cuando una actualización dirigida es más clara {#6-overwriting-rows-when-a-targeted-update-is-clearer}

Para cambios en filas, es preferible leer la fila, ajustar solo las celdas necesarias y luego llamar a `updateRow(...)` en lugar de reconstruir todo el `DataList` a ciegas.

## Patrón de uso recomendado {#recommended-usage-pattern}

| Escenario | API preferida |
|-----------|---------------|
| Construir la carga útil de un grid a partir de DTOs | `DataListUtil.fromBeanList(...)` |
| Añadir o leer columnas en código de servicio | `DataListUtil` |
| Leer resultados de consultas en DTOs tipados | `DataListUtil.asBeanList(...)` |
| Entender los helpers de enlace de beans de tipo petición usados por las rutas actuales del framework | `DataListUtil.getParameterBeanValue(...)` / `getParameterBeanListValue(...)` |
| Usar el `ConversionService` de Spring o formateadores personalizados en tu propia clase de servicio | `DataListService` |

## Siguiente paso {#next-step}

Si estás definiendo servicios Java, continúa con [Definición de servicios](service-definition.md). Si estás consumiendo el resultado en la capa de UI, la siguiente parada habitual es [Acciones](actions.md).
