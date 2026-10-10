---
id: v4-migration
title: Guía de migración a v4.0
sidebar_label: Guía de migración a v4.0
---

## **Estructura de la aplicación** {#application-structure}

* Cambie la estructura del proyecto a una estructura de proyecto Spring:

> * **[application-name]** -> Archivos del proyecto (pom.xml, package.json...)
    >
* src
  >
* main
  >
* **java** -> ApplicationBoot + clases Java
>          * **resources** -> Propiedades
             >
* application/[application-name] -> Archivos XML
>              * config -> Propiedades de AWE sobrescritas
>              * js -> Archivos Javascript
>              * css -> Archivos CSS
>              * less -> Archivos LESS
>              * schemas -> Esquemas XSD
>              * static -> Imágenes/Fuentes
>              * webpack -> Configuración de Webpack
>              * sql -> Archivos SQL de inicialización
>      * test
         >
* *java* -> Pruebas JUnit
>          * *resources* -> Propiedades de prueba
>          * *selenium* -> Suites de Selenium

## **Maven** {#maven}

* Cambie el archivo POM:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-starter-parent</artifactId>
    <version>4.0.7</version>
    <relativePath/>
  </parent>

  <artifactId>[project-name]</artifactId>
  <groupId>[project-group]</groupId>
  <version>[project-version]</version>

  <name>[Project name]</name>
  <description>[Project description]</description>

  <properties>
    <application.acronym>[project-acronym]</application.acronym>
    <start-class>[project-group].AppBootApplication</start-class>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <project.build.frontend>${project.build.directory}/classes/static/</project.build.frontend>
    <java.version>17</java.version>
  </properties>

  <dependencies>

    <!-- AWE -->
    <dependency>
      <groupId>com.almis.awe</groupId>
      <artifactId>awe-spring-boot-starter</artifactId>
    </dependency>

    <dependency>
      <groupId>com.almis.awe</groupId>
      <artifactId>awe-client-angular</artifactId>
    </dependency>

    <!-- JDBC Drivers (ADD ONLY WHAT YOU NEED) -->

    <!-- ORACLE -->
    <dependency>
      <groupId>com.oracle</groupId>
      <artifactId>ojdbc6</artifactId>
      <version>11.2.0.3</version>
      <scope>runtime</scope>
    </dependency>

    <!-- SQL SERVER -->
    <dependency>
      <groupId>com.microsoft.sqlserver</groupId>
      <artifactId>sqljdbc4</artifactId>
      <version>4.0.0</version>
      <scope>runtime</scope>
    </dependency>

    <!-- HSQL -->
    <dependency>
      <groupId>org.hsqldb</groupId>
      <artifactId>hsqldb</artifactId>
      <version>2.3.3</version>
      <scope>runtime</scope>
    </dependency>

  </dependencies>

  <build>
    <finalName>[project-name]</finalName>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>
    <plugins>

      <!-- Copy static files -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <phase>generate-resources</phase>
            <id>unpack awe-generic-screens</id>
            <goals>
              <goal>unpack-dependencies</goal>
            </goals>
            <configuration>
              <includeGroupIds>com.almis.awe</includeGroupIds>
              <includeArtifactIds>awe-generic-screens</includeArtifactIds>
              <includes>schemas/**,docs/**</includes>
              <outputDirectory>${project.build.frontend}</outputDirectory>
            </configuration>
          </execution>
          <execution>
            <phase>generate-resources</phase>
            <id>unpack awe-client-angular</id>
            <goals>
              <goal>unpack-dependencies</goal>
            </goals>
            <configuration>
              <includeGroupIds>com.almis.awe</includeGroupIds>
              <includeArtifactIds>awe-client-angular</includeArtifactIds>
              <includes>images/**,fonts/**,js/**,css/**,less/**</includes>
              <outputDirectory>${project.build.frontend}</outputDirectory>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <!-- Copy images -->
      <plugin>
        <artifactId>maven-resources-plugin</artifactId>
        <version>3.0.2</version>
        <executions>
          <execution>
            <id>copy-images</id>
            <phase>prepare-package</phase>
            <goals>
              <goal>copy-resources</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.build.directory}/classes/static/images/</outputDirectory>
              <resources>
                <resource>
                  <directory>${project.basedir}/src/main/resources/images/</directory>
                </resource>
              </resources>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <!-- Spring boot -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
      </plugin>

      <!-- Frontend generation -->
      <plugin>
        <groupId>com.github.eirslett</groupId>
        <artifactId>frontend-maven-plugin</artifactId>
        <version>1.6</version>
        <executions>
          <execution>
            <id>install node and yarn</id>
            <goals>
              <goal>install-node-and-yarn</goal>
            </goals>
            <configuration>
              <nodeVersion>v6.9.1</nodeVersion>
              <yarnVersion>v1.6.0</yarnVersion>
            </configuration>
          </execution>
          <execution>
            <id>yarn install</id>
            <goals>
              <goal>yarn</goal>
            </goals>
            <configuration>
              <arguments>install</arguments>
            </configuration>
          </execution>
          <execution>
            <id>webpack</id>
            <goals>
              <goal>webpack</goal>
            </goals>
            <configuration>
              <arguments>--output-path "${project.build.frontend}"</arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <!-- Xml validation -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>xml-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>validate</id>
            <phase>compile</phase>
            <goals>
              <goal>validate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <!-- Build an executable JAR -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.0.2</version>
        <configuration>
          <archive>
            <manifest>
              <addClasspath>true</addClasspath>
              <classpathPrefix>lib/</classpathPrefix>
              <mainClass>${start-class}</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

## **Generación de Javascript y CSS** {#javascript--css-generation}

* Defina un archivo de webpack para generar javascript y css personalizados

## **Archivos XML** {#xml-files}

### Estructura de rutas de recursos {#resource-path-structure}

AWE 4 introduce un nivel adicional `application/` en la ruta de recursos. Todos los archivos XML (pantallas, consultas,
locales, etc.) deben colocarse bajo `application/[module]/` en lugar de directamente bajo `[module]/`:

| Ruta en AWE 3 | Ruta en AWE 4 |
|------------|------------|
| `src/main/resources/{module}/global/Queries.xml` | `src/main/resources/application/{module}/global/Queries.xml` |
| `src/main/resources/{module}/global/Maintain.xml` | `src/main/resources/application/{module}/global/Maintain.xml` |
| `src/main/resources/{module}/global/Services.xml` | `src/main/resources/application/{module}/global/Services.xml` |
| `src/main/resources/{module}/global/Enumerate.xml` | `src/main/resources/application/{module}/global/Enumerate.xml` |
| `src/main/resources/{module}/screen/` | `src/main/resources/application/{module}/screen/` |
| `src/main/resources/{module}/local/` | `src/main/resources/application/{module}/locale/` |

### **Pantallas** {#screens}

* Expresiones regulares

    * La dependencia `source-type="action"` ya no es necesaria. Simplemente añada las acciones de su dependencia y se
      lanzarán cuando se cumplan las condiciones.
    * Los atributos `source-type="none"` y `target-type="none"` son valores por defecto, por lo que no necesita
      establecerlos.
    * Elimine `source-type="action"`, `source-type="none"` y `target-type="none"`:

```regexp
^(.*)source\-type\s*=\s*["']action["']\s*(\S+.*)$ => $1$2
^(.*)source\-type\s*=\s*["']none["']\s*(\S+.*)$ => $1$2
^(.*)target\-type\s*=\s*["']none["']\s*(\S+.*)$ => $1$2
```

> **Nota:** `XXXX => YYYY` significa que debe buscar la expresión `XXXX` y sustituirla por la expresión `YYYY`

* Sustitución directa:

    * Corregir las condiciones de las dependencias:

```regexp
condition="lte" => condition="le"
condition="gte" => condition="ge"
```

* De paginación de rejilla a paginación gestionada de rejilla:

```regexp
pagination="true" => managed-pagination="true"
```

* De la acción `control-empty-cancel` a la acción `control-unique-cancel`:

```regexp
control-empty-cancel => control-unique-cancel
```

* La clase CSS `modal-xl` se ha renombrado a `modal-xlg`:

```regexp
modal-xl => modal-xlg
```

### **Query y Maintain** {#query--maintain}

* Expresiones regulares

    * Corregir los filtros de query y maintain:

```regexp
^(\s*<filter.*\s+)value(.*/>.*)$ => $1left-variable$2 -> Replace by left-variable and add the variable name
^(\s*<filter.*\s+)variable(.*/>.*)$ => $1right-variable$2
^(\s*<filter.*\s+)counterfield(.*/>.*)$ => $1right-field$2
^(\s*<filter.*\s+)countertable(.*/>.*)$ => $1right-table$2
^(\s*<filter.*\s+)field(.*/>.*)$ => $1left-field$2
^(\s*<filter.*\s+)table(.*/>.*)$ => $1left-table$2
^(\s*<)field(.*\s+value.*/>.*)$ => $1constant$2
```

* Sustitución directa:

    * Corregir las condiciones de las query:

```regexp
condition="=" => condition="eq"
condition="!=" => condition="ne"
condition="<>" => condition="ne"
condition="LIKE" => condition="like"
condition="NOT LIKE" => condition="not like"
condition="&gt;" => condition="gt"
condition="&gt;=" => condition="ge"
condition="&lt;" => condition="lt"
condition="&lt;=" => condition="le"
condition="IS NULL" => condition="is null"
condition="IS NOT NULL" => condition="is not null"
condition="IN" => condition="in"
condition="NOT IN" => condition="not in"
condition="EXISTS" => condition="exists"
condition="NOT EXISTS" => condition="not exists"
```

* Los identificadores de variable no se pueden usar en los alias de campo.
* Las definiciones de CASE y CONCAT deben definirse ahora con las nuevas etiquetas `<case>` y `<operation>` de AWE.
  Más información en [Definiciones de query](../api/query-definition.md).
* Los valores estáticos deben definirse como etiquetas `<constant>`
* AWE ahora ofrece más flexibilidad al generar queries y filtros:

````xml

<query id="testRowNumber">
  <table id="ope"/>
  <field id="l1_nom" alias="name"/>
  <over alias="rowNumber">
    <field function="ROW_NUMBER"/>
  </over>
  <order-by field="l1_nom" type="ASC"/>
</query>

<query id="testCoalesce">
<table id="ope"/>
<field id="l1_nom" alias="name"/>
<operation operator="COALESCE" alias="nameNotNull">
  <field id="l1_trt"/>
  <constant type="NULL"/>
  <field id="l1_nom"/>
</operation>
<where>
  <filter condition="eq" ignorecase="true">
    <left-operand>
      <field id="l1_nom"/>
    </left-operand>
    <right-operand>
      <constant value="test"/>
    </right-operand>
  </filter>
</where>
<order-by field="l1_nom" type="ASC"/>
</query>

<query id="testCaseWhenElse">
<table id="AweThm"/>
<case alias="value">
  <when left-field="Nam" condition="eq" right-variable="sunset">
    <then>
      <constant value="1" type="INTEGER"/>
    </then>
  </when>
  <when left-field="Nam" condition="eq" right-variable="sunny">
    <then>
      <constant value="2" type="INTEGER"/>
    </then>
  </when>
  <when left-field="Nam" condition="eq" right-variable="purple-hills">
    <then>
      <constant value="3" type="INTEGER"/>
    </then>
  </when>
  <else>
    <constant value="0" type="INTEGER"/>
  </else>
</case>
<case alias="label">
  <when condition="eq">
    <left-operand>
      <field id="Nam"/>
    </left-operand>
    <right-operand>
      <constant value="sunset"/>
    </right-operand>
    <then>
      <constant value="SUNSET"/>
    </then>
  </when>
  <when condition="eq">
    <left-operand>
      <field id="Nam"/>
    </left-operand>
    <right-operand>
      <constant value="sunny"/>
    </right-operand>
    <then>
      <constant value="SUNNY"/>
    </then>
  </when>
  <when condition="eq">
    <left-operand>
      <field id="Nam"/>
    </left-operand>
    <right-operand>
      <constant value="purple-hills"/>
    </right-operand>
    <then>
      <constant value="PURPLE-HILLS"/>
    </then>
  </when>
  <else>
    <constant value="other"/>
  </else>
</case>
<order-by field="Nam" type="ASC" nulls="FIRST"/>
</query>

<query id="TestFieldDateFunctions">
<table id="ope" alias="awe"/>
<field id="dat_mod" table="awe" alias="year" function="YEAR"/>
<field id="dat_mod" table="awe" alias="month" function="MONTH"/>
<field id="dat_mod" alias="day" function="DAY"/>
<field id="dat_mod" alias="hour" function="HOUR"/>
<field id="dat_mod" alias="minute" function="MINUTE"/>
<field id="dat_mod" alias="second" function="SECOND"/>
<where>
  <filter left-field="l1_nom" condition="eq" ignorecase="true">
    <right-operand>
      <constant value="test"/>
    </right-operand>
  </filter>
</where>
<order-by field="dat_mod" table="awe" function="YEAR"/>
</query>
````

### **Servicios** {#services}

* Las referencias a clases de servicio Java en las definiciones de servicio `<java>` deben actualizarse a la nueva
  convención de nombres `@Service`. AWE 4 ya no usa el patrón `Controller`/`Manager`; todas las clases del backend
  son `@Service`:

```regexp
classname="([a-z.]+)\.services\.controller\.(\w+)Controller" => classname="$1.service.$2Service"
classname="([a-z.]+)\.services\.manager\.(\w+)Manager" => classname="$1.service.$2Service"
```

  Ejemplo:

```xml
<!-- AWE 3 -->
<service id="MyDataService">
  <java classname="com.almis.myapp.services.controller.MyDataController" method="getMyData">
    <service-parameter name="code" type="STRING"/>
  </java>
