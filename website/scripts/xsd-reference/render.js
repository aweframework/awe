'use strict';

/** Renders the reference model as MDX pages (one per schema plus an index). */

const {percent} = require('./report');

// An enumeration with more values than this is not repeated in every attribute row: the row links to the
// section of the type instead, which keeps the attribute tables readable.
const MAX_INLINE_VALUES = 8;

/** Escapes free text so MDX renders it literally. */
function esc(text) {
	return String(text)
		.replace(/\\/g, '\\\\')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/\{/g, '&#123;')
		.replace(/\}/g, '&#125;')
		.replace(/\|/g, '\\|')
		.replace(/([`*_[\]])/g, '\\$1');
}

/** Inline code; a pipe is escaped because the value may sit in a table cell. */
function code(text) {
	const value = String(text).replace(/\|/g, '\\|');
	return value.includes('`') ? `\`\` ${value} \`\`` : `\`${value}\``;
}

function typeAnchor(name) {
	return `type-${name.toLowerCase()}`;
}

function pageOf(file) {
	return `${file.replace(/\.xsd$/, '')}.mdx`;
}

function link(label, file, currentFile, anchor) {
	const page = file === currentFile ? '' : pageOf(file);
	return `[${label}](${page}#${anchor})`;
}

function frontMatter(fields) {
	// Strings are JSON-quoted, which is valid YAML and safe for titles with colons or quotes.
	const lines = Object.entries(fields).map(([key, value]) => `${key}: ${typeof value === 'string' ? JSON.stringify(value) : value}`);
	return `---\n${lines.join('\n')}\n---\n`;
}

function cardinality(child) {
	if (child.min === child.max) {
		return String(child.min);
	}
	return `${child.min}..${child.max === 'unbounded' ? 'n' : child.max}`;
}

function typeCell(type, currentFile) {
	const inlineValues = type.values.length > 0 && type.values.length <= MAX_INLINE_VALUES && type.patterns.length === 0;
	if (type.name && !inlineValues) {
		const suffix = type.values.length > 0 ? ` (${type.values.length} values)` : '';
		return `${link(code(type.name), type.file, currentFile, typeAnchor(type.name))}${suffix}`;
	}
	if (type.values.length > 0 || type.patterns.length > 0) {
		const values = type.values.map((v) => code(v)).join(', ');
		const patterns = type.patterns.map((p) => `pattern ${code(p)}`).join(', ');
		return [values, patterns].filter(Boolean).join(', ');
	}
	return code(type.builtin || 'string');
}

function attributeTable(attributes, currentFile) {
	const rows = attributes.map((a) => {
		const fallback = a.fixed !== null ? `${code(a.fixed)} (fixed)` : '';
		const defaultValue = a.default !== null ? code(a.default) : fallback;
		return `| ${code(a.name)} | ${typeCell(a.type, currentFile)} | ${a.required ? 'yes' : 'no'} | ${defaultValue} | ${a.documentation ? esc(a.documentation) : 'Not documented yet'} |`;
	});
	return ['| Attribute | Type | Required | Default | Description |', '| --- | --- | --- | --- | --- |', ...rows].join('\n');
}

function elementLink(ref, currentFile) {
	const label = code(ref.name);
	return link(label, ref.file, currentFile, ref.anchor);
}

function renderElement(element, schema) {
	const out = [`### ${code(element.name)} {#${element.anchor}}`, ''];
	if (element.documentation) {
		out.push(esc(element.documentation), '');
	}
	if (element.typeName) {
		out.push(`**Type:** ${code(element.typeName)}`, '');
	}
	const contains = element.children.map((c) => `${elementLink(c, schema.file)} (${cardinality(c)})`);
	out.push(`**Contains:** ${contains.length > 0 ? contains.join(', ') : 'no child elements'}`, '');
	const parents = element.parents.map((p) => elementLink(p, schema.file));
	out.push(`**Contained by:** ${parents.length > 0 ? parents.join(', ') : 'none (document root)'}`, '');
	if (element.textContent) {
		out.push(`**Text content:** ${code(element.textContent)}`, '');
	}
	if (element.attributes.length > 0) {
		out.push(attributeTable(element.attributes, schema.file), '');
	} else {
		out.push('**Attributes:** none', '');
	}
	return out;
}

function renderSimpleType(type) {
	const out = [`### ${code(type.name)} {#${typeAnchor(type.name)}}`, ''];
	if (type.documentation) {
		out.push(esc(type.documentation), '');
	}
	if (type.builtin) {
		out.push(`**Base type:** ${code(type.builtin)}`, '');
	}
	if (type.values.length > 0) {
		out.push(`**Allowed values:** ${type.values.map((v) => code(v)).join(', ')}`, '');
	}
	if (type.patterns.length > 0) {
		out.push(`**Pattern:** ${type.patterns.map((p) => code(p)).join(', ')}`, '');
	}
	return out;
}

function assertUniqueAnchors(schema) {
	const seen = new Set();
	const anchors = [...schema.elements.map((e) => e.anchor), ...schema.simpleTypes.map((t) => typeAnchor(t.name))];
	for (const anchor of anchors) {
		if (seen.has(anchor)) {
			throw new Error(`Duplicate anchor "${anchor}" in ${schema.file}`);
		}
		seen.add(anchor);
	}
}

function renderSchema(schema, position) {
	assertUniqueAnchors(schema);
	const out = [
		frontMatter({id: schema.id, title: schema.title, sidebar_label: schema.file, sidebar_position: position}),
		`Generated from \`${schema.file}\`.` +
		(schema.includes.length > 0 ? ` It includes ${schema.includes.map((f) => `[${f}](${pageOf(f)})`).join(', ')}.` : ''),
		'',
	];
	if (schema.documentation) {
		out.push(esc(schema.documentation), '');
	}
	const {attributes, documented} = schema.stats;
	out.push(`${schema.elements.length} elements, ${attributes} declared attributes (${documented} documented).`, '');
	if (schema.elements.length > 0) {
		out.push('## Elements', '');
		out.push(`Elements: ${schema.elements.map((e) => link(code(e.name), schema.file, schema.file, e.anchor)).join(', ')}`, '');
		schema.elements.forEach((element) => out.push(...renderElement(element, schema)));
	}
	if (schema.simpleTypes.length > 0) {
		out.push('## Simple types', '');
		schema.simpleTypes.forEach((type) => out.push(...renderSimpleType(type)));
	}
	return `${out.join('\n').replace(/\n{3,}/g, '\n\n').trimEnd()}\n`;
}

function renderIndex(reference) {
	const rows = reference.schemas.map((s) => {
		const coverage = percent(s.stats.documented, s.stats.attributes);
		return `| [${s.file}](${pageOf(s.file)}) | ${esc(s.title)} | ${s.elements.length} | ${s.stats.attributes} | ${coverage === null ? '-' : `${coverage}%`} |`;
	});
	return [
		frontMatter({id: 'index', title: 'XSD reference', sidebar_label: 'Overview', sidebar_position: 0, slug: '/'}),
		'Reference of the XML schemas (XSD) that define the AWE descriptor files: screens, queries, maintain targets, menus and so on.',
		'This reference is generated from the schemas at every site build, so it always matches the framework sources.',
		'It is available in English only.',
		'',
		'| Schema | Description | Elements | Declared attributes | Documented |',
		'| --- | --- | --- | --- | --- |',
		...rows,
		'',
		'The "Documented" column is the share of declared attributes that carry an `xs:documentation` entry in the schema.',
		'To improve a description, add `xs:documentation` to the attribute in the XSD under `awe-generic-screens`.',
		'',
	].join('\n');
}

/**
 * @returns {Object<string, string>} MDX content by file name
 */
function renderPages(reference) {
	const pages = {'index.mdx': renderIndex(reference)};
	reference.schemas.forEach((schema, index) => {
		pages[pageOf(schema.file)] = renderSchema(schema, index + 1);
	});
	return pages;
}

module.exports = {renderPages};
