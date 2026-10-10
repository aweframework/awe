---
id: release-lines-and-support
title: Líneas de versiones y soporte
sidebar_label: Líneas de versiones y soporte
---

Esta guía está dirigida a los desarrolladores de productos basados en AWE y a los mantenedores de AWE. Explica cómo
AWE mantiene más de una versión mayor a la vez, qué se ejecuta en la integración continua (CI) en cada rama, cómo
publicar cada línea y cómo viaja una corrección entre líneas.

## Líneas de versiones {#release-lines}

AWE desarrolla una versión mayor cada vez en `develop`, mientras que las versiones mayores anteriores que todavía
tienen soporte viven en ramas dedicadas `support/<major>.x`.

- **`develop` / `5.x`** — la línea de desarrollo activa. `develop` es la rama de integración de la próxima versión
  `5.y.z`; `master` contiene el último código publicado de `5.x`. Aquí es donde llegan las nuevas funcionalidades y los
  cambios de API.
- **`support/4.x`** — la línea de mantenimiento de AWE 4. Se creó a partir de `develop` en el momento en que `develop`
  pasó a `5.0.0-SNAPSHOT`, por lo que parte del último estado `4.12.x` de `develop` y no de una etiqueta anterior. Aquí
  solo llegan correcciones de seguridad, correcciones de errores críticos y parches de dependencias/CVE; no hay nuevas
  funcionalidades ni cambios de API.

Una rama de soporte es una rama normal de larga duración, no un fork: tiene su propia pipeline de CI, sus propias
versiones y sus propias etiquetas, y se retira cuando termina su periodo de soporte.

## Versionado y etiquetas {#versioning-and-tags}

Cada rama sigue el versionado semántico (`MAJOR.MINOR.PATCH`), con `-SNAPSHOT` entre versiones publicadas. `develop`
avanza por `5.0.0-SNAPSHOT`, `5.0.1-SNAPSHOT`, etc.; `support/4.x` avanza por `4.12.10-SNAPSHOT`,
`4.12.11-SNAPSHOT`, y así sucesivamente. Las etiquetas de versión usan el prefijo `v` configurado en el
gitflow-maven-plugin (`versionTagPrefix=v`), por ejemplo `v4.12.10` o `v5.0.0`.

Como ambas líneas se versionan de forma independiente, una etiqueta por sí sola lo dice todo: una etiqueta `v4.y.z`
siempre procede de `support/4.x`, y una etiqueta `v5.y.z` (o posterior) siempre procede de `develop`/`master`.

## Qué se ejecuta en la CI de cada rama {#what-runs-in-ci-on-each-branch}

| Job / área | `develop` | `master` | `support/4.x` | Merge request |
|---|---|---|---|---|
| Compilación, pruebas unitarias/de BD/de frontend, javadoc | sí | sí | sí | sí |
| Análisis de dependencias | sí | sí | sí | sí |
| Lint del título de la merge request (bloqueante) y aviso de tamaño | no | no | no | sí |
| Pruebas de integración de navegador con Playwright | sí | sí | no | sí |
| Pruebas de integración de navegador con Selenium | solo pipeline semanal programada | sí | sí | no |
| Análisis de Sonar (`sonar.branch.name` definido) | sí | sí | sí | sí (análisis de MR) |
| Generar javadoc y esquemas | sí | sí | sí | no |
| Construir paquete (imagen de Docker) | sí | sí | sí | no |
| Etiquetas flotantes de Docker (`4`, `4.12`) | no | no | sí | no |
| Desplegar snapshot | sí | no | sí | no |
| Iniciar una nueva versión (manual) | sí | no | sí | no |
| Desplegar a preproducción / producción (Kubernetes) | no | sí (prod) | no | no |
| DAST | no | sí | no | no |
| GitLab Pages (sitio de documentación) | sí | sí | no | no |
| Pipeline de etiqueta (Maven Central, hitos, notas de versión) | en etiqueta | en etiqueta | en etiqueta | no |

Las suites de navegador se ejecutan con dos herramientas, y ambas son bloqueantes allí donde se ejecutan: una suite
fallida hace fallar la pipeline, detiene `Launch Sonar` (y con él los jobs de publicación) e impide que Renovate haga
automerge. Una prueba inestable se vuelve a ejecutar sola una vez y se informa como inestable; una prueba que falla dos
veces hace fallar el job, y un job solo se reintenta cuando falla el runner (no ante un fallo de prueba ni un tiempo de
espera agotado).