</service>

<!-- AWE 4 -->
<service id="MyDataService">
  <java classname="com.almis.myapp.service.MyDataService" method="getMyData">
    <service-parameter name="code" type="STRING"/>
  </java>
</service>
```

* Las llamadas a servicios web se han cambiado por llamadas a microservicios:

Ejemplos:

```xml

<service id="simpleGETMicroservice">
  <microservice name="alu-microservice" method="GET" endpoint="/invoke" content-type="JSON"/>
</service>

<service id="simpleGETMicroservice2">
<microservice name="alu-microservice2" method="GET" endpoint="/invoke" content-type="JSON"/>
</service>

<service id="simpleGETMicroserviceWithWrapper">
<microservice name="alu-microservice" method="GET" endpoint="/invoke"
              wrapper="com.almis.awe.service.dto.ServiceDataWrapper" content-type="JSON"/>
</service>

<service id="simpleGETMicroserviceWithParameter">
<microservice name="alu-microservice" method="GET" endpoint="/invoke" content-type="JSON">
  <service-parameter name="param1" type="STRING"/>
</microservice>
</service>

<service id="simpleGETMicroserviceWithWildcard">
<microservice name="alu-microservice" method="GET" endpoint="/invoke/{param1}" content-type="JSON">
  <service-parameter name="param1" type="STRING"/>
