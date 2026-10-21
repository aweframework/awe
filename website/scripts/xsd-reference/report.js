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
		undocumentedNames: schema.stats.undocumentedNames,
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

/**
 * Compares the undocumented attributes of each schema with its threshold (maximum allowed; 0 for a schema without one).
 * The thresholds only go down: "lowerable" lists the schemas that now have fewer undocumented attributes than allowed.
 */
function checkThresholds(report, maxUndocumented) {
	const rows = report.rows.map((r) => ({...r, max: maxUndocumented[r.file] ?? 0}));
	const failures = rows.filter((r) => r.undocumented > r.max);
	return {passed: failures.length === 0, failures, lowerable: rows.filter((r) => r.undocumented < r.max)};
}

function formatThresholdCheck(check) {
	const lines = check.failures.flatMap((f) => [
		`${f.file}: ${f.undocumented} undocumented attributes, the threshold allows ${f.max}. Document them with xs:annotation/xs:documentation:`,
		...f.undocumentedNames.map((name) => `  - ${name}`),
	]);
	lines.push(...check.lowerable.map((l) => `Fewer undocumented attributes: lower the threshold of ${l.file} from ${l.max} to ${l.undocumented} in thresholds.json`));
	return lines.join('\n');
}

module.exports = {coverageReport, formatReport, checkThresholds, formatThresholdCheck, percent};
