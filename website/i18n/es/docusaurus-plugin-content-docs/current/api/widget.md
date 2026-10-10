---
id: widget
title: Widgets
---

Un **widget** incrusta un componente JavaScript personalizado en una pantalla de AWE. Es el punto de extensión para enriquecer la interfaz que AWE ofrece de serie con tus propios componentes de framework (y sus estilos), sin tener que habilitarlos uno a uno en el framework.

Los widgets se declaran con el elemento `<widget>` dentro de cualquier contenedor de pantalla (un `tag`, `window`, etc.):

```xml
<tag source="center">
  <widget type="file-manager" id="file-manager" style="expand"/>
</tag>
```

## Cómo se resuelve {#how-resolution-works}

El atributo `type` es la **clave que resuelve el componente cliente**. El backend renderiza el widget como un elemento personalizado con el nombre derivado de `type`:

```
<widget type="event-calendar" id="myCalendar"/>
  →  <awe-event-calendar event-calendar-id="myCalendar"></awe-event-calendar>
```

- **Cliente Angular (`awe-client-angular`)**: el elemento se resuelve contra el registro de directivas de AngularJS. Una directiva llamada `aweEventCalendar` (elemento `awe-event-calendar`) renderiza el widget. Las aplicaciones registran sus propias directivas en su bundle, de modo que los widgets son extensibles a nivel de aplicación sin cambios en el núcleo del framework.
- **Cliente React (`awe-react-client`)**: el `type` se resuelve contra un registro de widgets del lado del cliente. Las aplicaciones registran sus propios componentes con `registerWidget(type, component)` (exportado por `awe-react-client`), de modo que los widgets son extensibles a nivel de aplicación sin cambios en el núcleo del framework. Consulta la [guía de widgets personalizados (React)](../guides/custom-widgets-react).
- Si ningún componente coincide con `type`, el cliente muestra un marcador de posición (`The widget <type> has not been created yet.`) en lugar de fallar.

Para crear y registrar tu propio widget, consulta la [guía de widgets personalizados](../guides/custom-widgets) (Angular) o la [guía de widgets personalizados (React)](../guides/custom-widgets-react).

## Widgets incluidos {#built-in-widgets}

El cliente Angular incluye de serie estos tipos de widget:

| `type`         | Descripción                              |
| -------------- | ---------------------------------------- |
| `file-manager` | Gestor / explorador de archivos          |
| `pdf-viewer`   | Visor de documentos PDF                  |
| `log-viewer`   | Visor de logs en tiempo real             |
| `help-viewer`  | Visor de contenido de ayuda              |
| `carousel`     | Carrusel de imágenes / contenido         |

### Cliente React {#react-client}

El cliente React (`awe-react-client`) resuelve el mismo `type` contra un registro del lado del cliente e incluye los mismos tipos de widget. Las aplicaciones añaden los suyos llamando una vez al arrancar a `registerWidget(type, component)` (exportado por `awe-react-client`); los widgets incluidos usan ese mismo mecanismo. Los atributos del nodo widget (`id`, `style`, ...) se pasan al componente como props. Consulta la [guía de widgets personalizados (React)](../guides/custom-widgets-react).

## Atributos {#attributes}

| Atributo        | Obligatorio | Descripción                                                                                       |
| --------------- | ----------- | ------------------------------------------------------------------------------------------------- |
| `id`            | Sí          | Identificador único del widget (se usa como id del componente en el cliente).                     |
| `type`          | No          | Clave del componente que selecciona el componente cliente (ver [resolución](#how-resolution-works)). |
| `component`     | No          | Nombre del componente Javascript (avanzado/heredado).                                             |
| `style`         | No          | Clases CSS aplicadas al contenedor del widget (p. ej. `expand` para ocupar el espacio disponible). |
| `visible`       | No          | Si el widget es visible (`true` / `false`).                                                       |
| `help`          | No          | Clave de locale del texto de ayuda del widget.                                                    |
| `help-image`    | No          | Ruta de la imagen de ayuda.                                                                       |
| `server-action` | No          | Acción de servidor para obtener los datos del widget.                                             |
| `target-action` | No          | Acción destino / consulta ejecutada para alimentar el widget.                                     |
| `initial-load`  | No          | Acción de carga inicial ejecutada al arrancar la pantalla.                                        |

## Parámetros del widget {#widget-parameters}

Usa `<widget-parameter>` para pasar valores de configuración al componente cliente. Los parámetros llegan al componente a través de su controlador (`component.controller.parameters` en el cliente Angular):

```xml
<widget type="event-calendar" id="myCalendar" style="expand">
  <widget-parameter type="string" name="initialView" value="dayGridMonth"/>
  <widget-parameter type="boolean" name="editable" value="true"/>
</widget>
```

| Atributo  | Obligatorio | Descripción                                                                                                    |
| --------- | ----------- | -------------------------------------------------------------------------------------------------------------- |
| `type`    | Sí          | Tipo del parámetro: `string`, `label`, `boolean`, `integer`, `long`, `float`, `double`, `array`, `object`, `null`. |
| `value`   | Sí          | Valor del parámetro (interpretado según `type`).                                                               |
| `name`    | No          | Nombre del parámetro (clave) tal como lo recibe el componente.                                                 |

Los parámetros de tipo `array` y `object` pueden anidar más elementos `<widget-parameter>`.
