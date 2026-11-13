// Remark plugin for the docs of the default instance: rewrites a relative markdown link (`screen.md`,
// `../guides/x.md#anchor`) to a path from the root of the docs it lives in (`api/screen.md`, `guides/x.md#anchor`).
//
// Why: Docusaurus resolves `./x.md` and `../x.md` against the folder of the page's own source file, but a page of
// the Spanish locale lives in `i18n/es/...` only when it is translated, and falls back to `docs/` when it is not.
// A relative link from an English (fallback) page to a translated page, or the other way round, looks for the
// target in the wrong tree and breaks the build ("Docusaurus found broken links"). A link from the content root
// is tried against the localized root first and then against the original one, so it finds the page either way.
//
// Only links that stay inside the docs root are rewritten; anything else is left untouched.

const fs = require('fs');
const path = require('path');

const ROOT = /^(docs|versioned_docs\/version-[^/]+|i18n\/[^/]+\/docusaurus-plugin-content-docs\/(current|version-[^/]+))\/(.+)$/;

// The folders Docusaurus tries first for a link that does not start with ./ or ../ (the content roots)
function contentRoots(root) {
	const own = root[1];
	if (!own.startsWith('i18n/')) return [own];
	return [own, root[2] === 'current' ? 'docs' : `versioned_docs/${root[2]}`];
}

function hasTranslatedTree(siteDir, version) {
	const i18n = path.join(siteDir, 'i18n');
	if (!fs.existsSync(i18n)) return false;
	return fs.readdirSync(i18n).some((locale) =>
		fs.existsSync(path.join(i18n, locale, 'docusaurus-plugin-content-docs', version)));
}

function rewrite(url, filePathFromSite, siteDir) {
	if (/^([a-z][a-z0-9+.-]*:|\/|#)/i.test(url)) return url;
	const match = /^([^#?]+\.mdx?)([#?].*)?$/.exec(url);
	if (!match) return url;
	const root = ROOT.exec(filePathFromSite);
	if (!root) return url;
	// A docs version without any translated tree (e.g. the injected 4.x maintenance line) has no cross-tree links:
	// Docusaurus resolves its relative links as written, so leave them alone
	const version = /^versioned_docs\/(version-[^/]+)$/.exec(root[1]);
	if (version && siteDir && !hasTranslatedTree(siteDir, version[1])) return url;
	// A link such as api/x.md already is a link from the content root (Docusaurus tries the roots first): keep it
	if (siteDir && !/^\.\.?\//.test(match[1])
		&& contentRoots(root).some((r) => fs.existsSync(path.join(siteDir, r, match[1])))) return url;
	const fromRoot = path.posix.normalize(path.posix.join(path.posix.dirname(root[3]), match[1]));
	if (fromRoot.startsWith('../') || fromRoot === '..') return url;
	return fromRoot + (match[2] || '');
}

function visit(node, fn) {
	fn(node);
	if (node.children) node.children.forEach((child) => visit(child, fn));
}

module.exports = function remarkContentRootLinks({siteDir = process.cwd()} = {}) {
	return (tree, file) => {
		const filePath = file && (file.path || (file.history && file.history[0]));
		if (!filePath) return;
		const fromSite = path.relative(siteDir, filePath).split(path.sep).join('/');
		visit(tree, (node) => {
			if ((node.type === 'link' || node.type === 'definition') && node.url) {
				node.url = rewrite(node.url, fromSite, siteDir);
			}
		});
	};
};

module.exports.rewrite = rewrite;
