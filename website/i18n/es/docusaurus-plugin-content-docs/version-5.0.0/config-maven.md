---
id: maven
title: Maven y Frontend
sidebar_label: Maven y Frontend
---

Una aplicación AWE es un proyecto Spring Boot: añada el starter de AWE, elija un cliente frontend
(AngularJS o React) y compile los recursos del frontend con Webpack a través de Maven. Esta página cubre
esa configuración y los comandos para ejecutar y empaquetar la aplicación. Para el ciclo rápido de
edición y recarga durante el desarrollo, consulte [Dev Tools](dev-tools).

## Configuración de Maven {#maven-setup}

Añada la dependencia del starter de AWE:

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-spring-boot-starter</artifactId>
  <version>${awe.version}</version>
</dependency>
```

Añada el plugin de dependencias que descomprime las pantallas genéricas incluidas con AWE:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <executions>
    <execution>
      <phase>prepare-package</phase>
      <id>unpack awe-generic-screens</id>
      <goals>
        <goal>unpack-dependencies</goal>
      </goals>
      <configuration>
        <includeGroupIds>com.almis.awe</includeGroupIds>
        <includeArtifactIds>awe-generic-screens</includeArtifactIds>
        <includes>schemas/**</includes>
        <outputDirectory>${project.build.directory}/classes/static/</outputDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Donde **PROJECT-ACRONYM** es el acrónimo del proyecto en mayúsculas y **project-acronym** es el
mismo acrónimo en minúsculas.

## Compilación del frontend {#frontend-build}

AWE compila todos los archivos JavaScript y LESS con Webpack, lanzado desde `pom.xml` mediante el
`frontend-maven-plugin`. Las compilaciones usan por defecto el modo **production**; `npm run build` es el
alias de producción.

Para generar un bundle de desarrollo local, sobrescriba la propiedad de Maven:

```bash
mvn compile -Dbuild.environment=development
```

Añada la propiedad del entorno de compilación y configure el plugin para ejecutar el script npm específico de cada modo:

```xml
<properties>
  <build.environment>production</build.environment>
</properties>

<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>install node and npm</id>
      <goals>
        <goal>install-node-and-npm</goal>
      </goals>
      <configuration>
        <nodeVersion>v24.14.0</nodeVersion>
        <npmVersion>11.11.1</npmVersion>
      </configuration>
    </execution>
    <execution>
      <id>npm ci</id>
      <goals>
        <goal>npm</goal>
      </goals>
      <configuration>
        <arguments>ci --include=dev</arguments>
      </configuration>
    </execution>
    <execution>
      <id>npm run build</id>
      <goals>
        <goal>npm</goal>
      </goals>
      <configuration>
        <arguments>run build:${build.environment}</arguments>
        <environmentVariables>
          <NODE_ENV>${build.environment}</NODE_ENV>
        </environmentVariables>
      </configuration>
    </execution>
  </executions>
</plugin>
```

`npm ci --include=dev` instala explícitamente las herramientas de desarrollo (como Jest y `webpack-cli`) para
CI, pruebas y compilaciones. `NODE_ENV=${build.environment}` se aplica solo a la ejecución de la compilación, por lo que
las compilaciones de producción siguen siendo de producción.

Cada paquete frontend que use Webpack debe exponer estos scripts:

```json
{
  "scripts": {
    "build": "npm run build:production",
    "build:development": "webpack --config webpack.config.js --mode development",
    "build:production": "webpack --config webpack.config.js --mode production",
    "test": "jest --config jest.config.js",
    "test:coverage": "jest --config jest.config.js --coverage"
  }
}
```

Esto mantiene por defecto las versiones de Maven y a sus consumidores en bundles optimizados, sin cambiar las
rutas de los recursos generados. Use `npm test` para las pruebas unitarias locales y `npm run test:coverage` para Maven,
CI, Sonar o cualquier ruta que necesite los informes JUnit y LCOV de Jest en `target/reports/jest/`.

> **Nota:** más información sobre el plugin de LESS [aquí](https://github.com/marceloverdijk/lesscss-maven-plugin).

## Elección de un cliente frontend {#choosing-a-frontend-client}

AWE ofrece dos clientes frontend. Elija uno por proyecto; la parte del servidor es idéntica.

| Cliente | Cómo añadirlo | Notas |
| --- | --- | --- |
| **AngularJS** | Dependencia Maven `awe-client-angular` | El cliente histórico; un único `webpack.config.js`. |
| **React** | Dependencia npm `awe-react-client` | El cliente más reciente; configuración de Webpack dividida (`dev`/`prod`). |

### Cliente AngularJS {#angularjs-client}

Añada la dependencia:

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-client-angular</artifactId>
  <version>${awe.version}</version>
</dependency>
```

En el `maven-dependency-plugin` anterior, añada esta ejecución después de `unpack awe-generic-screens`:

```xml
<execution>
  <phase>prepare-package</phase>
  <id>unpack awe-client-angular</id>
  <goals>
    <goal>unpack-dependencies</goal>
  </goals>
  <configuration>
    <includeGroupIds>com.almis.awe</includeGroupIds>
    <includeArtifactIds>awe-client-angular</includeArtifactIds>
    <includes>images/**,fonts/**,js/**,css/**,less/**</includes>
    <outputDirectory>${project.build.directory}/classes/static/</outputDirectory>
  </configuration>
</execution>
```

