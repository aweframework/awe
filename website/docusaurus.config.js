module.exports = {
	title: 'Awe framework',
	tagline: 'Low coding complete functional web applications',
	url: 'https://docs.aweframework.com',
	baseUrl: '/',
	onBrokenLinks: 'throw',
	favicon: 'icon/favicon.ico',
	organizationName: 'aweframework',
	projectName: 'awe',
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
					type: 'doc',
					docId: 'training/awe-101',
					position: 'left',
					label: 'Training',
					activeSidebarClassName: 'navbar__link--active',
				},
				{
					to: 'blog',
					label: 'Blog',
					position: 'left'
				},
				{
					href: 'https://forum.aweframework.com/',
					label: 'Forum', position: 'left'
				},
				{
					alt: 'Gitlab repository',
					href: 'https://gitlab.com/aweframework/awe',
          className: 'header-gitlab-link',
					position: 'right'
				},
			],
		},
		footer: {
			style: 'dark',
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
						},
						{
							label: 'Forum',
							href: 'https://forum.aweframework.com/',
						},
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
			respectPrefersColorScheme: true,
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
					// Please change this to your repo.
					editUrl: 'https://gitlab.com/aweframework/awe/edit/master/website/',
					includeCurrentVersion: true,
					showLastUpdateTime: true,
					showLastUpdateAuthor: true,
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
