---
id: map
title: Mapa
---

Un mapa muestra las filas de una consulta como marcadores sobre un mapa geográfico. Se describe en el XML de la pantalla con un elemento `map`. El mapa carga sus datos de una consulta, como cualquier otro componente de AWE, y al hacer clic en un marcador se selecciona su fila, de modo que las [dependencias](dependencies.md) del mapa (y los componentes que dependen de él) reaccionan a la selección.

> **Nota:** El mapa está disponible en los dos motores, AngularJS y React, con el mismo comportamiento.

El mapa se dibuja con [Leaflet](https://leafletjs.com/) (licencia BSD-2-Clause) y las teselas proceden de [OpenStreetMap](https://www.openstreetmap.org/) por defecto. Ninguno necesita clave de API.

## Elemento map {#map-element}

```xml
<map id="fleet" initial-load="query" target-action="TstMapPoints"
     latitude="Lat" longitude="Lon" marker-label="Lug"
     zoom="4" center-latitude="43.86" center-longitude="1.11" style="expand"/>
```

| Atributo | Uso | Tipo | Descripción | Valores |
| --- | --- | --- | --- | --- |
| id | **Requerido** | String | Identificador del mapa | |
| style | Opcional | String | Clases CSS del mapa | `expand` hace que el mapa ocupe el espacio de su contenedor |
| initial-load | Opcional | String | Carga el mapa al generar la pantalla | Solo el valor `query` |
| server-action | Opcional | String | Tipo de acción de servidor que carga el mapa | |
| target-action | Opcional | String | Nombre de la consulta que carga el mapa | |
| max | Opcional | Número | Número de marcadores a cargar | `0` significa todos |
| autorefresh | Opcional | Número | Intervalo de recarga automática del mapa | |
| latitude | Opcional | String | Nombre de la columna de datos con la latitud del marcador | Por defecto `latitude` |
| longitude | Opcional | String | Nombre de la columna de datos con la longitud del marcador | Por defecto `longitude` |
| marker-label | Opcional | String | Nombre de la columna de datos que se muestra en el popup del marcador | Sin él los marcadores no tienen popup |
| zoom | Opcional | Número | Nivel de zoom inicial | Por defecto `4`. Se aplica cuando el mapa tiene centro |
| center-latitude | Opcional | Decimal | Latitud del centro inicial del mapa | Indíquelo junto con `center-longitude` |
| center-longitude | Opcional | Decimal | Longitud del centro inicial del mapa | Indíquelo junto con `center-latitude` |

Sin `center-latitude` y `center-longitude` el mapa ajusta su vista a los marcadores. Un mapa sin marcadores muestra una vista por defecto.

## Formato de los datos {#data-format}

La consulta devuelve **una fila por marcador**. El `id` de la fila es el valor del marcador, las columnas `latitude` y `longitude` (o las que indiquen los atributos) lo sitúan y la columna `marker-label` es el texto de su popup. Una fila con coordenadas vacías o no numéricas no tiene marcador.

```xml
<query id="TstMapPoints" service="TstMapPoints"/>
```

| id | Lat | Lon | Lug |
| --- | --- | --- | --- |
| 1 | 40.4168 | -3.7038 | Madrid |
| 2 | 41.3874 | 2.1686 | Barcelona |
| 3 | 39.4699 | -0.3763 | Valencia |
| 4 | 37.3891 | -5.9845 | Sevilla |

## Selección y dependencias {#selection}

Al hacer clic en un marcador se selecciona su fila (y se abre su popup). El `id` de la fila pasa a ser el valor seleccionado del mapa, de modo que el mapa se envía al servidor con el marcador seleccionado, y el marcador se dibuja con otro color. Las filas del mapa funcionan como las de una rejilla para las [dependencias](dependencies.md): el atributo `selectedRowValue` de un `dependency-element` es el valor de una columna del marcador seleccionado.

```xml
<criteria id="MapSel" label="Lugar seleccionado" component="text" readonly="true">
  <dependency source-type="formule" target-type="input" formule="'[place]'">
    <dependency-element id="fleet" column="Lug" attribute="selectedRowValue" alias="place"/>
  </dependency>
</criteria>
```

El mapa también puede tener sus propios hijos `dependency`, como los demás componentes.

## Dibujar una ruta {#route}

La [acción de cliente](actions.md) `polyline` dibuja una ruta sobre el mapa: los puntos van en el parámetro `rows`, con las columnas de latitud y longitud del mapa (`latitude` y `longitude` por defecto). Mientras se muestra la ruta los marcadores se ocultan y la vista se centra en el primer punto de la ruta (zoom `8`). Una ruta sin puntos elimina la ruta y los marcadores vuelven.

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

## Teselas y atribución {#tiles}

Las teselas proceden del servidor de OpenStreetMap (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`) salvo que establezca la propiedad [`awe.application.map.tile-url`](../properties.md#awe.application.map.tile-url) con la plantilla de URL de otro servidor de teselas, con los marcadores `{z}`, `{x}` e `{y}`:

```properties
awe.application.map.tile-url=https://tiles.example.com/{z}/{x}/{y}.png
```

> **Advertencia:** El servidor de teselas de OpenStreetMap es para uso ligero y tiene una [política de uso](https://operations.osmfoundation.org/policies/tiles/) que debe cumplir. Una aplicación con mucho tráfico debe usar su propio servidor de teselas o un proveedor comercial, porque la política permite a la Fundación OpenStreetMap bloquear el uso intensivo.

El mapa muestra la atribución "© OpenStreetMap contributors" con un enlace a la [página de copyright de OpenStreetMap](https://www.openstreetmap.org/copyright), como exige la licencia de los datos (ODbL), y el crédito de Leaflet. No las oculte. El zoom se limita a los niveles 3 a 19, los que ofrecen las teselas de OpenStreetMap.
