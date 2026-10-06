---
id: datalist-java
title: DataList in Java
sidebar_label: DataList in Java
---

`DataList` is AWE's standard tabular payload for Java-side data exchange. You will usually encounter it when a query returns rows, when a service prepares grid data, or when AWE binds row-based request data into Java objects.

## Quick path

1. Start with `DataListUtil` when you need to build rows, read cells, add columns, or convert beans.
2. Use `DataListService` when you explicitly want a Spring-managed conversion path through `ConversionService`.
3. Keep `DataList` itself as the container: rows live there, while most practical helper operations live in the utility layer.

## What DataList is in AWE

`DataList` is the framework DTO that represents tabular data in Java:

- A row is a `Map<String, CellData>`
- A table is a `List<Map<String, CellData>>`
- Metadata such as `records`, `page`, and `total` travels with the payload

In practice, AWE uses it as the common format between:

- query results
- service outputs
- grid-oriented client actions
- parameter payloads that come from screens or stored UI state

If you are filling a grid, reading query output, or transforming row-based request data into beans, you are already working in the `DataList` layer.

## DataListUtil vs DataListService

This distinction matters.

| API | How it is used | Best for | Current framework role |
|-----|----------------|----------|------------------------|
| `DataListUtil` | Static utility methods | Day-to-day `DataList` creation, reading, updates, filtering, sorting, and bean conversion | Most visible low-level utility API in the current codebase |
| `DataListService` | Spring-injected bean | Conversion scenarios that should go through Spring `ConversionService` | Available Spring-managed conversion bean |

### The rule of thumb

Start with `DataListUtil` when your task is row and column manipulation, simple bean conversion, or other low-level `DataList` work.

In the current source tree, it is also the utility used by framework helpers for parameter-to-bean binding and by many Java-side `DataList` operations.

Use `DataListService` when your conversion logic needs Spring-managed type conversion behavior, custom formatters, or an injected service API in your own service class.

## Core mental model

Think of the layer in three parts:

| Part | Responsibility |
|------|----------------|
| `DataList` | Owns rows and metadata |
| `CellData` | Wraps individual cell values |
| `DataListUtil` / `DataListService` | Read, build, transform, and convert |

`DataList` itself includes a few direct row operations such as `addRow`, `updateRow`, and `deleteRow`. Most higher-level work still happens through `DataListUtil`.

## Creating DataList values

### Create a DataList from columns

This is a common pattern when a service already has parallel lists and needs to fill a grid or return tabular data.

```java
public DataList buildStatusData(List<String> ids, List<String> labels, List<Integer> priorities) {
  DataList dataList = new DataList();

  DataListUtil.addColumn(dataList, "id", ids);
  DataListUtil.addColumn(dataList, "label", labels);
  DataListUtil.addColumn(dataList, "priority", priorities);

  return dataList;
}
```

### Create a DataList from beans

This is often the cleanest option when the source data already exists as Java DTOs.

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

### Create a one-row payload

Useful for compact service responses or single-record client actions.

```java
public DataList buildSummaryRow(String code, String message) {
  DataList dataList = new DataList();
  DataListUtil.addColumnWithOneRow(dataList, "code", code);
  DataListUtil.addColumnWithOneRow(dataList, "message", message);
  return dataList;
}
```

## Reading DataList rows and columns

### Read one cell

```java
public String firstCustomerName(DataList dataList) {
  CellData cell = DataListUtil.getCellData(dataList, 0, "name");
  return cell != null ? cell.getStringValue() : "";
}
```

### Read one row

```java
public Map<String, CellData> firstRow(DataList dataList) {
  return DataListUtil.getRow(dataList, 0);
}
```

### Read one full column

```java
public List<CellData> readPriorityColumn(DataList dataList) {
  return DataListUtil.getColumn(dataList, "priority");
}
```

### Read metadata

```java
long records = dataList.getRecords();
long page = dataList.getPage();
long totalPages = dataList.getTotal();
```

## Updating DataList rows and columns

### Add a new column to existing rows

```java
public void addStatusColumn(DataList dataList) {
  DataListUtil.addColumn(dataList, "status", "PENDING");
}
```

This adds the same default value to every existing row.

### Copy or rename columns

