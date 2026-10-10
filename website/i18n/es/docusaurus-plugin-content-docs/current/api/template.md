---
id: template
title: Plantilla
---

Una plantilla es el marco de una pantalla. Decide dónde se dibujan el título de la pantalla, las migas de pan, los botones y el contenido. Una plantilla tiene uno o más **puntos de origen** (*source*), y el XML de la pantalla rellena cada uno con un
[`tag`](tags.md) que lo nombra en su atributo `source`.

La plantilla de una pantalla se elige con el atributo obligatorio `template` del elemento [`screen`](screen.md), y sus puntos de origen se rellenan con elementos `tag`:

```xml
<screen template="window" label="SCREEN_TITLE_QUE"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd">
  <tag source="hidden"></tag>
  <tag source="buttons">
    <button label="BUTTON_SEND" icon="exchange" id="ButSndSyn">
      <button-action type="server" server-action="maintain" target-action="TstQueSndSyn" />
    </button>
  </tag>
  <tag source="center">
    <window label="SCREEN_TEXT_CRITERIA">
      ...
    </window>
  </tag>
</screen>
```

El ejemplo es una copia recortada de la pantalla `QueTst` de la aplicación de pruebas de React (`awe-tests/awe-boot-react`). El origen `center` contiene la propia pantalla y el origen `buttons` los botones de la esquina superior derecha de la ventana.

> **Nota:** Las plantillas ya no son archivos JSP. Ambos clientes dibujan la pantalla a partir del XML: el cliente AngularJS usa plantillas generadas por el servidor (StringTemplate), y el cliente React usa componentes React.

## Puntos de origen de las plantillas estándar {#source-points-of-the-standard-templates}

AWE tiene tres plantillas estándar. Sus puntos de origen son los mismos en ambos motores:

| Plantilla  | Para                                                      | `center` | `buttons` | `modal` | `hidden` | Título y migas de pan |
| ---------- | --------------------------------------------------------- | :------: | :-------: | :-----: | :------: | :-------------------: |
| `full`     | Sin marco, solo los orígenes: página principal, barras de navegación, pantallas hechas para incluirse | Sí       | No        | Sí      | Sí       | No                    |
| `window`   | Pantallas que se abren desde el menú, dentro de la aplicación | Sí   | Sí        | Sí      | Sí       | Sí                    |
| `document` | Pantallas que muestran un documento, como el manual de la aplicación | Sí | Sí    | Sí      | Sí       | Solo motor AngularJS  |

Los puntos de origen son:

* `center`: el contenedor central de la plantilla. Aquí defines la estructura de la pantalla.
* `buttons`: los botones de la zona superior derecha de la pantalla. Solo en `window` y `document`.
* `modal`: el lugar para las ventanas modales. Coloca aquí los elementos [`dialog`](dialog.md) y los elementos
  [`include`](include.md) que traen diálogos de otras pantallas.
* `hidden`: un contenedor oculto para criterios y mensajes ocultos.

El `title` de la pantalla (atributo `label`) y las migas de pan los rellena la plantilla; no se definen con un origen.

Cada origen puede aparecer **una vez** por pantalla (el esquema lo comprueba), y el `tag` de un origen debe ser hijo directo de `screen`. Un tag con un origen que la plantilla no tiene (por ejemplo `buttons` en `full`) no se dibuja, y en el motor AngularJS un nombre de origen que la plantilla no declara hace fallar la pantalla. En el motor React el `style` de los tags `center`, `buttons` y `modal` se añade a la clase de su contenedor.

> **Nota:** Las pantallas genéricas propias de AWE usan otros valores (`base`, `home`, `login`, `message`) que no están definidos por las plantillas estándar de los clientes. No los uses en pantallas de aplicación: el cliente React muestra el mensaje `The template <name> has not been created yet.` para una plantilla que no conoce.

## Plantillas propias {#own-templates}

Si necesitas un marco distinto (por ejemplo una pantalla con un panel lateral), puedes añadir tu propia plantilla. Cómo hacerlo depende del motor:

* **Motor React.** Escribe un componente React que reciba la estructura de la pantalla (`elementList`) y dibuje los orígenes que necesite, y regístralo por nombre con `registerTemplate(name, component)`, exportado por el paquete `awe-react-client`.
  Después usa `template="[name]"` en tus pantallas. El registro falla si el nombre ya existe, por lo que no puedes reemplazar `full`, `window` ni `document`.
* **Motor AngularJS.** Añade un archivo de grupo StringTemplate en `templates/[module]/templates.stg` en tu aplicación, donde `[module]` es uno de los módulos de `awe.application.module-list` (consulta las [propiedades](../properties.md)). Una plantilla es una definición cuyos parámetros son los puntos de origen (`breadcrumbs`, `title`, `buttons`, `center`, `modal` y `hidden` en las estándar) y cuyo cuerpo los usa con `$center$`, `$modal$`...

Una pantalla que usa tu propia plantilla debe tener la plantilla en **ambos** motores si la aplicación se ejecuta en los dos.

## Páginas relacionadas {#related-pages}

* [Screen](screen.md): el atributo `template`.
* [Tags](tags.md): el atributo `source` que rellena un punto de origen.
* [Include](include.md): reutiliza el contenido de un origen de otra pantalla.
* [Layout](layout.md): cómo se expande el contenido de un origen.
* [Screens](screens.md): la lista de elementos de pantalla.

_Revisado para AWE 5._
