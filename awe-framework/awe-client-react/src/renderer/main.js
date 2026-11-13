import {createChartServer} from "./chartServer";

/**
 * Entry point of the AWE chart server. Environment:
 * - `PORT`: port to listen to (7801, the port of the Highcharts export server, by default)
 * - `HOST`: address to listen to (all the addresses by default)
 * - `MAX_BODY_SIZE`: maximum size of a request body in bytes (10 MB by default)
 */
const port = Number(process.env.PORT || 7801);
const host = process.env.HOST || "0.0.0.0";
const maxBodySize = process.env.MAX_BODY_SIZE ? Number(process.env.MAX_BODY_SIZE) : undefined;

const server = createChartServer({maxBodySize});
server.listen(port, host, () => console.info(`[INFO] AWE chart server listening on ${host}:${port}`));

const stop = () => server.close(() => process.exit(0));
process.on("SIGTERM", stop);
process.on("SIGINT", stop);