</microservice>
</service>

<service id="simpleGETMicroserviceWithWildcardAndParameter">
<microservice name="alu-microservice" method="GET" endpoint="/invoke/{param1}" content-type="JSON">
  <service-parameter name="param1" type="STRING"/>
  <service-parameter name="param2" type="STRING"/>
</microservice>
</service>

<service id="simplePOSTMicroserviceWithParameters">
<microservice name="alu-microservice" method="POST" endpoint="/invoke" content-type="JSON">
  <service-parameter name="param1" type="STRING"/>
  <service-parameter name="param2" type="STRING"/>
</microservice>
</service>

<service id="simplePUTMicroserviceWithParameters">
<microservice name="alu-microservice" method="PUT" endpoint="/invoke" content-type="JSON">
  <service-parameter name="param1" type="STRING"/>
  <service-parameter name="param2" type="STRING"/>
</microservice>
</service>

<service id="simpleDELETEMicroserviceWithWildcard">
<microservice name="alu-microservice" method="DELETE" endpoint="/invoke/{param1}" content-type="JSON">
  <service-parameter name="param1" type="STRING"/>
</microservice>
</service>
```

### **Locales** {#locales}

* **Renombre** los archivos `Local-XX.xml` a `Locale-xx-XX.xml` usando la correspondencia de códigos de locale:

| Nombre de archivo en AWE 3 | Nombre de archivo en AWE 4 |
|----------------|----------------|
| `Local-ES.xml` | `Locale-es-ES.xml` |
| `Local-EN.xml` | `Locale-en-GB.xml` |
| `Local-FR.xml` | `Locale-fr-FR.xml` |
| `Local-PA.xml` | `Locale-es-PA.xml` |
| `Local-CA.xml` | `Locale-ca-ES.xml` |

* Sustitución directa (etiqueta raíz y etiquetas de entrada):

```regexp
<locals => <locales
</locals> => </locales>
<local => <locale
</local> => </locale>
```

## **Propiedades** {#properties}

Vuelva a codificar las propiedades codificadas con `ENC(xxxx)` en la pantalla de utilidad de cifrado (Encrypt).

## **Archivos Java** {#java-files}

### Refactorización de paquetes {#package-refactorization}

* Sustitución directa:
    * Paquetes

```regexp
com.almis.awe.core.services.data.global.XMLWrapper => com.almis.awe.model.entities.XMLFile
com.almis.awe.core.services.data.global.XMLElement => com.almis.awe.model.entities.XMLFile
com.almis.awe.core.services.data.service.ServiceData => com.almis.awe.model.dto.ServiceData
com.almis.awe.core.services.controller.DataController => com.almis.awe.service.QueryService
com.almis.awe.core.services.controller.MaintainController => com.almis.awe.service.MaintainService
com.almis.awe.core.util.DateUtil => com.almis.awe.model.util.data.DateUtil
com.almis.awe.core.beans.ComponentAddress => com.almis.awe.model.entities.actions.ComponentAddress
com.almis.awe.core.services.data.action.ClientAction => com.almis.awe.model.entities.actions.ClientAction
com.almis.awe.dto => com.almis.awe.model.dto
com.almis.awe.core.services.data.global => com.almis.awe.model.dto
com.almis.awe.type => com.almis.awe.model.type
com.almis.awe.core.services.data.query => com.almis.awe.model.entities.queries
com.almis.awe.core.services.data.maintain => com.almis.awe.model.entities.maintain
com.almis.awe.core.exception => com.almis.awe.exception
XMLElement => XMLWrapper
AWEConstants => AweConstants
AweConstants.PARAMETER_MAX => AweConstants.COMPONENT_MAX
```

### Migración a Jakarta EE (Spring Boot 3) {#jakarta-ee-migration-spring-boot-3}

AWE 4 sobre Spring Boot 3 requiere **Jakarta EE 9+**. Todos los imports `javax.*` deben sustituirse por `jakarta.*`:

```regexp
javax.persistence. => jakarta.persistence.
javax.servlet. => jakarta.servlet.
javax.validation. => jakarta.validation.
javax.transaction. => jakarta.transaction.
javax.annotation. => jakarta.annotation.
```

> **Nota:** Esto afecta a todas las entidades JPA (`@Entity`, `@Table`, `@Column`, `@Id`...), los filtros de servlet,
> Bean Validation (`@NotNull`, `@Size`...) y las anotaciones de transacción (`@Transactional`).

* Logging
    1. Elimine com.almis.awe.core.util.LogUtil
    2. Importe `org.apache.logging.log4j.LogManager` y `org.apache.logging.log4j.Logger`
    3. Cree un campo de logger estático:

```java
  // Logger
