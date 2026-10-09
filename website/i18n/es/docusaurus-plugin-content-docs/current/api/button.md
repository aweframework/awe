---
id: button
title: Botón
---

Un botón es un elemento de control gráfico que proporciona al usuario una forma simple de lanzar un evento, como realizar una consulta, o interactuar con cuadros de diálogo, como confirmar una acción.

Al hacer clic en un botón de AWE se lanza la lista de [acciones](actions.md) definidas ([acciones de botón](#button-actions)) que son llamadas secuencialmente.

<img alt="Botón" src={require('@docusaurus/useBaseUrl').default('img/Boton.png')} />

---

<img alt="Botones" src={require('@docusaurus/useBaseUrl').default('img/Botones.png')} />


Cuando el tamaño de la pantalla es demasiado pequeño (dispositivos móviles) los botones definidos en el **source** `buttons` de la pantalla son **movidos al borde inferior**:

<img alt="Botones en formato móvil" src={require('@docusaurus/useBaseUrl').default('img/Botones_movil.png')} />

## Esqueleto de XML {#xml-skeleton}

```xml 
<button id="[button-identifier]" button-type="[button-type]" label="[button-label]" icon="[button-icon]" 
        style="[button-style]" size="[button-size]">
  <button-action... />
  <dependency... />
</button>
```

## Estructura del botón {#button-structure}

| Elemento                            | Uso             | Varias instancias | Descripción                                                  |
| ----------------------------------- | --------------- | ----------------- | ------------------------------------------------------------ |
| [button](#button-attributes)      | **Obligatorio** | No                | Elemento principal del botón. Define los atributos del botón |
| [button-action](#button-actions) | Opcional        | Si                | Lista de acciones que se lanzan al pulsar el botón           |
| [dependency](dependencies.md)       | Opcional        | Si                | Lista de dependencias definidas en el botón                  |

## Atributos del botón {#button-attributes}

| Atributo        | Uso      | Tipo   | Descripción                                                                         | Valores                                                                             |
| --------------- | -------- | ------ | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| id              | Opcional | String | Identificador del botón. Con fines de referencia                                    |                                                                                     |
| label           | Opcional | String | Texto del botón                                                                     | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                |
| style           | Opcional | String | Clases CSS del botón                                                                |                                                                                     |
| icon            | Opcional | String | Identificador de icono                                                              | **Note:** You can check all iconsets at [icons](icons.md) screen                    |
| button-type     | Opcional | String | Comportamiento por defecto del botón                                                | `button` (por defecto), `submit` o `reset`. Ver [tipos de botones](#button-types) |
| size            | Opcional | String | Tamaño del criterio                                                                 | `sm` (por defecto), `md` o `lg`.                                                    |
| value           | Opcional | String | Establece un valor para el botón (puede ser utilizado como variable en el servidor) |                                                                                     |
| help-text       | Opcional | String | Texto para mostrar en el botón como ayuda                                           | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                |
| imagen de ayuda | Opcional | String | URL de imagen para mostrar en el botón como ayuda                                   | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                |

## Tipos de botón {#button-types}

| Tipo     | Descripción                                                                      | Imagen                                                                                                          |
| -------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `button` | Botón estándar. No hace nada si no hay acciones de botón adjuntas                | <img alt="Botón" src={require('@docusaurus/useBaseUrl').default('img/Button.png')} />          |
| `submit` | Botón de enviar. Se llama cuando un usuario presiona INTRO dentro de un criterio | <img alt="Enviar" src={require('@docusaurus/useBaseUrl').default('img/Submit.png')} />         |
| `reset`  | Botón de reiniciar. Si se hace clic lanza una acción de restablecimiento         | <img alt="Restaurar datos" src={require('@docusaurus/useBaseUrl').default('img/Reset.png')} /> |

## Eventos del botón {#button-events}

| Evento  | Descripción                                      |
| ------- | ------------------------------------------------ |
| `click` | Se lanza cuando un usuario hace clic en el botón |

## Acciones de botón {#button-actions}

Dentro de un botón, puede definir una lista de [acciones](actions.md) que se ejecutarán cuando el usuario *haga clic en* el botón. El orden en que se definen las acciones de los botones es el mismo en el que serán lanzadas.

Consulte la sección de [acciones](actions.md) para obtener más detalles.

## Ejemplos {#examples}

### Botón estándar con acciones y dependencias (borrar fila en tabla) {#standard-button-with-actions-and-dependencies-delete-row-in-grid}

El botón está desactivado hasta que se seleccione al menos un elemento en la tabla.

```xml 
<button label="BUTTON_DELETE" icon="trash" id="ButDel">
  <button-action type="check-some-selected" target="Grd..."/>
  <button-action type="confirm" target="DelMsg" />
  <button-action type="server"  server-action="maintain" target-action="...Del"/>
  <button-action type="filter"  target="Grd..."/>
  <dependency target-type="disable" initial="true">
    <dependency-element id="Grd..." condition="&lt;" value="1"/>
  </dependency>
</button>
```

### Botón que lanza un evento `click` {#button-launched-on-click-event}

El mismo caso que antes, pero activado con una dependencia (y tal vez con otras condiciones de elementos)

```xml 
<button label="BUTTON_DELETE" icon="trash" id="ButDel">
  <dependency target-type="disable" initial="true">
    <dependency-element id="Grd..." condition="&lt;" value="1"/>
  </dependency>
  <dependency initial="true">
    <dependency-element id="ButDel" event="click"/>
    <dependency-element .../>
    <dependency-action type="check-some-selected" target="Grd..."/>
    <dependency-action type="confirm" target="DelMsg" />
    <dependency-action type="server"  server-action="maintain" target-action="...Del"/>
    <dependency-action type="filter"  target="Grd..."/>
  </dependency>
</button>
```