- **Playwright** (jobs `Playwright IT 1/4` a `Playwright IT 4/4`: Chromium y Firefox, cada uno contra la aplicación
  AngularJS y la aplicación React) es la herramienta de navegador de la pipeline diaria: merge requests con cambios de
  código, `develop` y `master`. No se ejecuta en `support/*`: `support/4.x` no tiene adaptador de Playwright.
- **Selenium** (jobs `Selenium IT 1/4` a `Selenium IT 4/4`: Chrome y Firefox, cada uno contra la aplicación AngularJS y
  la aplicación React) se ejecuta en `master`, en `support/*` y en las pipelines de la **programación de pipeline
  «Weekly Check» en `develop`**. No se ejecuta en merge requests ni en un push ordinario a `develop`. La programación es
  un ajuste del proyecto en GitLab (CI/CD, Schedules), no un archivo del repositorio; la programación «Renovate»
  ejecuta solo el job de Renovate.

Cada herramienta ejecuta los mismos cuatro grupos de suites dos veces: contra la aplicación de pruebas AngularJS
(`awe-tests/awe-boot`) y contra la aplicación de pruebas del motor React (`awe-tests/awe-boot-react`). `Launch Sonar`
espera a los jobs de navegador que se hayan ejecutado en la pipeline (ambas herramientas en `master` y en la
programación semanal de `develop`).

Solo las pruebas fallidas dejan evidencias. Abra la pestaña **Tests** de la pipeline, elija la prueba fallida y use
**View details** para ver su captura de pantalla; la salida de la prueba también enlaza la captura y el vídeo, y el
final del log de cada job enlaza a la carpeta `browser-evidence/` de los artefactos del job, donde se almacenan todas
las capturas y vídeos de ese job.

Los jobs de la pipeline de etiqueta son independientes de la rama: leen la versión de `$CI_COMMIT_TAG`, por lo que se
comportan igual tanto si la etiqueta procede de `develop` como de `support/4.x`.

Las pipelines de merge request son selectivas: los jobs se añaden según las rutas que toca la merge request. Un cambio
limitado a `website/` construye solo el sitio de documentación; un cambio limitado a
`awe-framework/awe-client-angular/` ejecuta las pruebas unitarias del frontend, las suites de Playwright de AngularJS,
Sonar y el análisis de dependencias, pero no la matriz de bases de datos ni las suites de Playwright de React; un
cambio limitado a `awe-framework/awe-client-react/` ejecuta las pruebas unitarias y el lint de React, ambos conjuntos
de suites de Playwright, Sonar y el análisis de dependencias; un cambio de backend ejecuta la compilación, la matriz de
bases de datos, ambos conjuntos de suites de Playwright, Sonar, el análisis de dependencias y la comprobación de
javadoc, pero ni las pruebas unitarias del frontend ni la compilación de la documentación. Un cambio en
`.gitlab-ci.yml` cuenta como si tocara todo; un cambio en el `pom.xml` raíz cuenta como un cambio de backend. Las
pipelines de rama en `develop`, `master` y `support/*` ejecutan siempre el conjunto completo, salvo que cada rama
ejecuta solo las herramientas de navegador indicadas arriba. Las pipelines de merge request son interrumpibles, por lo
que un nuevo push cancela automáticamente la pipeline superada; las pipelines de ramas protegidas no se cancelan. Los
jobs de base de datos y de navegador se generan a partir de definiciones `parallel:matrix` (`Embedded DB Tests`, un job
por motor de base de datos con y sin Flyway, los jobs de navegador de Playwright y Selenium por suite), y cada job tiene
un tiempo límite de aproximadamente el doble de su duración observada.

Las ramas de soporte deben ser **ramas protegidas** en GitLab (el patrón `support/*` se protege con la misma política
que `develop`). Las credenciales que usan `Build package` (Docker Hub), `Deploy snapshot` (repositorio Maven) y el job
de publicación (token de API, claves GPG, identidad de git) son variables de CI protegidas, y GitLab solo las inyecta
en pipelines de referencias protegidas. En una rama de soporte sin proteger esos jobs fallan con un login de Docker
vacío y un `401 Unauthorized` del repositorio Maven.

## Título y tamaño de la merge request {#merge-request-title-and-size}

