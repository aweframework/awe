const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '../../..');
const SRC = path.join(ROOT, 'src');
const HEAVY = ['echarts', 'leaflet', 'react-pivottable', 'react-player', 'react-pdfobject', 'react-lazylog', 'primereact/editor', 'quill'];
// The chart server bundles everything in one Node file: it is the only code that loads ECharts at once
const STATIC_ECHARTS = ['src/renderer/chartRenderer.js', 'src/utilities/echartsSetup.ts'];

// The first external of the library build: the one that keeps the heavy packages out of the main bundle
const [heavyExternal] = require(path.join(ROOT, 'webpack.common.js')).externals;
const resolveExternal = (request) => new Promise((resolve, reject) =>
  heavyExternal({request}, (error, result) => error ? reject(error) : resolve(result)));

const sources = (dir) => fs.readdirSync(dir, {withFileTypes: true}).flatMap((entry) => {
  const file = path.join(dir, entry.name);
  return entry.isDirectory() ? sources(file) : /\.(js|jsx|ts|tsx)$/.test(entry.name) ? [file] : [];
});
const relative = (file) => path.relative(ROOT, file).split(path.sep).join('/');
const packageOf = (name) => name.replace('/', '[/]');

describe('Heavy packages of the library build', () => {
  it.each([
    'echarts/core', 'echarts/i18n/langES-obj', 'leaflet', 'leaflet/dist/leaflet.css', 'react-pivottable/PivotTableUI',
    'react-pivottable/pivottable.css', 'react-player', 'react-pdfobject', 'react-lazylog', 'primereact/editor', 'quill'
  ])('loads %s on demand, as an import external', async (request) => {
    await expect(resolveExternal(request)).resolves.toBe(`import ${request}`);
  });

  it.each(['primereact/button', 'primereact/api', 'react', 'react-redux', 'echartsish', 'quill-delta'])(
    'leaves %s to the node externals', async (request) => {
      await expect(resolveExternal(request)).resolves.toBeUndefined();
    });

  it('imports no heavy package statically in the source: it would be a Promise at runtime', () => {
    const heavy = HEAVY.map(packageOf).join('|');
    const staticImport = new RegExp(String.raw`(?:^|\n)\s*(?:(?:import|export)\s+(?!type\b)[^'";]*?from\s*|import\s*)['"](?:${heavy})(?:[/'"])`);

    const offenders = sources(SRC).filter((file) => !STATIC_ECHARTS.includes(relative(file)))
      .filter((file) => staticImport.test(fs.readFileSync(file, 'utf8'))).map(relative);

    expect(offenders).toEqual([]);
  });

  it('loads the static ECharts setup only from the chart server', () => {
    const offenders = sources(SRC).filter((file) => /utilities\/echartsSetup['"]/.test(fs.readFileSync(file, 'utf8')))
      .map(relative).filter((file) => !file.startsWith('src/renderer/'));

    expect(offenders).toEqual([]);
  });
});
