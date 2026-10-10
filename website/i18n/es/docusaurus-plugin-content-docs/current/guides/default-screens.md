---
id: default-screens
title: Ventanas por defecto
sidebar_label: Ventanas por defecto
---

## Introducción {#introduction}

AWE tiene algunas pantallas comunes prediseñadas que son muy útiles para una primera versión de una aplicación. De esta manera, ya está implementada la gestión de [usuarios](#users) y [perfiles](#profiles), algunos [temas](#themes) útiles para personalizar la aplicación, gestión de seguridad como el [acceso a pantallas](#screen-access) y la [configuración de pantallas](#screen-configuration), y algunas herramientas de gestión a alto nivel como la [base de datos](#databases), la [configuración de módulos](#modules) y los [sitios](#sites).

AWE también permite visualizar una pantalla de ayuda autogenerada basada en la definición de pantalla y algunos atributos adicionales como `help` y `help-image`. Esta pantalla de ayuda puede ser generada por pantalla o también hay una [pantalla de ayuda de la aplicación](#application-help) que contiene la documentación sobre la aplicación completa (basada en el menú).

## Usuarios {#users}

La pantalla de usuario le permite definir, actualizar o eliminar los *usuarios* que pueden acceder a la aplicación y sus opciones predeterminadas, como el idioma, el servidor de correo, el tema por defecto y la pantalla inicial.

<img alt="Pantalla de usuario" src={require('@docusaurus/useBaseUrl').default('img/UsersScreen.png')} />

## Perfiles {#profiles}

En la pantalla de perfiles se pueden administrar nuevos *perfiles* para agrupar usuarios en categorías de seguridad, como administradores, usuarios regulares o usuarios especiales.

<img alt="Pantalla de perfiles" src={require('@docusaurus/useBaseUrl').default('img/ProfilesScreen.png')} />

## Temas {#themes}

La pantalla de temas permite definir nuevos *temas en CSS* para la aplicación. En esta pantalla sólo puede definir el nombre del tema, pero también es necesario generar los nuevos archivos CSS del tema en su aplicación.

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/ThemesScreen.png')} />

Ahora puede actualizar el esquema de colores definido para cada tema en la pantalla `Theme customization`. Basta con seleccionar un tema en la pantalla `Themes` y hacer clic en el botón `Customize`:

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/theme_customization/Themes_screen.png')} />

Será redirigido a la pantalla `Theme customization`, donde puede cambiar un conjunto de propiedades (principalmente colores) para personalizar el tema y adaptarlo a sus necesidades:

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/theme_customization/Customize_screen.png')} />

Cuando se modifica una propiedad, aparece un botón `save` para almacenar el valor modificado y mostrar una vista previa del tema en el panel derecho.

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/theme_customization/Change_properties.png')} />

Cada tema tiene dos modos, `light` y `dark`, que se alternan con el botón de modo de la barra de navegación.

Los botones de la barra de navegación se definen en la pantalla `info-buttons.xml`. Aquí se encuentran el `themeModeSelector` y el `themeSelector`.

## Sitios {#sites}

Esta es una opción de alto nivel que permite separar la lógica de la aplicación en sitios, cada uno con su propia base de datos (o conjunto de bases de datos). Aquí se pueden definir los sitios y la relación entre ellos y las bases de datos que gestionan. También se pueden definir los módulos que puede utilizar cada sitio.

## Módulos {#modules}

Un módulo es un conjunto especial de opciones definidas en el menú que se puede utilizar para separar funcionalidades dentro de una aplicación. Un usuario solo puede tener un módulo activo a la vez, por lo que cuando se cambia de módulo, las opciones del menú se actualizan con las opciones comunes y las del módulo.

Esta pantalla permite definir los módulos seleccionables, basándose en los que están definidos en el menú. Aquí también se puede definir la relación entre módulos y perfiles, usuarios o sitios (y bases de datos).

## Bases de datos {#databases}

En la opción de bases de datos se puede gestionar un conjunto de conexiones a orígenes de datos de cualquiera de los tipos de base de datos soportados, como Oracle, SQL Server, H2, etc.

Dentro de la pantalla de gestión de una base de datos se puede establecer la configuración de sitio y módulo de cada una.

## Servidores de correo {#mail-servers}

La opción de servidores de correo contiene un conjunto de servidores de correo externos que se utilizan para enviar correos electrónicos en algunas partes de la aplicación, como la impresión de pantallas y los informes del planificador.

## Colas de mensajes {#message-queues}

Esta opción permite definir una lista de colas de mensajes que se utilizan como comunicación externa con las aplicaciones AWE. Cada entrada contiene la definición de una cola de un broker que puede ser utilizada por el archivo [Queues.xml](api/jms-queues-definition.md).

## Secuencias {#sequences}

La pantalla de secuencias contiene una lista de identificadores y valores de secuencia que AWE utiliza en los archivos de mantenimiento. Estas secuencias se utilizan como si fueran campos autonuméricos, algo muy útil para modelos de base de datos antiguos que no disponen de ellos.

## Parámetros de la aplicación {#application-parameters}

La pantalla de parámetros de la aplicación define una especie de propiedades adicionales de la aplicación que pueden sobrescribir las definidas en el archivo `application.properties`. Es una capa extra para mejorar la personalización de la aplicación.

## Difusión {#broadcasting}

Esta pantalla es simplemente una forma de enviar mensajes a todos los usuarios conectados a la aplicación, o un mensaje específico a un único usuario.

Aparecerá como un mensaje en la parte superior de la pantalla.

## Registro {#log}

La pantalla de registro da acceso a todos los registros de la aplicación. Se dejará de utilizar próximamente para dar paso a una nueva pila de observabilidad en una arquitectura distribuida.

## Acceso a las pantallas {#screen-access}

El acceso a pantallas contiene una lista de restricciones de opciones que se pueden definir para todos los usuarios de la aplicación, para un perfil concreto o incluso para un usuario concreto.

Puede buscar una restricción concreta con los criterios superiores, o actualizar

## Acceso al menú {#menu-access}

Esta es una nueva pantalla diseñada para mejorar la usabilidad de la restricción de opciones de menú. En esta pantalla hay un árbol de menú y tres criterios: `User`, `Profile` y `Module`. El árbol inferior se basa en las selecciones realizadas en los criterios: por ejemplo, el árbol inicial muestra el menú de restricciones para todos los usuarios y todos los perfiles, para las opciones que no pertenecen a ningún módulo.

Cuando se selecciona un módulo en los criterios, el árbol inferior muestra todas las opciones de menú de ese módulo, incluidas las opciones que no tienen módulo definido.

Si se elige un perfil en el criterio `Profile`, el árbol muestra las restricciones de ese perfil (y en la columna `access` se ven las restricciones generales para todos los usuarios y perfiles de cada opción).

Al elegir un usuario en el criterio `User`, el árbol muestra las restricciones de ese usuario. La columna `access` muestra las restricciones específicas del perfil del usuario en cada opción.

<img alt="Menu restrictions" src={require('@docusaurus/useBaseUrl').default('img/MenuRestrictionsScreen.png')} />

Para cambiar las restricciones de cualquier opción, se puede seleccionar la opción y hacer *clic derecho* sobre ella. Se mostrará un menú contextual con las opciones de restricción (permitir, restringir o eliminar la restricción en caso de estar definida). También se puede seleccionar la opción y pulsar los botones situados debajo del árbol para cambiar el valor de una restricción.

## Utilidad de cifrado {#encrypt-util}

Esta opción es muy útil para generar valores o propiedades cifrados. Basta con introducir el texto que se desea cifrar en el criterio `Text` y pulsar el botón `Encrypt`. Se rellenará el texto `Encrypted` con el texto que hay que añadir en las columnas cifradas de la base de datos y `Encrypted property`, que es el texto cifrado que se utiliza en los valores del archivo `application.properties`.

<img alt="Utilidad de cifrado" src={require('@docusaurus/useBaseUrl').default('img/EncryptManagerScreen.png')} />

## Configuración de pantalla {#screen-configuration}

Esta pantalla permite cambiar la funcionalidad de la pantalla definida en el archivo XML. La nueva funcionalidad se almacena en la base de datos y puede restringirse por usuario o perfil. Para cambiar un elemento de una pantalla solo hay que definir la pantalla, el elemento que se va a restringir o modificar y el atributo con los nuevos valores.

### Ejemplo: {#example}

Si tenemos un criterio que será obligatorio `validation="required"`.

``` xml
 <criteria id="CrtSit" label="PARAMETER_NAME" component="suggest" server-action="data" target-action="SitSug" style="col-xs-7 col-sm-6 col-lg-3" validation="required"/>
```

Y queremos que deje de ser obligatorio, establezca la nueva configuración como se muestra en la imagen siguiente:

<img alt="Configuración de pantalla" src={require('@docusaurus/useBaseUrl').default('img/screen_conf.png')} />

### Cómo se usa: {#how-to-use-it}

Si desea acceder a esta pantalla y utilizarla, añada la siguiente opción en su `private.xml` o `public.xml`.

``` xml
 <option name="screen_configuration" label="MENU_TOOLS_SCR_CNF" screen="ScrCnf" icon="laptop" />
```

## Manual de usuario {#user-manual}

Esta opción permite al usuario acceder a un archivo PDF definido en la aplicación como manual de usuario. Este archivo PDF puede definirse en varios idiomas.

## Ayuda de la aplicación {#application-help}

<img alt="Ayuda de la aplicación" src={require('@docusaurus/useBaseUrl').default('img/ApplicationHelpScreen.png')} />