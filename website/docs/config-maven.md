---
id: maven
title: Maven and Frontend
sidebar_label: Maven and Frontend
---

An AWE application is a Spring Boot project: add the AWE starter, pick a frontend client
(AngularJS or React), and build the frontend assets with Webpack through Maven. This page covers
that setup and the commands to run and package the application. For the fast edit-and-reload
development loop, see [Dev Tools](dev-tools).

## Maven setup

Add the AWE starter dependency:

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-spring-boot-starter</artifactId>
  <version>${awe.version}</version>
</dependency>
```

Add the dependency plugin that unpacks the generic screens shipped with AWE:

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

Where **PROJECT-ACRONYM** is the acronym of the project in uppercase, and **project-acronym** is the
same acronym in lowercase.

## Frontend build

AWE compiles all JavaScript and LESS files with Webpack, driven from `pom.xml` by the
`frontend-maven-plugin`. Builds default to **production** mode; `npm run build` is the production
alias.

To produce a local development bundle, override the Maven property:

```bash
mvn compile -Dbuild.environment=development
```

Add the build-environment property and configure the plugin to run the mode-specific npm script:

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

`npm ci --include=dev` explicitly installs development tooling (such as Jest and `webpack-cli`) for
CI, tests, and builds. `NODE_ENV=${build.environment}` is scoped to the build execution only, so
production builds stay production builds.

Each frontend package that uses Webpack should expose these scripts:

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

This keeps Maven releases and consumers on optimized bundles by default, without changing the
generated asset paths. Use `npm test` for local unit tests, and `npm run test:coverage` for Maven,
CI, Sonar, or any path that needs Jest JUnit and LCOV reports under `target/reports/jest/`.

> **Note:** more info about the LESS plugin [here](https://github.com/marceloverdijk/lesscss-maven-plugin).

## Choosing a frontend client

AWE offers two frontend clients. Pick one per project; the server side is identical.

| Client | Add it via | Notes |
| --- | --- | --- |
| **AngularJS** | Maven dependency `awe-client-angular` | The long-standing client; single `webpack.config.js`. |
| **React** | npm dependency `awe-react-client` | The newer client; split Webpack config (`dev`/`prod`). |

### AngularJS client

Add the dependency:

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-client-angular</artifactId>
  <version>${awe.version}</version>
</dependency>
```

In the `maven-dependency-plugin` above, add this execution after `unpack awe-generic-screens`:

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

The `webpack.config.js` file resolves the mode from CLI arguments or `NODE_ENV`, disables sourcemaps
for production packaging, and preserves the output directory and `publicPath`:

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

For Babel, keep Istanbul instrumentation only for non-release flows (development or test) and remove
it from production builds so distributed artifacts are not instrumented.

### React client

Add the client to `package.json`:

```json
"dependencies": {
  "awe-react-client": "AWE-REACT-VERSION"
}
```

where `AWE-REACT-VERSION` is the `awe-react-client` version you want to use (these versions are not
the same as the AWE Framework versions).

The React client keeps the same production-default contract, with a Webpack config split by
environment: `webpack.config.js` holds the common configuration, `webpack.dev.js` the development
bundle, and `webpack.prod.js` the production bundle. Production builds should disable sourcemaps
(`devtool: false`) and avoid optional documentation-generation side effects during packaging.

```json
{
  "scripts": {
    "build": "npm run build:production",
    "build:development": "webpack --config webpack.dev.js",
    "build:production": "webpack --config webpack.prod.js"
  }
}
```

## Running and building the application

| Goal | Command |
| --- | --- |
| Run the application | `npm start` (runs `mvn spring-boot:run`) |
| Run with hot reload (development) | `npm run start:hot-reload` — see [Dev Tools](dev-tools) |
| Build a development bundle | `mvn compile -Dbuild.environment=development` |
| Build / package for production | `npm run build` or `mvn package` |

Production is the default for `mvn package` and `npm run build`, so releases and consumers stay on
optimized, hashed bundles unless you explicitly ask for a development build.

For the fast development loop — incremental JS/LESS rebuilds and live XML reload without restarting
the JVM — continue to [Dev Tools](dev-tools).
