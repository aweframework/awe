'use strict';

/** Documentation coverage of the declared attributes, per schema. */

/** Share of documented attributes, rounded to a whole percentage; null when there is no attribute. */
function percent(documented, attributes) {
	return attributes === 0 ? null : Math.round((100 * documented) / attributes);
}

function coverageReport(reference) {
	const rows = reference.schemas.map((schema) => ({
		file: schema.file,
		attributes: schema.stats.attributes,
		documented: schema.stats.documented,
		undocumented: schema.stats.attributes - schema.stats.documented,
		coverage: percent(schema.stats.documented, schema.stats.attributes),
	}));
	const attributes = rows.reduce((sum, r) => sum + r.attributes, 0);
	const documented = rows.reduce((sum, r) => sum + r.documented, 0);
	return {
		rows,
		total: {file: 'TOTAL', attributes, documented, undocumented: attributes - documented, coverage: percent(documented, attributes)},
	};
}

function formatReport(report) {
	const header = ['schema', 'attributes', 'documented', 'undocumented', 'coverage'];
	const lines = [...report.rows, report.total].map((r) => [r.file, r.attributes, r.documented, r.undocumented, r.coverage === null ? '-' : `${r.coverage}%`].map(String));
	const widths = header.map((h, i) => Math.max(h.length, ...lines.map((l) => l[i].length)));
	const format = (cells) => cells.map((c, i) => (i === 0 ? c.padEnd(widths[i]) : c.padStart(widths[i]))).join('  ');
	return ['XSD attribute documentation coverage', format(header), ...lines.map(format)].join('\n');
}

module.exports = {coverageReport, formatReport, percent};
