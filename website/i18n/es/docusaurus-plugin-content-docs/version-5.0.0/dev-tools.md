---
id: dev-tools
title: Herramientas de desarrollo
sidebar_label: Herramientas de desarrollo
---

La recarga en caliente (hot reload) te permite editar una aplicación AWE y ver el cambio de inmediato, sin detener ni
reiniciar la JVM. Los recursos del frontend (JS, LESS, CSS) se reconstruyen de forma incremental con Webpack, y las
definiciones XML de AWE (pantallas, consultas, menús y el resto) se recargan en caliente en el contexto en ejecución.
Es un modo **exclusivo de desarrollo**, deshabilitado por defecto, sin ninguna huella en producción.

## Camino rápido {#quick-path}

```bash
npm run start:hot-reload
```

1. Ejecuta el comando anterior en la raíz del proyecto (en lugar de `npm start`).
2. Edita un archivo fuente: un XML de pantalla bajo `src/main/resources`, o un recurso LESS/JS.
3. El navegador se refresca automáticamente, tanto para las definiciones XML como para los bundles de frontend reconstruidos.

No se requiere reiniciar la JVM para los recursos del frontend ni para las definiciones XML. Los cambios en el código fuente Java siguen
provocando un reinicio de `spring-boot-devtools`.

## Qué se recarga y cómo {#what-reloads-and-how}

| Cambio | Mecanismo | Navegador |
| --- | --- | --- |
| **LESS / CSS / JS** | Webpack watch reconstruye el bundle de forma incremental (~50 ms); el watcher detecta el bundle reconstruido y difunde un refresco | Se refresca automáticamente |
| **Definiciones XML de AWE** | El watcher de XML sincroniza el archivo con el classpath y recarga en caliente las definiciones correspondientes | Se refresca automáticamente |
| **Clases Java** | `spring-boot-devtools` reinicia el contexto de la aplicación | Se refresca automáticamente (LiveReload de devtools) |

Definiciones XML cubiertas: consultas (queries), maintains, servicios, enumerados, colas, correos, acciones,
perfiles, pantallas, locales y menús.

## Cómo funciona el comando de lanzamiento {#how-the-launch-command-works}

