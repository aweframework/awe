---
id: react-client-upgrade
title: Actualización del cliente React
sidebar_label: Actualización del cliente React
---

Esta guía está dirigida a las aplicaciones que dependen del paquete npm `awe-react-client` en su línea 2.x (por ejemplo,
un proyecto generado con el arquetipo de React de AWE 4) y pasan al cliente publicado con AWE 5.

## Qué ha cambiado en el paquete {#what-changed-in-the-package}

El cliente React ahora vive en el repositorio de AWE (`awe-framework/awe-client-react`) y se publica junto con el
framework:

- **La versión del paquete es la versión del framework.** AWE `5.0.0` publica `awe-react-client@5.0.0`, AWE `5.1.0`
  publica `awe-react-client@5.1.0`, y así sucesivamente. Antes, el cliente tenía su propia línea de versiones (`2.x`)
  sin relación con la versión del framework.
- **Se publica desde las etiquetas de publicación de AWE 5 y posteriores** (`v5.*`), con una declaración de
  [procedencia de npm](https://docs.npmjs.com/generating-provenance-statements) que vincula cada versión con la
  pipeline y el commit que la construyó. Las versiones preliminares (`-rc`, `-beta`...) se publican bajo el dist-tag
  `next`, las versiones finales bajo `latest`. Consulte [Líneas de versiones y soporte](release-lines-and-support.md#npm-package).
- **Use la versión que coincide con su versión de AWE.** El cliente y el servidor de una aplicación se publican y
  prueban juntos. El cliente 5.x dibuja los gráficos a partir de un modelo que solo envía el servidor de AWE 5 (véase
  más abajo), así que no lo mezcle con un servidor de AWE 4.

## Pasos de actualización {#upgrade-steps}

1. Actualice la versión de AWE de su aplicación (la propiedad `awe.version` o la versión de `awe-starter-parent` en el
   `pom.xml`) a AWE 5.
2. Establezca el cliente en el `package.json` de la aplicación a la misma versión, exactamente:

   ```json
   "dependencies": {
     "awe-react-client": "5.0.0"
   }
   ```

   El arquetipo de React de AWE 5 genera esta dependencia a partir de su propia versión, por lo que un proyecto nuevo
   empieza alineado. Fije la versión exacta (o actualice el framework y el cliente juntos): un rango como `^5.0.0`
   puede instalar un cliente más nuevo que su servidor.
3. Reinstale las dependencias y confirme el nuevo archivo de bloqueo. El bloqueo de un proyecto 2.x resuelve
   `awe-react-client` a una versión 2.x, y `npm ci` falla cuando no coincide con `package.json`, así que ejecute
   `npm install` una vez:

   ```bash
   npm install
   ```

   Si su proyecto se generó a partir del arquetipo de AWE 5, todavía no hay archivo de bloqueo: el primer `npm install`
   lo crea, y confirmarlo hace reproducibles las siguientes compilaciones.
4. Compile la aplicación y revise los puntos siguientes.

## Qué revisar {#what-to-review}

### Los gráficos usan Apache ECharts {#charts-use-apache-echarts}

El motor React dibuja los gráficos con [Apache ECharts](https://echarts.apache.org/) en lugar de Highcharts. El XML del
gráfico no cambia: el servidor de AWE 5 lo traduce a un modelo de ECharts y el cliente dibuja ese modelo. Un servidor
que no envía el modelo (un servidor de AWE 4) produce gráficos vacíos y un aviso en la consola del navegador.

Las dependencias `highcharts` y `highcharts-react-official` han desaparecido del cliente y se ha añadido `echarts`
6.1.0. Si su aplicación tiene componentes personalizados que importan Highcharts, declárelo en su propio
`package.json` (compruebe su licencia) o pase a ECharts. La lista completa de diferencias en los gráficos (paleta,
temas, gráficos 3D, formatos, impresión, reglas CSS) está en [Actualización a AWE 5](../api/chart.md#upgrading-to-awe-5).

### El HTML de las celdas se sanitiza {#html-in-cells-is-sanitized}

El texto de una celda de rejilla (y la etiqueta de un `tag`) puede llevar HTML, porque las transformaciones `TEXT_HTML`
y `MARKDOWN_HTML` de una consulta lo generan. El cliente React ya no inserta ese HTML en la página tal como llega: lo
sanitiza con [DOMPurify](https://github.com/cure53/DOMPurify) y una lista de permitidos estricta.

- **Etiquetas conservadas**: `a`, `abbr`, `b`, `blockquote`, `br`, `code`, `del`, `em`, `h1` a `h6`, `hr`, `i`, `ins`,
  `li`, `mark`, `ol`, `p`, `pre`, `s`, `small`, `span`, `strong`, `sub`, `sup`, `u` y `ul`.
- **Atributos conservados**: `class`, `href`, `target` y `title`. Un enlace con `target` obtiene
  `rel="noopener noreferrer"`.
- **Eliminado**: scripts, marcos, imágenes, formularios, estilos, `style` en línea, atributos `data-*`, manejadores de
  eventos (`onclick`, `onerror`...) y enlaces a URL `javascript:`. El texto dentro de una etiqueta eliminada se
  conserva.

Las etiquetas de un `tag` siguen las mismas reglas, con la misma lista: ninguna de las etiquetas y locales del
framework, sus módulos y sus aplicaciones de prueba lleva marcado que la lista elimine (contienen texto, y algunas usan
saltos de línea o caracteres de tipo Markdown, que permanecen como texto). Si una etiqueta de su aplicación usa un
`style` en línea, sustitúyalo por una clase CSS (`class` se conserva).

Un valor que el servidor escapó (`TEXT_HTML` escapa `<` y `>`) se muestra como texto, etiquetas incluidas. Si una celda
de su aplicación mostraba una imagen, un estilo en línea u otra etiqueta que no está en la lista, use en su lugar la
columna que la renderiza (por ejemplo una columna de imagen) o una clase CSS.

### Dependencias del cliente {#dependencies-of-the-client}

El cliente instala sus propias dependencias, y varias de ellas se actualizaron desde `awe-react-client` 2.2.5, entre
ellas `react-router` (de 7.13.2 a 7.18.2), `i18next-http-backend` (de 2.6.1 a 4.0.2, una versión mayor), `primereact`
(de 10.9.8 a 10.9.9) y `@reduxjs/toolkit` (de 1.9.5 a 1.9.7). Si su aplicación declara alguno de estos paquetes por sí
misma, alinee las versiones con las del cliente para evitar dos copias en el bundle. Las versiones se listan en el
`package.json` del paquete publicado, y las dependencias de ejecución publicadas también se listan en el SBOM CycloneDX
conservado como artefacto del job `Publish npm` de cada etiqueta de publicación (consulte
[Líneas de versiones y soporte](release-lines-and-support.md#npm-package)).

### Otros cambios {#other-changes}

Entre `awe-react-client` 2.2.5 y la primera versión de AWE 5 el cliente también recibió correcciones de comportamiento
(criterios, rejillas, árboles, asistentes, impresión, las pantallas del planificador) encontradas mientras sus suites
de integración se migraban al repositorio de AWE, además de atributos `data-testid` en sus componentes para que las
pruebas de navegador no dependan de clases CSS. El historial de la línea 2.x, con las merge requests de cada versión,
se conserva en
[`CHANGELOG-2.x.md`](https://gitlab.com/aweframework/awe/-/blob/master/awe-framework/awe-client-react/CHANGELOG-2.x.md).
A partir de AWE 5, los cambios del cliente forman parte de las notas de versión del framework.

Las versiones `2.x` ya publicadas siguen disponibles en npm. Las aplicaciones que se quedan en AWE 4 siguen usándolas:
AWE 5 no publica nuevas versiones `2.x`.
