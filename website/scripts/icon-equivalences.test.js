// Run with: node --test website/scripts
// The icon pages document the equivalence table the engines use (awe-design-tokens/icons/icon-equivalences.json):
// keep the tables of the docs (en + es) in sync with it.
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');

const WEBSITE = path.join(__dirname, '..');
const EQUIVALENCES = path.join(WEBSITE, '..', 'awe-framework', 'awe-design-tokens', 'icons', 'icon-equivalences.json');
const LANGUAGES = {en: 'docs', es: 'i18n/es/docusaurus-plugin-content-docs/current'};

const equivalences = JSON.parse(fs.readFileSync(EQUIVALENCES, 'utf8'));
const read = (root, page) => fs.readFileSync(path.join(WEBSITE, root, page), 'utf8');

// Table rows "| `a`, `b` | `target` |" -> [[a, target], [b, target]]
function codeRows(markdown) {
	const rows = [];
	markdown.split('\n').forEach((line) => {
		const match = /^\|\s*((?:`[^`]+`(?:,\s*)?)+)\s*\|\s*`([^`]+)`\s*\|\s*$/.exec(line);
		if (match) {
			[...match[1].matchAll(/`([^`]+)`/g)].forEach(([, name]) => rows.push([name, match[2]]));
		}
	});
	return rows;
}

// Table rows "| `name` | plain text |" -> names
function reasonRows(markdown) {
	return markdown
		.split('\n')
		.map((line) => /^\|\s*`([^`]+)`\s*\|\s*[^`|\s][^|]*\|\s*$/.exec(line))
		.filter(Boolean)
		.map((match) => match[1]);
}

const expectedRows = () => [
	...Object.entries(equivalences.fa),
	...Object.entries(equivalences.material).map(([name, lucide]) => [`mdi:${name}`, lucide]),
];
const sorted = (rows) => rows.map((row) => row.join(' -> ')).sort();

Object.entries(LANGUAGES).forEach(([language, root]) => {
	test(`${language} icons page lists every fa and mdi equivalence of the engines`, () => {
		assert.deepStrictEqual(sorted(codeRows(read(root, 'api/icons.md'))), sorted(expectedRows()));
	});

	test(`${language} icons page lists every name without equivalence`, () => {
		assert.deepStrictEqual(reasonRows(read(root, 'api/icons.md')).sort(), [...equivalences.fallback.fa].sort());
	});

	test(`${language} icons page names the pinned Lucide version`, () => {
		assert.match(read(root, 'api/icons.md'), new RegExp(`lucide-static\` ${equivalences.lucideVersion.replace(/\./g, '\\.')}`));
	});

	test(`${language} foundations examples use the Lucide icon of the equivalence table`, () => {
		const rows = codeRows(read(root, 'design-system/foundations.md'));
		assert.ok(rows.length > 0, 'the iconography example table is missing');
		rows.forEach(([name, lucide]) => assert.strictEqual(lucide, equivalences.fa[name.replace(/^fa-/, '')], name));
	});
});