private static Logger logger=LogManager.getLogger(MyClass.class);
```

4. Use el logger estático. Por ejemplo:

```java
  logger.log(Level.INFO,"[{}] No books defined for this treatment",treatment.getID());
```

* Logger alternativo: Lombok

1. Añada la anotación `@Slf4j` sobre la clase:

 ```java
@Slf4j
public MyClass{
        ...
        } 
```

2. Use el logger de lombok:

```java
  log.error("My error message {}",moreInformationInVariables,exception);
```

### Paquetes de AWE {#awe-packages}

* Hay **dos paquetes principales** en **AWE 4.0**: `awe-spring-boot-starter` y `awe-model`.
    * **awe-spring-boot-starter** es el **paquete núcleo** de AWE. Las aplicaciones web basadas en AWE deben importar
      este paquete.
        * Para llamar a los servicios de AWE, inyecte con autowire los servicios de `com.almis.awe.services` (ya no hay
          llamadas a Controller)
    * **awe-model** es el **paquete de interfaz** de AWE. Las aplicaciones relacionadas con AWE (módulos de
      comunicación, microservicios, etc.) pueden importar este paquete para acceder a las clases de interfaz.

### Los servicios Java deben migrarse a la arquitectura Spring {#java-services-must-be-migrated-to-spring-architecture}

* Elimine todos los controllers si no hacen nada
* La estructura de clases de AWE ha cambiado radicalmente. Revise sus imports de Java
* Mueva el paquete `manager` al paquete `service`
* Renombre todas las clases **Xxx**Manager.java a clases **Xxx**Service.java
* Añada la anotación `@Service` a las clases **Xxx**Service.java
* Use la metodología de Spring (constructores con `@Autowired`, `@Value` para obtener propiedades, etc.)
* Haga que todas las clases **Xxx**Service extiendan de `ServiceConfig` si usan:
    * `com.almis.awe.core.singleton.LocalSingleton` => Extienda de `com.almis.awe.config.ServiceConfig` y
      llame a los métodos `getLocale`
    * `com.almis.awe.core.singleton.PropertySingleton` => Extienda de `com.almis.awe.config.ServiceConfig` y
      llame a los métodos `getProperty` (y mejor aún, use `@Value` en lugar de los métodos `getProperty`)
    * `com.almis.awe.core.services.controller.SessionController` => Extienda de `com.almis.awe.config.ServiceConfig` y
      llame a `getSession`
    * `com.almis.awe.core.services.data.global.Context` => Extienda de `com.almis.awe.config.ServiceConfig` y
      llame a `getRequest` para obtener los parámetros de la petición
    * Elimine también el acceso a ContextUtil.getContext()
    * Use también `getRequest().getTargetAction()` para obtener el destino de la acción llamada.
    * Más información sobre [Obtención de locales](#locale-retrieval), [obtención de propiedades](#property-retrieval)
      , [obtención de sesión](#session-retrieval) y [obtención de petición](#request-retrieval).
* Adapte la autenticación personalizada si está sobrescrita en la aplicación
* Use `QueryService` en lugar de `DataController`. Todos los métodos `launchQuery` ahora devuelven beans `ServiceData`
  en lugar de `DataList`. Puede obtener el `DataList` con el método `serviceData.getDataList()`.
* Use `MaintainService` en lugar de `MaintainController`.

#### Ejemplo completo de migración (AWE 3 → AWE 4) {#complete-migration-example-awe-3--awe-4}

```java
// AWE 3 — Controller + Manager pattern
public class MyController {
  public ServiceData myMethod() throws AWException {
    return new MyManager().myMethod();
  }
}

