---
id: notifier
title: Módulo de notificaciones
sidebar_label: Módulo de notificaciones
---

El módulo de notificación permite a tus usuarios suscribirse a las notificaciones en tu aplicación.

## Pasos para utilizarlo {#steps-to-use-it}

Para utilizar este módulo, se necesitan los siguientes pasos:

- Añade **las dependencias del módulo notificador de awe** al descriptor pom.xml.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-notifier-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

- Agregue las pantallas de notificación en su archivo `private.xml`:

```xml
<option name="user-settings" screen="user-settings" invisible="true"/>
<option name="notifier" label="MENU_NOTIFIER" icon="flash">
  <option name="subscriptions" screen="subscriptions" label="MENU_NOTIFIER_SUBSCRIPTIONS" icon="ticket">
    <option name="new-subscription" screen="new-subscription" invisible="true" />
    <option name="update-subscription" screen="update-subscription" invisible="true" />
  </option>
  <option name="notifications" screen="notifications" label="MENU_NOTIFIER_NOTIFICATIONS" icon="bell" />
</option>
```

- Incluya la herramienta de notificación en el archivo `home_navbar.xml`:

```xml
<tag type="ul" style="nav navbar-nav pull-right right-navbar-nav">
  ...
  <include target-screen="notification-panel" target-source="notification-panel"/>
  ...
</tag>
...
<info id="ButUsrAct" icon="user" initial-load="query" target-action="connectedUser">
  ...
  <include target-screen="notification-panel" target-source="user-settings"/>
  ...
</info>
```

- Configure el valor de la propiedad para añadir `awe-notifier` a la lista de módulos.

```properties
awe.application.module-list = APP, ..., awe-notifier, ..., awe
```

- Si utiliza `flyway`, añada las tablas del notificador al módulo de migración:

```properties
awe.database.migration-modules=AWE,...,NOTIFIER,...
```

- Por último, añada las propiedades de nombre y correo del remitente (`from`) para establecer el remitente del correo de notificaciones:

```properties
awe.notifier.from-name=Notifier
awe.notifier.from-email=notifier-sender@test.com
```