![logo_almis](website/static/img/logo_almis.png)

# **Almis Web Engine**

AWE is a light-weight Java web framework. Allows you build web applications in the fastest way.

- Automatic server-client communication with WebSocket support
- Use Xml or Java for building views
- Modern UI responsive components
- Themes and Multi-language support
- Multiple data binding. Rest, SQL and noSql database, ...
- Built-in Spring Boot support
- Easiest learning curve

## AWE Project main page

Please visit us at [https://www.aweframework.com](https://www.aweframework.com)

## Documentation main page

Please visit us at [https://docs.aweframework.com](https://docs.aweframework.com)

## Getting Started

Start with the dedicated [Getting Started guide](website/docs/getting-started.md). It is the canonical onboarding path for prerequisites, archetype selection, project generation, first run, and next steps.

If you want to contribute to AWE itself, import this repository as a multi-module Maven project in your preferred IDE.

If you want to create your first AWE project, choose one of the current Maven archetypes:

- `awe-boot-angular-archetype`
- `awe-boot-react-archetype`

### AngularJS archetype

```
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-angular-archetype \
 -DarchetypeVersion=[Archetype version] \
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT
```

### React archetype

```
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-react-archetype \
 -DarchetypeVersion=[Archetype version] \
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT 
```

### Prerequisites
You must have Maven 3.x installed on your computer as well as a **JDK 17** or higher

## Built With
* [Maven](https://maven.apache.org/) - Dependency Management
* [Spring framework](https://spring.io/) - AWE Spring boot starter
* [Angular JS](https://angularjs.org/) - Angular JS framework
* [Bootstrap](https://getbootstrap.com/) - Bootstrap web toolkit
* [Highcharts](https://www.highcharts.com/) - Interactive charts library

[![StackShare](https://img.shields.io/badge/tech-stack-0690fa.svg?style=flat)](https://stackshare.io/almis-informatica-financiera/aweframework)

## Main guides

* **[Introduction](website/docs/intro.md)**
* **[Getting Started](website/docs/getting-started.md)**
* **[Installation](website/docs/installation.md)**
* **Guides**
  * [Developer tools](website/docs/devtools-module.md)
  * [Scheduler guide](website/docs/guides/scheduler-guide.md)
  * [Notifier guide](website/docs/guides/notifier-guide.md)
  * [Selenium tests guide](website/docs/guides/selenium-test-guide.md)
  * [Printing engine guide](website/docs/guides/print-guide.md)
  * [Debugging guide](website/docs/guides/debugging-guide.md)

* **Migration guides**
  * [AWE 4.0.0](website/docs/guides/awe-4.0-migration-guide.md)
  
## Changelogs

Latest changelog file: [CHANGELOG.md](CHANGELOG.md)

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## License

All parts of AWE, **except the contents of the graphical charts library (HighCharts)**, are licensed
under Apache License v2.0 see the [LICENSE.md](LICENSE.md) file for details.
