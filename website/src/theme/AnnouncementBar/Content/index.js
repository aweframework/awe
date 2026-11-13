/**
 * Content of the announcement bar (swizzled, ejected from @docusaurus/theme-classic).
 *
 * The theme renders `themeConfig.announcementBar.content` as raw HTML, which cannot be translated: the config is loaded
 * once for every locale. This component renders the AWE 5 release announcement with <Translate> (es strings in
 * i18n/es/code.json) and <Link>, which prefixes the locale (/es/...). The `content` of the config is only the fallback
 * text the theme needs to show the bar; change its `id` with every new announcement.
 */
import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Translate from '@docusaurus/Translate';
import styles from './styles.module.css';

export default function AnnouncementBarContent(props) {
	return (
		<div {...props} className={clsx(styles.content, props.className)}>
			<Link className="awe-announcement" to="/docs/guides/v5-migration">
				<span className="awe-announcement-emoji" aria-hidden="true">🎉</span>
				<span className="awe-announcement-title">
					<Translate id="announcementBar.awe5.title" description="Title of the AWE 5 release announcement">
						AWE 5 is out!
					</Translate>
				</span>
				<span className="awe-announcement-link">
					<Translate id="announcementBar.awe5.link" description="Link of the AWE 5 announcement to the migration guide">
						Read the migration guide
					</Translate>
					{' →'}
				</span>
			</Link>
		</div>
	);
}