`npm run start:hot-reload` ejecuta dos procesos en paralelo con [`concurrently`](https://www.npmjs.com/package/concurrently):

```json
"start:hot-reload": "concurrently -k -n watch,app -c cyan,green \"npm run build:watch\" \"mvn spring-boot:run -Phot-reload -Dspring-boot.run.profiles=hot-reload\""
```

- `npm run build:watch`: Webpack en modo watch, el único que escribe los bundles del frontend.
- `mvn spring-boot:run -Phot-reload`: la aplicación, con el perfil de Maven `hot-reload` activo y
  el perfil de Spring `hot-reload` seleccionado.

## Configuración {#configuration}

La recarga en caliente se apoya en tres piezas, todas incluidas en el arquetipo (y en `awe-boot`):

### 1. Scripts de npm {#1-npm-scripts}

```json
{
  "scripts": {
    "build:watch": "webpack --config webpack.config.js --watch --mode development",
    "start:hot-reload": "concurrently -k -n watch,app -c cyan,green \"npm run build:watch\" \"mvn spring-boot:run -Phot-reload -Dspring-boot.run.profiles=hot-reload\""
  }
}
```

`concurrently` es una `devDependency`. `build:watch` es el dueño de los bundles del frontend, de modo que la aplicación
nunca los reconstruye a espaldas del watcher.

### 2. Perfil de Maven `hot-reload` {#2-maven-hot-reload-profile}

El perfil mantiene al watcher de Webpack como **único escritor de bundles**; de lo contrario, `mvn spring-boot:run`
ejecutaría `npm ci` y una segunda compilación de Webpack, colisionando con el watcher y dejando la aplicación colgada
en la pantalla de carga. Omite la compilación de frontend de producción heredada con una única propiedad
(`skip.frontend`, vinculada al `skip` del plugin de frontend) y mantiene `spring-boot-devtools` para el reinicio
en caliente de Java:

```xml
<profile>
  <id>hot-reload</id>
  <!-- Webpack watch owns the frontend bundles here: skip node install, npm ci and the
       production build in one property so Maven never overwrites the watcher's output. -->
  <properties>
    <skip.frontend>true</skip.frontend>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-devtools</artifactId>
      <scope>provided</scope>
    </dependency>
  </dependencies>
</profile>
```

### 3. Perfil de Spring `hot-reload` {#3-spring-hot-reload-profile}

Seleccionado por el script `start:hot-reload` (`-Dspring-boot.run.profiles=hot-reload`), vive en
`application-hot-reload.properties` y relaja el tratamiento de los recursos estáticos para que los bundles reconstruidos se
sirvan de inmediato, además de habilitar el watcher de XML:

```properties
# Serve rebuilt bundles immediately (no content hashing, no caching)
spring.web.resources.cache.period=0
spring.web.resources.chain.cache=false
spring.web.resources.chain.strategy.content.enabled=false

# Enable the XML watcher (off by default) and watch the source folder
awe.application.xml-hot-reload=true
awe.application.xml-hot-reload-sources=src/main/resources

# Keep spring-boot-devtools from restarting when the synchronized XML changes
spring.devtools.restart.additional-exclude=application/**
```

Al dejar intacto el `application.properties` de producción, las compilaciones de producción conservan su
hash de contenido y su caché.

## Recarga en caliente del frontend (JS / LESS / CSS) {#frontend-hot-reload-js--less--css}

Webpack watch recompila el bundle en cada guardado (~50 ms). Como el perfil de Spring `hot-reload`
deshabilita el hash y la caché de los recursos, el nuevo bundle se sirve en la siguiente petición.

El watcher también vigila la salida de webpack (los bundles `specific.js` / `specific.css` bajo
`static/`): cuando se reconstruye un bundle, difunde el mismo refresco `reload-page` por el WebSocket,
de modo que el navegador se recarga y recoge el nuevo bundle automáticamente, sin refresco manual. Esto reutiliza la
difusión de recarga de XML, por lo que no necesita ninguna extensión del navegador y se comporta igual para todos los desarrolladores. Como
con XML, requiere una conexión WebSocket abierta; cuando no hay ninguna disponible, refrescas manualmente.

> **Imágenes.** Las imágenes importadas desde JS o LESS se gestionan mediante este refresco del bundle: las pequeñas se
> incrustan en el bundle, las más grandes las emite webpack y su referencia cambia al reconstruir.
> Las imágenes servidas directamente bajo `static/` y referenciadas por URL (por ejemplo, un logotipo referenciado desde una
> pantalla) no se recargan en caliente; refresca el navegador manualmente tras reemplazar una.

## Recarga en caliente de XML (definiciones del backend) {#xml-hot-reload-backend-definitions}

El bucle para un cambio de XML es:

```
edit an XML file under src/main/resources
        │
        ▼
synchronized to the exploded classpath (target/classes)
        │
        ▼
validated against its schema  ──► invalid ──► logged and skipped (previous version kept)
        │
        ▼ valid
matching definitions reloaded in the running context
        │
        ▼
connected browsers refreshed automatically
```

Tres salvaguardas mantienen el bucle rápido y seguro:

- **Validación de esquema.** Antes de recargar, el archivo modificado se valida contra su XSD (el mismo
  esquema que usa en compilación el `xml-maven-plugin`; consulta [herramientas XSD](guides/xsd-tooling.md) para obtener la misma
  validación y autocompletado en tu IDE). Una edición inválida se notifica con un mensaje limpio de
  `línea:columna` y la recarga se **omite**, de modo que la aplicación en ejecución conserva la última
  versión válida en lugar de fallar por un error de análisis.
- **Se ignoran los guardados sin cambios.** Los editores a menudo reescriben un archivo al guardar aunque no haya cambiado nada.
  Una protección basada en un hash del contenido omite la recarga cuando el contenido guardado es idéntico a la última
  versión procesada, de modo que un simple `Ctrl+S` no recarga.
- **Refresco automático del navegador.** Tras una recarga correcta, el servidor difunde un refresco por la
  conexión WebSocket existente, de modo que el navegador recarga la pantalla actual sin refresco manual.
  Esto requiere una conexión WebSocket abierta (AWE mantiene una); cuando no hay ninguna disponible, la recarga sigue
  produciéndose y refrescas manualmente.

### Propiedades {#properties}

| Propiedad | Descripción | Valor por defecto |
| --- | --- | --- |
| [`awe.application.xml-hot-reload`](properties#awe.application.xml-hot-reload) | Habilita el watcher de XML (solo desarrollo) | `false` |
| [`awe.application.xml-hot-reload-sources`](properties#awe.application.xml-hot-reload-sources) | Directorios fuente a vigilar; el XML modificado se copia al classpath antes de recargar | — |
| `spring.devtools.restart.additional-exclude` | Necesario cuando `spring-boot-devtools` está presente, para que los cambios de XML sincronizados bajo `target/classes` no provoquen un reinicio completo de devtools | — |

Usa `additional-exclude` (que conserva los valores por defecto de devtools), no `exclude` (que los reemplaza).
Ajusta el patrón `application/**` si la raíz de XML de tu aplicación
([`awe.application.paths.application`](properties#awe.application.paths.application)) no es la
predeterminada `/application/`.

### Proyectos multimódulo {#multi-module-projects}

**Para recargar en caliente XML que vive en un módulo hermano, coloca el `target/classes` de ese módulo en el classpath
de ejecución como directorio.** El watcher solo recarga un módulo cuyos recursos de classpath están explotados
en disco (una raíz `file:`); un módulo consumido como **jar de dependencia** expone URLs `jar:` en las que no puede
escribir. Así, cuando un módulo `web` aporta las definiciones `application/<module>/**` y un módulo `boot`
ejecuta la aplicación, editar los archivos de `web` no hace nada y el log muestra:

```
Source folder '.../web/src/main/resources/application/<module>' has no exploded classpath root to synchronize to. Ignoring it
```

Soluciónalo en tres pasos:

1. **Expón las clases del módulo como raíz explotada** en el perfil de Maven `hot-reload`. Usa
   `additionalClasspathElements`: el goal `run` los antepone a los jars de dependencias, de modo que el
   directorio tapa al jar obsoleto. (`<directories>` **no** es un parámetro del goal `run`; Maven lo ignora
   silenciosamente.)

   ```xml
   <configuration>
     <additionalClasspathElements>
       <additionalClasspathElement>../web/target/classes</additionalClasspathElement>
     </additionalClasspathElements>
   </configuration>
   ```

2. **Lista su carpeta fuente** en el perfil de Spring `hot-reload`:

   ```properties
   awe.application.xml-hot-reload-sources=src/main/resources,../web/src/main/resources
   ```

3. **Compila primero el módulo hermano** para que exista `target/classes`: `mvn -pl web -am install -DskipTests`.

Añade un `additionalClasspathElement` y una carpeta fuente por cada módulo que contenga definiciones editables.

> **Devtools + multimódulo.** Si el perfil conserva `spring-boot-devtools`, su classloader de reinicio
> carga los jars de los módulos propios de la aplicación en el classloader base mientras los jars del framework se cargan con el de reinicio,
> duplicando tipos compartidos y haciendo fallar el arranque (un bean "could not be found" para un tipo que claramente
> existe). Mantenlos en un solo cargador ampliando `META-INF/spring-devtools.properties`:
> ```properties
> restart.include.awe=/awe.+\.jar
> restart.include.<app>=/<app>.+\.jar
> ```

## Seguridad en producción {#production-safety}

La recarga en caliente de XML es exclusiva de desarrollo y está deshabilitada por defecto:

- En los despliegues **empaquetados en jar**, las definiciones XML viven dentro de los jars, por lo que el watcher no tiene nada que
  vigilar aunque la propiedad se deje habilitada por error.
- En los despliegues con **classpath explotado**, simplemente no habilites la propiedad en producción; el watcher
  registra un `WARN` al arrancar como recordatorio.

## Siguiente paso {#next-step}

- Compilación del frontend y configuración del cliente: [Maven y Frontend](maven).
- Referencia completa de propiedades: [Propiedades](properties).
