---
id: dialog
title: Diálogo
---

Un diálogo es un elemento gráfico que proporciona la funcionalidad de mostrar una ventana modal con un mensaje y título seleccionados (y un icono, si se quiere). Un cuadro de diálogo debe estar asociado a una acción de botón con tipo "dialog" (Ver [acciones de botón](button.md#button-actions)).

---

## Esqueleto de XML {#xml-skeleton}

```xml 
<dialog id="[dialog-identifier]" label="[dialog-label]" icon="[dialog-icon]"
  modal="[is-modal-screen]" style="[dialog-style]" help="[dialog-help]" on-close="[on-close]" deferred="[deferred]">
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
| deferred | Opcional | Booleano | Carga los datos de los componentes del diálogo la primera vez que se abre, no con la pantalla. Ver [carga diferida](#deferred-loading) | `true`, `false` (por defecto)                                                                         |

> **Nota:** Puedes añadir los estilos `modal-lg`, `modal-md` o `modal-sm` para cambiar el ancho del diálogo.

## Carga diferida {#deferred-loading}

Por defecto, el servidor ejecuta las consultas `initial-load` de todos los componentes de la pantalla cuando la genera,
incluidos los componentes de un diálogo que quizá nunca se abra. Con `deferred="true"` los datos de los componentes del
diálogo se cargan la primera vez que se abre el diálogo, de modo que una pantalla con diálogos pesados carga más rápido y
envía una respuesta más pequeña. Sin el atributo no cambia nada. Los clientes AngularJS y React se comportan igual.

```xml
<dialog id="Summary" label="SCREEN_TEXT_SUMMARY" deferred="true">
  <tag type="div" style="modal-body">
    <criteria id="Customer" component="select" initial-load="query" target-action="CustomerList" label="PARAMETER_CUSTOMER"/>
    <grid id="Orders" initial-load="query" target-action="OrderList" max="20">
      ...
    </grid>
  </tag>
</dialog>
```

**Qué se difiere.** El `initial-load` de cada componente dentro del diálogo, a cualquier profundidad (criterios, grids,
gráficas, pestañas, etc.). Las listas de opciones de las columnas de un grid (una columna `select` con su propio
`initial-load`) forman parte de la definición del grid: se siguen cargando con la pantalla.

**Cuándo se carga.** En la primera apertura, el cliente envía una [acción de servidor](actions.md#server-actions)
`deferred-data` silenciosa con el identificador del diálogo. El servidor ejecuta las consultas de los componentes del
diálogo y responde con una acción de cliente [`initial-model`](actions.md#component-actions) para cada uno, que aplica los
datos como lo hace la carga de la pantalla: el modelo cargado es también al que vuelven las acciones `restore` y
`restore-target`. El diálogo no se vuelve a cargar al abrirlo de nuevo. Un componente con `autoload="true"` dentro del
diálogo también espera a la primera apertura.

Las consultas reciben los valores que tiene la pantalla **en el momento de la primera apertura**, no los que tenía cuando
se cargó.

**Antes de la primera apertura.** Los componentes del diálogo están en la pantalla, con su controlador, pero vacíos (sin
filas y sin valor seleccionado). Esto es lo que ve cada parte de la pantalla:

| Qué                                                     | Comportamiento antes de la primera apertura                                                                                                                                                                                                                                               |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Valores enviados por las acciones (`server`, `validate`, impresión) | Los componentes del diálogo se envían vacíos, como cualquier criterio o grid vacío. Una consulta o un maintain que dependa de uno de ellos debe esperarlo vacío hasta que se abra el diálogo.                                                                      |
| Validación                                              | Los criterios de un diálogo que nunca se ha abierto se **ignoran** en todas las reglas de validación (`required` y el resto), en las acciones `validate`, completas o [con destino](#validation-of-deferred-containers): puede que el usuario ni siquiera pueda abrirlo. Una vez abierto y cargado, validan como siempre. |
| Acciones que dan datos a un componente (`filter`, `server`) | Los datos se conservan. En la primera apertura no se vuelven a cargar los componentes que ya tienen filas o un valor seleccionado, así que el diálogo nunca sobrescribe lo que hizo antes una acción o una dependencia. Un selector al que solo se le puso el valor sigue cargando sus opciones. |
| Dependencias que tienen como destino un componente del diálogo | Funcionan desde la carga de la pantalla, sobre el modelo vacío, y el valor que establecen se conserva cuando se carga el diálogo.                                                                                                                                 |
| Dependencias cuyo origen es un componente del diálogo   | Se evalúan con el modelo vacío y otra vez cuando llegan los datos del diálogo (igual que tras la carga de una pantalla).                                                                                                                                                         |
| Diálogos dentro de un diálogo diferido                  | Un diálogo o una página de pestaña diferidos dentro de un diálogo diferido esperan a su propia apertura. Un diálogo que no es diferido se carga con el exterior.                                                                                                                  |

> **Nota:** Si fallan las consultas del diálogo, el error se muestra como mensaje al abrir el diálogo, no con la pantalla.

### Validación de contenedores diferidos {#validation-of-deferred-containers}

Un diálogo o una página de pestaña diferidos que nunca se han abierto no se validan: la acción `validate` omite sus
criterios y grids, sea cual sea la regla (`required`, longitud, formato...). Puede que un usuario no tenga permiso para
abrir el diálogo (por ejemplo, el botón que lo abre está restringido por perfil), así que la validación nunca bloquea la
pantalla por campos a los que el usuario no puede llegar. Lo mismo ocurre con los botones de guardado que validan antes
de lanzar la acción de servidor.

- Un `validate` completo omite todos los contenedores diferidos que aún no se han abierto.
- Un `validate` con destino (`<button-action type="validate" target="Panel"/>`) sobre un contenedor que tiene un diálogo
  o una pestaña diferidos sin abrir omite esa parte y valida el resto.
- Un `validate` con destino que es en sí mismo un contenedor diferido sin abrir no valida nada y pasa.
- Cuando el contenedor se ha abierto y cargado, sus criterios validan como siempre, en validaciones completas y con destino.

El servidor no comprueba `required` para estos criterios (es una regla del cliente), así que un guardado no falla porque
falten. Tenlo en cuenta si una consulta o un maintain necesita un valor del diálogo: lo recibe vacío hasta que se abra.

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
