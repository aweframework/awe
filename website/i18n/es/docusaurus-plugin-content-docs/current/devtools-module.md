---
id: developer-tools
title: Módulo DevTools
sidebar_label: Módulo DevTools
---

Este módulo incluye algunas herramientas de gestión muy útiles para administrar los recursos del servidor:
- [Gestor de archivos](#file-manager)
- [Extractor SQL](#sql-extractor)

Para usar este módulo, sigue estos pasos:

- Añade las **dependencias awe tools** al descriptor pom.xml.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-tools-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

- Configura el valor de la propiedad para añadir `awe-tools` a la lista de módulos.

```properties
awe.application.module-list = APP, ..., awe-tools, ..., awe
```

## Extractor SQL {#sql-extractor}
El Extractor SQL es una herramienta para ejecutar consultas SQL directamente sobre la base de datos de la aplicación

<img alt="SQL Extractor" src={require('@docusaurus/useBaseUrl').default('img/SQL_Extractor.png')} />


- Añade las pantallas del módulo tools en tu archivo `private.xml`:

```xml
<option name="sql-extractor" label="MENU_SQL_EXTRACTOR" screen="sqlExtractor" icon="database"/>
```

### Ejecutar consultas SQL con la base de datos en uso. {#run-sql-queries-with-the-database-in-use}

"El Extractor SQL solo puede ejecutar consultas. Si quieres insertar, actualizar o eliminar, contacta con el equipo de AWE"


### Guardar cadenas de consulta en archivos. {#save-query-strings-in-files}

Elige el nombre del archivo sin extensión. El Extractor SQL lo guardará en .txt por ti. Solo podemos guardar una consulta por archivo. Este archivo se creará automáticamente en tu directorio personal. "c:/users/\{user}/aweFiles/"

<img alt="save_query" src={require('@docusaurus/useBaseUrl').default('img/save_query.png')} />

### Cargar cadenas de consulta desde un archivo para relanzarlas. {#load-querie-strings-from-file-to-relaunch-it}

Carga archivos desde tu directorio personal "c:/users/\{user}/aweFiles/"

<img alt="load_query" src={require('@docusaurus/useBaseUrl').default('img/load_query.png')} />

### MODO LECTURA Y MODO ESCRITURA {#read-mode-and-write-mode}

Por defecto, la pantalla sqlExtractor está en modo "Read", por lo que solo puedes ejecutar sentencias "select". Hay un parámetro oculto en la pantalla llamado "sqlType" que tiene por defecto "R", de "Read Mode" (modo lectura).

&lt;criteria id="sqlType" component="hidden" value="R" /&gt;


Para permitir que algunos usuarios o perfiles ejecuten sentencias que modifiquen la base de datos (insert, delete, drop, update) debes cambiar el valor del parámetro oculto "sqlType" a "W" (modo escritura) para la pantalla sqlExtractor en la opción de configuración de pantallas.

:::tip

Si quieres cambiar la funcionalidad debes sobrescribir el servicio de la siguiente manera:

```XML
  <service id="selectExtract">
    <java classname="com.almis.{project}.services.controller.SqlExtractorController" method="extractData" >
      <service-parameter name="select"   type="STRING"/>
    </java>
  </service>
```

Y añadir tus propios SqlExtractorController.java y SqlExtractorManager.java que sobrescriban los métodos de AWE.
:::

### Ejemplo: {#example}

Deben empezar con select(1)

```sql
select(1) INSERT INTO x (columns,...) VALUES (y,...)
```

## Gestor de archivos {#file-manager}

Con el gestor de archivos puedes mover, copiar, renombrar, eliminar, comprimir, descomprimir, descargar y subir archivos...

<img alt="filemanager" src={require('@docusaurus/useBaseUrl').default('img/filemanager.gif')} />

Para usar esta herramienta, sigue estos pasos (tras añadir la dependencia):


- Añade las pantallas del módulo tools en tu archivo `private.xml`:

```xml
<option name="file-manager" label="MENU_TEST_FILEMANAGER" screen="filemanager-test" icon="folder"/>
```

- Crea una **nueva pantalla** con el widget awe-file-manager como `filemanager.xml`
```xml
<screen template="full" label="MENU_TEST_FILEMANAGER"
        xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
        xsi:noNamespaceSchemaLocation='../../sch/awe/screen.xsd'>
  <tag source="center">
      <widget type="awe-file-manager" id="file-manager" style="expand"/>
  </tag>
</screen>
```

- Añade esta pantalla como **nueva opción de menú** con tu propio locale y la opción file-manager
```xml
    <option name="filemanager-test" label="MENU_TEST_FILEMANAGER" screen="filemanager-test" icon="folder" />
    <option name="file-manager" screen="file-manager" invisible="true"/>
```
