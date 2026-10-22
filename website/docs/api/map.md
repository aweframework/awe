---
id: map
title: Map
---

A map shows the rows of a query as markers over a geographic map. You describe it in the screen XML with a `map`
element. The map loads its data from a query, like any other AWE component, and clicking a marker selects its row, so
the [dependencies](dependencies.md) of the map (and the components that depend on it) react to the selection.

> **Note:** The map is available in the React engine. AngularJS support is pending (#798).

The map is drawn with [Leaflet](https://leafletjs.com/) (BSD-2-Clause licence) and the tiles come from
[OpenStreetMap](https://www.openstreetmap.org/) by default. Neither needs an API key.

## Map element {#map-element}

```xml
<map id="fleet" initial-load="query" target-action="TstMapPoints"
     latitude="Lat" longitude="Lon" marker-label="Lug"
     zoom="4" center-latitude="43.86" center-longitude="1.11" style="expand"/>
```

| Attribute | Use | Type | Description | Values |
| --- | --- | --- | --- | --- |
| id | **Required** | String | Map identifier | |
| style | Optional | String | CSS classes of the map | `expand` makes the map fill the space of its container |
| initial-load | Optional | String | Loads the map when the screen is generated | Only the `query` value |
| server-action | Optional | String | Type of server action that loads the map | |
| target-action | Optional | String | Name of the query that loads the map | |
| max | Optional | Number | Number of markers to load | `0` stands for all of them |
| autorefresh | Optional | Number | Interval of the automatic reload of the map | |
| latitude | Optional | String | Name of the data column with the latitude of the marker | Defaults to `latitude` |
| longitude | Optional | String | Name of the data column with the longitude of the marker | Defaults to `longitude` |
| marker-label | Optional | String | Name of the data column shown in the popup of the marker | Without it the markers have no popup |
| zoom | Optional | Number | Initial zoom level | Defaults to `4`. It applies when the map has a center |
| center-latitude | Optional | Decimal | Latitude of the initial center of the map | Set it together with `center-longitude` |
| center-longitude | Optional | Decimal | Longitude of the initial center of the map | Set it together with `center-latitude` |

Without `center-latitude` and `center-longitude` the map fits its view to the markers. A map with no markers shows a
default view.

## Data format {#data-format}

The query returns **one row per marker**. The `id` of the row is the value of the marker, the `latitude` and
`longitude` columns (or the ones named by the attributes) place it and the `marker-label` column is the text of its
popup. A row whose coordinates are empty or not numeric has no marker.

```xml
<query id="TstMapPoints" service="TstMapPoints"/>
```

| id | Lat | Lon | Lug |
| --- | --- | --- | --- |
| 1 | 40.4168 | -3.7038 | Madrid |
| 2 | 41.3874 | 2.1686 | Barcelona |
| 3 | 39.4699 | -0.3763 | Valencia |
| 4 | 37.3891 | -5.9845 | Sevilla |

## Selection and dependencies {#selection}

Clicking a marker selects its row (and opens its popup). The `id` of the row becomes the selected value of the map, so
the map is sent to the server with the selected marker, and the marker is drawn in a different color. The rows of the map
work like the rows of a grid for the [dependencies](dependencies.md): the attribute `selectedRowValue` of a
`dependency-element` is the value of a column in the selected marker.

```xml
<criteria id="MapSel" label="Selected place" component="text" readonly="true">
  <dependency source-type="formule" target-type="input" formule="'[place]'">
    <dependency-element id="fleet" column="Lug" attribute="selectedRowValue" alias="place"/>
  </dependency>
</criteria>
```

The map can also hold its own `dependency` children, like the other components.

## Drawing a route {#route}

The `polyline` [client action](actions.md) draws a route over the map: the points go in the `rows` parameter, with the
latitude and longitude columns of the map (`latitude` and `longitude` by default). While the route is shown the markers
are hidden and the view centers on the first point of the route (zoom level `8`). A route without points removes the
route and the markers come back.

```java
public ServiceData route() {
  List<Map<String, Object>> rows = List.of(
    Map.of("Lat", 40.4168, "Lon", -3.7038),
    Map.of("Lat", 41.3874, "Lon", 2.1686));

  ClientActionBuilder<?> builder = new ClientActionBuilder<>();
  builder.setType("polyline");
  builder.setTarget("fleet");
  builder.addParameter("rows", rows);
  return new ServiceData().addClientAction(builder.build());
}
```

## Tiles and attribution {#tiles}

The tiles come from the OpenStreetMap server (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`) unless you set the
property [`awe.application.map.tile-url`](../properties.md#awe.application.map.tile-url) to the URL template of another
tile server, with the `{z}`, `{x}` and `{y}` markers:

```properties
awe.application.map.tile-url=https://tiles.example.com/{z}/{x}/{y}.png
```

> **Warning:** The OpenStreetMap tile server is for light use and has an
> [usage policy](https://operations.osmfoundation.org/policies/tiles/) that you have to follow. An application with a lot
> of traffic must use its own tile server or a commercial tile provider, because the policy allows the OpenStreetMap
> Foundation to block heavy use.

The map shows the attribution "© OpenStreetMap contributors" with a link to the
[OpenStreetMap copyright page](https://www.openstreetmap.org/copyright), as the data licence (ODbL) requires, and the
Leaflet credit. Do not hide them. The zoom is limited to levels 3 to 19, the ones that the OpenStreetMap tiles provide.
