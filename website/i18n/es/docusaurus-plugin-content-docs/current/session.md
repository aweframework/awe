---
id: session
title: Configuración de sesión
sidebar_label: Configuración de sesión
---

AWE permite a los desarrolladores configurar la sesión de la aplicación de dos maneras: utilizando el `HttpSession` por defecto (sesión básica)
que proporciona el servidor de aplicaciones o utilizando Spring Session en su lugar.

## Sesión básica {#basic-session}

AWE, por defecto, utiliza un objeto `HttpSession` básico del servidor de aplicaciones, por lo que no es necesaria ninguna acción para configurar esta opción.
Esta opción es útil cuando se despliega la aplicación en un entorno en clúster y el servidor balanceador utiliza la cookie `JSESSIONID`
generada por el servidor de aplicaciones.

## Spring session {#spring-session}

Spring session permite desplegar la aplicación utilizando un almacenamiento de sesión compartido en un servidor, como REDIS, MONGO, HAZELCAST o
incluso conexiones JDBC. Para activar el uso de spring session en su aplicación, simplemente añada la siguiente dependencia
a su archivo `pom.xml`:  

```xml
<!-- Spring session -->
<dependency>
  <groupId>org.springframework.session</groupId>
  <artifactId>spring-session</artifactId>
</dependency>
```

La configuración de spring session se define en `session.properties`. Estas propiedades se definen en la [página de configuración de propiedades](properties#awe-session-properties).