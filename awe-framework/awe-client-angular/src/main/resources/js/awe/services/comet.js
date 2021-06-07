import {aweApplication} from "../awe";
import {Stomp} from "@stomp/stompjs";
import SockJS from "sockjs-client";

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
          let client = Stomp.over(new SockJS($utilities.getContextPath() + '/websocket', undefined, {timeout: 10 * 1000}));
          client.connectHeaders = {'Authorization': connectionToken};
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

          // Set connection
          $comet.setConnection($comet.getWebsocketClient());

          // On connection
          $comet.getConnection().onConnect = function (frame) {
            // Subscribe to all broadcasted messages
            $comet.subscribe("broadcast");

            // Subscribe to own connection
            $comet.subscribe(connectionToken);

            // Resolve initialization
            events["connect"].resolve();
          };

          // On connection error
          $comet.getConnection().onStompError = function (frame) {
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
          $comet.getConnection().activate();

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
