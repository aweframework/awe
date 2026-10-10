---
id: architecture
title: Arquitectura
sidebar_label: Arquitectura
---

## Tabla de contenidos {#table-of-contents}

* **[Arquitectura física](#physical-architecture)**
* **[Arquitectura de comunicaciones](#communications-architecture)**
* **[Arquitectura del cliente](#client-architecture)**
* **[Arquitectura del servidor](#server-architecture)**

---

## Arquitectura física {#physical-architecture}

Las aplicaciones web desarrolladas con AWE pueden desplegarse en muchos entornos distintos según los requisitos de negocio: en cualquier servidor de aplicaciones compatible con J2EE.

Con el framework Ehcache se permite una arquitectura descentralizada. También proporciona escalabilidad en servidores multinodo y clústeres. Además, las aplicaciones pueden integrarse con servicios en la nube como Amazon o Azure.

AWE dispone de conectores con los principales motores de bases de datos o con algunos servidores de mensajería, por ejemplo ActiveMQ y MQSeries, servidores de impresión o servidores de correo. En cuanto al proceso de inicio de sesión, puede ejecutarse contra el directorio activo.

Por el lado del cliente, el uso de media queries de CSS3 y de librerías como Bootstrap nos permite generar aplicaciones adaptables.

En resumen, las aplicaciones basadas en AWE pueden adaptarse fácil y rápidamente al entorno.

<img alt="Arquitectura" src={require('@docusaurus/useBaseUrl').default('img/Arquitectura_física.png')} />

## Arquitectura de comunicaciones {#communications-architecture}

AWE utiliza el framework Atmosphere para adaptar la comunicación entre el navegador y el servidor. Los mensajes entre ambos utilizan formato JSON.

Se establece una comunicación bidireccional, de modo que el servidor no solo responde a las peticiones de los clientes, sino que envía datos a los clientes conectados en cualquier momento.  

## Arquitectura del cliente {#client-architecture}

El lado del cliente se basa en una arquitectura típica de Angular: MVW (Model View Whatever).

La **Vista** se implementa mediante plantillas HTML proporcionadas por el servidor. Estas plantillas dependen de los componentes que pertenecen a la pantalla que se quiere mostrar.

El usuario interactúa con la vista y el controlador de Angular se encarga de actualizar el modelo con los resultados de esta acción del usuario. El servidor también puede cambiar el modelo como resultado de una petición. Cuando Angular.js detecta cualquier cambio en el modelo, actualiza la vista para mostrarlo.

Las peticiones al servidor se realizan mediante 'server-actions'. Cada una devuelve una o más 'client-action' relacionadas con los componentes de la pantalla.

Todos los componentes generados por las pantallas de AWE están gestionados por directivas complejas de Angular.js (compuestas por una o más subdirectivas) en las que pueden encapsularse plugins desarrollados en Angular.js.

Cuando la aplicación arranca o se selecciona un nuevo idioma, se descarga un nuevo paquete de idioma y todos los textos de la aplicación se actualizan de inmediato.

<img alt="Diseño del cliente" src={require('@docusaurus/useBaseUrl').default('img/front_design.png')} />

## Arquitectura del servidor {#server-architecture}

La principal característica de la arquitectura del lado del servidor es ser una arquitectura multicapa sobre el estándar MVC. El objetivo del motor AWE es crear aplicaciones modulares que permitan cambios rápidos y sencillos.

La capa de controlador gestiona la lógica de negocio. Atiende las peticiones de los clientes redirigiéndolas a la capa de servicio, donde las gestiona el motor correspondiente:

* Motor de pantallas: Genera código fuente JavaScript y archivos CSS a partir del archivo XML de la pantalla. También gestiona las acciones de pantalla.

* Motor de datos: Accede a sistemas externos para obtener información.

* Motor de mantenimiento: Gestiona las peticiones para ejecutar numerosos procesos: consultas a bases de datos, envío de correos electrónicos o generación de informes.

* Motor de seguridad: Es una capa entre las capas de modelo y de controlador, donde se gestionan las peticiones para proporcionar servicios de seguridad frente a los ataques más comunes: inyección SQL, XSS, etc.

* Motor de documentos: Genera informes mediante la librería Jasper.

<img alt="Arquitectura de diseño del servidor" src={require('@docusaurus/useBaseUrl').default('img/server_design_architecture.png')} />