public class MyManager {
  public ServiceData myMethod() throws AWException {
    Context ctx = ContextUtil.getContext();
    String param = ctx.getParameter("myParam").textValue();
    DataList result = new DataController().launchQuery("MyQuery");
    LogUtil.log(MyManager.class.getName(), Level.INFO, "Query done");
    return new ServiceData();
  }
}

// AWE 4 — Single @Service
@Slf4j
@Service
public class MyService extends ServiceConfig {

  private final QueryService queryService;

  @Autowired
  public MyService(QueryService queryService) {
    this.queryService = queryService;
  }

  public ServiceData myMethod() throws AWException {
    String param = getRequest().getParameterAsString("myParam");
    ServiceData result = queryService.launchPrivateQuery("MyQuery");
    DataList dataList = result.getDataList();
    log.info("Query done");
    return new ServiceData();
  }
}
```

### Obtención de locales {#locale-retrieval}

* Al extender de `ServiceConfig` obtiene acceso a los métodos `getLocale`:

```java
  getLocale("ERROR_TITLE_LAUNCHING_MAINTAIN");
```

* Puede pasar variables para sustituir en el locale simplemente añadiéndolas como argumentos:

```java
  getLocale("ERROR_TITLE_LAUNCHING_MAINTAIN",treatment.getID(),task.getID());
