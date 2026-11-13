'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const {renderPage, MARKER, typeOf, escapeText, conflictingTags} = require('../render');

const SPEC = {
	openapi: '3.1.0',
	info: {title: 'AWE Rest API', description: 'Rest API to access AWE services', version: 'v1.0.0'},
	tags: [
		{name: 'Public API', description: 'Public interface'},
		{name: 'Protected API', description: 'Protected interface'},
		{name: 'Protected API', description: 'Protected interface'},
	],
	paths: {
		'/api/data/{queryId}': {
			post: {
				tags: ['Protected API'],
				summary: 'Query service.',
				description: 'Launch a query given <id> and {parameters} | more.',
				parameters: [{name: 'queryId', in: 'path', required: true, description: 'Query ID', schema: {type: 'string'}}],
				requestBody: {content: {'application/json': {schema: {$ref: '#/components/schemas/RequestParameter'}}}},
				responses: {
					200: {description: 'Success', content: {'*/*': {schema: {$ref: '#/components/schemas/Result'}}}},
					401: {description: 'Unauthorized'},
				},
				security: [{bearerAuth: []}],
			},
		},
		'/api/public/ping': {get: {tags: ['Public API'], summary: 'Ping.', responses: {200: {description: 'Pong'}}}},
	},
	components: {
		schemas: {
			RequestParameter: {type: 'object', description: 'Query parameters', properties: {parameters: {type: 'object', description: 'Values', additionalProperties: {}}}},
			Result: {
				type: 'object',
				required: ['type'],
				properties: {
					type: {type: 'string', enum: ['ok', 'error']},
					rows: {type: 'array', items: {$ref: '#/components/schemas/RequestParameter'}},
					total: {type: 'integer', format: 'int64', title: 'Total rows'},
				},
			},
		},
		securitySchemes: {bearerAuth: {type: 'http', scheme: 'bearer', bearerFormat: 'JWT', description: 'AWE local JWT token.'}},
	},
};

test('page has MDX front matter, the generated marker and the API title and version', () => {
	const page = renderPage(SPEC);
	assert.ok(page.startsWith('---\nid: rest-api\ntitle: REST API reference\n'));
	assert.ok(page.includes(MARKER));
	assert.ok(page.includes('Rest API to access AWE services'));
	assert.ok(page.includes('**Version:** `v1.0.0`'));
});

test('operations are grouped by tag, tags sorted and de-duplicated', () => {
	const page = renderPage(SPEC);
	const protectedAt = page.indexOf('## Protected API');
	const publicAt = page.indexOf('## Public API');
	assert.ok(protectedAt > 0 && publicAt > protectedAt);
	assert.equal(page.split('## Protected API').length, 2);
	assert.ok(page.indexOf('### `POST /api/data/{queryId}`') > protectedAt);
	assert.ok(page.indexOf('### `GET /api/public/ping`') > publicAt);
	assert.ok(page.includes('Protected interface'));
});

test('an operation shows summary, escaped description, parameters, request body, responses and security', () => {
	const page = renderPage(SPEC);
	assert.ok(page.includes('**Query service.**'));
	assert.ok(page.includes('Launch a query given &lt;id&gt; and \\{parameters\\} \\| more.'));
	assert.ok(page.includes('| `queryId` | path | `string` | yes | Query ID |'));
	assert.ok(page.includes('`application/json`: [`RequestParameter`](#requestparameter)'));
	assert.ok(page.includes('| `200` | Success | [`Result`](#result) |'));
	assert.ok(page.includes('| `401` | Unauthorized |  |'));
	assert.ok(page.includes('**Security:** `bearerAuth` (HTTP bearer, JWT)'));
});

test('an operation without security requirements is public', () => {
	const page = renderPage(SPEC);
	const publicPart = page.slice(page.indexOf('### `GET /api/public/ping`'));
	assert.ok(publicPart.includes('**Security:** none (public)'));
	assert.ok(!publicPart.includes('**Parameters**'));
	assert.ok(!publicPart.includes('**Request body**'));
});

test('schemas are listed with their properties, required flag, enums and array item types', () => {
	const page = renderPage(SPEC);
	assert.ok(page.includes('## Schemas'));
	assert.ok(page.includes('### Result'));
	assert.ok(page.includes('| `type` | `string` (`ok`, `error`) | yes |  |'));
	assert.ok(page.includes('| `rows` | array of [`RequestParameter`](#requestparameter) | no |  |'));
	assert.ok(page.includes('| `total` | `integer` (int64) | no | Total rows |'));
});

test('security schemes are documented', () => {
	const page = renderPage(SPEC);
	assert.ok(page.includes('## Authentication'));
	assert.ok(page.includes('`bearerAuth`'));
	assert.ok(page.includes('AWE local JWT token.'));
});

test('typeOf describes refs, arrays, maps, formats and untyped schemas', () => {
	assert.equal(typeOf({$ref: '#/components/schemas/Foo'}), '[`Foo`](#foo)');
	assert.equal(typeOf({type: 'array', items: {type: 'string'}}), 'array of `string`');
	assert.equal(typeOf({type: 'object', additionalProperties: {type: 'string'}}), 'map of `string`');
	assert.equal(typeOf({type: 'object', additionalProperties: {}}), 'map of `any`');
	assert.equal(typeOf({type: 'string', format: 'date-time'}), '`string` (date-time)');
	assert.equal(typeOf({}), '`any`');
	assert.equal(typeOf(undefined), '`any`');
});

test('escapeText escapes MDX braces, angle brackets and pipes but keeps code spans', () => {
	assert.equal(escapeText('a {b} <c> | `{d}`'), 'a \\{b\\} &lt;c&gt; \\| `{d}`');
	assert.equal(escapeText('line one\nline two'), 'line one<br/>line two');
	assert.equal(escapeText(undefined), '');
});

test('rendering is deterministic', () => {
	assert.equal(renderPage(SPEC), renderPage(JSON.parse(JSON.stringify(SPEC))));
});

test('a tag declared with different descriptions shows all of them, sorted, whatever the declaration order', () => {
	const spec = (tags) => ({info: {title: 'T'}, tags, paths: {'/a': {get: {tags: ['X'], responses: {}}}}});
	const forward = renderPage(spec([{name: 'X', description: 'B text'}, {name: 'X', description: 'A text'}]));
	const backward = renderPage(spec([{name: 'X', description: 'A text'}, {name: 'X', description: 'B text'}]));
	assert.equal(forward, backward);
	assert.ok(forward.indexOf('A text') > 0 && forward.indexOf('A text') < forward.indexOf('B text'));
});

test('conflictingTags lists the tags whose descriptions differ', () => {
	const tags = [{name: 'X', description: 'a'}, {name: 'X', description: 'b'}, {name: 'Y', description: 'c'}, {name: 'Y', description: 'c'}];
	assert.deepEqual(conflictingTags({tags}), ['X']);
	assert.deepEqual(conflictingTags({}), []);
});