Cuatro jobs rápidos de la etapa `build` comprueban cada pipeline de merge request, antes de construir nada. Un quinto,
[`Lint frontend`](#frontend-lint-blocking), comprueba el código de frontend de las merge requests que lo modifican.

### Formato del título (bloqueante) {#title-format-blocking}

La merge request se integra con squash, por lo que su título se convierte en el mensaje de commit en `develop`. El job
`MR title lint` lo comprueba con [commitlint](https://commitlint.js.org/) y hace fallar la pipeline cuando no sigue
Conventional Commits:

```
type(scope): description (#issue Ttask)
```

- **type**: `feat`, `fix`, `chore`, `ci`, `docs`, `test`, `refactor`, `build`, `perf`, `style` o `revert`, en
  minúsculas. Añada `!` después del tipo o del ámbito para un cambio incompatible.
- **scope**: opcional y una lista abierta: el módulo o área, como `awe-client-react`, `awe-model`, `ci` o `deps`. Varios
  ámbitos se separan con una coma.
- **description**: cualquier capitalización, sin punto final. Ponga la referencia a la incidencia al final, por ejemplo
  `(#794 T7)` para una tarea de una incidencia o `(#795)` para la incidencia. La cabecera puede tener hasta 150
  caracteres. Un prefijo `Draft:` se ignora.

```
feat(awe-model): ECharts option model for charts beside the Highcharts one (#795 T1a)
fix: guard null dates in DateUtil.asLocalTime
chore(deps): update dependency postcss to v8.5.28 (develop)
```

Los títulos que escribe Renovate ya siguen el formato, por lo que sus merge requests pasan sin cambios. Para corregir un
título que falla, edítelo en GitLab y reintente el job.

Las reglas están en `.gitlab/commitlint/commitlint.config.mjs`; las versiones de commitlint están fijadas en
`.gitlab/commitlint/package.json` y `package-lock.json`, que actualiza Renovate. Para comprobar un título en local
(Node.js 22.12 o posterior):

```bash
bin/lint-mr-title.sh "feat(awe-model): new option model (#795 T1a)"
```

### Aviso de tamaño (no bloqueante) {#size-warning-not-blocking}

El job `MR size warning` cuenta las líneas que cambia la merge request (adiciones más eliminaciones) y avisa por encima
de **400**, el tamaño que un revisor todavía puede leer con atención. No hace fallar la pipeline: el job termina con
código de salida 64, que tiene permitido fallar, y la pipeline muestra un aviso. El log del job lista los archivos más
grandes. Dividir el trabajo en merge requests más pequeñas (un comportamiento con sus pruebas y documentación cada una)
mantiene las revisiones ágiles.

Estos archivos no se cuentan: archivos de bloqueo (`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `*.lock`,
`skills-lock.json`), archivos generados (`*.min.js`, `*.min.css`, `*.svg`, carpetas `generated/` y `target/`),
`CHANGELOG.md`, la documentación del sitio web (`website/docs/**`, `website/i18n/**`) y archivos binarios. Ejecútelo en
local contra la rama de destino:

```bash
bin/mr-size.sh origin/develop   # base to compare with; head is HEAD
```

### Aviso de documentación (no bloqueante) {#docs-warning-not-blocking}

El job `MR docs warning` forma parte de la definición de terminado: la documentación cambia junto con el código. Avisa
cuando una merge request cambia código (`awe-framework/`, `awe-samples/`, `bin/` o `pom.xml`, sin fuentes de prueba,
archivos de bloqueo ni markdown) pero no cambia documentación (`website/`, `CONTRIBUTING.md`, `README.md` o
`AGENTS.md`) y no dice por qué no hace falta ninguna. Como el aviso de tamaño, termina con código de salida 64, que
tiene permitido fallar, por lo que la pipeline muestra un aviso y nunca bloquea la integración.

Para eliminarlo, actualice la documentación en la merge request, o marque uno de los dos elementos de la plantilla de
merge request: **Docs updated**, o **No docs needed because:** con el motivo escrito a continuación (los `…` de la
plantilla no son un motivo). Una merge request cuyo título sea una actualización de dependencias (`chore(deps)` o
`build(deps)`) no se comprueba. El job lee la descripción a través de la variable de CI `CI_MERGE_REQUEST_DESCRIPTION`;
cuando GitLab trunca una descripción larga (conserva 2700 caracteres) los elementos no se pueden leer y el job no
avisa. La comprobación es solo informativa: cuando no puede hacer su trabajo (sin base con la que comparar, un commit
base ausente en un clon superficial) imprime un aviso y termina con 0. Ejecútelo en local contra la rama de destino
(compara con la base de fusión, como hace una merge request):

```bash
CI_MERGE_REQUEST_DESCRIPTION="$(cat description.md)" bin/mr-docs.sh origin/develop
```

### Aviso de desfase de traducción (no bloqueante) {#translation-drift-warning-not-blocking}

Las traducciones al español viven en el repositorio (`website/i18n/es`), y una página sin traducción recurre al inglés.
El job `MR translation drift warning` avisa cuando una merge request cambia una página en inglés bajo `website/docs`
cuya traducción al español existe en `website/i18n/es/docusaurus-plugin-content-docs/current` y no cambia esa
traducción en la misma merge request. Las versiones congeladas no se comprueban. Termina con código de salida 64
(tiene permitido fallar) o 0, como los demás avisos; ejecútelo en local con:

```bash
bin/mr-translations.sh origin/develop
```

`bin/test-mr-checks.sh` prueba los cuatro scripts; el job `MR checks tests` lo ejecuta en las merge requests que los
modifican.

## Lint del frontend (bloqueante) {#frontend-lint-blocking}

El job `Lint frontend` ejecuta [ESLint](https://eslint.org/) en ambos motores de frontend, el cliente AngularJS
(`awe-client-angular`) y el cliente React (`awe-client-react`), en la etapa `build`, de modo que un fallo se muestra en
alrededor de un minuto, antes de que empiecen las pruebas unitarias. Se ejecuta en las merge requests que cambian un
cliente y en `develop`, `master` y `support/*`.

Cada cliente tiene un script `lint:ci` en su `package.json` con `--max-warnings` establecido al número de avisos que
tiene hoy. Un error de ESLint hace fallar el job, y también cualquier aviso por encima de ese número: **los avisos solo
pueden bajar**. Una merge request que corrige avisos reduce el número en el mismo cambio; `npm run lint` imprime el
recuento actual.

```bash
npm --prefix awe-framework/awe-client-angular run lint:ci
npm --prefix awe-framework/awe-client-react run lint:ci
```

La compilación de Maven ejecuta el mismo `lint:ci` antes de las pruebas unitarias, por lo que `mvn test` falla en local
con el mismo límite. La CI omite ese paso de Maven (`-Dskip.lint=true`, definido en `MAVEN_CLI_OPTS`), porque
`Lint frontend` ya lo ejecutó: el lint se ejecuta una vez por pipeline.

## Cadena de suministro {#supply-chain}

Cada imagen y servicio referenciado en `.gitlab-ci.yml` está fijado (los jobs que provienen de las plantillas de
seguridad propias de GitLab usan las imágenes de analizador que mantiene GitLab): una etiqueta de versión inmutable
cuando el proveedor la publica (`maven`, `docker`, `release-cli`), o una referencia
`tag@sha256:digest` cuando solo existe una etiqueta `latest` (las imágenes `docker-tools`, `selenoid/firefox`,
`selenoid/chrome`). La parte `tag` de `tag@digest` se conserva solo para que la
referencia siga siendo legible; el digest es lo que realmente congela la imagen. Nada en `.gitlab-ci.yml` sigue una
etiqueta flotante `latest`. [Renovate](#dependency-updates) es el responsable de abrir las merge requests que actualizan
estos fijados (incluyendo la resolución de nuevo de los digests), por lo que un fijado nunca se actualiza editando
`latest` a mano.

`Build project` genera un SBOM CycloneDX (`target/awe-sbom.json`) para todo el reactor de Maven con el
`cyclonedx-maven-plugin`. Se publica como artefacto de la pipeline, y `Build package` adjunta ese mismo SBOM a la imagen
`awe-boot` enviada al registro de GitLab como una atestación de [cosign](https://github.com/sigstore/cosign), de modo
que el SBOM viaja con la propia imagen, no solo con la ejecución de la pipeline. No se declara como informe `cyclonedx`
de GitLab a propósito: la lista de dependencias solo acepta SBOM que llevan las propiedades CycloneDX propias de GitLab
y ya la alimenta el job de análisis de dependencias `gemnasium`, que cubre el mismo reactor de Maven. El cliente React
tiene su propio SBOM CycloneDX, generado por el job `Publish npm` (consulte [paquete npm](#npm-package)).

`Build package` también firma las imágenes `awe-boot` y `awe-boot-react` sin clave, usando la propia identidad OIDC de
GitLab (`id_tokens: SIGSTORE_ID_TOKEN`) en lugar de una clave privada almacenada: tanto la imagen del registro de
GitLab como la imagen de Docker Hub se firman por digest, una sola vez, ya que todas las etiquetas flotantes
(`$PROJECT_VERSION`, y en las ramas `support/*` los alias mayor/mayor.menor) apuntan a ese mismo digest.

La identidad del certificado es la definición de pipeline que produjo la firma
(`https://gitlab.com/aweframework/awe//.gitlab-ci.yml@refs/heads/<branch>`), por lo que la expresión de abajo solo
acepta imágenes construidas desde las líneas de publicación, no desde una rama de funcionalidad.
Verifique una imagen publicada con:

```bash
cosign verify \
  --certificate-identity-regexp '^https://gitlab.com/aweframework/awe//\.gitlab-ci\.yml@refs/heads/(develop|master|support/.+)$' \
  --certificate-oidc-issuer https://gitlab.com \
  registry.gitlab.com/aweframework/awe/awe-boot:<version>

cosign verify-attestation --type cyclonedx \
  --certificate-identity-regexp '^https://gitlab.com/aweframework/awe//\.gitlab-ci\.yml@refs/heads/(develop|master|support/.+)$' \
  --certificate-oidc-issuer https://gitlab.com \
  registry.gitlab.com/aweframework/awe/awe-boot:<version>
```

Los mismos comandos verifican `awe-boot-react`. El SBOM adjunto a ambas imágenes es el de todo el reactor de Maven.

Los escáneres se ejecutan en distintos puntos de la pipeline:

- **El análisis de dependencias** (`gemnasium-maven-dependency_scanning`) se ejecuta en merge requests y en las
  pipelines de rama `develop`/`master`/`support/*`, como antes.
- **La detección de secretos** (`secret_detection`) se ejecuta en cada merge request y en cada pipeline de rama.
- **El análisis de contenedores** (`container_scanning`) se ejecuta solo en las pipelines de rama `develop`, `master` y
  `support/*`, después de que `Build package` haya enviado la imagen; tiene `allow_failure: true` durante las primeras
  semanas tras su adopción, de modo que los hallazgos aparecen sin bloquear las publicaciones, y se endurecerá una vez
  clasificados.

OWASP dependency-check no se usa intencionadamente: necesita una clave de API de NVD para ejecutarse a una velocidad
práctica, y su función ya la cubren el análisis de dependencias de GitLab y el SBOM CycloneDX, por lo que añadirlo solo
duplicaría esa cobertura.

## Actualizaciones de dependencias {#dependency-updates}

Las dependencias se mantienen al día con un bot [Renovate](https://docs.renovatebot.com/) autoalojado. Se ejecuta como
el job `Renovate` en `.gitlab-ci.yml`, iniciado solo por la programación de pipeline llamada «Renovate», que define
`RENOVATE_RUN=true`. Esa pipeline no contiene ningún otro job. La configuración es `renovate.json` en `develop`:
Renovate siempre lee la rama por defecto, también cuando actualiza `support/4.x`.

**Qué gestiona.** Maven (propiedades de `pom.xml`, importaciones de BOM y plugins, incluido el BOM en
`awe-framework/awe-dependencies/pom.xml`), los manifiestos de npm y sus archivos `package-lock.json` (el mantenimiento
de archivos de bloqueo se ejecuta el lunes a primera hora), el Dockerfile de `awe-boot`, cada entrada `image:` y
`services:` de `.gitlab-ci.yml` (resolviendo de nuevo los digests `latest@sha256` y fijando las etiquetas de versión
con digests), además de las versiones de Node y npm del frontend-maven-plugin y la coordenada del plugin CycloneDX en
línea, la pila `docker-compose` de observabilidad bajo `awe-tests` y las imágenes de bases de datos de Testcontainers
fijadas en `awe-testing-images.properties`. No gestiona el `package.json` raíz (un resto sin uso) ni las plantillas de
arquetipos.

| Rama | Actualizaciones abiertas | Automerge |
|---|---|---|
| `develop` | Todo: mayor, menor, parche, fijado, digest, archivo de bloqueo y seguridad | Versiones de parche de dependencias directas de Maven y npm, 3 días después de su publicación, solo cuando la pipeline de la merge request pasa (la regla de aprobación se levanta únicamente para esas merge requests). Los digests, fijados, imágenes de CI, el Dockerfile, los archivos compose y el mantenimiento de archivos de bloqueo esperan a una persona |
| `support/4.x` | Parches, fijados, digests y correcciones de seguridad (OSV) | Ninguno: un mantenedor integra |

Las actualizaciones relacionadas se agrupan en una merge request: Spring Boot, Spring Cloud, Selenium (con el gestor de
WebDriver), plugins de Maven, Babel, Jest, Docusaurus, las imágenes de CI, las imágenes de docker-compose y la cadena de
herramientas de Node. Hay como máximo 8 merge requests del bot abiertas a la vez (las correcciones de seguridad están
exentas de ese límite). Cada merge request lleva la etiqueta `update-dependencies` (más `security` para correcciones de
vulnerabilidades) y un mensaje Conventional Commit, `fix(deps)` para dependencias de ejecución y `chore(deps)` en los
demás casos. Una incidencia «Dependency Dashboard» lista las actualizaciones pendientes, abiertas y bloqueadas; las
actualizaciones mayores del cliente AngularJS heredado (`awe-client-angular`, `awe-tools` y las aplicaciones de
`awe-tests`) solo se abren después de que alguien las marque allí, ya que ese cliente se sustituye en AWE 5. Las
alertas de seguridad proceden de la base de datos OSV y cubren solo las dependencias directas.

Las merge requests del bot pasan por la pipeline selectiva de merge request: un cambio en `pom.xml` ejecuta los jobs de
backend y Selenium, un cambio de npm bajo el cliente ejecuta los jobs de frontend, un cambio en `website/` ejecuta la
compilación del sitio web y un cambio en `.gitlab-ci.yml` ejecuta todo. El automerge se limita a los dos gestores cuyos
cambios esa pipeline ejercita de extremo a extremo; la imagen `awe-boot`, por ejemplo, solo se construye en pipelines
de rama, por lo que sus fijados del Dockerfile los integra una persona.

### Configuración y operación {#setup-and-operations}

1. Cree un token de acceso de proyecto con el rol Developer, los ámbitos `api` y `write_repository`, y una caducidad de
   como máximo un año. Rótelo antes de que caduque.
2. Añada las variables de CI `RENOVATE_TOKEN` y `RENOVATE_GITHUB_COM_TOKEN`, ambas enmascaradas, protegidas y con
   **ámbito de entorno `renovate`** (el job declara ese entorno, de modo que ningún otro job en una referencia
   protegida las recibe). El token de GitHub es un token de acceso personal sin ámbitos (solo lectura pública);
   Renovate lo necesita para descargar la compilación de Node que usa para refrescar los archivos `package-lock.json` y
   para obtener los registros de cambios. Sin él, el límite de tasa anónimo de GitHub se alcanza en una sola ejecución y
   las merge requests de npm llegan con un archivo de bloqueo obsoleto, que `npm ci` rechaza.
3. Cree la programación de pipeline «Renovate» en `develop`, por ejemplo `0 5 * * 1-5` en la zona horaria
   Europe/Madrid, con la variable `RENOVATE_RUN=true`. El mantenimiento de archivos de bloqueo solo se ejecuta cuando
   una pipeline de Renovate se produce un lunes entre las 00:00 y las 05:59 de Europe/Madrid
   (`lockFileMaintenance.schedule` en `renovate.json`), así que mantenga al menos una ejecución semanal dentro de esa
   ventana.
4. Ejecútelo primero con una variable de programación adicional `RENOVATE_DRY_RUN=full`, lea el log del job y después
   elimine esa variable. El automerge se apoya además en dos ajustes del proyecto que se cumplen hoy y deben seguir así:
   los Developers pueden integrar en `develop`, y «Prevent editing approval rules in merge requests» está desactivado
   (Renovate levanta la regla de aprobación añadiendo una regla de cero aprobaciones a sus propias merge requests). Si
   cualquiera de los dos cambia, las merge requests del bot simplemente permanecen abiertas.
5. El job `Validate renovate config` se ejecuta en cada merge request que toca `renovate.json`. En local, desde la raíz
   del repositorio, ejecute `npx --yes --package renovate -- renovate-config-validator --strict`.

## Publicar desde una rama de soporte {#releasing-from-a-support-branch}

Las publicaciones se lanzan con el job manual `Start a new release` de la pipeline, en una pipeline de push de
`support/4.x` (no una pipeline de merge request). El job despacha según la rama: `develop` sigue ejecutando
`mvn gitflow:release`; cualquier rama `support/*` ejecuta en su lugar `bin/support-release.sh`.

`bin/support-release.sh` hace lo siguiente, de forma no interactiva:

1. Verifica que se ejecuta en una rama `support/*` (desde `$CI_COMMIT_REF_NAME` en CI, o la rama actual en local) y que
   el árbol de trabajo está limpio.
2. Lee la versión `-SNAPSHOT` actual de `pom.xml` y calcula la versión de publicación (la misma versión sin
   `-SNAPSHOT`, o la sustitución `RELEASE_VERSION`) y el siguiente snapshot de parche.
3. Ejecuta `mvn gitflow:hotfix-start -DfromBranch=support/4.x -DhotfixVersion=<version>` seguido de
   `gitflow:hotfix-finish` con `-DskipMergeProdBranch` y `-DskipMergeDevBranch`, de modo que la publicación se etiqueta
   sin integrarse en `master` ni en `develop`. Es el flujo de hotfix del gitflow-maven-plugin, reutilizado aquí porque
   la versión del plugin fijada por AWE no tiene un objetivo dedicado de «publicar una rama de soporte».
4. Sube la rama al siguiente `-SNAPSHOT` de parche con `versions:set` y `versions:set-property` (reflejando tanto
   `<version>` como la propiedad `<revision>`, en el `pom.xml` de cada módulo), confirma ese cambio y envía la rama y la
   nueva etiqueta juntas con `git push --atomic`.
5. Ejecuta `bin/docusaurus.sh`, que corta una instantánea versionada de la documentación solo cuando la publicación es
   una versión menor o mayor (`X.Y.0`); una publicación de parche como `4.12.10` no obtiene su propia instantánea de
   documentación.

Cuando el job termine, compruebe:

- Que la nueva etiqueta (`v4.y.z`) existe y que su pipeline de etiqueta se ejecutó (despliegue en Maven Central, notas
  de versión, publicación en GitLab, generación de hitos).
- Que `support/4.x` ha vuelto al siguiente `-SNAPSHOT` en cada `pom.xml`.
- Que la imagen de Docker de la nueva versión se envió y que las etiquetas flotantes `4` / `4.12` se reapuntaron a
  ella (véase más abajo).

## Backport de una corrección {#backporting-a-fix}

La política completa está en [`CONTRIBUTING.md`](https://gitlab.com/aweframework/awe/-/blob/develop/CONTRIBUTING.md):
una corrección que afecta a ambas líneas nace en `support/4.x` y después se promociona a `develop`.

Ejemplo práctico, para una corrección de puntero nulo en el motor de consultas:

1. Cree la rama `fix/758-query-npe` desde `support/4.x`, corrija el error y abra la merge request
   **«Fix NPE in QueryService#execute when filter is empty»** hacia `support/4.x`. Revise e integre como de costumbre.
2. Abra una segunda merge request, **«[backport] Fix NPE in QueryService#execute when filter is empty»**, desde una
   rama basada en `develop`, aplicando con cherry-pick (o fusionando) el commit del paso 1. Etiquétela como `backport`.
   Resuelva los conflictos causados por cambios de API exclusivos de 5.x directamente en esta merge request e
   intégrela en `develop`.

Una corrección nunca viaja en sentido contrario (de `develop` a `support/4.x`), salvo que solo tenga sentido en
`develop` porque depende de código exclusivo de 5.x; esas correcciones no se llevan a ninguna otra rama.

## Etiquetas de imagen de Docker {#docker-image-tags}

Cada compilación se etiqueta con su versión exacta del proyecto, por ejemplo `aweframework/awe-boot:4.12.10-SNAPSHOT` o
`awe-boot:5.0.0`. Las compilaciones de `support/4.x` obtienen además dos alias flotantes: la versión mayor (`4`) y la
versión mayor.menor (`4.12`). Estos alias se reapuntan en cada compilación de esa línea, incluidas las compilaciones
snapshot entre publicaciones, por lo que `4` y `4.12` siempre apuntan a la última compilación de `support/4.x`, no
necesariamente a una versión publicada. No hay etiqueta `latest` en ninguna rama ni línea: fije siempre una versión
explícita o, deliberadamente, uno de estos alias flotantes. La imagen `awe-boot-react` sigue el mismo etiquetado.

## Imagen de la aplicación de pruebas React {#react-test-application-image}

`Build package` construye una segunda imagen, `awe-boot-react`, a partir de `awe-tests/awe-boot-react`: la aplicación de
pruebas que se ejecuta sobre el motor React, la contraparte de `awe-boot`. Sigue las mismas reglas que `awe-boot`
(construida en `develop`, `master` y `support/*`, con las mismas etiquetas, registro de GitLab
`registry.gitlab.com/aweframework/awe/awe-boot-react` y Docker Hub `aweframework/awe-boot-react`, firmada por digest y
con la atestación del SBOM). Un job aparte, `Build React package`, empaqueta su jar a partir de los módulos y del bundle
del cliente construido por `Build project`. Ningún job de despliegue usa esta imagen: preproducción y producción
despliegan solo `awe-boot`.

## Paquete npm {#npm-package}

El cliente React se publica en npm como [`awe-react-client`](https://www.npmjs.com/package/awe-react-client) mediante el
job `Publish npm`. No se publica en un repositorio Maven.

- **Cuándo.** Solo en las pipelines de las etiquetas de publicación de la línea 5.x y posteriores (`v5.*`, `v6.*`...),
  después de que `Deploy Maven Central` haya tenido éxito. Una etiqueta `v4.*` de `support/4.x` nunca lo publica,
  porque esa línea no tiene cliente React.
- **Versión.** La etiqueta sin la `v`: `v5.0.0` publica `awe-react-client@5.0.0`, que es también la versión de Maven del
  commit etiquetado. El job construye el cliente con esa versión y falla si el `dist/package.json` generado no lleva
  exactamente esa.
- **Dist-tags.** Una versión preliminar (cualquier versión con guion, como `5.1.0-rc.1`) se publica bajo el dist-tag
  `next`, una versión final bajo `latest`.
- **Procedencia.** El paquete se publica con `npm publish --provenance`: GitLab emite un token OIDC para el job
  (`id_tokens: SIGSTORE_ID_TOKEN`) y npm registra una declaración firmada que vincula la versión con este repositorio,
  la etiqueta y la pipeline que la construyó. Compruébelo en un proyecto que instale el paquete con
  `npm audit signatures`. El campo `repository` del paquete debe coincidir con el proyecto de GitLab para que npm
  acepte la declaración.
- **Credenciales.** El job se autentica con la variable de CI `NPM_AUTH_TOKEN`, que debe ser una variable enmascarada y
  protegida, disponible para las pipelines de etiquetas protegidas. Se escribe en un `.npmrc` local del job a través
  del entorno y nunca se imprime.
- **Reejecuciones.** npm se niega a publicar una versión dos veces, por lo que el job comprueba primero con `npm view` y,
  cuando la versión ya existe, termina con éxito sin volver a publicar.
- **SBOM.** El job escribe un SBOM CycloneDX de las dependencias de ejecución del cliente
  (`awe-react-client-sbom.json`, de `npm sbom`) como artefacto del job que no caduca. No se adjunta a la publicación de
  GitLab.

Las aplicaciones que migran desde la línea 2.x siguen la
[guía de actualización del cliente React](react-client-upgrade.md). El arquetipo de React genera un proyecto que
depende de la versión exacta del arquetipo.

## Documentación por línea {#documentation-per-line}

`bin/docusaurus.sh` corta una instantánea versionada de la documentación (mediante `yarn docusaurus docs:version`) solo
para publicaciones menores o mayores (versiones que terminan en `.0`) en `develop`/`master`; no hace nada en una rama
`support/*`, ya que la línea de mantenimiento publica su documentación en vivo (véase más abajo). Las instantáneas
congeladas ya cortadas antes de que una línea pasara a mantenimiento (de `4.8.0` a `4.12.0`) permanecen en
`versioned_docs/` como historial de solo lectura.

La línea de mantenimiento `4.x` se publica como una versión en vivo «4.x (maintenance)», construida directamente desde
`support/4.x` en cada compilación del sitio web de `develop`/`master`. `bin/website-maintenance-docs.sh` obtiene
`support/4.x`, toma una instantánea de su `website/docs` y `website/sidebars.js` con las herramientas `docs:version` de
Docusaurus y restaura la documentación de `develop` antes de que se construya el sitio; nada de esta inyección se
confirma nunca. Por tanto, un cambio de documentación para la línea 4.x se hace **una sola vez**, en una merge request
contra `support/4.x`, y aparece automáticamente en el sitio público tras la siguiente ejecución de la pipeline de
`develop`, sin ningún cambio en el propio `develop` (véase la incidencia #787). Las traducciones al español de las
páginas de 4.x no forman parte de esa inyección: la página recurre al inglés salvo que exista una traducción bajo
`website/i18n/es` en `develop`.

## Periodo de soporte (propuesto) {#support-window-proposed}

Tal como se propuso a la dirección, `support/4.x` está previsto que reciba correcciones de seguridad y críticas hasta
que FMB 21 complete su migración a AWE 5, y durante al menos 18 meses después de la versión GA 5.0, lo que ocurra más
tarde. Es una propuesta, no una política comprometida; consulte el gestor de incidencias para la decisión final antes
de depender de una fecha de finalización concreta.
