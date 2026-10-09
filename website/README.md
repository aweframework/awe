# Website

This website is built using [Docusaurus 2](https://v2.docusaurus.io/), a modern static website generator.

### Installation

```
$ npm install
```

### Local Development

```
$ npm start
```

This command starts a local development server and open up a browser window. Most changes are reflected live without having to restart the server.

### Build

```
$ npm run build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

### Deployment

```
$ GIT_USER=<Your GitHub username> USE_SSH=true npm run deploy
```

If you are using GitHub pages for hosting, this command is a convenient way to build the website and push to the `gh-pages` branch.

### Generated reference

The "XSD reference" (`/reference`) is generated from the schemas in `awe-framework/awe-generic-screens` by
`scripts/xsd-reference/`. It is written to the ignored `reference/` folder before every `start`, `build`,
`write-translations`, `crowdin:sync` and `docusaurus` run (npm `pre` scripts), and by `docusaurus.config.js` when it is
missing, so any other docusaurus command finds it. It is English only and is not uploaded to Crowdin.

```
$ npm run generate:reference   # regenerate it and print the attribute documentation coverage
$ npm run test:reference       # unit tests of the generator
```

To document an attribute, add an `xs:annotation/xs:documentation` to it in the XSD.
