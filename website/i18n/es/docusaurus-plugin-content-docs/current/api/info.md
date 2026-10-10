---
id: info
title: Barra de navegación
---

La barra de navegación se utiliza para añadir elementos visuales con funciones lógicas en la zona superior de la aplicación. Esta parte se muestra en todas las pantallas de la aplicación.

Puedes añadir elementos `info` para mostrar más información y elementos `info-button` como botones con acciones. Además, puedes poner elementos `info-criteria` dentro de los elementos info como criterios para mostrar más información.

También puedes usar el componente `avatar` para mostrar la imagen del usuario y, por supuesto, usarlo como menú desplegable.

<img alt="NavBar" src={require('@docusaurus/useBaseUrl').default('img/NavBar.png')} />

## Estructura XML {#xml-structure}

La estructura XML del componente de barra de navegación es la siguiente:

```xml
<tag type="div" id="main-navbar-collapse" style="collapse navbar-collapse main-navbar-collapse">
  <tag type="div" style="right clearfix">
    <tag type="ul" style="nav navbar-nav pull-right right-navbar-nav">
      <info id="[id]" icon="[icon]" title="[info-title]">
        <info-criteria id="[id]" icon="[icon]" title="[info-criteria-title]" component="[component]" initial-load="[initial-load]" 
          target-action="[target-action]" session="module" style="[style]" info-style="[info-style]">
          <dependency/>
        </info-criteria>
        ... more info-criteria elements
      </info>
      ... more info elements
      <info-button id="[id]" icon="[icon]" title="[button-title]">
        <button-action type="[action-type]" />
        ... more button.action
      </info-button>
      ... more info-button
    </tag>
  </tag>
</tag>
```

## Estructura de la barra de navegación {#navbar-structure}

