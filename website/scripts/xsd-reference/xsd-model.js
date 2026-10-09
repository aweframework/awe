'use strict';

/**
 * Reads the AWE XSD files and builds the reference model the site pages are rendered from:
 * for every schema its elements (attributes, children, containers, documentation) and simple types.
 *
 * The AWE schemas include each other through absolute https://aweframework.gitlab.io/... URLs. Those
 * URLs are only used as names: the file is always read from the local schema directory.
 */

const fs = require('node:fs');
const path = require('node:path');
const {DOMParser} = require('@xmldom/xmldom');

const XS = 'http://www.w3.org/2001/XMLSchema';
const PARTICLES = ['element', 'sequence', 'choice', 'group', 'any'];

function parseXml(text, file) {
	const onError = (level, message) => {
		if (level !== 'warning') {
			throw new Error(`Cannot parse ${file} (${level}): ${message}`);
		}
	};
	return new DOMParser({onError}).parseFromString(text, 'text/xml');
}

/** Child elements of an XSD node, optionally filtered by local name. */
function xsChildren(node, ...names) {
	const result = [];
	for (let child = node.firstChild; child; child = child.nextSibling) {
		if (child.nodeType === 1 && child.namespaceURI === XS && (names.length === 0 || names.includes(child.localName))) {
			result.push(child);
		}
	}
	return result;
}

function stripPrefix(qname) {
	return qname.includes(':') ? qname.split(':').pop() : qname;
}

function textOf(node) {
	return node.textContent.replace(/\s+/g, ' ').trim();
}

/** Text of the xs:documentation entries of the xs:annotation of a node, or null when there is none. */
function documentationOf(node) {
	const texts = [];
	for (const annotation of xsChildren(node, 'annotation')) {
		for (const documentation of xsChildren(annotation, 'documentation')) {
			const text = textOf(documentation);
			if (text) {
				texts.push(text);
			}
		}
	}
	return texts.length > 0 ? texts.join(' ') : null;
}

function schemaFiles(dir) {
	return fs.readdirSync(dir).filter((f) => f.endsWith('.xsd')).sort();
}

function parseSchema(dir, file) {
	const dom = parseXml(fs.readFileSync(path.join(dir, file), 'utf8'), file);
	const root = dom.documentElement;
	const schema = {
		file,
		root,
		includes: [],
		title: null,
		documentation: documentationOf(root),
		components: {element: new Map(), attribute: new Map(), complexType: new Map(), simpleType: new Map(), group: new Map(), attributeGroup: new Map()},
	};
	for (const annotation of xsChildren(root, 'annotation')) {
		for (const appinfo of xsChildren(annotation, 'appinfo')) {
			schema.title = schema.title || textOf(appinfo);
		}
	}
	for (const child of xsChildren(root)) {
		const kind = child.localName;
		if (kind === 'include') {
			const location = child.getAttribute('schemaLocation');
			const target = location.split('/').pop();
			if (!fs.existsSync(path.join(dir, target))) {
				throw new Error(`Cannot resolve include "${location}" of ${file}: ${path.join(dir, target)} does not exist`);
			}
			schema.includes.push(target);
		} else if (schema.components[kind] && child.hasAttribute('name')) {
			schema.components[kind].set(child.getAttribute('name'), child);
		}
	}
	return schema;
}

/**
 * Loads the given schema files (all the *.xsd of the directory by default) and, recursively, the
 * schemas they include. The result is sorted by file name.
 */
function loadSchemas(dir, entries = schemaFiles(dir)) {
	const loaded = new Map();
	const pending = [...entries];
	while (pending.length > 0) {
		const file = pending.shift();
		if (!loaded.has(file)) {
			const schema = parseSchema(dir, file);
			loaded.set(file, schema);
			pending.push(...schema.includes);
		}
	}
	return [...loaded.values()].sort((a, b) => a.file.localeCompare(b.file));
}

function declaredAttributeStats(schema) {
	let attributes = 0;
	let documented = 0;
	// An attribute with a "ref" only uses a declaration counted where it is declared.
	for (const node of Array.from(schema.root.getElementsByTagNameNS(XS, 'attribute')).filter((n) => n.hasAttribute('name'))) {
		attributes += 1;
		if (documentationOf(node)) {
			documented += 1;
		}
	}
	return {attributes, documented};
}

