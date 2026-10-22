---
id: tab-and-tabcontainer
title: Pestañas y contenedor de pestaña
---

Una **pestaña** es un componente muy útil para dividir los componentes de la pantalla en *pestañas*:

<img alt="Pestaña" src={require('@docusaurus/useBaseUrl').default('img/Tab.png')} />

## Esqueleto de XML {#xml-skeleton}

```xml 
<tab id="[tab-identifier]" initial-load="[initial-load]" target-action="[target-action]" maximize="[maximize-tab]">
  <tabcontainer id="[tabcontainer-identifier-1]">
  ...
  <tabcontainer id="[tabcontainer-identifier-n]">
  <dependency...></dependency>
  <context-menu...></context-menu>
</tab>
```

La lista de pestañas se rellena con los campos `value` y `label` de la consulta o enumerado lanzado con `[target-action]`.

## Estructura de pestañas {#tab-structure}

```xml
<tab id="[tab-identifier]" initial-load="[initial-load]" target-action="[target-action]">
   ...
</tab>
```

| Elemento                                 |       Uso       | Varias instancias | Descripción                                                |
| ---------------------------------------- |:---------------:|:-----------------:| ---------------------------------------------------------- |
| [tab](#tab-attributes)                   | **Obligatorio** |        No         | Nodo global de pestaña. Define los atributos de la pestaña |
| [tabcontainer](#tabcontainer-attributes) | **Obligatorio** |        Si         | Lista de contenedores de pestañas a mostrar                |
| [dependency](dependencies.md)            |    Opcional     |        Si         | Lista de dependencias de la pestaña                        |
| [context-menu](context-menu.md)          |    Opcional     |        No         | Menú contextual de la pestaña                              |

## Atributos de pestaña {#tab-attributes}

| Atributo      |       Uso       |  Tipo   | Descripción                                                                                     | Valores                                                                                             |
| ------------- |:---------------:|:-------:| ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| id            | **Obligatorio** | String  | Identificador de pestañas. Debe coincidir con los valores de target-action |                                                                                                     |
| initial-load  | **Obligatorio** | String  | Llamada de acción al servidor para cargar los datos de criterio (se lanza al cargar la ventana) | `enum` (para [enumerado](enumerate-definition.md)) o `query` (para [consulta](query-definition.md)) |
| target-action | **Obligatorio** | String  | Destino al que llamar en el servidor                                                            |                                                                                                     |
| style         |    Opcional     | String  | Clases CSS de la pestaña                                                                        | **Nota:** Aquí puedes usar la clase `expand` para permitir que la ventana se expanda                |
| maximize      |    Opcional     | Boolean | Mostrar el icono maximizar o no                                                                 | **Nota:** Funciona en ambos motores como una ventana: la lista de pestañas y su contenido ocupan el área de contenido (el menú y la barra superior siguen visibles) y el botón Restaurar los devuelve a su tamaño; no hay atajo con la tecla Escape |
| orientation   |    Opcional     | String  | Orientación de los pasos del asistente                                                          | El valor por defecto es `vertical`                                                                  |


## Estructura del contenedor de pestaña {#tabcontainer-structure}

Un contenedor de pestaña (tabcontainer) es una ventana que se abre al seleccionar una pestaña. Tenga en cuenta que `[tabcontainer-identifier]` debe coincidir con el valor de la consulta o lista enumerada definida en el elemento `Tab`.

```xml
<tabcontainer id="[tabcontainer-identifier]" type="[type]" label="[label]" style="[style]" expandible="[expandible]" deferred="[deferred]">
  ...
</tabcontainer>
```

## Atributos del contenedor de pestaña {#tabcontainer-attributes}

| Atributo   |       Uso       |  Tipo  | Descripción                                                                                        | Valores                                                                              |
| ---------- |:---------------:|:------:| -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| id         | **Obligatorio** | String | Identificador del contenedor. Debe coincidir con los valores de target-action |                                                                                      |
| label      |    Opcional     | String | Título del contenedor                                                                              | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                 |
| style      |    Opcional     | String | Clases CSS del contenedor                                                                          | **Nota:** Aquí puedes usar la clase `expand` para permitir que la ventana se expanda |
| type       |    Opcional     | String | Tipo de etiqueta HTML del contenedor                                                               | `div`, `span`, `p`, ...                                                              |
| expandible |    Opcional     | String | Cómo [expandir](layout.md) los elementos del contenedor                                            | `vertical`, `horizontal`                                                             |
| deferred   |    Opcional     | Booleano | Carga los datos de los componentes del contenedor la primera vez que se selecciona la pestaña, no con la pantalla. Ver [carga diferida](#deferred-loading) | `true`, `false` (por defecto) |

## Carga diferida {#deferred-loading}

Por defecto, el servidor ejecuta las consultas `initial-load` de los componentes de todos los contenedores de pestaña
cuando genera la pantalla, incluidas las pestañas que no se muestran. Con `deferred="true"` en un contenedor de pestaña,
los datos de sus componentes se cargan la primera vez que se selecciona la pestaña, de modo que una pantalla con muchas
pestañas o con pestañas pesadas carga más rápido. Sin el atributo no cambia nada. Los clientes AngularJS y React se
comportan igual.

```xml
<tab id="Customer" initial-load="enum" target-action="CustomerTabs">
  <tabcontainer id="Summary">
    ...
  </tabcontainer>
  <tabcontainer id="Orders" deferred="true">
    <grid id="OrderGrid" initial-load="query" target-action="OrderList" max="20">
      ...
    </grid>
  </tabcontainer>
</tab>
```

Las reglas son las de los [diálogos diferidos](dialog.md#deferred-loading), aplicadas a la pestaña: los componentes del
contenedor de pestaña están en la pantalla pero vacíos hasta que se selecciona la pestaña por primera vez; entonces una
acción de servidor `deferred-data` silenciosa los carga (con los valores que tiene la pantalla en ese momento) y la
pestaña no se vuelve a cargar, salvo si falló una consulta: se muestra el error y la siguiente selección vuelve a
cargarla. Algunos detalles de las pestañas:

- La pestaña seleccionada al mostrar la pantalla también se carga por su cuenta (justo después de la pantalla), porque se ve de inmediato. Difiere primero las páginas que el usuario no ve.
- En el primer contenedor de pestaña el atributo no aporta ventaja; úsalo en las demás páginas.
- Un contenedor de pestaña diferido dentro de otro contenedor diferido espera a su propia selección.
- Un contenedor de pestaña diferido que nunca se ha seleccionado **no se valida**: sus criterios se ignoran en todas las reglas de validación, también en validaciones con destino. Ver [validación de contenedores diferidos](dialog.md#validation-of-deferred-containers).
- El `initial-load` de la propia `tab` (la lista de páginas) nunca se difiere.