```

### Obtención de propiedades {#property-retrieval}

* Al extender de `ServiceConfig` obtiene acceso a los métodos `getProperty`:

```java
  getProperty("var.trt.thd.sug.tim",100);
```

* De todos modos, es más legible y rápido obtener las propiedades a la manera de Spring:

```java
  @Value("${var.trt.thd.sug.tim:100}")
private Integer suggestTime;
```

:::caution Elimine los envoltorios de propiedades específicos de la aplicación
**No** inyecte beans de propiedades a nivel de aplicación (p. ej., un componente personalizado `BaseConfigProperties` o
`AppProperties`) solo para llamar a `getProperty()`. Si la clase ya extiende `ServiceConfig`, llame a `getProperty()`
directamente — no hace falta ninguna inyección adicional:

```java
// AWE 3 — unnecessary intermediary
@Autowired
private BaseConfigProperties baseConfigProperties;
String value = baseConfigProperties.getProperty("my.prop");

// AWE 4 — use ServiceConfig directly (if class extends ServiceConfig)
String value = getProperty("my.prop");
// or, even better, use @Value injection
@Value("${my.prop:defaultValue}")
private String myProp;
```
:::

### Obtención de sesión {#session-retrieval}

* Al extender de `ServiceConfig` obtiene acceso a los métodos `getSession`:

```java
  getSession().getParameter(AweConstants.SESSION_DATABASE);
```

### Obtención de petición {#request-retrieval}

* Al extender de `ServiceConfig` obtiene acceso a los métodos `getRequest` en lugar de obtenerlos de `Context`:

```java
  getRequest().getParameter(AweConstants.PARAMETER_MAX).textValue();
```

o

```java
  getRequest().getParameterAsString(AweConstants.PARAMETER_MAX);
