---
id: debugging
title: Depuración
sidebar_label: Depuración
---

## Herramientas de depuración {#debugging-tools}

Los desarrolladores con AWE cuentan con algunas herramientas interesantes que facilitan su trabajo

### Depuración del servidor {#server-debug}

Para depurar el lado servidor de un proyecto de aplicación web, normalmente trabajamos con el depurador integrado del IDE (*Netbeans*, *Eclipse*, *IntelliJ*, *Visual Studio Code*). Esta guía le mostrará cómo depurar en *Eclipse*

Para iniciar el servidor en modo depuración podemos hacer clic derecho sobre el servidor y elegir la opción de depurar.

<img alt="Modo depuración" src={require('@docusaurus/useBaseUrl').default('img/debug_mode.png')} />

Podemos añadir un punto de interrupción en cualquier lugar del código haciendo clic derecho en el margen y eligiendo alternar/añadir punto de interrupción.

Para más información sobre depuración consulte la documentación de eclipse:

````mdx-code-block
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs
  defaultValue="IntelliJ"
  values={[
    {label: 'IntelliJ', value: 'IntelliJ'},
    {label: 'Eclipse',  value: 'Eclipse'},
    {label: 'Netbeans', value: 'Netbeans'},
    {label: 'Visual Studio Code', value: 'Visual Studio Code'},
  ]}>
  <TabItem value="IntelliJ"><a target="_blank" rel="noopener noreferrer" href="https://www.jetbrains.com/help/idea/debugging-code.html">Depuración en IntelliJ</a></TabItem>
  <TabItem value="Eclipse"><a target="_blank" rel="noopener noreferrer" href="https://www.eclipse.org/community/eclipse_newsletter/2017/june/article1.php">Depuración en Eclipse</a></TabItem>
  <TabItem value="Netbeans"><a target="_blank" rel="noopener noreferrer" href="https://netbeans.org/features/java/debugger.html">Depuración en Netbeans</a></TabItem>
  <TabItem value="Visual Studio Code"><a target="_blank" rel="noopener noreferrer" href="https://code.visualstudio.com/docs/editor/debugging">Depuración en Visual Studio Code</a></TabItem>
</Tabs>
````

### Depuración del navegador {#browser-debug}
Podemos ver el flujo de información entre el servidor y el cliente pulsando la tecla F12 y entrando en la pestaña de red.
Hay algunas opciones útiles allí, pero nos centraremos en WS (WebSocket)

En el siguiente ejemplo se muestran las interacciones entre el servidor y el cliente, y podemos buscar dentro de la estructura JSON para ver toda la información y las variables.

<img alt="Web sockets" src={require('@docusaurus/useBaseUrl').default('img/websockets.png')} />

Podemos ver la acción que el servidor envió al cliente, en este caso una acción de tipo fill con los parámetros adjuntos

### Angular JS Batarang {#angular-js-batarang}
Angular JS Batarang es una extensión para navegadores que añade herramientas para depurar y analizar el rendimiento de aplicaciones AngularJS.

 [Extensión de Chrome Angular JS Batarang](https://chrome.google.com/webstore/detail/angularjs-batarang/ighdmehidhipcmcojjgiloacoafjmpfk)

Esta extensión nos permite ver la información del scope de angular. El scope es la parte de enlace entre el HTML (vista) y el JavaScript (controlador).
Una vez instalada la extensión, al pulsar F12 y acceder a la pestaña de elementos podemos ver la nueva opción $scope.

La información que se muestra allí contiene todos los métodos y parámetros de JavaScript, en formato JSON.

Podemos seleccionar un elemento de la ventana haciendo clic en la opción de inspeccionar para ver toda la información del scope relativa únicamente a ese elemento.
En chrome la opción es la siguiente:

<img alt="Inspeccionar en Chrome" src={require('@docusaurus/useBaseUrl').default('img/inspect_chrome.png')} />

En este ejemplo seleccionamos una rejilla que contiene información de usuarios. Entre todas las opciones podemos ver la información del `controller`, como los atributos de la rejilla, los tipos de variables...

<img alt="Controlador del scope" src={require('@docusaurus/useBaseUrl').default('img/scope_controller.png')} />

También es interesante ver la opción `model`, ya que contiene los propios datos.

<img alt="Modelo del scope" src={require('@docusaurus/useBaseUrl').default('img/model_scope.png')} />

## Logs {#logs}

### Logs del servidor {#server-logs}
Para buscar en los logs del servidor podemos usar las herramientas que proporciona Eclipse.
Una vez que el servidor está en ejecución, los logs aparecerán en la pestaña Console.
Cada acción que ejecuta el servidor se mostrará aquí, como las consultas SQL.

En el siguiente ejemplo tenemos una rejilla que muestra información de la tabla de usuarios, y un criterio que filtra esa rejilla ejecutando una consulta SQL asociada a él:

<img alt="Logs de la rejilla" src={require('@docusaurus/useBaseUrl').default('img/window_grid_log.png')} />

Una vez que pulsamos el botón de búsqueda, el servidor ejecutará la consulta SQL y el log de la consola la mostrará:

<img alt="Ejemplo de log de SQL" src={require('@docusaurus/useBaseUrl').default('img/log_query.png')} />

En este caso podemos ver la fecha y la hora en que se ejecutó la acción, así como el usuario, la pantalla, el identificador de la consulta y la consulta SQL completa.

### Consola del navegador {#browser-console}

Para buscar en la consola del navegador, pulse la tecla F12 y haga clic en la pestaña Console.

#### Acciones {#actions}

En esta pestaña podemos ver las acciones que tienen lugar en el lado cliente, junto con los tiempos que tardan y los parámetros enviados al servidor.
En este ejemplo hay una acción fill, utilizada para rellenar una rejilla con datos de una consulta.

<img alt="Ejemplo de log del navegador" src={require('@docusaurus/useBaseUrl').default('img/browser_log.png')} />

#### Dependencias {#dependencies}

En el siguiente ejemplo tenemos una rejilla y un botón de ver con una dependencia asociada. La dependencia comprueba si el número de filas seleccionadas no es igual a 1. Si la condición es verdadera, el botón de ver se oculta. En caso contrario, el botón se activa.

<img alt="Ejemplo de log de dependencia" src={require('@docusaurus/useBaseUrl').default('img/dependency_log.png')} />

Como solo hay una fila seleccionada, el resultado de la condición es falso, por lo tanto, el botón no se oculta.

<img alt="Ejemplo de condición de dependencia" src={require('@docusaurus/useBaseUrl').default('img/dependency_condition.png')} />