/** Resolution of names across the include closure of a schema. */
class Resolver {
	constructor(schemas) {
		this.byFile = new Map(schemas.map((s) => [s.file, s]));
		this.closures = new Map();
		this.cache = new Map();
	}

	closure(file) {
		if (!this.closures.has(file)) {
			const order = [];
			const visit = (f) => {
				if (!order.includes(f)) {
					order.push(f);
					this.byFile.get(f).includes.forEach(visit);
				}
			};
			visit(file);
			this.closures.set(file, order);
		}
		return this.closures.get(file);
	}

	lookup(kind, name, fromFile) {
		for (const file of this.closure(fromFile)) {
			const node = this.byFile.get(file).components[kind].get(name);
			if (node) {
				return {node, file};
			}
		}
		return null;
	}

	require(kind, name, fromFile) {
		const found = this.lookup(kind, name, fromFile);
		if (!found) {
			throw new Error(`Cannot resolve ${kind} "${name}" referenced from ${fromFile}`);
		}
		return found;
	}

	memo(key, compute) {
		if (!this.cache.has(key)) {
			this.cache.set(key, compute());
		}
		return this.cache.get(key);
	}

	/** Describes a simple type: allowed values, patterns and the built-in type it ends up in. */
	simpleType(node, file, name) {
		const describe = () => {
			const info = {name, builtin: null, values: [], patterns: [], file, documentation: documentationOf(node)};
			for (const child of xsChildren(node, 'restriction', 'union', 'list')) {
				if (child.localName === 'restriction') {
					const base = child.hasAttribute('base') ? this.typeInfo(child.getAttribute('base'), null, file) : null;
					const inline = xsChildren(child, 'simpleType')[0];
					const parent = base || (inline ? this.simpleType(inline, file, null) : null);
					const values = xsChildren(child, 'enumeration').map((e) => e.getAttribute('value'));
					const patterns = xsChildren(child, 'pattern').map((p) => p.getAttribute('value'));
					info.builtin = parent ? parent.builtin : null;
					info.values = values.length > 0 ? values : parent ? parent.values : [];
					info.patterns = patterns.length > 0 ? patterns : parent ? parent.patterns : [];
				} else if (child.localName === 'union') {
					const members = (child.getAttribute('memberTypes') || '').split(/\s+/).filter(Boolean)
						.map((m) => this.typeInfo(m, null, file));
					members.push(...xsChildren(child, 'simpleType').map((s) => this.simpleType(s, file, null)));
					info.values = [...new Set(members.flatMap((m) => m.values))];
					info.patterns = [...new Set(members.flatMap((m) => m.patterns))];
				} else {
					info.builtin = 'list';
				}
			}
			return info;
		};
		return name ? this.memo(`simple:${file}#${name}`, describe) : describe();
	}

	/** Type of an attribute or element: a built-in type, a named simple type or an inline one. */
	typeInfo(typeName, inlineNode, file) {
		if (inlineNode) {
			return this.simpleType(inlineNode, file, null);
		}
		if (!typeName) {
			return {name: null, builtin: 'string', values: [], patterns: [], file: null, documentation: null};
		}
		if (!typeName.includes(':')) {
			const found = this.lookup('simpleType', typeName, file);
			if (found) {
				return this.simpleType(found.node, found.file, typeName);
			}
		}
		return {name: null, builtin: stripPrefix(typeName), values: [], patterns: [], file: null, documentation: null};
	}

	attribute(node, file) {
		if (node.hasAttribute('ref')) {
			const global = this.require('attribute', stripPrefix(node.getAttribute('ref')), file);
			const declared = this.attribute(global.node, global.file);
			return {
				...declared,
				required: node.getAttribute('use') === 'required' || declared.required,
				default: node.hasAttribute('default') ? node.getAttribute('default') : declared.default,
				fixed: node.hasAttribute('fixed') ? node.getAttribute('fixed') : declared.fixed,
				documentation: documentationOf(node) || declared.documentation,
			};
		}
		return {
			name: node.getAttribute('name'),
			type: this.typeInfo(node.getAttribute('type'), xsChildren(node, 'simpleType')[0], file),
			required: node.getAttribute('use') === 'required',
			default: node.hasAttribute('default') ? node.getAttribute('default') : null,
			fixed: node.hasAttribute('fixed') ? node.getAttribute('fixed') : null,
			documentation: documentationOf(node),
			file,
		};
	}

