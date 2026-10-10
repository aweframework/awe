const versions = require('./versions.json');
const {ensureGenerated} = require('./scripts/xsd-reference/generate');
const {ensureGenerated: ensurePropertiesGenerated} = require('./scripts/properties-reference/generate');
const remarkContentRootLinks = require('./scripts/remark-content-root-links');

// The "reference" docs instance below needs its folder for ANY docusaurus command (build, deploy, swizzle,
// docs:version...), not only the npm scripts that regenerate it first (see the "pre" scripts in package.json).
ensureGenerated();
ensurePropertiesGenerated();

// "current" (develop, 5.0) is always present. A maintenance-line entry (e.g. "4.x") is
// only present in versions.json inside a CI checkout, after bin/website-maintenance-docs.sh
// has injected it -- local/MR builds without that injection must still configure and build.
const docVersions = {
	current: {
		label: 'Next (5.0)',
	},
};
for (const version of versions) {
	if (/^\d+\.x$/.test(version)) {
		docVersions[version] = {
			label: `${version} (maintenance)`,
			banner: 'none',
		};
	}
}

module.exports = {
	title: 'Awe framework',
	tagline: 'Low coding complete functional web applications',
	url: 'https://docs.aweframework.com',
	baseUrl: '/',
	onBrokenLinks: 'throw',
	onBrokenAnchors: 'warn',
	favicon: 'icon/favicon.ico',
	organizationName: 'aweframework',
	projectName: 'awe',
	markdown: {
		mermaid: true,
	},
	themes: ['@docusaurus/theme-mermaid'],
	plugins: [
		// Reference generated from the sources at build time (see scripts/xsd-reference). It is a docs
		// instance of its own so that it is English only (it has no translations) and is kept out of the
		// "docs:version" snapshots of the default instance.
		[
			'@docusaurus/plugin-content-docs',
			{
				id: 'reference',
				path: 'reference',
				routeBasePath: 'reference',
				sidebarPath: require.resolve('./reference-sidebars.js'),
			},
		],
	],
	themeConfig: {
		docs: {
			sidebar: {
				hideable: true,
			},
		},
		navbar: {
			title: 'Awe',
			logo: {
				alt: 'Awe framework Logo',
				src: 'img/logo.svg',
				// srcDark: 'img/logo white.svg'
			},
			hideOnScroll: true,
			items: [
				{
					type: 'docsVersion',
					position: 'left',
					label: 'Docs',
				},
				{
					type: 'docsVersionDropdown',
					position: 'right',
				},
				{
					type: 'localeDropdown',
					position: 'right',
				},
				{
					type: 'docSidebar',
					docsPluginId: 'reference',
					sidebarId: 'reference',
					position: 'left',
					label: 'XSD reference',
				},
				{
					type: 'doc',
					docId: 'training/awe-101',
					position: 'left',
					label: 'Training'
				},
				{
					to: 'blog',
					label: 'Blog',
					position: 'left'
				},
				{
					href: 'https://gitlab.com/aweframework/awe',
					className: 'header-gitlab-link',
					position: 'right',
					'aria-label': 'Awe framework GitLab repository',
					title: 'Awe framework GitLab repository'
				},
			],
		},
		footer: {
			style: 'dark',
			logo: {
				alt: 'Awe framework Logo',
				src: 'img/logo.svg',
				href: 'https://docs.aweframework.com',
			},
			links: [
				{
					title: 'Docs',
					items: [
						{
							label: 'Introduction',
							to: 'docs/',
						},
						{
							label: 'Migration from v3 to v4',
							to: 'docs/guides/v4-migration',
						},
					],
				},
				{
					title: 'Community',
					items: [
						{
							label: 'Stack Overflow',
							href: 'https://stackoverflow.com/questions/tagged/awe',
						}
					],
				},
				{
					title: 'More',
					items: [
						{
							label: 'Gitlab repository',
							href: 'https://gitlab.com/aweframework/awe',
						},
						{
							label: 'Javadoc',
							href: 'https://aweframework.gitlab.io/awe/javadoc-api/index.html',
						}
					],
				},
			],
			copyright: `Copyright © ${new Date().getFullYear()} Awe framework, Almis Informática S.L.`,
		},
		algolia: {
            appId: 'G6FC3RBAES',
            apiKey: 'bc4e0c0dd0883aac1d7ce0ae2dcb1ad8',
            indexName: 'aweframework_awe',
			contextualSearch: true,
		},
		colorMode: {
			defaultMode: 'dark',
			disableSwitch: false,
			respectPrefersColorScheme: false,
		},
		prism: {
			additionalLanguages: ['java'],
		},
	},
	i18n: {
		defaultLocale: 'en',
		locales: ['en', 'es'],
		localeConfigs: {
			en: {
				label: 'English',
				direction: 'ltr',
			},
			es: {
				label: 'Español',
				direction: 'ltr',
			},
		}
	},
	presets: [
		[
			'@docusaurus/preset-classic',
			{
				docs: {
					sidebarPath: require.resolve('./sidebars.js'),
					// Per-version edit link: current -> develop, a maintenance line (e.g. "4.x")
					// -> its own support/<version> branch, any other (frozen) version -> master.
					editUrl: ({ version, docPath }) => {
						if (version === 'current') {
							return `https://gitlab.com/aweframework/awe/edit/develop/website/docs/${docPath}`;
						}
						if (/^\d+\.x$/.test(version)) {
							return `https://gitlab.com/aweframework/awe/edit/support/${version}/website/docs/${docPath}`;
						}
						return `https://gitlab.com/aweframework/awe/edit/master/website/versioned_docs/version-${version}/${docPath}`;
					},
					// Relative .md links resolve against the page's own tree, which breaks between translated and
					// untranslated (fallback) pages of the es locale: see the plugin for the detail.
					beforeDefaultRemarkPlugins: [[remarkContentRootLinks, {siteDir: __dirname}]],
					includeCurrentVersion: true,
					showLastUpdateTime: true,
					showLastUpdateAuthor: true,
					lastVersion: 'current',
					versions: docVersions,
				},
				blog: {
					showReadingTime: true,
					editUrl: 'https://gitlab.com/aweframework/awe/edit/master/website/',
					postsPerPage: 3,
					feedOptions: {
						type: 'all',
						language: 'es',
						copyright: `Copyright © ${new Date().getFullYear()} Almis, Inc.`,
					},
				},
				theme: {
					customCss: require.resolve('./src/css/custom.css'),
				},
			},
		],
	],
};
