---
id: dialog
title: Diálogo
---

Un diálogo es un elemento gráfico que proporciona la funcionalidad de mostrar una ventana modal con un mensaje y título seleccionados (y un icono, si se quiere). Un cuadro de diálogo debe estar asociado a una acción de botón con tipo "dialog" (Ver [acciones de botón](button.md#button-actions)).

---

## Esqueleto de XML {#xml-skeleton}

```xml 
<dialog id="[dialog-identifier]" label="[dialog-label]" icon="[dialog-icon]"
  modal="[is-modal-screen]" style="[dialog-style]" help="[dialog-help]" on-close="[on-close]">
  [define the content of the dialog]
</dialog>
```

## Estructura de diálogo {#dialog-structure}

| Elemento                        | Uso             | Varias instancias | Descripción                                               |
| ------------------------------- | --------------- | ----------------- | --------------------------------------------------------- |
| [dialog](#dialog-attributes) | **Obligatorio** | No                | Nodo global del diálogo. Define los atributos del diálogo |

## Atributos de diálogo {#dialog-attributes}

| Atributo | Uso      | Tipo   | Descripción                                            | Valores                                                                                               |
| -------- | -------- | ------ | ------------------------------------------------------ | ----------------------------------------------------------------------------------------------------- |
| id       | Opcional | String | Identificador del diálogo. Con fines de referencia     |                                                                                                       |
| label    | Opcional | String | Título del diálogo                                     | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                  |
| icon     | Opcional | String | Identificador de icono                                 | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md)                                      |
| style    | Opcional | String | Estilo de la pantalla (clases de Css)                  | Clases CSS separadas por espacio (`' '`)                                                              |
| help    | Opcional | String | Texto de ayuda que quieres mostrar                     | El nombre de un literal con el mensaje                                                                |
| on-close | Opcional | String | Comportamiento de la pila después de cerrar el diálogo | `accept` (por defecto), `reject`- Reject cancela la pila, accept continúa ejecutando acciones de pila |

> **Nota:** Puedes añadir los estilos `modal-lg`, `modal-md` o `modal-sm` para cambiar el ancho del diálogo.

## Ejemplos {#examples}

### Diálogo estándar con título, cuerpo y pie de página {#standard-dialog-with-title-body-and-footer}

Este es un ejemplo con un diálogo definido dentro de una etiqueta modal en una pantalla. En primer lugar, la acción relacionada con el diálogo (en el atributo target).

<img alt="Imagen de diálogo" src={require('@docusaurus/useBaseUrl').default('img/DialogImage.png')} />

```xml 

    <button id="SetContribution" button-type="submit" label="BUTTON_SET_CONTRIBUTION" icon="floppy-o">
      <button-action type="filter" target="Resume" />
      <button-action type="dialog" target="Summary" />
    </button>

    <dialog id="Summary" label="SCREEN_TEXT_SET_CONTRIBUTION" icon="info-circle">
      <tag type="div" style="modal-body scrollable">
        <tag-list type="div" id="Resume" target-action="Resume" autoload="true">
          <tag type="div" style="text-bg padding-sm">
            <tag type="i" style="fa fa-arrow-right text-info fa-fw" />
            <tag>
              <text> [Value]</text>
            </tag>
          </tag>
        </tag-list>
      </tag>
      <tag type="div" style="modal-footer">
        <tag type="div" style="pull-right">
          <button label="BUTTON_CANCEL" icon="close" id="ButDiaCan">
            <button-action type="close" target="Summary" />
          </button>
          <button label="BUTTON_ACCEPT" icon="check" id="ButDiaVal" button-type="submit">
            <button-action type="server" server-action="maintain" target-action="SetContribution" />
            <button-action type="close" target="Summary" />
          </button>
        </tag>
      </tag>
    </dialog>
```

### Diálogo estándar en una nueva pantalla {#standard-dialog-on-a-new-screen}

Este ejemplo muestra una pantalla de diálogo incluida en la sección modal de una ventana.

<img alt="Diálogo de impresión" src={require('@docusaurus/useBaseUrl').default('img/DialogImagePrint.png')} />

```xml

<button icon="print" label="BUTTON_PRINT" id="ButPrn" button-type="button">
  <button-action type="validate"/>
  <button-action type="dialog" target="PrnOpt"/>
</button>

<include target-screen="PrnOpt" target-source="center"/>

<screen xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd" template="window">
  <tag source="center">
    <dialog id="PrnOpt" modal="true" style="normal" label="SCREEN_TEXT_PRINT_EMAIL" icon="print" help="HELP_SCREEN_TEXT_PRINT_EMAIL">
      <tag type="div" style="modal-body row">
        [body content]
      </tag>
      <tag type="div" style="modal-footer">
        [footer content]
      </tag>
    </dialog>
  </tag>
</screen>
```