```

* Para preparar variables para una llamada a query o maintain desde un servicio que extiende `ServiceConfig`, construya
  una instantánea explícita de parámetros y páselos al lanzador:

```java
  ObjectNode parameters = getMutableRequestParameters();
  putRequestParameter(parameters, "opeId", id);

  queryService.launchQuery("MyQuery", parameters);
  maintainService.launchMaintain("MyMaintain", parameters);
```

Use `QueryUtil` directamente solo desde código de más bajo nivel que no extienda `ServiceConfig`.

### Tipo Datalist {#datalist-type}

* El método `getRows` de DataList ha cambiado su firma de `ArrayList<HashMap<String, CellData>>` a una firma más
  genérica: `List<Map<String, CellData>>`.

### Cambios en la API de DataList {#datalist-api-changes}

Se han eliminado varios métodos de instancia de `DataList` y se han sustituido por métodos de utilidad estáticos en `DataListUtil`:

| AWE 3 (método de instancia) | AWE 4 (método estático de `DataListUtil`) |
|--------------------------|---------------------------------------|
| `dataList.getCellData(rowIndex, columnName)` | `DataListUtil.getCellData(dataList, rowIndex, columnName)` |
| `dataList.addColumn(name, valueList, type)` | `DataListUtil.addColumn(dataList, name, valueList)` |
| `DataListUtil.getJSONColumnValues(dataList, col)` | `DataListUtil.getColumnAsArrayNode(dataList, col)` |
| `dataList.filter(columnName, value)` | `dataList.getRows().removeIf(row -> !value.equals(DataListUtil.getCellData(dataList, row, columnName)))` |
| `dataList.getRow(index)` | `dataList.getRows().get(index)` |
| `dataList.getColumn(name)` | Stream sobre `dataList.getRows()` |

Import necesario:

```java
import com.almis.awe.model.util.data.DataListUtil;
```

**Ejemplo de filtrado de filas:**

```java
// AWE 3
dataList.filter("status", getLocale("STATUS_ACTIVE"));

// AWE 4
String filterValue = getLocale("STATUS_ACTIVE");
dataList.getRows().removeIf(row -> {
  CellData cell = row.get("status");
  return cell == null || !filterValue.equals(cell.getStringValue());
});
```

**Ejemplo de lectura del valor de una celda:**

```java
// AWE 3
String value = dataList.getCellData(0, "myColumn").getStringValue();

// AWE 4
String value = DataListUtil.getCellData(dataList, 0, "myColumn").getStringValue();
```

**Ejemplo de adición de una columna:**

```java
// AWE 3
dataList.addColumn("myColumn", valueList, "STRING");

// AWE 4
DataListUtil.addColumn(dataList, "myColumn", valueList);
```

### Beans {#beans}

* Use un constructor de copia en lugar de la interfaz Cloneable:

```java
public class MyClass implements Copyable<MyClass> {

  private String myProp1;
  private String myProp2;

  /**
   * Default constructor 
   */
  public MyClass() {
  }

  /**
   * Copy constructor 
   */
  public MyClass(MyClass other) {
    this.myProp1 = other.myProp1;
    this.myProp2 = other.myProp2;
  }

  /**
   * Copy method
   * @return Copy of this object
   */
  public MyClass copy() {
    return new MyClass(this);
  }
}
```

o use Lombok:

```java

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MyClass {
  private String myProp1;
  private String myProp2;
}
```

... y clónelo con el builder:

```java
MyClass myNewClass=myOldClass.toBuilder().build();
```

* Elimine de los beans todos los métodos que usen cualquier clase externa. Un bean solo debería tener métodos que
  interactúen sobre sus propios campos.

### La acción de cliente `fill` ahora tiene un único parámetro: `datalist`, que contendrá el DataList completo: {#fill-client-action-now-has-only-one-parameter-datalist-which-will-contain-the-full-datalist}

```java
serviceData.addClientAction(new ClientAction("fill")
        .setAddress(address)
        .addParameter("datalist",datalist)
        .setAsync(true));
```

... pero es más sencillo usar los nuevos builders de ClientAction:

```java
serviceData.addClientAction(new FillActionBuilder(address,datalist).setAsync(true).build());
```

### `ClientAction` — parámetros booleanos {#clientaction--boolean-parameters}

Los métodos setter de `ClientAction` para `async` y `silent` ahora aceptan **`boolean`** en lugar de `String`:

```java
// AWE 3
action.setAsync("true");
action.setSilent("true");

