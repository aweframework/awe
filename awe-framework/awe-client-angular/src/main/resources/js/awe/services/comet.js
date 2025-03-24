import {aweApplication} from "../awe";
import {Client} from "@stomp/stompjs";

// Comet service
aweApplication.factory('Comet',
  ['AweUtilities', 'ActionController',
    /**
     * Retrieve the comet connection object
     * @param {Service} $utilities AweUtilities service
     * @param {Service} actionController Action controller
     * @returns {Object} Comet connection
     */
    function ($utilities, actionController) {

      // Service variables
      let connection = null;
      let encodeTransmission = false;
      let connectionToken = null;
      let events = {};

      const $comet = {
        /**
         * Set connection
         * @param {object} _connection
         * @private
         */
        setConnection: function(_connection) {
          connection = _connection;
        },
        /**
         * Retrieve connection
         * @private
         */
        getWebsocketClient: function() {
          const client = new Client({
            brokerURL: $utilities.getContextPath().replace("http", "ws") + '/websocket',
            connectHeaders: {
              'Authorization': connectionToken
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
          });

          return client;
        },
        /**
         * Retrieve connection
         * @private
         */
        getConnection: function() {
          return connection;
        },
        /**
         * Retrieve if connection is active
         * @private
         */
        isConnected: function () {
          return $comet.getConnection() !== null && $comet.getConnection().active;
        },
        /**
         * Init WebSocket Connection
         * @param encode Encode transmission
         * @param token Connection token
         */
        init: function (encode, token) {
          encodeTransmission = encode;
          connectionToken = token;
          return $comet._connect();
        },
        /**
         * Restart WebSocket Connection
         * @param token
         */
        restart: function (token) {
          connectionToken = token;
          $comet._reconnect();
        },
        /**
         * Disconnect comet connection
         */
        disconnect: function () {
          if ($comet.isConnected()) {
            return $comet._disconnect();
          }
        },
        /**
         * Connection Management
         * @private
         */
        _connect: function () {
          // Set defer
          events["connect"] = $utilities.q.defer();

          const client = $comet.getWebsocketClient();

          // Set connection
          $comet.setConnection(client);

          // On connection
          client.onConnect = function (frame) {
            // Subscribe to all broadcasted messages
            $comet.subscribe("broadcast");

            // Subscribe to own connection
            $comet.subscribe(connectionToken);

            // Resolve initialization
            events["connect"].resolve();
          };

          client.onWebSocketClose = evn => {
            switch (evn.code) {
              // Session disconnected
              case 1008:
                if ($comet.isConnected()) {
                  console.warn("Session disconnected. Returning to signin screen", evn);
                  actionController.addActionList([
                    {
                      type: "screen",
                      async: true,
                      silent: true,
                    },
                    {
                      type: "message",
                      async: true,
                      silent: true,
                      parameters: {
                        type: "warn",
                        title: "ERROR_TITLE_SESSION_EXPIRED",
                        message: "ERROR_MESSAGE_SESSION_EXPIRED"
                      }
                    },
                  ], true, {});
                }
                break;
              // Server disconnected
              case 1001:
                console.warn("Server disconnected, trying to reconnect", evn);
                break;
              // Server reconnection
              case 1006:
                break;
              // Graceful disconnection
              default:
                console.info("Graceful websocket disconnection", evn);
            }
          };

          // On connection error
          client.onStompError = frame => {
            // Reject initialization
            events["connect"].reject();

            // Will be invoked in case of error encountered at Broker
            // Bad login/passcode typically will cause an error
            // Complaint brokers will set `message` header with a brief message. Body may contain details.
            // Compliant brokers will terminate the connection after any error
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
          };

          // Activate client
          client.activate();

          // Return connection promise
          return events["connect"].promise;
        },
        _disconnect: function () {
          // Deactivate client
          return $comet.getConnection().deactivate();
        },
        /**
         * Reconnect connection
         */
        _reconnect: function () {
          // Set connection
          if ($comet.isConnected()) {
            $comet._disconnect().then($comet._connect);
          } else {
            return $comet._connect();
          }
        },
        /**
         * Subscribe
         * @param token
         */
        subscribe: function (token) {
          // Subscribe to token
          $comet.getConnection().subscribe(`/topic/${token}`, $comet.manageBroadcast);
        },
        /**
         * Receive broadcast message
         * @param {Object} message Message received
         */
        manageBroadcast: function (message) {
          // Decode data
          let data = $utilities.decodeData(message.body, encodeTransmission);

          if (angular.isArray(data)) {
            // Launch broadcasted actions
            actionController.addActionList(data, true, {address: data.target, context: ""});
          }
        },
        /**
         * Close WebSockect Connection
         * @private
         */
        _close: function () {
          $comet._disconnect();
        }
      };
      return $comet;
    }
  ]);
