---
id: menu
title: Definición del menú
sidebar_label: Definición del menú
---

El menú de navegación para una aplicación AWE se define en archivos XML. Los dos archivos XML del menú son:
* **public.xml:** Contiene las pantallas públicas (aquellas a las que se puede acceder sin iniciar sesión en el sistema).
* **private.xml:** Pantallas dentro de la protección de una sesión del sistema

:::tip Every element and attribute of menus is listed in the generated [XSD reference](/reference/menu). :::

:::**Nota:** Todos los descriptores XML del menú están definidos en la **carpeta de menú**. Ver la [estructura de proyecto](../guides/project-structure.md#menu-folder) para más información. :::

## Estructura del menú {#menu-structure}

Los archivos de menú deben tener la siguiente estructura:

```xml
<menu screen="[initial-screen]" context="[default-context]" default-action="[default-action]">
</menu>
```

| Atributo       | Uso             | Tipo   | Descripción                                                                                  | Valores                    |
| -------------- | --------------- | ------ | -------------------------------------------------------------------------------------------- | -------------------------- |
| screen         | **Obligatorio** | String | Es la pantalla por defecto que se mostrará al principio                                      |                            |
| context        | Opcional        | String | Es el contexto donde se lanzarán todas las opciones (si no se define un contexto específico) | Ver [contextos](#contexts) |
| default-action | Opcional        | String | Es la acción a la que llamarán todas las opciones (si no se define una acción específica)    |                            |

Hay una nueva etiqueta también para definir la posición, y el tipo de menú dentro de una pantalla: menu-container:

```xml
<menu-container type="[orientation]"/>
```

Donde [orientation] puede ser vertical u horizontal. Otra forma de cambiar el tipo de menú es seleccionando la pantalla de menú inicial en el archivo private.xml:

```xml
<menu screen="HomHor"…
```

Para menú horizontal

```xml
<menu screen="HomVer"…
```

Para menú vertical

### Contextos {#contexts}

El atributo [context](#menu-structure) define **dónde** se van a lanzar las opciones definidas dentro del menú. Hay varios contextos definidos que pueden ser usados dependiendo del tipo de menú:

| Ruta del contexto     | Menú    | Descripción                                                                                                                                                                                                                                                                                                                                                                                              |
| --------------------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `screen`              | public  | Útil para una sola página de inicio de sesión sin menú                                                                                                                                                                                                                                                                                                                                                   |
| `screen/public/home`  | public  | Este contexto se utiliza cuando la página principal debe tener un menú. En este caso, el parámetro `screen.configuration.home` no debe **** estar vacío, ya que esta será la pantalla mostrada dentro del menú. La pantalla inicial del contenedor de menú será la definida en el atributo de la pantalla de menú                                                                                        |
| `screen/private/home` | private | Este contexto se utilizará cuando se registre en la aplicación. En este caso el parámetro `screen.configuration.information` no debe **** estar vacío, ya que esta será la pantalla mostrada dentro del menú privado (cuando el usuario y el perfil no tienen ninguna definición inicial de pantalla). La pantalla inicial del contenedor de menú será la definida en el atributo de la pantalla de menú |

## Estructura de opciones {#options-structure}

Esta es la estructura de código de un conjunto de opciones:

```xml
<option module="[module]" name="[Option name]" label="[Option label] icon="[Icon option]">
  <option name="[Option name]" label="[Option label]" screen="[Screen name]" menu-screen="[Is menu screen]"/>
  ---
</option>
---
<option/>
```

Dentro de la opción de herramientas hay un conjunto de opciones, algunas de ellas son "invisibles". Es necesario definir todas las pantallas a las que puede acceder el usuario, porque si una pantalla no está definida en el menú no será accesible para ningún usuario. Además, puedes añadir opciones dentro de las opciones, creando un menú de varios niveles.

| Atributo    | Uso             | Tipo    | Descripción                                                                                                                                                                                                                   | Valores                                                                                                                    |
| ----------- | --------------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| name        | **Obligatorio** | String  | Es el nombre de la opción, que se utiliza en los archivos de perfil y en la tabla AweScrRes para restringir el acceso a una opción. Si una opción padre está restringida, todas las opciones hijas también están restringidas |                                                                                                                            |
| screen      | Opcional        | String  | Define la pantalla a la que se accederá cuando el usuario haga clic en la opción                                                                                                                                              |                                                                                                                            |
| label       | Opcional        | String  | Es un literal que contiene el nombre de la opción                                                                                                                                                                             | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                       |
| module      | Opcional        | String  | Define el nombre del módulo. Se pueden definir varias opciones de menú por cada módulo                                                                                                                                        | Estos nombres de módulos deben configurarse en la tabla AweMod y debe haber definida una variable de sesión para el módulo |
| separator   | Opcional        | Boolean | Si se establece en true, la opción se convierte en una línea separadora                                                                                                                                                       | El valor predeterminado es `false`                                                                                         |
| icon        | Opcional        | String  | Define un icono que se mostrará antes del texto de la opción                                                                                                                                                                  | Puedes ver toda la lista de iconos [aquí](http://fortawesome.github.io/Font-Awesome/icons/)                                |
| menu-screen | Opcional        | Boolean | La opción es una pantalla autogenerada con sub opciones                                                                                                                                                                       | Ver [ventana de opciones](#menu-screen) para más información                                                               |
| expanded    | Opcional        | Boolean | If set to true, the option will be initially expanded (only for vertical menus)                                                                                                                                               | El valor predeterminado es `falso`                                                                                         |

## Ventana de opciones {#menu-screen}

La **ventana de opciones** es un nuevo tipo de pantalla autogenerada que contiene una lista de botones con las opciones definidas dentro de la opción de tipo `menu-screen`. La visibilidad de estos botones se puede gestionar con las pantallas de configuración de acceso a opciones de menú.

Este es un ejemplo de ventana de opciones:

```xml 
<option name="button-menu" label="MENU_TEST_BUTTON_MENU" icon="th" menu-screen="true" screen="test-button-screen">
  <option name="button-option-0" label="MENU_TEST_CRITERIA" screen="CrtTst" icon="flask"/>
  <option name="button-group-1" label="MENU_TEST_BUTTON_MENU_GROUP_1" icon="keyboard-o">
    <option name="button-option-1" label="MENU_TEST_CRITERIA" screen="CrtTst" icon="keyboard-o"/>
    <option name="button-option-2" label="MENU_TEST_CRITERIA" screen="CrtTstLeft" icon="align-right"/>
    <option name="button-option-3" label="MENU_TEST_CRITERIA_RESET" screen="RstTst" icon="refresh"/>
    <option name="button-option-31" label="MENU_TEST_CRITERIA_RESET" screen="RstTst" icon="refresh"/>
  </option>
  <option name="button-group-2" label="MENU_TEST_BUTTON_MENU_GROUP_2" icon="file-text-o">
    <option name="button-option-4" label="MENU_TEST_CRITERIA" screen="CrtTst" icon="keyboard-o"/>
    <option name="button-option-5" label="MENU_TEST_CRITERIA" screen="CrtTstLeft" icon="align-right"/>
    <option name="button-option-6" label="MENU_TEST_CRITERIA_RESET" screen="RstTst" icon="refresh"/>
    <option name="button-option-7" label="MENU_TEST_CRITERIA" screen="CrtTst" icon="table"/>
    <option name="button-option-8" label="MENU_TEST_CRITERIA" screen="CrtTstLeft" icon="align-right"/>
    <option name="button-option-9" label="MENU_TEST_CRITERIA_RESET" screen="RstTst" icon="sitemap"/>
  </option>
  <option name="button-group-3" label="MENU_TEST_BUTTON_MENU_GROUP_3">
    <option name="button-option-10" label="MENU_TEST_CRITERIA" screen="CrtTst" icon="keyboard-o"/>
    <option name="button-option-11" label="MENU_TEST_CRITERIA" screen="CrtTstLeft" icon="align-right"/>
    <option name="button-option-12" label="MENU_TEST_CRITERIA_RESET" screen="RstTst" icon="refresh"/>
    <option name="button-option-13" label="MENU_TEST_CRITERIA_RESET" screen="RstTst" icon="refresh"/>
    <option name="button-option-14" label="MENU_TEST_CRITERIA_RESET" screen="RstTst" icon="refresh"/>
  </option>
</option>
```

Lo que genera una pantalla como:

<img alt="Ventana de opciones" src={require('@docusaurus/useBaseUrl').default('img/MenuScreen.png')} />

Cada botón enlaza a la opción correspondiente definida dentro de la opción de tipo `menu-screen`. También hay un nivel de agrupación (sólo uno) que genera una cabecera para agrupar algunas opciones dentro.

## Ejemplos {#examples}

```xml
<menu screen="home_horizontal" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
xsi:noNamespaceSchemaLocation = "../../sch/menu.xsd" context="screen/home" default-action="screen">
  <option name="information" screen="info" invisible="true" />
  <option module="Inf" name="information_restricted" screen="info" invisible="true" />
  <option module="Inf Changed" name="information_restricted_changed" screen="info" invisible="true" />
  <option name="tools" label="MENU_TOOLS" icon="wrench" expanded="true">    
    <option name="themes" label="MENU_TOOLS_THEMES" screen="Thm" icon="picture-o"/>
    <option name="new_theme" screen="ThmNew" invisible="true" />
    <option name="update_theme" screen="ThmUpd" invisible="true" />
    <option name="view_theme" screen="ThmViw" invisible="true" />
    <option name="security" label="MENU_TOOLS_SECURITY" icon="unlock-alt">
      <option name="screen_access" label="MENU_TOOLS_SCREENS_ACCESS" screen="ScrAccRes" icon="eye-slash"/>
      <option name="encrypt_tools" label="MENU_TOOLS_SCREEN_ENCRYPT" screen="ScrEncTxt" icon="lock"/>
    </option>    
  </option>
  <option name="HlpSep" separator="true"/>
  <option name="help" label="MENU_HELP" icon="question-circle">
    <option name="user_help" label="MENU_USER_HELP" screen="Hlp" icon="question"/>
    <option name="application_help" label="MENU_HELP_APPLICATION" screen="AppHlp" icon="question"/>
    <option name="application_info" label="MENU_APP_INF" screen="ViwAppInf" icon="info"/>
  </option>
</menu>
```
## Favourites **(NEW)**

Favourites is a brand new functionality added since 4.7.1 version of AWE. This functionality allows the user to mark any subpage (on report view) as favourite.

<img alt="engine" src={require('@docusaurus/useBaseUrl').default('img/favourites/favourites1.png')} />

To use this functionality, simply add this piece of code into your navbar:

```xml
<include target-screen="info-buttons" target-source="favourites"/>
```

This will add a **star** button on the top navbar which will appear while the user navigates through the screens.

<img alt="engine" src={require('@docusaurus/useBaseUrl').default('img/favourites/favourites2.png')} />

This button will be filled up when the current screen is marked as favourite. To mark/unmark a screen as favourite simply click on this button.

<img alt="engine" src={require('@docusaurus/useBaseUrl').default('img/favourites/favourites3.png')} />

When a screen is marked as favourite, it will appear on a new menu option called `Favourites` on the first position on the menu:

<img alt="engine" src={require('@docusaurus/useBaseUrl').default('img/favourites/favourites4.png')} />

To remove it from the `Favourites` option, simply navigate to the screen and unmark it on the **star** button.

## Menu option search **(NEW)**

The menu includes a **search box** to quickly find any option in large or deeply nested menus. A search icon is shown on the opposite edge to where the options expand: at the **top** of a vertical menu and on the **right** of a horizontal menu.

Clicking the icon reveals a text input. As you type, the menu shows a **flat list of the matching options** together with their breadcrumb path (`Parent › Child › Option`), so you can locate an option without navigating the tree. Selecting a result launches that option exactly as if it had been clicked in the menu.

Matching is case- and accent-insensitive, token based (every typed word must match), and runs against the **translated** option labels as well as the breadcrumb path. Use the arrow keys to move through the results, `Enter` to open the highlighted one and `Escape` (or a click outside) to close the panel.

The search is enabled by default. To hide it, set the [`awe.application.menu-search-enabled`](../properties.md#awe.application.menu-search-enabled) property to `false`:

```properties
awe.application.menu-search-enabled=false
```
