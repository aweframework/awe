---
id: popover
title: Popover
---

Un popover es un [diálogo](dialog.md) anclado a un control de la pantalla: se abre junto a ese control (debajo por
defecto, o encima, a su izquierda o a su derecha), con una cabecera, un cuerpo y un pie opcional. Úselo para contenido
breve ligado a un control (una edición rápida, unas pocas opciones, notas) sin tapar la pantalla. Se abre y se cierra
exactamente como un diálogo: con una acción de botón de tipo `dialog` (ver [acciones de botón](button.md#button-actions))
y con las acciones `close` y `close-cancel`. Lo soportan los dos motores web (AngularJS y React).

<img alt="Popover" src={require('@docusaurus/useBaseUrl').default('img/popover.png')} />

---

## Esqueleto de XML {#xml-skeleton}

```xml
<popover id="[popover-identifier]" label="[popover-label]" icon="[popover-icon]" style="[popover-style]"
         anchor="[control-identifier]" position="[bottom|top|left|right]" max-width="[css-length]"
         on-close="[on-close]" deferred="[deferred]">
  [define the content of the popover]
  <tag type="div" style="modal-footer">
    [footer buttons]
  </tag>
</popover>
```

## Estructura del popover {#popover-structure}

| Elemento                       | Uso             | Múltiples instancias | Descripción                                            |
|--------------------------------|-----------------|----------------------|--------------------------------------------------------|
| [popover](#popover-attributes) | **Obligatorio** | No                   | Nodo global del popover. Define los atributos del popover |

## Atributos del popover {#popover-attributes}

| Atributo  | Uso          | Tipo    | Descripción                                                                                                                    | Valores                                                                                          |
|-----------|--------------|---------|--------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| id        | Opcional     | String  | Identificador del popover. Es el destino de las acciones `dialog`, `close` y `close-cancel`. Es único entre los diálogos, paneles laterales y popovers de la pantalla |                                     |
| anchor    | **Obligatorio** | String | Identificador del control de la pantalla al que se ancla el popover (un botón, un criterio, una rejilla...)                  |                                                                                                  |
| position  | Opcional     | String  | Lado del ancla en el que se abre el popover                                                                                    | `bottom` (por defecto), `top`, `left`, `right`                                                   |
| max-width | Opcional     | String  | Ancho máximo del popover, que toma el ancho de su contenido hasta este                                                         | Una longitud CSS, por ejemplo `30rem`, `320px` o `30vw`. Por defecto `20rem`                     |
| label     | Opcional     | String  | Título de la cabecera                                                                                                          | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)                  |
| icon      | Opcional     | String  | Icono de la cabecera (motor AngularJS)                                                                                         | **Nota:** Puede consultar todos los juegos de iconos en la pantalla de [iconos](icons.md)        |
| style     | Opcional     | String  | Estilo del popover (clases css)                                                                                                | Clases CSS separadas por espacio (`' '`)                                                         |
| on-close  | Opcional     | String  | Comportamiento de la pila al cerrar el popover con su botón de cierre, con Esc o con un clic fuera                             | `accept` (por defecto), `reject`. Reject cancela la pila, accept continúa ejecutando sus acciones |
| deferred  | Opcional     | Boolean | Carga los datos de los componentes del popover la primera vez que se abre, no con la pantalla. Ver [carga diferida](dialog.md#deferred-loading) | `true`, `false` (por defecto)                                                  |

## Apertura y cierre {#opening-and-closing}

El popover se abre con una acción `dialog` cuyo destino es el popover, igual que un diálogo. Normalmente el control que
lo abre es su ancla:

```xml
<button id="ButPopBottom" label="BUTTON_POPOVER_BOTTOM" icon="arrow-down">
  <button-action type="dialog" target="PopBottom"/>
</button>
```

Se abre en el lado de su ancla indicado en `position` y la sigue cuando la página se desplaza o la ventana cambia de
tamaño. Cuando el ancla no está en la pantalla, se abre en el centro de la ventana.

Se cierra:

- con una acción `close` (las acciones que siguen a la acción `dialog` continúan) o `close-cancel` (se cancelan),
  normalmente en los botones de su pie;
- con el botón de cierre de su cabecera, con Esc o con un clic fuera de él. Un clic en su ancla también lo cierra, y no
  lo vuelve a abrir. Estos siguen el atributo `on-close`: `accept` (por defecto) se comporta como `close` y `reject` como
  `close-cancel`.

Un clic en las listas y calendarios que se abren desde los controles del popover no es un clic fuera de él.

## Pie {#footer}

Ponga los botones del popover en una etiqueta hija con el estilo `modal-footer`.

## Contenido {#content}

El popover toma el ancho de su contenido, hasta `max-width`. Una rejilla dentro de él necesita un ancho y un alto: dele el
estilo `col-xs-12` y un estilo de alto (`height-md`, `height-lg`...), como en el [ejemplo](#example).

## Foco {#focus}

Mientras el popover está abierto, el foco del teclado se queda dentro: Tab desde su último control vuelve al primero y
Shift+Tab desde el primero va al último. Al cerrarse el popover, el foco vuelve al control que lo abrió.

## Ejemplo {#example}

Un popover diferido debajo de su botón con dos criterios, una rejilla y un pie con Cancelar y Aceptar (la lista y la
rejilla cargan sus datos la primera vez que se abre), y un popover diferido a la derecha de su botón, de hasta 30rem de
ancho, que rechaza la pila cuando se descarta:

```xml
<tag source="center">
  <window style="expand" label="SCREEN_TITLE_POPOVER" expandible="vertical">
    <tag type="div" style="panel-body">
      <button id="ButPopBottom" label="BUTTON_POPOVER_BOTTOM" icon="arrow-down">
        <button-action type="dialog" target="PopBottom"/>
      </button>
      <button id="ButPopRight" label="BUTTON_POPOVER_RIGHT" icon="arrow-right">
        <button-action type="dialog" target="PopRight"/>
      </button>
    </tag>
    <tag type="div" style="panel-body">
      <criteria id="CrtPopResult" label="PARAMETER_POPOVER_RESULT" component="text" readonly="true"
                style="col-xs-12 col-sm-6"/>
    </tag>
  </window>

  <popover id="PopBottom" label="POPOVER_TITLE_BOTTOM" anchor="ButPopBottom" max-width="40rem" deferred="true">
    <tag type="div" style="panel-body">
      <criteria id="CrtPopName" label="PARAMETER_NAME" component="text" style="col-xs-12 col-sm-6"/>
      <criteria id="CrtPopUser" label="PARAMETER_USER" component="select" initial-load="query"
                target-action="TestComponentInitialLoadQuery" optional="true" style="col-xs-12 col-sm-6"/>
      <grid id="GrdPopThemes" style="col-xs-12 height-lg" initial-load="query" server-action="data"
            target-action="ThemeStyleTst" max="5">
        <column label="PARAMETER_THEME" name="name" align="left" charlength="20"/>
        <column label="PARAMETER_POPOVER_THEME_ID" name="id" align="right" charlength="10"/>
      </grid>
    </tag>
    <tag type="div" style="modal-footer">
      <button id="ButPopBottomCancel" label="BUTTON_CANCEL" icon="times">
        <button-action type="close-cancel" target="PopBottom"/>
      </button>
      <button id="ButPopBottomAccept" label="BUTTON_ACCEPT" icon="check">
        <button-action type="close" target="PopBottom"/>
        <button-action type="value" target="CrtPopResult" value="accepted"/>
      </button>
    </tag>
  </popover>

  <popover id="PopRight" label="POPOVER_TITLE_RIGHT" anchor="ButPopRight" position="right" max-width="30rem"
           on-close="reject" deferred="true">
    <tag type="div" style="panel-body">
      <criteria id="CrtPopNotes" label="PARAMETER_DESCRIPTION" component="textarea" style="col-xs-12"/>
    </tag>
    <tag type="div" style="modal-footer">
      <button id="ButPopRightClose" label="BUTTON_CLOSE" icon="times">
        <button-action type="close" target="PopRight"/>
      </button>
    </tag>
  </popover>
</tag>
```

Con el [módulo builder](../builder-module.md), `PopoverBuilder` construye un `<popover>`:

```java
new PopoverBuilder()
  .setAnchor("ButPopRight")
  .setPosition(PopoverPosition.RIGHT)
  .setMaxWidth("30rem")
  .setOnClose(OnClose.REJECT)
  .setDeferred(true)
  .setId("PopRight")
  .setLabel("POPOVER_TITLE_RIGHT");
```
