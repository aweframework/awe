'use strict';

/** Which properties still miss a description or a default value (report only: it never fails the build). */
function propertyReport(entries) {
	return {
		total: entries.length,
		withoutDescription: entries.filter((e) => !e.description).map((e) => e.name),
		withoutDefault: entries.filter((e) => !e.defaultValue).map((e) => e.name),
	};
}

function formatReport(report) {
	const lines = [
		`Properties reference: ${report.total} properties`,
		`  without description: ${report.withoutDescription.length}`,
		...report.withoutDescription.map((name) => `    ${name}`),
		`  without default: ${report.withoutDefault.length} (many have none by design: optional, per-environment or list/map values)`,
		...report.withoutDefault.map((name) => `    ${name}`),
	];
	return lines.join('\n');
}

module.exports = {propertyReport, formatReport};