El archivo `webpack.config.js` resuelve el modo a partir de los argumentos de la CLI o de `NODE_ENV`, desactiva los sourcemaps
para el empaquetado de producción y conserva el directorio de salida y el `publicPath`:

```javascript
const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LessPluginAutoPrefix = require('less-plugin-autoprefix');
const dir = path.join(__dirname, "src", "main", "resources", "webpack");
const styleDir = path.resolve(__dirname, "src", "main", "resources", "less");
const autoprefixerBrowsers = ['last 2 versions', '> 1%', 'opera 12.1', 'bb 10', 'android 4', 'IE 10'];
const validModes = new Set(["development", "production"]);

const resolveMode = (env = {}, argv = {}) => {
  const cliMode = argv.mode;
  const envMode = env.NODE_ENV || env.mode || process.env.NODE_ENV;

  if (validModes.has(cliMode)) {
    return cliMode;
  }

  if (validModes.has(envMode)) {
    return envMode;
  }

  return "production";
};

module.exports = (env = {}, argv = {}) => {
  const mode = resolveMode(env, argv);
  const isProduction = mode === "production";

  return {
    mode,
    devtool : isProduction ? false : "source-map",
    entry : {
      "specific" : path.join(dir, "app.config.js")
    },
    output : {
      filename : "js/[name].js",
      path: path.join(__dirname, 'target', 'classes', 'static'),
      publicPath : "../"
    },
    module : {
      rules : [
        { test: /\.jsx?$/, exclude: /node_modules/, use: [{loader: 'babel-loader'}]},
        { test : /\.css$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader"]},
        { test : /\.less$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader", {
            loader: "less-loader", options: { lessOptions: { plugins: [ new LessPluginAutoPrefix({browsers: autoprefixerBrowsers}) ] }, sourceMap: false } }]},
        { test : /\.(jpg|gif|png)$/, type: 'asset', parser: { dataUrlCondition: { maxSize: 100000 } }, generator: { filename: './images/[hash][ext][query]' }},
        { test : /\.woff[2]*?(\?v=\d+\.\d+\.\d+)?$/, type: 'asset', parser: { dataUrlCondition: { maxSize: 100000 } }, generator: { filename: './fonts/[hash][ext][query]', dataUrl: content => `data:application/font-woff;base64,${content.toString('base64')}` }},
        { test : /\.(ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: { filename: './fonts/[hash][ext][query]' }},
      ]
    },
    resolve : {
      extensions : [ ".js", ".css", ".less", "*" ]
    },
    plugins : [ new MiniCssExtractPlugin({
      filename: "css/specific.css"
    })]
  };
};
```

Para Babel, mantenga la instrumentación de Istanbul solo en los flujos que no son de release (desarrollo o pruebas) y elimínela
de las compilaciones de producción para que los artefactos distribuidos no estén instrumentados.

### Cliente React {#react-client}

Añada el cliente a `package.json`:

```json
"dependencies": {
  "awe-react-client": "AWE-REACT-VERSION"
}
```

donde `AWE-REACT-VERSION` es la versión de `awe-react-client` correspondiente a su versión del framework AWE: desde AWE 5 la
versión del paquete es la versión del framework (AWE `5.0.0` usa `awe-react-client@5.0.0`). Las aplicaciones que aún usen
`awe-react-client` 2.x pueden seguir la [guía de actualización del cliente React](guides/react-client-upgrade.md).

El cliente React mantiene el mismo contrato de producción por defecto, con una configuración de Webpack dividida por
entorno: `webpack.config.js` contiene la configuración común, `webpack.dev.js` el bundle de
desarrollo y `webpack.prod.js` el bundle de producción. Las compilaciones de producción deben desactivar los sourcemaps
(`devtool: false`) y evitar efectos secundarios opcionales de generación de documentación durante el empaquetado.

```json
{
  "scripts": {
    "build": "npm run build:production",
    "build:development": "webpack --config webpack.dev.js",
    "build:production": "webpack --config webpack.prod.js"
  }
}
```

## Ejecución y compilación de la aplicación {#running-and-building-the-application}

| Objetivo | Comando |
| --- | --- |
| Ejecutar la aplicación | `npm start` (ejecuta `mvn spring-boot:run`) |
| Ejecutar con recarga en caliente (desarrollo) | `npm run start:hot-reload` — consulte [Dev Tools](dev-tools) |
| Compilar un bundle de desarrollo | `mvn compile -Dbuild.environment=development` |
| Compilar / empaquetar para producción | `npm run build` o `mvn package` |

Producción es el modo por defecto de `mvn package` y `npm run build`, por lo que las versiones y sus consumidores se mantienen en
bundles optimizados y con hash, salvo que se solicite explícitamente una compilación de desarrollo.

Para el ciclo rápido de desarrollo — recompilaciones incrementales de JS/LESS y recarga de XML en vivo sin reiniciar
la JVM — continúe en [Dev Tools](dev-tools).