	/** Attributes, children and text content of a complex type, following groups and extensions. */
	complexType(node, file, name, owner = name) {
		const compute = () => {
			// "owner" names the type or element that declares local elements, to give them a unique anchor.
			const content = {attributes: new Map(), children: [], textContent: null, locals: [], owner};
			this.collectContent(node, file, content, {min: 1, max: 1}, new Set());
			return content;
		};
		return name ? this.memo(`complex:${file}#${name}`, compute) : compute();
	}

	collectContent(node, file, content, occurs, visiting) {
		for (const child of xsChildren(node)) {
			switch (child.localName) {
				case 'attribute': {
					const attribute = this.attribute(child, file);
					content.attributes.set(attribute.name, attribute);
					break;
				}
				case 'attributeGroup': {
					const group = this.require('attributeGroup', stripPrefix(child.getAttribute('ref')), file);
					this.guard(visiting, `attributeGroup:${group.file}#${child.getAttribute('ref')}`, () => this.collectContent(group.node, group.file, content, occurs, visiting));
					break;
				}
				case 'sequence':
				case 'choice':
				case 'all':
				case 'group':
					this.collectParticle(child, file, content, occurs, visiting);
					break;
				case 'simpleContent':
				case 'complexContent':
				case 'extension':
				case 'restriction':
					this.collectDerivation(child, file, content, occurs, visiting);
					break;
				default:
					break;
			}
		}
	}

	collectDerivation(node, file, content, occurs, visiting) {
		if (node.localName === 'extension' || node.localName === 'restriction') {
			const base = node.getAttribute('base');
			const baseType = base && !base.includes(':') ? this.lookup('complexType', base, file) : null;
			if (baseType) {
				this.guard(visiting, `complexType:${baseType.file}#${base}`, () => this.collectContent(baseType.node, baseType.file, content, occurs, visiting));
			} else if (base && node.parentNode.localName === 'simpleContent') {
				content.textContent = this.typeInfo(base, null, file).builtin || stripPrefix(base);
			}
		}
		this.collectContent(node, file, content, occurs, visiting);
	}

	collectParticle(node, file, content, occurs, visiting) {
		const min = node.hasAttribute('minOccurs') ? Number(node.getAttribute('minOccurs')) : 1;
		const maxAttr = node.hasAttribute('maxOccurs') ? node.getAttribute('maxOccurs') : '1';
		const max = maxAttr === 'unbounded' ? 'unbounded' : Number(maxAttr);
		const local = {
			min: occurs.min * min,
			max: occurs.max === 'unbounded' || max === 'unbounded' ? 'unbounded' : occurs.max * max,
		};
		if (node.localName === 'group') {
			const group = this.require('group', stripPrefix(node.getAttribute('ref')), file);
			this.guard(visiting, `group:${group.file}#${node.getAttribute('ref')}`, () => {
				for (const particle of xsChildren(group.node, 'sequence', 'choice', 'all')) {
					this.collectParticle(particle, group.file, content, local, visiting);
				}
			});
			return;
		}
		const alternatives = node.localName === 'choice' && xsChildren(node, ...PARTICLES).length > 1;
		const inherited = alternatives ? {min: 0, max: local.max} : local;
		for (const child of xsChildren(node)) {
			if (child.localName === 'element') {
				this.addChild(child, file, content, inherited);
			} else if (['sequence', 'choice', 'all', 'group'].includes(child.localName)) {
				this.collectParticle(child, file, content, inherited, visiting);
			}
		}
	}