// AWE 4
action.setAsync(true);
action.setSilent(true);
```

Esto también se aplica al encadenar en los builders:

```java
new FillActionBuilder(address, dataList).setAsync(true).setSilent(true).build();
```

### `SelectActionBuilder` — rellenar componentes select/combo {#selectactionbuilder--populate-selectcombo-components}

Use `SelectActionBuilder` para rellenar un componente select desde un servicio:

```java
import com.almis.awe.model.util.data.builder.SelectActionBuilder;

// AWE 4
serviceData.addClientAction(new SelectActionBuilder(address, dataList).build());
```

### `DownloadActionBuilder` — servir descargas de archivos {#downloadactionbuilder--serve-file-downloads}

Use `DownloadActionBuilder` para lanzar una respuesta de descarga de archivo:

```java
import com.almis.awe.model.util.data.builder.DownloadActionBuilder;

// AWE 4
FileData fileData = new FileData(fileName, fileSize, "application/octet-stream")
    .setFilePath(tempPath);
serviceData.addClientAction(new DownloadActionBuilder(fileData).build());
```

### El bean `FileData` tiene ahora una nueva implementación: {#filedata-bean-has-now-a-new-implementation}

Use `FileUtil.fileDataToString` para generar un token:

```java
public String fileDataToString(FileData fileData)
```

Use `FileUtil.stringToFileData` para obtener un bean FileData a partir de un token:

```java
public FileData stringToFileData(String fileStringEncoded)
```

> **Nota:** FileUtil es un @Component inyectable con autowire

## **Servicios web** :arrow_right_hook: **Microservicios** {#web-services-arrow_right_hook-microservices}

* Adapte la interfaz del servicio web como microservicio

Awe tiene una nueva capa de conectores de servicio para hacer peticiones a servicios `microservices` y `rest`.
Consulte la documentación de [servicios](/api/service-definition.md).

:::warning
**Nota:** Para migrar los servicios web existentes en las aplicaciones que se usaban con `AWE 3`, debe tener en cuenta
los siguientes puntos:
:::

- **Envío de parámetros:**

    - *Maintains:* El atributo name de `service-parameter` debe ser igual al atributo `id` del campo `variable`
      de los elementos serve.
      ```jsx title=maintains.xml
      <target name="MyMaintain">
         <serve service="MyService">
           <variable id="id1" type="STRING" name="criterion1.selected"/>
           <variable id="id2" type="STRING" name="criterion2.selected"/>
           <variable id="id3" type="STRING" name="criterion3.selected"/>
        </serve>
      </target>
      ```
       ```jsx title=global/Services.xml
      <service id="CtrEvnDetIsuCnfSer">
        <microservice name="alu-microservice" method="POST" endpoint="/maintain/myMicroservice" content-type="JSON">
          <service-parameter name="id1" type="STRING" list="true"/>
          <service-parameter name="id2" type="STRING" list="true"/>
          <service-parameter name="id3" type="STRING" list="true"/>
        </microservice>
      </service>
      ```
      ```jsx title=webservice/services.xml
       <service name="myMicroService" type="MAINTAION" call="myWebService">
         <param list="false" name="id1" type="STRING"/>
         <param list="false" name="id2" type="STRING"/>
         <param list="false" name="id3" type="STRING"/>
       </service>
      ```

    - *Queries:* El atributo name de `service-parameter` debe ser igual al atributo `id` del `field` de la query.
      Normalmente, debe añadir un campo alias con el nombre para describir ese campo.
      ```jsx title=queries.xml
       <query id="MyQuery" service="MyService">
         <field id="id1" alias="alias1"/>
         <field id="id2" alias="alias2"/>
         <field id="id3" alias="alias3"/>
         ...
      </query>      
      ```
      ```jsx title=global/services.xml
       <service id="MyService">
         <microservice name="alu-microservice" method="POST" endpoint="/data/myMicroService" content-type="JSON">
           <service-parameter list="false" name="id1" type="STRING"/>
           <service-parameter list="false" name="id2" type="STRING"/>
           <service-parameter list="false" name="id3" type="DATE"/>
         </microservice>
      </service>
      ```
      ```jsx title=webservice/services.xml
       <service name="myMicroService" type="DATA" call="myWebService">
         <param list="false" name="id1" type="STRING"/>
         <param list="false" name="id2" type="STRING"/>
         <param list="false" name="id3" type="DATE"/>
       </service>
      ```
    - El número de `service-parameters` debe ser igual al de `param` del elemento service (webservice).
    - El orden de los `service-parameters` importa.
