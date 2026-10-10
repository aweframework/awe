---
id: getting-started
title: Primeros pasos
sidebar_label: Primeros pasos
---

Comience aquí si quiere el camino más rápido desde cero hasta una aplicación AWE en ejecución. Esta guía muestra el camino principal y después le dirige a documentación de referencia más detallada.

## Ruta rápida {#quick-path}

1. Instale Java 17+ y Maven 3.x.
2. Elija el arquetipo de AngularJS o de React.
3. Genere su proyecto con Maven.
4. Ejecute la aplicación con Spring Boot.
5. Confirme que la aplicación generada arranca en `http://localhost:18080`.

## Requisitos previos {#prerequisites}

- JDK 17 o superior
- Maven 3.x o superior
- Su IDE o editor preferido

## Elija su arquetipo {#choose-your-archetype}

Utilice el frontend que corresponda a su proyecto.

| Opción | Arquetipo | Úselo cuando |
| --- | --- | --- |
| AngularJS | `awe-boot-angular-archetype` | Quiere la pila clásica de AWE con AngularJS |
| React | `awe-boot-react-archetype` | Quiere el proyecto inicial de AWE con React |

Utilice la versión actual de AWE de Maven Central: [![Version](https://img.shields.io/maven-central/v/com.almis.awe/awe-starter-parent.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22com.almis.awe%22%20AND%20a:%22awe-starter-parent%22)

## Genere su proyecto {#generate-your-project}

### Proyecto AngularJS {#angularjs-project}

```bash
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-angular-archetype \
 -DarchetypeVersion=[Archetype version] \
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT
```

### Proyecto React {#react-project}

```bash
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-react-archetype \
 -DarchetypeVersion=[Archetype version] \
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT
```

## Ejecute la aplicación generada {#run-the-generated-application}

Desde el directorio del proyecto generado:

```bash
mvn spring-boot:run
```

## Qué esperar {#what-to-expect}

- El arquetipo crea una aplicación AWE basada en Maven.
- La aplicación generada está configurada para ejecutarse en el puerto `18080`.
- La configuración por defecto incluye la estructura estándar de una aplicación AWE en `src/main/resources/application/<your-acronym>/`.
- La configuración generada incluye un datasource HSQLDB embebido para que pueda empezar en local sin tener que conectar antes una base de datos externa.

Abra `http://localhost:18080` tras el arranque y confirme que la aplicación carga sin errores.

## Siguiente paso tras la primera ejecución {#next-step-after-first-run}

Continúe con [awe-101 Su primera aplicación AWE](training/awe-101.md) para el primer tutorial práctico. Retoma esta guía rápida y le muestra cómo:

- entender las carpetas generadas de AWE,
- localizar pantallas y menús,
- y realizar una pequeña personalización visible.

## Dónde continuar {#where-to-go-next}

Utilice estos documentos como referencia tras la primera ejecución:

- [Estructura del proyecto](guides/project-structure.md) para la disposición XML y las carpetas de AWE
- [Herramientas XSD](guides/xsd-tooling.md) para el autocompletado y la validación de XML en IntelliJ IDEA y VS Code
- [Contenedor de desarrollo](guides/devcontainer.md) para compilar y probar AWE desde VS Code sin instalar un conjunto de herramientas
- [awe-101 Su primera aplicación AWE](training/awe-101.md) para el primer tutorial práctico tras la guía rápida
- [Instalación](installation.md) para los detalles de configuración del servidor y del entorno
- [Despliegue](deployment.md) para las opciones de empaquetado en JAR, WAR, Docker y nube