	addChild(node, file, content, occurs) {
		const ref = node.getAttribute('ref');
		const childName = stripPrefix(ref || node.getAttribute('name'));
		const declaring = ref ? this.require('element', childName, file).file : file;
		// A local declaration is not a global element: it gets an entry of its own, anchored under its owner.
		const anchor = ref ? elementAnchor(childName) : elementAnchor(`${content.owner}--${childName}`);
		if (!ref && !content.locals.some((l) => l.anchor === anchor)) {
			content.locals.push({node, file, anchor});
		}
		const min = occurs.min * (node.hasAttribute('minOccurs') ? Number(node.getAttribute('minOccurs')) : 1);
		const maxAttr = node.hasAttribute('maxOccurs') ? node.getAttribute('maxOccurs') : '1';
		const max = occurs.max === 'unbounded' || maxAttr === 'unbounded' ? 'unbounded' : occurs.max * Number(maxAttr);
		const existing = content.children.find((c) => c.anchor === anchor && c.file === declaring);
		if (existing) {
			existing.min = Math.min(existing.min, min);
			existing.max = existing.max === 'unbounded' || max === 'unbounded' ? 'unbounded' : Math.max(existing.max, max);
		} else {
			content.children.push({name: childName, file: declaring, anchor, min, max});
		}
	}

	guard(visiting, key, action) {
		if (!visiting.has(key)) {
			visiting.add(key);
			try {
				action();
			} finally {
				visiting.delete(key);
			}
		}
	}

	element(node, file, anchor = elementAnchor(node.getAttribute('name'))) {
		const name = node.getAttribute('name');
		const typeName = node.getAttribute('type');
		const inline = xsChildren(node, 'complexType')[0];
		let content = {attributes: new Map(), children: [], textContent: null, locals: []};
		let typeDocumentation = null;
		if (inline) {
			content = this.complexType(inline, file, null, name);
			typeDocumentation = documentationOf(inline);
		} else if (typeName) {
			const complex = typeName.includes(':') ? null : this.lookup('complexType', typeName, file);
			if (complex) {
				content = this.complexType(complex.node, complex.file, typeName);
				typeDocumentation = documentationOf(complex.node);
			} else {
				const simple = this.typeInfo(typeName, null, file);
				content.textContent = simple.name || simple.builtin;
			}
		}
		const element = {
			name,
			file,
			anchor,
			typeName: typeName ? stripPrefix(typeName) : null,
			documentation: documentationOf(node) || typeDocumentation,
			attributes: [...content.attributes.values()],
			children: content.children,
			textContent: content.textContent,
			parents: [],
		};
		// Internal: local element declarations found while resolving, turned into entries by buildReference.
		Object.defineProperty(element, 'locals', {value: content.locals});
		return element;
	}
}

function elementAnchor(name) {
	return `element-${name}`;
}

/**
 * Builds the reference model of the loaded schemas.
 * @returns {{schemas: Array}} one entry per schema with its elements, simple types and attribute stats
 */
function buildReference(schemas) {
	const resolver = new Resolver(schemas);
	const entries = [];
	const seen = new Set();
	const add = (element) => {
		seen.add(`${element.file}#${element.anchor}`);
		entries.push(element);
	};
	schemas.forEach((schema) => [...schema.components.element.values()].forEach((node) => add(resolver.element(node, schema.file))));
	// Local element declarations become entries of their own (appended: entries grows while it is walked).
	for (let i = 0; i < entries.length; i += 1) {
		for (const local of entries[i].locals) {
			if (!seen.has(`${local.file}#${local.anchor}`)) {
				add(resolver.element(local.node, local.file, local.anchor));
			}
		}
	}
	const result = schemas.map((schema) => ({
		file: schema.file,
		id: schema.file.replace(/\.xsd$/, ''),
		title: schema.title || schema.file,
		documentation: schema.documentation,
		includes: schema.includes,
		elements: entries.filter((e) => e.file === schema.file),
		simpleTypes: [...schema.components.simpleType.entries()].map(([name, node]) => resolver.simpleType(node, schema.file, name)),
		stats: declaredAttributeStats(schema),
	}));
	const byKey = new Map(entries.map((e) => [`${e.file}#${e.anchor}`, e]));
	for (const parent of entries) {
		for (const child of parent.children) {
			const target = byKey.get(`${child.file}#${child.anchor}`);
			if (target && !target.parents.some((p) => p.file === parent.file && p.anchor === parent.anchor)) {
				target.parents.push({name: parent.name, file: parent.file, anchor: parent.anchor});
			}
		}
	}
	return {schemas: result};
}

module.exports = {loadSchemas, buildReference};
