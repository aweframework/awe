---
id: developer
title: Módulo DevTools
sidebar_label: Módulo Developer
---

Este módulo incluye algunas herramientas de gestión muy útiles para administrar literales:
- [Gestor de literales](#literals-manager)

Para usar este módulo, sigue estos pasos:

- Añade la **dependencia awe developer** al descriptor pom.xml.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-developer-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

- Configura el valor de la propiedad para añadir `awe-developer` a la lista de módulos.

```properties
awe.application.module-list = APP, ..., awe-developer, ..., awe
```

## Gestor de literales {#literals-manager}
El *Gestor de literales* es una herramienta que ayuda a gestionar los literales existentes en una aplicación. Todas las acciones y modificaciones disponibles se almacenan tanto en memoria como en los múltiples XML. De esta forma, no es necesario reiniciar el servidor *Tomcat* cada vez que se realiza un cambio. El desarrollador solo tendrá que refrescar la página para ver todos los cambios.

<img alt="lit-man" src={require('@docusaurus/useBaseUrl').default('img/lit-man.png')} />

### Funcionalidades {#features}

1. Buscar literales existentes.
2. Crear nuevos literales.
3. Modificar literales existentes.
4. Traducir literales automáticamente a otros idiomas.
5. Eliminar literales.

### Buscar literales existentes {#searching-existing-literals}

<img alt="search-lite" src={require('@docusaurus/useBaseUrl').default('img/search-lite.png')} />

Puedes buscar por código o por literal. El idioma de búsqueda por defecto es el mismo que el idioma por defecto de la aplicación, pero se puede configurar para buscar en cualquier otro idioma *instalado*. Al hacer clic en el botón de búsqueda junto al campo de texto, todos los resultados aparecen en la tabla de la izquierda. Para gestionar estos literales, basta con hacer clic en cualquiera de ellos para mostrar los detalles y las traducciones de la palabra elegida.

### Eliminar literales {#deleting-literals}

<img alt="lit-list" src={require('@docusaurus/useBaseUrl').default('img/lit-list.png')} />

Este proceso se completa seleccionando el literal que hay que eliminar y haciendo clic en el botón de eliminar.

### Crear literales {#creating-literals}

<img alt="new-lit" src={require('@docusaurus/useBaseUrl').default('img/new-lit.png')} />

Se pueden crear nuevos literales haciendo clic en el botón *Nuevo*. Aparecerá una vista modal con dos campos de entrada: el primero para el *código* del literal y el segundo para el contenido del literal en el idioma por defecto o elegido. La herramienta genera automáticamente las traducciones para todos los demás idiomas existentes.

### Modificar literales {#modifying-literals}

<img alt="translation-list" src={require('@docusaurus/useBaseUrl').default('img/translation-list.png')} />

Para modificar literales existentes, hay que elegir un literal de la lista mostrada en la tabla de la izquierda. El contenido del literal aparece en el editor de Texto / Markdown. Markdown es un lenguaje de marcado con una sintaxis de formato de texto plano diseñado para que pueda convertirse a HTML y a muchos otros formatos. Esto permite añadir estilos de forma sencilla.

### Traducir literales {#translating-literals}

#### Texto {#text}

<img alt="text-trans" src={require('@docusaurus/useBaseUrl').default('img/text-trans.png')} />

#### Markdown {#markdown}

<img alt="markdown-trans" src={require('@docusaurus/useBaseUrl').default('img/markdown-trans.png')} />

Selecciona el idioma de origen en el editor de markdown, después selecciona el idioma de destino y haz clic en el botón de traducir. ¡Voilà!

### Lanzamiento {#launch}

Para usarlo, coloca un enlace a estas ventanas en algún lugar del menú de tu proyecto, `public.xml` o `private.xml`.

Ejemplo:

``` XML
  <option name="developer" label="MENU_DEVELOPER" icon="paint-brush">
    <option name="path-manager"  label="MENU_PATH" screen="path-manager" icon="italic"/>
    <option name="local-manager" label="MENU_LANGUAGES" screen="local-manager" icon="language"/>
  </option>
```

- Añade el directorio de tus archivos XML usando la ventana de rutas dentro del menú developer.

<img alt="path" src={require('@docusaurus/useBaseUrl').default('img/path.png')} />

- El directorio especificado debe tener estas carpetas

<img alt="folders" src={require('@docusaurus/useBaseUrl').default('img/folders.png')} />

- Ejemplo del directorio de AWE en una máquina local

<img alt="path" src={require('@docusaurus/useBaseUrl').default('img/path.png')} />

* Esta herramienta está pensada para ser utilizada únicamente en máquinas locales.
