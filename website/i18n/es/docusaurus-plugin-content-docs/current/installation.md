---
id: installation
title: Instalación
sidebar_label: Instalación
---

Para instalar una aplicación basada en AWE basta con desplegarla en un servidor de aplicaciones, como Apache Tomcat, Wildfly (antiguo JBoss) o IBM WebSphere

## Optimización {#optimization}

Existen varios ajustes que pueden definirse en el servidor de aplicaciones para optimizar el tiempo de acceso entre el navegador del cliente y el servidor, y entre el servidor y las bases de datos:

### Compresión del servidor {#server-compression}

La compresión del servidor reduce el tamaño de los paquetes enviados al navegador del cliente comprimiéndolos en GZIP, formato que pueden descomprimir todos los navegadores modernos.

Para activar este ajuste (en Tomcat) utilice los siguientes atributos en el archivo **server.xml**:

```xml
<Connector port="[connectorPort]" protocol="HTTP/1.1"
  connectionTimeout="[connectionTimeout]"
  redirectPort="[redirectPort]" compression="on" 
  compressionMinSize="128" 
  noCompressionUserAgents="gozilla, traviata" 
  compressableMimeType="text/html,text/xml,application/json,text/json,text/x-json,text/javascript,application/javascript,application/x-javascript,text/css,application/font-sfnt,image/svg+xml,application/x-font-ttf"/>
```

### Datasources {#datasources}

Un datasource es un pool de conexiones gestionado por el servidor que acelera el acceso a la base de datos. La conexión en Apache Tomcat tiene dos pasos:

**server.xml**: Define la conexión a la base de datos

```xml
<Resource name="[resourceName]" auth="Container"
  type="javax.sql.DataSource" driverClassName="com.microsoft.sqlserver.jdbc.SQLServerDriver"
  url="[jdbcUrl]"
  username="[databaseUsername]" password="[databasePassword]" maxActive="20" maxIdle="-1"
  maxWait="-1" removeAbandoned="true" logAbandoned="true" validationQuery="select 1 from ope"/>
```

**context.xml**: Define el punto de acceso del servidor para permitir el acceso al datasource

```xml
<ResourceLink global="[resourceName]" name="[datasourceName]" type="javax.sql.DataSource"/>
```

> **Nota:** Estos ajustes de optimización son *solo* para Apache Tomcat. Wildfly y WebSphere tienen sus propios ajustes para habilitar la compresión del servidor y los datasources

### Endurecimiento del servidor de aplicaciones {#application-server-hardening}

#### Apache Tomcat {#apache-tomcat}

Recomendamos seguir estos pasos para mejorar la seguridad del servidor de aplicaciones.
*  Siga las recomendaciones de seguridad de OWASP para Apache Tomcat https://www.owasp.org/index.php/Securing_tomcat
*  Configure la conexión HTTPS en Tomcat https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html. Puede que necesite certificados SSL de pago (o certificados emitidos por su CA).
*  Habilite los registros de acceso extendidos. Edite el archivo server.xml y compruebe si el siguiente código está habilitado dentro de la etiqueta host:

```xml
<Valve className="org.apache.catalina.valves.AccessLogValve"
    directory="logs" prefix="localhost_access_log." suffix=".txt"
    pattern="common" resolveHosts="false"/>
```

*  Habilite la protección contra clickjacking (solo para Tomcat versión 8 o superior).
**web.xml**: Descomente el siguiente código

```xml
<filter>
  <filter-name>httpHeaderSecurity</filter-name>
  <filter-class>org.apache.catalina.filters.HttpHeaderSecurityFilter</filter-class>
  <async-supported>true</async-supported>
</filter>
```

 Y añada el siguiente código justo después de la sección descomentada:

 ```xml
<filter-mapping>
  <filter-name>httpHeaderSecurity</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>
```

*  Si HTTPS está habilitado, añada una regla para redirigir las peticiones HTTP a HTTPS. En el archivo web.xml añada el siguiente código al final de la etiqueta web-app (dentro de ella):

```xml
<security-constraint>
 <web-resource-collection>
   <web-resource-name>Secure SSL</web-resource-name>
   <url-pattern>/*</url-pattern>
 </web-resource-collection>
 <user-data-constraint>
   <transport-guarantee>CONFIDENTIAL</transport-guarantee>
 </user-data-constraint>
</security-constraint>
```


