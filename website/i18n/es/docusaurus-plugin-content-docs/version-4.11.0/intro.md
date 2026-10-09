---
id: intro
title: Motor Web de Almis
sidebar_label: Introducción
slug: /
---

<img style={{ width: "100%", margin: "5% 10% 5% 0%" }} alt="AWE logo" src={require('@docusaurus/useBaseUrl').default('img/awe_logo.png')} />

AWE es un framework web de Java ligero. Permite construir aplicaciones web de la manera más rápida.

- :white_check_mark: Comunicación automática de servidor a cliente con soporte de WebSockets
- :white_check_mark: Utilización de Xml o Java para construir ventanas de usuario
- :white_check_mark: Componentes adaptables modernos en la interfaz de usuario
- :white_check_mark: Temas de estilo y soporte multiidioma
- :white_check_mark: Múltiples enlaces de datos. Rest, base de datos SQL y noSql, ...
- :white_check_mark: Built-in Spring Boot 3 support
- :white_check_mark: Curva de aprendizaje más fácil

## Página principal del Proyecto AWE {#awe-project-main-page}

Please visit us at [https://www.aweframework.com](https://www.aweframework.com)

## Prerequisites

You must have Maven 3.x installed on your computer and **JDK 17** or higher

## Primeros pasos {#getting-started}

This is a multi-module maven project. Puede importarlo como proyecto de maven con el IDE que prefiera. If you want to create your first AWE project, use maven archetype `awe-boot-angular-archetype` or `awe-boot-react-archetype` with version [![Version](https://img.shields.io/maven-central/v/com.almis.awe/awe-starter-parent.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22com.almis.awe%22%20AND%20a:%22awe-starter-parent%22)

### AWE with AngularJS

```bash
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-angular-archetype \
 -DarchetypeVersion=[Archetype version]
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT 
```

### AWE with ReactJS

```bash
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-react-archetype \
 -DarchetypeVersion=[Archetype version]
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT
```

## Creado con {#built-with}

- [Maven](https://maven.apache.org/) - Dependency Management
- [Spring framework](https://spring.io/) - AWE Spring boot starter
- [Angular JS](https://angularjs.org/) - Angular JS framework
- [Bootstrap](https://getbootstrap.com/) - Bootstrap web toolkit
- [Highcharts](https://www.highcharts.com/) - Interactive charts library

[![StackShare](https://img.shields.io/badge/tech-stack-0690fa.svg?style=flat)](https://stackshare.io/almis-informatica-financiera/aweframework)

## Registro de cambios {#changelogs}

Latest changelog file: [CHANGELOG.md](https://gitlab.com/aweframework/awe/-/blob/master/CHANGELOG.md)

## Contribuciones {#contributing}

Please read [CONTRIBUTING.md](https://gitlab.com/aweframework/awe/-/blob/master/CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Licencia {#license}

All parts of AWE, **except the contents of the graphical charts library (HighCharts)**, are licensed
under Apache License v2.0 see the [LICENSE.md](https://gitlab.com/aweframework/awe/-/blob/master/LICENSE.md) file for details.