| Elemento                                   | Uso        | Varias instancias   | Descripción                                                                                                                         |
|--------------------------------------------|------------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| [info](#info-attributes)                   | Opcional   | Si                  | Elemento gráfico para mostrar información                                                                                           |
| [info-button](#info-button-attributes)     | Opcional   | Si                  | Botón de la barra de navegación para ejecutar una tarea. Igual que el elemento button en AWE                                        |
| [info-criteria](#info-criteria-attributes) | Opcional   | Si                  | Criterio de la barra de navegación para obtener datos del usuario y enviarlos al servidor de la aplicación (lógica de negocio). Igual que el elemento criteria en AWE |
| [avatar](#avatar-attributes)               | Opcional   | Si                  | Componente de la barra de navegación que muestra la imagen del usuario. Puede usarse como botón o como menú desplegable de la barra  |


### Atributos de info {#info-attributes}

| Atributo         | Uso          | Tipo   | Descripción                                                        | Valores                                                                    |
|------------------|--------------|--------|--------------------------------------------------------------------|----------------------------------------------------------------------------|
| `id`             | **Obligatorio** | String | Identificador del elemento info. Con fines de referencia        |                                                                            |
| `field`          | Opcional     | String | Nombre del atributo del modelo del que obtener el valor de target-action | Ej.: `field="label"` Obtiene el atributo label del resultado de datos de target-action |
| `title`          | Opcional     | String | Texto estático de info que se muestra al pasar el ratón por encima | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) |
| `label`          | Opcional     | String | Texto de info (fuera del criterio)                                 | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) | 
| `style`          | Opcional     | String | Clases CSS de info                                                 |                                                                            | 
| `dropdown-style` | Opcional     | String | Clases CSS de la caja desplegable de info                          |                                                                            | 
| `icon`           | Opcional     | String | Identificador del icono                                            | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla de [iconos](icons.md) | 
| `unit`           | Opcional     | String | Unidad de info. Úsala con el icono de info para mostrar un número en estilo apilado |                                                           | 
| `session`        | Opcional     | String | Variable de sesión desde la que cargar el criterio                 | Identificador de la variable de sesión                                     | 
| `value`          | Opcional     | String | Valor por defecto de info                                          |                                                                            | 
| `property`       | Opcional     | String | Variable de propiedad desde la que cargar el criterio              | Identificador de la variable de propiedad                                  |  
| `server-action`  | Opcional     | String | Llamada a acción de servidor                                       | Ver [lista de acciones de servidor](actions.md#server-actions)             |  
| `target-action`  | Opcional     | String | Destino al que llamar en el servidor                               |                                                                            |  



### Atributos de info-button {#info-button-attributes}

| Atributo     | Uso      | Tipo   | Descripción                        | Valores                                                                    |
|--------------|----------|--------|------------------------------------|----------------------------------------------------------------------------|
| `title`      | Opcional | String | Texto estático que se muestra al pasar el ratón por encima del botón | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) |
| `info-style` | Opcional | String | Estilo CSS del contenedor de info-button |                                                                      |

> **Nota:** el elemento `info-button` tiene otros atributos como el elemento `button`. Puedes ver más información [aquí](button.md#button-attributes).

### Atributos de info-criteria {#info-criteria-attributes}

| Atributo     | Uso      | Tipo   | Descripción                          | Valores                                                                    |
|--------------|----------|--------|--------------------------------------|----------------------------------------------------------------------------|
| `title`      | Opcional | String | Texto estático que se muestra al pasar el ratón por encima del criterio | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) |
| `info-style` | Opcional | String | Estilo CSS del contenedor de info-criteria | Ej.: `info-style="form-group"`                                       |

> **Nota:** el elemento `info-criteria` tiene los mismos atributos que el elemento `criteria`. Puedes ver más información [aquí](criteria.md#criteria-structure).

### Atributos de avatar {#avatar-attributes}

| Atributo     | Uso      | Tipo    | Descripción                                             | Valores                                                                    |
|--------------|----------|---------|---------------------------------------------------------|----------------------------------------------------------------------------|
| `title`      | Opcional | String  | Texto estático que se muestra al pasar el ratón por encima del elemento | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) |
| `show-label` | Opcional | Boolean | Si se muestra o no la etiqueta junto a la imagen del usuario | El valor por defecto es `true`                                       |
| `image`      | Opcional | String  | Ruta relativa o absoluta de la imagen a usar como avatar |                                                                           |

> **Nota:** el elemento avatar tiene los mismos atributos que el elemento `button`. Puedes ver más información [aquí](button.md#button-attributes).


## Ejemplos {#examples}

- Barra de navegación con algunos elementos info y un info-button

<img alt="Info-button" src={require('@docusaurus/useBaseUrl').default('img/Info-button.png')} />

```xml
<tag type="div" id="main-navbar-collapse" style="collapse navbar-collapse main-navbar-collapse">
  <tag type="div" style="right clearfix">
   <tag type="ul" style="nav navbar-nav pull-right right-navbar-nav">            
     <info id="ButUsrAct" icon="user" field="Val" server-action="data" target-action="ConUsr"/>
      <info-button id="ButLogOut" icon="sign-out" title="BUTTON_LOGOUT">
        <button-action type="logout" />
      </info-button>
    </tag>
  </tag>
</tag>
```

- Barra de navegación con un info con elementos info-criteria

<img alt="Info-criteria" src={require('@docusaurus/useBaseUrl').default('img/Info-criteria.png')} />

```xml
<tag type="div" id="main-navbar-collapse" style="collapse navbar-collapse main-navbar-collapse">
  <tag type="div" style="right clearfix">
   <tag type="ul" style="nav navbar-nav pull-right right-navbar-nav">
     <info id="ButUsrAct" icon="user" field="Val" server-action="data" target-action="ConUsr">
       <info-criteria icon="image" title="PARAMETER_THEME" component="select" id="theme" initial-load="query" 
           target-action="ThmSelVal" session="theme" style="col-xs-12 no-label" info-style="form-group">
         <dependency>
           <dependency-element id="theme" />
           <dependency-action type="server" server-action="maintain-silent" target-action="SesVarThm" silent="true" />
           <dependency-action type="change-theme" target="theme" />
         </dependency>
       </info-criteria>
       <info-criteria icon="language" title="PARAMETER_LANGUAGE" component="select" id="language" initial-load="enum" 
          target-action="Lan" session="language" style="col-xs-12 no-label" info-style="form-group">
        <dependency>
          <dependency-element id="language" />
          <dependency-action type="server" server-action="maintain-silent" target-action="SesVarLan" silent="true" />
          <dependency-action type="change-language" target="language" />
        </dependency>
       </info-criteria>
      </info>
    </tag>
  </tag>
</tag>
```

- Info con unidad y dependencias

<img alt="Info_unit" src={require('@docusaurus/useBaseUrl').default('img/Info_unit.png')} />

```xml
<info id="Dab" icon="database" unit="3" style="nav-icon-btn-success">
  <dependency source-type="criteria-text" target-type="label" target-action="database" initial="true">
   <dependency-element id="database"/>
  </dependency>
</info>
```

- Elemento avatar con menú desplegable

<img alt="Avatar" src={require('@docusaurus/useBaseUrl').default('img/avatar.png')} />

```xml
<tag type="div" id="main-navbar-collapse" style="collapse navbar-collapse main-navbar-collapse">
  <tag type="div" style="right clearfix">
    <tag type="ul" style="nav navbar-nav pull-right right-navbar-nav">
      <avatar id="ButUsrAct" image="/images/logo/logo-awe-nuevo.svg" icon="user" field="Val" server-action="data" target-action="ConUsr"/>
      <info-button id="ButLogOut" icon="sign-out" title="BUTTON_LOGOUT">
        <button-action type="logout" />
      </info-button>
    </tag>
  </tag>
</tag>
```
