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

We use `webpack` in the `pom.xml` file to compile all javascript and less files

```xml
<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>install node and yarn</id>
      <goals>
        <goal>install-node-and-yarn</goal>
      </goals>
      <configuration>
        <nodeVersion>v18.19.0</nodeVersion>
        <yarnVersion>v1.22.21</yarnVersion>
      </configuration>
    </execution>
    <execution>
      <id>yarn install</id>
      <goals>
        <goal>yarn</goal>
      </goals>
      <configuration>
        <arguments>install</arguments>
      </configuration>
    </execution>
    <execution>
      <id>webpack</id>
      <goals>
        <goal>webpack</goal>
      </goals>
      <configuration>
        <arguments>--output-path "${project.build.frontend}"</arguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

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

The `webpack.config.js` file must contain the following code:

```javascript
const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LessPluginAutoPrefix = require('less-plugin-autoprefix');
const dir = path.join(__dirname, "src", "main", "resources", "webpack");
const styleDir = path.resolve(__dirname, "src", "main", "resources", "less");
const autoprefixerBrowsers = ['last 2 versions', '> 1%', 'opera 12.1', 'bb 10', 'android 4', 'IE 10'];

module.exports = {
  mode: process.env.NODE_ENV,
  devtool : "source-map",
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
          loader: "less-loader", options: { lessPlugins: [ new LessPluginAutoPrefix({browsers: autoprefixerBrowsers}) ], sourceMap: true}}]},
      { test : /\.(jpg|gif|png)$/, use: [{loader: 'url-loader', options: {limit: 100000, name: './images/[hash].[ext]'}}]},
      { test : /\.woff[2]*?(\?v=\d+\.\d+\.\d+)?$/, use: [{loader: 'url-loader', options: {limit: 100000, mimetype: 'application/font-woff', name: './fonts/[hash].[ext]'}}]},
      { test : /\.(ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/, use: [{loader: 'file-loader', options: {name: './fonts/[hash].[ext]'}}]},
    ]
  },
  resolve : {
    extensions : [ ".js", ".css", ".less", "*" ]
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/specific.css",
    disable: false,
    allChunks: true
  })]
};
```

## React Frontend

To use the new React frontend you must add the following dependency on the `package.json` file:

```json
"dependencies": {
  "awe-react-client": "AWE-REACT-VERSION"
}
```

where `AWE-REACT-VERSION` is the awe-react-client version you want to use (not the same versions as 
AWE Framework).

The `webpack.config.js` file has changed slightly:

```javascript
const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyPlugin = require("copy-webpack-plugin");
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ThymeLeafPlugin = require('awe-react-client/plugins/thymeleaf-plugin');

module.exports = {
  entry : {
    "bundle" : path.resolve(__dirname, "src", "js", "main.js")
  },
  output : {
    clean: true,
    filename : "js/[name].js",
    path: path.resolve(__dirname, 'target', 'classes', 'static'),
  },
  optimization: {
    minimize: false,
    splitChunks: {
      name: "main",
      cacheGroups: {
        "commons": {
          test: /[\\/]node_modules[\\/]((?!(awe\-react\-client|primereact)))/,
          name: 'commons',
          chunks: 'all'
        },
      }
    }
  },
  module : {
    rules : [
      {test: /\.js$/, enforce: 'pre', use: ['source-map-loader']},
      {test: /\.(tsx|ts|jsx|js)$/, exclude: /node_modules/, use: 'babel-loader'},
      {test: /\.(le|c)ss$/, use: [MiniCssExtractPlugin.loader, "css-loader", "postcss-loader", "less-loader"]},
      {test: /\.(jpg|gif|png|svg)$/, type: 'asset/resource', generator: { filename: 'images/[hash][ext][query]'}},
      {test: /\.(ttf|eot|woff(2)?)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: {filename: "fonts/[hash][ext][query]"}},
    ]
  },
  resolve : {
    extensions : [ ".js", ".css", ".less", ".*" ]
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/[name].css"
  }),
    new HtmlWebpackPlugin({
      template: require.resolve('awe-react-client/template.html'),
      filename: 'index.html',
      inject: "body",
    }),
    new ThymeLeafPlugin(),
    new CopyPlugin({
      patterns: [
        {from: path.resolve("node_modules/awe-react-client/static")}
      ]
    })
  ]
};
```