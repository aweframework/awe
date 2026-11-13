---
id: custom-widgets
title: Widgets personalizados
sidebar_label: Widgets personalizados
---

AWE le permite ampliar la interfaz que proporciona con **sus propios componentes JavaScript** — incluidas bibliotecas de terceros y sus estilos — incrustándolos en una pantalla mediante el elemento [`<widget>`](../api/widget). Los widgets se resuelven por su `type`, por lo que puede conectar lo que necesite a nivel de aplicación sin modificar el framework.

Esta guía recorre un ejemplo completo y funcional: un **calendario de eventos** (construido sobre la biblioteca de terceros [FullCalendar](https://fullcalendar.io/)) añadido a la aplicación de demostración `awe-boot`.

<img alt="Widget de calendario de eventos en awe-boot" src={require('@docusaurus/useBaseUrl').default('img/guides/event-calendar-widget.png')} />

## Cómo funciona {#how-it-works}

Cuando coloca un widget en una pantalla:

```xml
<widget type="event-calendar" id="eventCalendar" style="expand"/>
```

el backend lo renderiza como un elemento personalizado con el nombre de `type`:

```
<awe-event-calendar event-calendar-id="eventCalendar"></awe-event-calendar>
```

El cliente AngularJS resuelve ese elemento contra su registro de directivas. Por tanto, todo lo que necesita es una directiva llamada `aweEventCalendar` registrada en el bundle de su aplicación — sin cambios en el núcleo del framework. Si ninguna directiva coincide con el `type`, el cliente muestra un marcador de posición en lugar de fallar.

## Pasos {#steps}

### 1. Añadir la dependencia de la biblioteca {#1-add-the-library-dependency}

Instale la biblioteca de componentes en su proyecto (aquí, FullCalendar):

```bash
npm install --save @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/interaction
```

### 2. Escribir la directiva del widget {#2-write-the-widget-directive}

Cree una directiva en `src/main/resources/js/directives/`. Puntos clave:

- Regístrela con `aweApplication.directive('aweYourWidget', ...)`. `aweApplication` es un **global** expuesto por el bundle principal de AWE, así que haga referencia a él directamente (no hace falta importarlo).
- El atributo del elemento `<type>-id` enlaza el identificador del componente; expóngalo como la propiedad del scope aislado `widgetId`.
- Intégrese con el ciclo de vida del framework mediante el servicio `Component`.

```js
import {Calendar} from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';

aweApplication.directive('aweEventCalendar',
  ['Component', '$timeout', 'AweSettings',
    function (Component, $timeout, $settings) {
      return {
        restrict: 'E',
        replace: true,
        scope: { widgetId: '@eventCalendarId' },
        template: '<div class="awe-ec"><div class="awe-ec-mount"></div></div>',
        link: function (scope, element) {
          // Integrate as a framework component
          const component = new Component(scope, scope.widgetId);
          if (!component.asComponent()) {
            return false;
          }

          // Mount the third-party component once the node is laid out
          $timeout(function () {
            const calendar = new Calendar(element[0].querySelector('.awe-ec-mount'), {
              plugins: [dayGridPlugin],
              initialView: 'dayGridMonth',
              locale: $settings.getLanguage(),   // follow the AWE language
              events: [/* ... */]
            });
            calendar.render();

            // Release resources with the widget
            scope.$on('$destroy', () => calendar.destroy());
          });
        }
      };
    }
  ]);
```

:::tip Siga el idioma de AWE
Lea el idioma actual con `AweSettings.getLanguage()` (devuelve códigos como `en-GB`, `es-ES`) y vuelva a localizar en tiempo de ejecución escuchando el evento del framework: `scope.$on('languageChanged', (e, lang) => calendar.setOption('locale', map(lang)))`. No se base en el idioma del navegador.
:::

:::caution Sin diálogos nativos
No use `alert()` / `confirm()` / `prompt()` dentro de un widget: bloquean el digest de AngularJS. Construya su propia interfaz en línea y envuelva en `scope.$applyAsync(...)` las llamadas de retorno de la biblioteca que modifiquen el scope.
:::

### 3. Registrar la directiva en su bundle {#3-register-the-directive-in-your-bundle}

Añada un `require` de su directiva en la entrada de webpack de la aplicación (`src/main/resources/webpack/app.config.js`) para que se incluya en el bundle:

```js
require("../js/directives/eventCalendar");
```

### 4. Añadir los estilos {#4-add-the-styles}

Ponga los estilos de su widget en `src/main/resources/less/` e impórtelos desde `main.less`:

```less
// main.less
@import "./awe/event-calendar.less";
```

Las bibliotecas de terceros suelen exponer propiedades personalizadas de CSS que puede sobrescribir, acotadas a su widget, para integrarse con el tema activo de AWE.

### 5. Añadir la pantalla {#5-add-the-screen}

Cree una pantalla que incruste el widget:

```xml
<screen template="full" label="MENU_TEST_EVENT_CALENDAR"
        xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
        xsi:noNamespaceSchemaLocation='https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd'>
  <tag source="center">
    <widget type="event-calendar" id="eventCalendar" style="expand"/>
  </tag>
</screen>
```

### 6. Añadir la opción de menú y los locales {#6-add-the-menu-option-and-locales}

Registre una opción de menú que apunte a la pantalla y añada las claves de locale usadas por la pantalla y el widget:

```xml
<!-- menu/private.xml -->
<option name="event-calendar" label="MENU_TEST_EVENT_CALENDAR" screen="event-calendar-test" icon="calendar"/>
```

```xml
<!-- locale/Locale-en-GB.xml -->
<locale name="MENU_TEST_EVENT_CALENDAR" value="Event Calendar"/>
```

## Notas {#notes}

- **`type` es la clave de extensión** — es una cadena libre, por lo que no está limitado a un conjunto fijo de componentes. Las restricciones de pantalla/perfil siguen siendo el lugar adecuado para controlar el acceso.
- **Pase configuración** al componente con [`<widget-parameter>`](../api/widget#widget-parameters); llega a la directiva a través de `component.controller.parameters`.
- **Cliente React**: el cliente `awe-react` actualmente resuelve los widgets contra un registro integrado; el registro de widgets personalizados a nivel de aplicación se gestiona como una mejora aparte. Esta guía está orientada al cliente AngularJS.
