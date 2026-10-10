---
id: custom-widgets-react
title: Widgets personalizados (React)
sidebar_label: Widgets personalizados (React)
---

AWE le permite ampliar la interfaz que proporciona con **sus propios componentes JavaScript** — incluidas bibliotecas de terceros y sus estilos — incrustándolos en una pantalla mediante el elemento [`<widget>`](../api/widget). En el cliente React, los widgets se resuelven por su `type` contra un **registro de widgets del lado cliente**, por lo que puede conectar lo que necesite a nivel de aplicación sin modificar el framework.

Esta guía recorre un ejemplo completo y funcional: un **calendario de eventos** (construido sobre la biblioteca de terceros [FullCalendar](https://fullcalendar.io/)) añadido a un proyecto AWE con React.

<img alt="Widget de calendario de eventos en React" src={require('@docusaurus/useBaseUrl').default('img/guides/event-calendar-widget-react.png')} />

## Cómo funciona {#how-it-works}

Cuando coloca un widget en una pantalla:

```xml
<widget type="event-calendar" id="eventCalendar" style="expand"/>
```

el cliente React busca el `type` del widget en su registro y renderiza el componente correspondiente:

- El paquete `awe-react-client` expone `registerWidget(type, component)` y `getWidget(type)`. El cliente resuelve cada nodo `<widget>` contra el registro por su `type`.
- Los widgets **integrados** (`file-manager`, `log-viewer`, `help-viewer`, `pdf-viewer`, `carousel`) se registran mediante este mismo mecanismo, por lo que un widget de aplicación no es un caso especial.
- Todos los atributos del nodo del widget (`id`, `style`, ...) se pasan a su componente como **props**.
- Si ningún componente coincide con `type`, el cliente renderiza un marcador de posición (`The widget <type> has not been created yet.`) en lugar de fallar.

Para registrar su propio componente, importe `registerWidget` desde el punto de entrada principal del cliente y llámelo una vez al arrancar la aplicación.

## Pasos {#steps}

### 1. Añadir la dependencia de la biblioteca {#1-add-the-library-dependency}

Instale la biblioteca de componentes en su proyecto (aquí, FullCalendar y su adaptador para React):

```bash
npm install --save @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/list @fullcalendar/interaction @fullcalendar/react
```

### 2. Escribir el componente del widget {#2-write-the-widget-component}

Cree el componente en `src/js/widgets/`, por ejemplo `src/js/widgets/EventCalendar.jsx`. Puntos clave:

- Es un componente React normal. AWE pasa los atributos del nodo del widget como props, así que extraiga `id` y `style` de ellas.
- Renderice su componente de terceros (aquí, `<FullCalendar>`) dentro de su propio contenedor.
- Siga el idioma de AWE mediante `react-i18next`: `useTranslation()` le proporciona `t` para las etiquetas e `i18n.language` para el código del idioma activo de AWE (p. ej. `en-GB`, `es-ES`).

```jsx
import React, { useState } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import listPlugin from "@fullcalendar/list";
import interactionPlugin from "@fullcalendar/interaction";
import esLocale from "@fullcalendar/core/locales/es";
import enGbLocale from "@fullcalendar/core/locales/en-gb";

// AWE language codes mapped to FullCalendar locales
const CALENDAR_LOCALES = {
  "es-ES": esLocale,
  "en-GB": enGbLocale
};

// Receives the widget node attributes (id, style, ...) as props
function EventCalendar({ id, style = "" }) {
  const { t, i18n } = useTranslation();
  const [events, setEvents] = useState([/* ... in-memory demo events ... */]);

  return (
    <div id={id} className={`awe-ec ${style}`}>
      <div className="awe-ec-mount">
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
          initialView="dayGridMonth"
          height="100%"
          locale={CALENDAR_LOCALES[i18n.language] ?? enGbLocale}
          events={events}
        />
      </div>
      {/* Composer to create/edit events (not a native <form> — see caution below) */}
    </div>
  );
}

EventCalendar.propTypes = {
  id: PropTypes.string.isRequired,
  style: PropTypes.string
};

export default EventCalendar;
```

:::tip Siga el idioma de AWE
Lea el idioma activo con `react-i18next`: `const { t, i18n } = useTranslation()`. Use `i18n.language` (códigos como `en-GB`, `es-ES`) para elegir el locale de la biblioteca de terceros, y `t("KEY")` para sus propias etiquetas, de modo que cambien con el idioma de AWE. No se base en el idioma del navegador.
:::

:::caution Sin `<form>` nativo, sin diálogos nativos
La pantalla de AWE ya envuelve su contenido en un `<form>`, y los formularios HTML no se pueden anidar — por lo que **no añada otro `<form>`** dentro de su widget. Gestione crear/editar/eliminar con botones y manejadores `onClick`. Del mismo modo, mantenga las interacciones en línea en lugar de usar `alert()` / `confirm()` / `prompt()`, y persista los datos mediante el `server-action` del widget en lugar de en memoria (los eventos en memoria de arriba son solo para la demostración).
:::

### 3. Registrar el componente {#3-register-the-component}

Registre el widget una vez al arrancar la aplicación en `src/js/main.js`, usando `registerWidget` desde el punto de entrada principal del cliente:

```js
const { registerWidget } = require('awe-react-client/js/main');
const { default: EventCalendar } = require('./widgets/EventCalendar');

// Custom widgets
registerWidget("event-calendar", EventCalendar);
```

El primer argumento (`"event-calendar"`) es el `type` usado en el XML de la pantalla; el segundo es su componente.

### 4. Añadir los estilos {#4-add-the-styles}

Añada los estilos de su widget en `src/css/main.css` (importado por `src/js/main.js`). FullCalendar v6 inyecta su propio CSS base en tiempo de ejecución, por lo que principalmente sobrescribe sus propiedades personalizadas de CSS (acotadas al contenedor de su widget) para integrarse con el tema activo de AWE:

```css
.awe-ec {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.awe-ec-mount {
  /* FullCalendar v6 custom properties */
  --fc-border-color: #e2e8ec;
  --fc-today-bg-color: rgba(86, 158, 27, 0.08);
  --fc-event-bg-color: #569e1b;
  --fc-event-border-color: #569e1b;
}
```

### 5. Añadir la pantalla {#5-add-the-screen}

Cree una pantalla que incruste el widget. El `type` coincide con la clave que registró:

```xml
<screen xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd"
        template="window" label="MENU_TEST_EVENT_CALENDAR">
  <tag source="center" type="div" expandible="vertical" style="expand">
    <widget type="event-calendar" id="eventCalendar" style="expand"/>
  </tag>
</screen>
```

### 6. Añadir la opción de menú y los locales {#6-add-the-menu-option-and-locales}

Registre una opción de menú que apunte a la pantalla y añada las claves de locale usadas por la pantalla y el widget:

```xml
<!-- menu/private.xml -->
<option name="event-calendar-test" label="MENU_TEST_EVENT_CALENDAR" screen="event-calendar-test" icon="calendar"/>
```

```xml
<!-- locale/Locale-en-GB.xml -->
<locale name="MENU_TEST_EVENT_CALENDAR" value="Event calendar"/>
<locale name="SCR_EVENT_CALENDAR_NEW" value="New event"/>
<locale name="SCR_EVENT_CALENDAR_TITLE" value="Event title"/>
```

## Notas {#notes}

- **`type` es la clave de extensión** — es una cadena libre, por lo que no está limitado a un conjunto fijo de componentes. Las restricciones de pantalla/perfil siguen siendo el lugar adecuado para controlar el acceso.
- **Los widgets integrados y los personalizados comparten el mismo registro** — `registerWidget` es exactamente cómo el cliente registra `file-manager`, `log-viewer` y el resto, por lo que no hace falta ningún cambio en el framework para añadir el suyo.
- **Pase configuración** al componente con [`<widget-parameter>`](../api/widget#widget-parameters); el nodo del widget (incluidos sus parámetros) llega a su componente como props.
- Para el modelo de resolución compartido con el cliente Angular, consulte la [referencia de la API de Widgets](../api/widget).
