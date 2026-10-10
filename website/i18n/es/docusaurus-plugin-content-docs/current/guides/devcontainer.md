---
id: devcontainer
title: Contenedor de desarrollo
sidebar_label: Contenedor de desarrollo
---

El repositorio incluye un [contenedor de desarrollo](https://containers.dev/) para que pueda compilar y probar AWE sin
instalar nada en su máquina salvo Docker y un editor que admita contenedores de desarrollo (VS Code con la extensión
**Dev Containers**, o cualquier herramienta que lea `.devcontainer/devcontainer.json`).

## Qué incluye {#what-is-inside}

| Herramienta | Versión | Notas |
| --- | --- | --- |
| JDK | Temurin 21 | La CI compila con 21; el framework sigue apuntando a Java 17 |
| Maven | 3.10.0 | Configurado con el `.m2/settings.xml` del repositorio, como hace la CI |
| Node.js | 24.14.0 | La versión que descarga el frontend-maven-plugin para la compilación |
| Docker | docker-in-docker | Testcontainers y los navegadores docker de Selenium necesitan un demonio de Docker |
| Navegadores de Playwright | Chromium y Firefox | En la versión declarada por `playwright.version` en `awe-framework/awe-dependencies/pom.xml`, con sus bibliotecas de sistema |

Los navegadores de Selenium **no** están instalados en la imagen: las pruebas de Selenium los ejecutan como
contenedores docker. Nada en el contenedor guarda credenciales. `settings.xml` lee `MAVEN_REPO_USER`, `MAVEN_REPO_PASS`
y las variables de GPG del entorno, por lo que una compilación que solo compila y prueba nunca las necesita.

## Abrir el repositorio {#open-the-repository}

1. Instale Docker y la extensión **Dev Containers** en VS Code.
2. Abra la carpeta del repositorio y ejecute **Dev Containers: Reopen in Container**.
3. Espere al primer arranque: construye la imagen y ejecuta `.devcontainer/post-create.sh`, que descarga los navegadores
   de Playwright (aproximadamente 1 GB). Los siguientes arranques reutilizan el contenedor.

Después compile y pruebe como de costumbre:

```bash
mvn clean install -DskipTests
mvn test -pl awe-framework/awe-model
npm test --prefix awe-framework/awe-client-angular
```

`MAVEN_ARGS` apunta Maven a `.m2/settings.xml`, por lo que no necesita `-s`. Los perfiles de Maven no se modifican: la
CI excluye el perfil `devtools` (`-P!devtools`); añádalo a su línea de comandos si quiere lo mismo.

## Puertos {#ports}

| Puerto | Usado por |
| --- | --- |
| `8080` | Aplicaciones de pruebas de AWE (`awe-boot`, `awe-boot-react`) iniciadas con `mvn spring-boot:run` |
| `3000` | Sitio web, con `npm start --prefix website` |

Los puertos se reenvían a su máquina con una etiqueta. El contenedor no cambia la configuración de ninguna aplicación: si
el puerto `8080` está ocupado en su máquina, VS Code lo reenvía a otro puerto local y muestra cuál.

## Autocompletado de XML {#xml-autocompletion}

El contenedor registra el catálogo XML de AWE
(`awe-framework/awe-generic-screens/src/main/resources/schemas/awe/catalog.xml`) en la extensión **XML** de Red Hat,
de modo que las pantallas, los menús, las consultas y el resto de definiciones tienen autocompletado, documentación en
línea y validación sin más configuración. Consulte [Herramientas XSD](xsd-tooling.md) para los detalles y para otros
IDE.

## Pruebas de navegador {#browser-tests}

Las pruebas de Playwright encuentran los navegadores instalados por el contenedor. Los navegadores docker de Selenium
se ejecutan en el demonio de Docker del contenedor (docker-in-docker); consulte la
[guía de pruebas con Selenium](selenium-test-guide.md) para saber cómo configurarlos.

## Docker dentro del contenedor {#docker-inside-the-container}

El contenedor se ejecuta en modo privilegiado, porque docker-in-docker lo necesita. Por tanto, el código que se ejecuta
en el espacio de trabajo (plugins de Maven, scripts de npm, imágenes de Testcontainers) puede alcanzar el demonio de
Docker del contenedor con acceso elevado. Esto es aceptable para el desarrollo local; no abra repositorios ni ramas no
confiables en este contenedor.

## Mantenerlo al día {#keep-it-current}

Renovate actualiza el digest de la imagen, las características del contenedor y la versión de Node en
`.devcontainer/devcontainer.json` junto con el resto de dependencias. Node está fijado en dos lugares,
`.devcontainer/devcontainer.json` y `node.version` en `awe-framework/awe-dependencies/pom.xml`; deben cambiar juntos, y
Renovate agrupa ambos en una merge request (grupo `node toolchain`). Si cambia uno a mano, cambie el otro.

Después de que cambie la versión de una característica en `devcontainer.json`, regenere el archivo de bloqueo con la
CLI de Dev Containers:

```bash
npx -y @devcontainers/cli@0.89.0 upgrade --workspace-folder .
```

Si cambia `playwright.version` en `awe-framework/awe-dependencies/pom.xml`, reconstruya el contenedor
(**Dev Containers: Rebuild Container**) para que los navegadores coincidan. `post-create.sh` solo acepta allí una
versión simple (por ejemplo `1.63.0`) y falla con un mensaje en caso contrario.
