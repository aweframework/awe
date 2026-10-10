---
id: project-structure
title: Estructura del proyecto
sidebar_label: Estructura del proyecto
---

AWE (Almis Web Engine) es una herramienta de desarrollo de aplicaciones web que permite al desarrollador crear pantallas de aplicaciones web describiendo el comportamiento de la pantalla en un archivo XML.

AWE también permite al desarrollador diseñar los recursos de la aplicación, como consultas a base de datos y acciones (inserción, actualización y borrado), llamadas y lanzamiento de colas JMS, envío de correos electrónicos, generación de informes, lanzamiento de procesos por lotes y cualquier otra acción mediante la llamada a servicios externos desarrollados en Java y C.

## Estructura del proyecto {#project-structure}

AWE define una estructura de carpetas para los archivos XML para garantizar la compatibilidad del proyecto:

```
application.{PROJECT-ACRONYM}
|- global
|- locale
|- menu
|- profile
|- screen
```

### Carpeta global {#global-folder}

La carpeta *global* contiene los siguientes archivos:

* **Actions.xml** - Contiene las acciones específicas de la aplicación. Se usa en aplicaciones complejas con componentes específicos.
* **Email.xml** - Con las definiciones de contenidos y destinatarios de correo electrónico. Se usa para enviar correos automáticos.
* **Enumerated.xml** - Contiene grupos de valores pequeños para usar en listas desplegables o para traducir valores.
* **Queries.xml** - Con la definición de las peticiones de datos a base de datos y servicios.
* **Queues.xml** - Archivo de interfaz con las colas JMS.
* **Maintain.xml** - Con la definición de las llamadas a procesos de base de datos y servicios.
* **Services.xml** - Archivo de interfaz con servicios externos.

### Carpeta locale {#locale-folder}

La carpeta *locale* contiene un archivo por cada idioma definido en la aplicación. Todos los archivos se nombran Locale-*\{language-code}*.xml, donde **language-code** es el código de idioma según los códigos ISO 3166 e ISO 639 (es-ES, en-GB, fr-FR...).

### Carpeta menu {#menu-folder}

La carpeta *menu* contiene los siguientes archivos:

* **private.xml** - Con la definición del menú de la sección privada (con inicio de sesión) de la aplicación.
* **public.xml** - Con la definición del menú de la sección pública de la aplicación.

### Carpeta profile {#profile-folder}

La carpeta *profile* contiene un archivo por cada grupo de restricciones definido en la aplicación. Cada archivo contiene un conjunto de reglas para permitir o denegar el acceso a las opciones del menú.

### Carpeta screen {#screen-folder}

La carpeta *screen* contiene un archivo por cada pantalla definida en la aplicación. Estos archivos definen una estructura y un comportamiento para todos los componentes que se quieran mostrar en la pantalla.
