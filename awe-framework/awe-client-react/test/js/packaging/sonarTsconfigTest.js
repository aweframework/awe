const fs = require('fs');
const path = require('path');
const ts = require('typescript');

const ROOT = path.join(__dirname, '../../..');
const REPOSITORY = path.join(ROOT, '../..');
const SONAR_TSCONFIG = path.join(ROOT, 'tsconfig.sonar.json');

const readConfig = (file) => {
  const {config, error} = ts.readConfigFile(file, ts.sys.readFile);
  if (error) {
    throw new Error(ts.flattenDiagnosticMessageText(error.messageText, '\n'));
  }
  return config;
};

const parse = (file) => ts.parseJsonConfigFileContent(readConfig(file), ts.sys, path.dirname(file));
const compilerOptions = (file) => parse(file).options;

// Options of the strict family (and the path of the file itself), which the Sonar tsconfig may relax
const STRICT_FAMILY = [
  'strict', 'strictNullChecks', 'noImplicitAny', 'strictFunctionTypes', 'strictBindCallApply',
  'strictPropertyInitialization', 'strictBuiltinIteratorReturn', 'alwaysStrict', 'noImplicitThis',
  'useUnknownInCatchVariables', 'configFilePath'
];
const withoutStrictFamily = (options) => Object.fromEntries(
  Object.entries(options).filter(([key]) => !STRICT_FAMILY.includes(key))
);

// Sonar analyses every JavaScript file with type information. The files of the TypeScript program take its compiler
// options, and the rest (the AngularJS client, ...) take them merged from the tsconfig files it finds. With `strict`,
// every JSDoc type is non-nullable, so the null checks of the JavaScript code read as "always false" (javascript:S3403).
// Sonar reads a tsconfig without `strict`; the type check of the build (npm run typecheck) keeps it.
describe('TypeScript configuration of the Sonar analysis', () => {
  it('relaxes only the strict checks of the build tsconfig', () => {
    const build = parse(path.join(ROOT, 'tsconfig.json'));
    const sonar = parse(SONAR_TSCONFIG);

    expect(readConfig(SONAR_TSCONFIG).extends).toBe('./tsconfig.json');
    expect(build.options.strict).toBe(true);
    // S3403 depends on the nullability of the types: strictNullChecks must be off, whatever sets it
    expect(sonar.options.strict).toBe(false);
    expect(sonar.options.strictNullChecks).not.toBe(true);
    // Same files and same options otherwise: Sonar analyses what the build checks
    expect(sonar.fileNames).toEqual(build.fileNames);
    expect(withoutStrictFamily(sonar.options)).toEqual(withoutStrictFamily(build.options));
  });

  it('is the tsconfig Sonar reads', () => {
    const pom = fs.readFileSync(path.join(REPOSITORY, 'pom.xml'), 'utf8');

    expect(pom).toMatch(
      /<sonar\.typescript\.tsconfigPaths>awe-framework\/awe-client-react\/tsconfig\.sonar\.json<\/sonar\.typescript\.tsconfigPaths>/
    );
  });
});
