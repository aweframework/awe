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

Now you can update the defined color schema for each theme on the `Theme customization` screen. Simply select a theme on the `Themes` screen and click on the `Customize` button:

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/theme_customization/Themes_screen.png')} />

You will be redirected to the `Theme customization` screen where you can change a set of properties (mostly colors) to customize the theme properties and adapt it to your needing:

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/theme_customization/Customize_screen.png')} />

When a property has changed, a `save` button will appear to store the modified value and show a preview of the theme on the right panel.

<img alt="Pantalla de temas" src={require('@docusaurus/useBaseUrl').default('img/theme_customization/Change_properties.png')} />

Each theme has two modes, `light` and `dark`, which will be switched with the mode button on the navigation bar.

The navigation bar buttons are defined on the `info-buttons.xml` screen. Here you have the `themeModeSelector` and the `themeSelector`.

## Sitios {#sites}

This option is a high level option which allow you to separate application logic in sites, each one with its own database (or set of databases). Here you can define sites and the relationship between them and the databases they manage. You can also define the modules which can site can use.

## Módulos {#modules}

A module is a special set of options defined on the menu which can be used to separate functionalities inside an application. A user can only have an active module at a time, so when the module is changed, menu options will be refreshed with common and module options.

This screen allows to define the selectable modules, based on the ones which are defined on the menu. Here you can also define the relationship between modules and profiles, users or sites (and databases).

## Bases de datos {#databases}

In databases option you can manage a set of connections to datasources of any supported database types, like Oracle, SQL Server, H2, etc.

Inside a database management screen you can set the site/module configuration for each one.

## Servidores de correo {#mail-servers}

Mail server option contains a set of external mail servers used to send e-mail in some parts of the application, like print screens and scheduler reports.

## Colas de mensajes {#message-queues}

This option can define a list of message queues to be used as external communication with AWE applications. Each entry contains a broker queue definition which can be used by the [Queues.xml](api/jms-queues-definition.md) file.

## Secuencias {#sequences}

The sequences screen contains a list of identifiers and sequence values which awe uses in maintain files. These sequences are used as if they were autonumeric fields, very useful for old database models without autonumeric fields.

## Parámetros de la aplicación {#application-parameters}

The application parameters screen defines a sort of extra application properties which can override the ones defined on `application.properties` file. This is an extra layer to improve application customization.

## Difusión {#broadcasting}

This screen is simply a way to send messages to all connected users inside the application, or a specific message to a single user.

It will appear as a message in the top side of the screen.

## Registro {#log}

The log screen contains access to all the application logs. This will be deprecated soon in order to give way to new observability stack in a distributed architecture.

## Acceso a las pantallas {#screen-access}

Screen access contains a list of option restrictions which can be defined to all application users, to a specific profile or even to a specific user.

You can search a specific restriction with the upper criteria, or update

## Acceso al menú {#menu-access}

This is a new screen designed to improve the usability of menu option restriction. In this screen you have a menu tree and three criteria: `User`, `Profile` and `Module`. The tree below is based in the selections you have picked in the criteria: for example, the initial tree shows the restriction menu for all users and all profiles, for the options which doesn't belong to any module.

When you select a module in the criteria, the tree below will show all the menu options for this module, including the options which doesn't have module defined.

If you pick a profile in the `Profile` criterion, the tree will show the restrictions for this profile (and in the `access` column you will see the general restrictions for all users and profiles for each option).

Picking a user in the `User` criterion will make the tree to show the restrictions for the user. The `access` column will show the specific restrictions for the user profile in each option.

<img alt="Menu restrictions" src={require('@docusaurus/useBaseUrl').default('img/MenuRestrictionsScreen.png')} />

To change the restrictions for any option, you can do it by selecting the option and doing *right-click* on it. It will show a context-menu with the restriction options (allow, restrict or remove restriction in case of defined). You can also select the option and click on the buttons below the tree to change a restriction value.

## Utilidad de cifrado {#encrypt-util}

This option is very useful to generate encrypted values or properties. Simply put on the text you want to encrypt on the `Text` criterion and push the `Encrypt` button. You will have filled the `Encrypted` text with the text you need to add in database encrypted columns and the `Encrypted property` which is the encrypted text to be used in `application.properties` file values.

<img alt="Utilidad de cifrado" src={require('@docusaurus/useBaseUrl').default('img/EncryptManagerScreen.png')} />

## Configuración de pantalla {#screen-configuration}

This screen allows to change the functionality of the screen defined in XML file. The new functionality is stored in the database, and it can be restricted by user or profile. To change a screen element you only have to define the screen, the element to be restricted/modified and the attribute with the new values.

### Ejemplo: {#example}

If we have a criteria that will be required `validation="required"`.

``` xml
 <criteria id="CrtSit" label="PARAMETER_NAME" component="suggest" server-action="data" target-action="SitSug" style="col-xs-7 col-sm-6 col-lg-3" validation="required"/>
```

And we want to be not required, put the new configuration like in the picture below:

<img alt="Configuración de pantalla" src={require('@docusaurus/useBaseUrl').default('img/screen_conf.png')} />

### Cómo se usa: {#how-to-use-it}

If you want to access and use this screen, put the next option in your `private.xml` or `public.xml`.

``` xml
 <option name="screen_configuration" label="MENU_TOOLS_SCR_CNF" screen="ScrCnf" icon="laptop" />
```

## Manual de usuario {#user-manual}

This option allows the user to access a PDF file defined on the application, as a user manual. This PDF file can be defined in some languages

## Ayuda de la aplicación {#application-help}

<img alt="Ayuda de la aplicación" src={require('@docusaurus/useBaseUrl').default('img/ApplicationHelpScreen.png')} />