---
id: adr-001-react-modern-client
title: "ADR 001: React es el cliente moderno de AWE 5"
sidebar_label: "ADR 001: cliente moderno React"
---

| Campo | Valor |
| --- | --- |
| Estado | Aceptada |
| Fecha | 2026-10-27 |
| Alcance | Clientes web de AWE 5.x (`awe-client-react`, `awe-client-angular`) |
| Issue | #777 |

## Contexto {#context}

AWE tiene dos motores web que renderizan el mismo contrato XML de pantallas:

- **AngularJS** (`awe-client-angular`): el cliente original. El soporte a largo plazo de AngularJS terminó el 31 de
  diciembre de 2021, así que lleva años sin soporte de seguridad de sus autores.
- **React** (`awe-client-react`): nació como un proyecto aparte, el paquete npm `awe-react-client` 2.x que usaba el
  arquetipo React de AWE 4.

AWE 5 necesita un cliente web moderno que forme parte del framework de pleno derecho, y una indicación clara de qué
cliente deben usar los proyectos nuevos. Muchos productos construidos con AWE, incluidos los productos propios de Almis,
usan hoy el cliente AngularJS, y deben poder pasar a AWE 5 sin reescribir su frontend.

## Decisión {#decision}

1. **React es el cliente moderno de AWE 5** y el cliente recomendado para los proyectos nuevos.
2. **AngularJS sigue soportado en modo compatibilidad durante toda la línea 5.x.** El modo compatibilidad significa:
   - Los dos motores implementan el mismo contrato XML de pantallas. Una función que se añade al contrato (un componente,
     un atributo, una acción, una dependencia o un evento) funciona en los dos motores, y una pantalla se comporta igual
     en ambos.
   - El cliente AngularJS recibe correcciones de errores, correcciones de seguridad y actualizaciones de dependencias
     (CVE) en todas las versiones 5.x.
   - El trabajo que no cambia el contrato de pantallas, como la adopción de TypeScript, las refactorizaciones de la
     gestión de estado o el ajuste de rendimiento del motor, se hace solo en el cliente React.
3. **Pasar un proyecto a React es una decisión del proyecto, no de la versión.** Actualizar a AWE 5 no obliga a cambiar
   de cliente: un proyecto AWE 4 con AngularJS se actualiza a AWE 5 y mantiene AngularJS.
4. **El módulo mantiene su nombre**, `awe-client-react`. Coincide con el paquete npm `awe-react-client` y con la imagen
   `awe-boot-react`, y renombrarlo cambiaría coordenadas publicadas sin ningún beneficio.

Los dos motores siguen en AWE 5, y el framework no descarta más motores en el futuro: esta decisión nombra el cliente
recomendado, no sustituye un motor por otro.

## Por qué React {#why-react}

- **El motor ya existía.** El cliente React ya estaba publicado y se usaba con el arquetipo React de AWE 4. Elegir Angular
  moderno habría supuesto escribir un tercer motor desde cero.
- **Ahora forma parte del framework.** Desde #794 el motor React vive en el repositorio de AWE. Se construye y se prueba
  en el mismo pipeline que el servidor, con las mismas suites de navegador de Playwright y Selenium que el motor AngularJS,
  y se publica con la versión del framework como paquete npm `awe-react-client` y como imagen `awe-boot-react` (vea
  [Líneas de versiones y soporte](../guides/release-lines-and-support.md)).
- **El equipo.** El equipo de AWE ya construye y mantiene el motor React, así que el cliente moderno es el que el equipo
  puede hacer evolucionar más rápido.
- **El arquetipo.** `awe-boot-react-archetype` ya genera una aplicación React, así que los proyectos nuevos pueden empezar
  hoy con el cliente moderno.
