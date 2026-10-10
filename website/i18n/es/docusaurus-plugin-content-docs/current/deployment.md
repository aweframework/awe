---
id: deployment
title: Despliegue de aplicaciones AWE
sidebar_label: Despliegue
---

## Introducción {#intro}
El framework AWE, al igual que las aplicaciones Spring Boot, dispone de opciones de empaquetado flexibles que ofrecen una gran variedad de opciones a la hora de desplegar su aplicación. Puede desplegar aplicaciones AWE en diversas plataformas en la nube, en imágenes de contenedor (como Docker) o en máquinas virtuales/reales.

Esta sección cubre algunos de los escenarios de despliegue más comunes.

## Despliegue en Java Archive (JAR) como aplicación independiente {#deploying-in-java-archive-jar-as-a-standalone-application}

Las aplicaciones AWE pueden empaquetarse fácilmente en archivos JAR y desplegarse como aplicaciones independientes. Esto lo realiza el `spring-boot-maven-plugin`. El plugin se añade automáticamente al pom.xml al usar el arquetipo maven `awe-boot-angular-archetype`.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

Para empaquetar la aplicación en un único archivo jar (fat jar), ejecute el comando maven mvn package en el directorio del proyecto. Esto empaquetará la aplicación dentro de un archivo jar ejecutable con todas sus dependencias (incluido el contenedor de servlets embebido, si se trata de una aplicación web). Para ejecutar el archivo jar, use el siguiente comando estándar de la JVM `java -jar <jar-file-name>.jar`.

## Despliegue en servidor de aplicaciones {#deploying-in-application-server}
Si ejecuta su aplicación en un servidor de aplicaciones como Apache Tomcat, Jboss, Websphere, ... debe hacer unos pequeños cambios en su pom.xml.

1. Modifique su `pom.xml` para generar un paquete WAR en lugar de un JAR
```xml
<packaging>war</packaging>
```

2. Excluya el tomcat embebido marcando su ámbito (scope) como `provided`
```xml
<dependency> 
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-tomcat</artifactId>
  <scope>provided</scope>
</dependency>
```

3. Modifique su clase principal `AppBootApplication.java` para iniciar la aplicación en un contenedor de servlets independiente.

```java

@SpringBootApplication
public class AppBootApplication extends SpringBootServletInitializer {

    private static final Class<AppBootApplication> applicationClass = AppBootApplication.class;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(applicationClass);
    }

    /**
     * The goal of this method is only for running the application as a standalone
     * application, setting up an embedded Tomcat server.
     *
     * @param args Application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AppBootApplication.class, args);
    }
}
```

## Despliegue en contenedor Docker {#deploying-in-docker-container}

Antes de desplegar la aplicación en un contenedor Docker, primero empaquetaremos la aplicación en un archivo JAR (fat jar). Este proceso se ha explicado anteriormente, por lo que asumiremos que disponemos de un archivo jar.
En el primer paso, necesitamos construir una imagen de contenedor. Para ello, comenzamos creando un Dockerfile en el directorio raíz del proyecto como se indica a continuación:

Por ejemplo:

```shell
# Use an official Open jdk runtime as a parent image
FROM eclipse-temurin:17-jre-alpine
# Copy the current directory contents into the container at /app
ADD target/awe-demo.jar awe-demo.jar
# Volume of app logs
VOLUME /logs
# Volume of app data
VOLUME /tmp
# expose server port accept connections
EXPOSE 8080
# Execute jar
ENTRYPOINT ["java", "-Dspring.devtools.restart.enabled=false", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/awe-demo.jar"]
```

## Despliegue en la NUBE {#deploying-in-the-cloud}
Las aplicaciones AWE, al igual que los jars ejecutables de Spring Boot, están listas para los proveedores de PaaS (Platform-as-a-Service) en la nube más populares. Estos proveedores suelen exigir que "traiga su propio contenedor". Gestionan procesos de aplicación (no aplicaciones Java en concreto), por lo que necesitan una capa intermedia que adapte su aplicación a la noción de proceso en ejecución de la nube.

Idealmente, su aplicación, como un jar ejecutable de Spring Boot, lleva empaquetado todo lo que necesita para ejecutarse.

:::tip Información
Puede leer más documentación sobre el despliegue **[aquí](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#cloud-deployment)**
:::


