---
id: maven
title: Maven and Frontend
sidebar_label: Maven and Frontend
---

The maven dependency needed to run an application with AWE engine is the next one:

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-spring-boot-starter</artifactId>
  <version>${awe.version}</version>
</dependency>
```

Add the maven dependency plugin to retrieve the generic screens:

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

We use `webpack` in the `pom.xml` file to compile all javascript and less files. By default, AWE now builds frontend assets in `production` mode and keeps `npm run build` as a production alias.

If you need a local development bundle, override the Maven property:

```bash
mvn compile -Dbuild.environment=development
```

Add the build environment property and configure `frontend-maven-plugin` to execute the mode-specific npm script:

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

`npm ci --include=dev` explicitly installs development tooling such as Jest and `webpack-cli`
for CI, tests, and builds. `NODE_ENV=${build.environment}` is scoped to the build execution only,
so production builds remain production builds.

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

This keeps Maven releases and consumers on optimized bundles by default without changing the generated asset paths.
Use `npm test` for local unit tests, and use `npm run test:coverage` for Maven, CI, Sonar, or any path that needs Jest JUnit and LCOV reports under `target/reports/jest/`.

> **Note:** More info about less plugin [here](https://github.com/marceloverdijk/lesscss-maven-plugin)

Where **PROJECT-ACRONYM** is the acronym of the project in uppercase, and **project-acronym** is the same acronym but in lowercase.

Depending on the frontend engine you want to use on your project, you must follow one of the following instructions:

## Angular JS Frontend

To use the AngularJS frontend, you must add the following dependency:

```xml
<dependency>
  <groupId>com.almis.awe</groupId>
  <artifactId>awe-client-angular</artifactId>
  <version>${awe.version}</version>
</dependency>
```

In the previously added `maven-dependency-plugin` you must add the following execution after the `unpack awe-generic-screens` section:

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

The `webpack.config.js` file must resolve the mode from CLI arguments or `NODE_ENV`, disable sourcemaps for production packaging, and preserve the existing output directory and `publicPath`:

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

For Babel, keep Istanbul instrumentation only for non-release flows such as development or test, and remove it from production builds so distributed artifacts are not instrumented.

### Development mode with hot reload

> Available for both the AngularJS and React clients. The launch command is the same on both — `npm run start:hot-reload` (homogenized across clients) — with a small per-client difference in how the Maven profile skips the production frontend build (noted below).

AWE projects expose two launch scripts:

| Command | What it does |
| --- | --- |
| `npm start` | Starts the application normally (`mvn spring-boot:run`). |
| `npm run start:hot-reload` | Fast development loop: runs webpack in watch mode and the application together, so front-end edits (LESS/JS) are rebuilt incrementally (~50 ms) and served **without a JVM restart**. |

With `npm run start:hot-reload`, the loop is: edit LESS/JS → the watcher rebuilds → refresh the browser. Java and screen/locale/menu XML changes still trigger a `spring-boot-devtools` restart.

It relies on three pieces (all shipped by the archetype and `awe-boot`):

- **npm scripts**: a `build:watch` script (`webpack --config webpack.config.js --watch --mode development`) and `concurrently` (devDependency) to run the watcher and the app in parallel.
- **Maven `hot-reload` profile**: keeps the watcher as the sole bundle writer so the Maven-driven production build does not overwrite the dev bundles (otherwise `mvn spring-boot:run` would run `npm ci` and a second webpack, colliding with the watcher and leaving the app hanging on the splash screen), while keeping `spring-boot-devtools`. The two clients achieve this differently:
  - **AngularJS client**: unbinds the individual `frontend-maven-plugin` executions (`install node and npm`, `npm ci`, `npm run build`) by setting their phase to `none`.
  - **React client**: sets `<skip.frontend>true</skip.frontend>` in the profile, which skips the whole inherited frontend build (node install, `npm ci` and the production build) in one property rather than unbinding each execution.

  ```xml
  <profile>
    <id>hot-reload</id>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>provided</scope>
      </dependency>
    </dependencies>
    <build>
      <plugins>
        <plugin>
          <groupId>com.github.eirslett</groupId>
          <artifactId>frontend-maven-plugin</artifactId>
          <executions>
            <execution><id>install node and npm</id><phase>none</phase></execution>
            <execution><id>npm ci</id><phase>none</phase></execution>
            <execution><id>npm run build</id><phase>none</phase></execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
  ```

- **Relaxed static-resource handling in development** so rebuilt bundles are served immediately (no content hashing, no caching):

  ```properties
  spring.web.resources.cache.period=0
  spring.web.resources.chain.cache=false
  spring.web.resources.chain.strategy.content.enabled=false
  ```

  In generated projects this lives in an `application-hot-reload.properties` Spring profile, activated by the `start:hot-reload` script (`-Dspring-boot.run.profiles=hot-reload`), so the main `application.properties` keeps production-safe caching.

## React Frontend

To use the new React frontend you must add the following dependency on the `package.json` file:

```json
"dependencies": {
  "awe-react-client": "AWE-REACT-VERSION"
}
```

where `AWE-REACT-VERSION` is the awe-react-client version you want to use (not the same versions as 
AWE Framework).

The React frontend keeps the same production-default contract. `npm run build` should remain an alias of `build:production`, while development bundles should be generated explicitly with `build:development`.

```json
{
  "scripts": {
    "build": "npm run build:production",
    "build:development": "webpack --config webpack.dev.js",
    "build:production": "webpack --config webpack.prod.js"
  }
}
```

Keep the React Webpack split by environment: `webpack.config.js` contains the common
configuration, `webpack.dev.js` enables the development bundle, and `webpack.prod.js`
enables the production bundle. Production builds should disable sourcemaps with
`devtool: false` and avoid optional documentation-generation side effects during normal
application packaging.