- **Una base de diseño común.** Los dos motores usan los mismos tokens de diseño (#770) y la misma base de Tailwind con la
  capa de compatibilidad de Bootstrap 3 (#755), así que una pantalla se ve igual en ambos. Vea el
  [sistema de diseño](../design-system/overview.md).

## Consecuencias {#consequences}

- Los proyectos nuevos deben empezar con `awe-boot-react-archetype`. Vea [Primeros pasos](../getting-started.md).
- Los proyectos AWE 4 no tienen que cambiar de cliente al actualizar. Vea la
  [guía de migración a AWE 5](../guides/v5-migration.md) y, para los proyectos que ya usan el cliente React 2.x,
  [Actualización del cliente React](../guides/react-client-upgrade.md).
- Una merge request que cambia el contrato de pantallas debe cubrir los dos motores, o enlazar el issue que lleva el
  cambio al otro motor.
- Las suites de navegador siguen ejecutándose contra las dos aplicaciones (AngularJS y React) durante toda la línea 5.x.

## Lista de paridad {#parity-checklist}

La paridad se mide contra el cliente AngularJS: componentes, atributos, acciones, dependencias y eventos. Este es el
estado de cada issue de paridad de React del plan de AWE 5 a 2026-10-27. Como el resto de este ADR, la lista es un
registro fechado y no se actualiza después: cada issue mantiene su propio estado actual.

### Hecho {#done}

| Issue | Alcance | Merge requests |
| --- | --- | --- |
| #794 | Motor React movido al repositorio del framework, construido, probado y publicado con AWE 5 | !764, !778, !805, !853, !858, !859, !867, !1000 |
| #798 | Componente de mapa en el motor React (AngularJS: #882, vea [Huecos pendientes](#remaining-gaps)) | !1071, !1072, !1073, !1104 |
| #799 | Maximizar y restaurar paneles | !1090, !1111 |
| #800 | Columna de texto con formato y HTML saneado | !1057 |
| #801 | Menú contextual en criterios, botones, pestañas y asistentes | !1092, !1093, !1094, !1095, !1096 |
| #803 | Registro central de atajos de teclado | !1055, !1056, !1070, !1168 |
| #805 | ComponentRegistry y división de los deltas de Redux | !1069, !1108, !1132, !1156, !1158 |
| #806 | Sin renderizados innecesarios desde los selectores | !940 |
| #807 | Redux Toolkit 2 y react-redux 9 | !1069, !1089 |
| #808 | Contrato servidor-cliente tipado y adopción de TypeScript | !1083, !1106, !1109, !1123, !1160, !1162, !1169, !1171 |
| #809 | componentsReducer dividido por familia de componentes | !1108, !1127, !1128, !1129, !1130, !1131, !1155 |
| #810 | Registro explícito de acciones y test de paridad con AngularJS | !938 |
| #811 | Navegación del router disponible para los thunks desde el arranque | !938 |
| #817 | Acciones de cliente de imprimir y redimensionar | !938 |
| #818 | Paridad de dependencias: condiciones `contains`, eventos que faltaban y destinos específicos | !1064, !1065, !1066, !1080, !1081 |
| #819 | Eventos de gráficos y gráficos enlazados por zoom | !939 |
| #820 | Formatos de fecha y opciones de criterios del contrato de pantallas | !1018 |
| #821 | Atributos de grid, treegrid, pivot-table y file-viewer | !1068, !1107, !1118, !1124, !1133, !1134, !1159 |
| #825 | Celdas de grid que saltan de línea y filas que adaptan su altura | !1024 |
| #847 | Paridad de resizable, tag y window | !888, !921, !1172 |
| #868 | Disposición y gráficos de la pantalla de auditoría, select diferido en diálogo | !1067, !1084 |
| #871 | Enter en criterios de fecha, hora y rango de fechas envía el formulario | !1173 |
| #872 | Enter en criterios detiene la propagación y evita la acción por defecto | !1168 |
| #887 | Alturas de grid en diálogos y paneles flexibles, disposición del rango de fechas | !1161 |
| #888 | Mover filas de grid arrastrando y soltando | !1165 |
| #889 | El submenú expandido marca la pantalla abierta, no su primera opción | !1163 |
| #891 | Reducers y thunks de Redux en TypeScript | !1171 |
| #892 | Disposición dividida redimensionable de las dependencias de grid | !1172 |

### En curso {#in-progress}

| Issue | Alcance | Estado |
| --- | --- | --- |
| #880 | Un clic en una celda de grid lanza todas las dependencias de clic de columna, y `server-download` falla con una dirección de celda | En corrección |

### Huecos pendientes {#remaining-gaps}

Se encontraron al cerrar la lista. Tienen su propio issue y no bloquean esta decisión.

| Hueco | Propuesta |
| --- | --- |
| El componente `<map>` solo existe en el motor React (#798); en AngularJS un `<map>` no muestra nada. | Implementarlo en AngularJS como se describe en #882, como exige el modo compatibilidad. |
| Una opción de menú con un `server-action` distinto de `screen` bloquea la cola de acciones en los dos motores (#879). | Corregirlo en el generador del servidor, como propone #879; no es específico de React. |
| Los arquetipos no indican cuál es el recomendado: las dos descripciones de Maven solo nombran su motor. | Marcar `awe-boot-react-archetype` como el arquetipo recomendado para proyectos nuevos en su descripción (#897). |