```java
public void normalizeLabels(DataList dataList, DataList source) {
  DataListUtil.copyColumn(dataList, "displayLabel", source, "label");
  DataListUtil.renameColumn(dataList, "displayLabel", "screenLabel");
}
```

### Update a row

```java
public void markRowAsProcessed(DataList dataList, int rowIndex) {
  Map<String, CellData> row = new HashMap<>(DataListUtil.getRow(dataList, rowIndex));
  row.put("status", new CellData("PROCESSED"));
  dataList.updateRow(row, rowIndex);
}
```

### Delete a row

```java
public void deleteFirstRow(DataList dataList) {
  dataList.deleteRow(0);
}
```

## Bean conversion use cases

### Convert query output into typed beans with DataListUtil

This is the most common read-side pattern.

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

This is the simplest conversion path and matches many examples already present in the codebase.

### Convert with DataListService when you want Spring conversion in your own code

`DataListService` is registered as a Spring bean and delegates property assignment through Spring's `ConversionService`.

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

Choose this path when bean fields depend on configured converters or formatters.

### Convert beans back into DataList

This is useful when a service receives domain objects but must return grid-ready data.

```java
public ServiceData fillCustomers(List<CustomerRow> customers) {
  DataList dataList = DataListUtil.fromBeanList(customers);

  return new ServiceData()
    .setDataList(dataList);
}
```

## How this relates to service parameter binding

If your main goal is to declare bean parameters in `Services.xml`, read [Services Definition](service-definition.md) for the full XML contract and end-to-end examples.

The important link to this page is simpler:

- service parameter binding often starts from row-based payloads that end up as `DataList`
- current framework parameter-to-bean helpers commonly use `DataListUtil.getParameterBeanValue(...)` and `DataListUtil.getParameterBeanListValue(...)`
- `DataListService` is useful when you want an injected, Spring conversion-oriented API in your own service code

That is why this guide focuses on working with `DataList` itself, while the full service declaration details stay in the service-definition reference.

## Common utility operations you may also need

### Filter rows

```java
DataListUtil.filter(dataList, "status", "ACTIVE");
```

### Sort rows

```java
DataListUtil.sort(dataList, "name", "ASC");
```

### Merge multiple DataLists by key

```java
DataList merged = DataListUtil.mergeByKey(
  List.of("id"),
  baseCustomerData,
  enrichedCustomerData,
  auditCustomerData
);
```

This keeps the first row seen for each key and fills missing columns from later sources.

## Common mistakes and caveats

### 1. Reaching for DataListService before you need Spring conversion

Do not start there unless you actually need Spring conversion behavior. Many current AWE helper paths use `DataListUtil` directly.

### 2. Forgetting that rows are map-based

Column names must match the expected bean field names or consumer expectations exactly. A mismatch such as `user_id` vs `userId` can silently produce incomplete conversions.

### 3. Assuming every cell exists

`DataListUtil.getCellData(...)` can return `null`. Defensive checks matter when data comes from optional query fields or partially built rows.

### 4. Building uneven column lists without thinking about row shape

`addColumn(dataList, name, values)` grows rows as needed. If one column has three values and another has one, later reads may see partially populated rows.

### 5. Expecting bean conversion to fix naming or semantic problems

Conversion helps with types, not with poor structure. If the source column names do not represent the target bean correctly, fix the payload first.

### 6. Overwriting rows when a targeted update is clearer

For row changes, prefer reading the row, adjusting only the required cells, and then calling `updateRow(...)` instead of rebuilding the whole `DataList` blindly.

## Recommended usage pattern

| Scenario | Preferred API |
|----------|----------------|
| Build grid payload from DTOs | `DataListUtil.fromBeanList(...)` |
| Add or read columns in service code | `DataListUtil` |
| Read query results into typed DTOs | `DataListUtil.asBeanList(...)` |
| Understand request-style bean binding helpers used by current framework paths | `DataListUtil.getParameterBeanValue(...)` / `getParameterBeanListValue(...)` |
| Use Spring `ConversionService` or custom formatters in your own service class | `DataListService` |

## Next step

If you are defining Java services, continue with [Services Definition](service-definition.md). If you are consuming the result in the UI layer, the next common stop is [Actions](actions.md).
