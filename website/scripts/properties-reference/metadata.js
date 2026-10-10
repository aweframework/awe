'use strict';

/**
 * Reads the Spring Boot configuration metadata of the AWE modules and turns it into the entries of the properties page.
 *
 * The annotation processor already merges each module's additional-spring-configuration-metadata.json into its
 * generated META-INF/spring-configuration-metadata.json, so the generated files are the only input.
 */

const fs = require('node:fs');
const path = require('node:path');

/** Modules that generate spring-configuration-metadata.json (each one guards it with a ConfigurationMetadataTest). */
const MODULES = [
	'awe-framework/awe-model',
	'awe-framework/awe-controller',
	'awe-framework/awe-testing',
	'awe-framework/awe-starters/awe-spring-boot-starter',
	'awe-framework/awe-starters/awe-notifier-spring-boot-starter',
	'awe-framework/awe-starters/awe-scheduler-spring-boot-starter',
	'awe-framework/awe-starters/awe-rest-spring-boot-starter',
	'awe-framework/awe-starters/awe-developer-spring-boot-starter',
];

const METADATA_PATH = path.join('target', 'classes', 'META-INF', 'spring-configuration-metadata.json');

/** The metadata files of the modules under `root`, and the modules that have none (not built). */
function metadataFiles(root) {
	const files = [];
	const missing = [];
	for (const module of MODULES) {
		const file = path.join(root, module, METADATA_PATH);
		if (fs.existsSync(file)) {
			files.push(file);
		} else {
			missing.push(module);
		}
	}
	return {files, missing};
}

// The hand-written "Default value x" sentence that closes many descriptions (the metadata has a real default now)
const LEGACY_MARK = /(?:^|\s)(?:The )?Default value(?: is)?\s+/g;

/**
 * Splits the trailing "Default value x" sentence off a description. Only the LAST mention counts, and only when it is the
 * final sentence (nothing like ". Next sentence" after it): the phrase inside a description is kept. A description that is
 * nothing but that sentence (match at index 0) is kept as it is, so the page never shows an empty description.
 */
function splitLegacyDefault(text) {
	const marks = [...text.matchAll(LEGACY_MARK)];
	const last = marks[marks.length - 1];
	if (!last || last.index === 0) {
		return {text, legacyDefault: undefined};
	}
	const value = text.slice(last.index + last[0].length).replace(/\.?\s*$/, '');
	if (/\.\s+[A-Z]/.test(value)) {
		return {text, legacyDefault: undefined};
	}
	return {text: text.slice(0, last.index), legacyDefault: value};
}

/** Cleans a javadoc description into markdown, and returns the default that the description used to state. */
function cleanDescription(raw) {
	const split = splitLegacyDefault(String(raw || ''));
	const legacyDefault = split.legacyDefault;
	const text = split.text
		.replace(/\{@code\s+([^}]*)\}/g, '`$1`')
		.replace(/\{@link\s+([^}\s]*)[^}]*\}/g, '$1')
		.replace(/<a\s+href="([^"]*)"[^>]*>([\s\S]*?)<\/a>/g, '[$2]($1)')
		.replace(/<li>/g, '<br/>- ')
		.replace(/<\/?(?:ul|li|p)>/g, ' ')
		.replace(/\s+/g, ' ')
		.replace(/\s*<br\/>\s*/g, '<br/>')
		.trim();
	return {text, legacyDefault};
}

/** java.util.List<java.lang.String> -> List<String>; Outer$Inner -> Inner. */
function simplifyType(type) {
	if (!type) {
		return '';
	}
	return type.replace(/(?:[A-Za-z_]\w*\.)+/g, '').replace(/\w*\$/g, '');
}

function formatDefault(value) {
	if (value === undefined || value === null) {
		return '';
	}
	if (Array.isArray(value)) {
		return value.map(String).join(', ');
	}
	if (typeof value === 'object') {
		return JSON.stringify(value);
	}
	return String(value);
}

function byName(a, b) {
	return a.name < b.name ? -1 : a.name > b.name ? 1 : 0;
}

/** Merges metadata files into one deterministic snapshot: properties sorted by name, only the fields the page uses. */
function snapshotFromFiles(files) {
	const seen = new Map();
	for (const file of files) {
		for (const property of JSON.parse(fs.readFileSync(file, 'utf8')).properties || []) {
			if (seen.has(property.name)) {
				throw new Error(`Property ${property.name} is defined by more than one module (second one in ${file})`);
			}
			const entry = {name: property.name};
			for (const key of ['type', 'description', 'defaultValue', 'deprecation']) {
				if (property[key] !== undefined) {
					entry[key] = property[key];
				}
			}
			seen.set(property.name, entry);
		}
	}
	return {properties: [...seen.values()].sort(byName)};
}

/** Page entries: cleaned, sorted, and without the hidden (internal) properties. */
function normalize(snapshot, {hidden = []} = {}) {
	const hide = new Set(hidden);
	return snapshot.properties
		.filter((property) => !hide.has(property.name))
		.map((property) => {
			const {text, legacyDefault} = cleanDescription(property.description);
			const defaultValue = formatDefault(property.defaultValue) || formatDefault(legacyDefault);
			const entry = {name: property.name, type: simplifyType(property.type), description: text, defaultValue};
			if (property.deprecation) {
				entry.deprecated = property.deprecation;
			}
			return entry;
		})
		.sort(byName);
}

module.exports = {byName, MODULES, METADATA_PATH, metadataFiles, cleanDescription, simplifyType, formatDefault, snapshotFromFiles, normalize};
