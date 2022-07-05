import {aweApplication} from "./../awe";

// Loading service
aweApplication.factory('Load',
  ['AweUtilities', '$log', 'LoadingBar', 'AweSettings', 'Storage',
    function (Utilities, $log, loadingBar, $settings, Storage) {
      let  Phases = {COMPILE: "compile", INITIALIZE: "initialize"};
      let  Status = {START: "start", COMPILED: "compiled", LOADED: "loaded"};
      function Load(scope, view, components) {
        // Service variables;
        this.scope = scope;
        this.screen = scope.$root && scope.$root.screen ? scope.$root.screen : "";
        this.view = view;
        let  componentControllers = {};
        _.each(components, function(component) {
          componentControllers[component.id] = component.controller;
        });
        this.pending = {
          compile: componentControllers,
          initialize: {}
        };
        this.deferred = {
          compile: null,
          initialize: null
        };
        this.listeners = {
          compile: null,
          initialize: null,
          loading: null
        };
        this.times = {};
        this.loadTimer = null;
      }
      Load.prototype = {
        /**
         * Initialize loading
         */
        start: function () {
          this.times.start = new Date();
          let  pending = this.pending;
          let  listeners = this.listeners;
          let  scope = this.scope;
          let  view = this.view;
          let  _self = this;
          delete pending.compile["screen"];

          // Prepare load screen (disable animations and hide screen)
          Storage.get("status")[view] = Status.START;
          scope.visible = false;

          // Set compilation check
          listeners.compile = scope.$root.$on('component-initialized-' + view, function (event, component) {
            if (Storage.get("status")[view] === Status.START) {
              if (component && component.address && component.address.view === view && component.address.component in pending.compile) {
                delete pending.compile[component.address.component];
                loadingBar.endTask();
                // Store the component who has dependencies
                if (component.hasDependencies) {
                  pending.initialize[component.address.component] = true;
                  loadingBar.startTask();
                }
              }
              // Check if compilation phase is finished
              _self.checkPhase(Phases.COMPILE);
            }
          });

          // Set dependency wait
          listeners.initialize = scope.$root.$on('dependencies-initialized-' + view, function (event, address) {
            if (Storage.get("status")[view] === Status.COMPILED) {
              if (address && address.view === view && address.component in pending.initialize) {
                delete pending.initialize[address.component];
                loadingBar.endTask();
              }
              // Check if initialization phase is finished
              _self.checkPhase(Phases.INITIALIZE, scope, view);
            }
          });

          /**
           * Finish loading screen (enable animations and show screen)
           */
          listeners.loading = this.scope.$on('initialised', function () {
            scope.visible = true;
            listeners.loading();
          });

          // Start compilation
          this.compile()
            .then(function () {
              listeners.compile();
              return _self.initialize();
            }, function (data) {
              listeners.compile();
              _self.timeout(data);
            })
            .then(function () {
              listeners.initialize();
              _self.end();
            }, function (data) {
              listeners.initialize();
              _self.timeout(data);
            })
            .finally(function () {
              _self.destroy();
            });
        },
        /**
         * Initialize compiling
         */
        compile: function () {
          let  deferred = Utilities.q.defer();
          this.deferred[Phases.COMPILE] = deferred;
          let  pending = this.pending;
          loadingBar.startTasks(Utilities.objectLength(pending.compile));

          // Start checking compilation
          this.checkPhase(Phases.COMPILE);
          return deferred.promise;
        },
        /**
         * Initialize dependencies
         */
        initialize: function () {
          let  deferred = Utilities.q.defer();
          this.deferred[Phases.INITIALIZE] = deferred;
          Storage.get("status")[this.view] = Status.COMPILED;
          this.times.compile = new Date();
          Utilities.timeout.cancel(this.loadTimer);

          // Screen has been compiled
          let  dateDiff = (this.times.compile - this.times.start) / 1000;
          $log.debug("[SCREEN LOAD PHASE] Screen has been COMPILED", {screen: this.screen, view: this.view, compilationTime: dateDiff + "s"});
          Utilities.publishDelayed('compiled', this.view);

          // Start checking initialization
          this.checkPhase(Phases.INITIALIZE, this.scope, this.view);
          return deferred.promise;
        },
        /**
         * Finish screen load
         */
        end: function () {
          Storage.get("status")[this.view] = Status.LOADED;
          this.times.end = new Date();
          Utilities.timeout.cancel(this.loadTimer);
          let  initTime = (this.times.end - this.times.compile) / 1000;
          let  fullTime = (this.times.end - this.times.start) / 1000;
          $log.debug("[SCREEN LOAD PHASE] Screen has been INITIALIZED", {screen: this.screen, view: this.view, initializationTime: initTime + "s", pageLoadTime: fullTime + "s"});
          Utilities.publishDelayed('initialised', this.view);
        },
        /**
         *  Timeout loading a phase
         * @param {string} data timeout data
         */
        timeout: function (data) {
          $log.warn("[SCREEN LOAD PHASE] Screen load TIMEOUT", data);
          loadingBar.end();
          this.end();
        },
        /**
         * Check the status of the phase
         * @param {type} phase
         */
        checkPhase: function (phase) {
          // Set cancel timeout
          let  pending = this.pending[phase];
          let  promise = this.deferred[phase];
          let  screen = this.screen;
          let  view = this.view;
          Utilities.timeout.cancel(this.loadTimer);
          this.loadTimer = Utilities.timeout(function () {
            let  timeoutData = {phase: phase, pending: _.cloneDeep(pending), screen: screen, view: view};
            promise.reject(timeoutData);
          }, $settings.get("loadingTimeout"));

          // Check phase
          if (Utilities.objectLength(pending) === 0) {
            promise.resolve();
          }
        },
        /**
         * Destroy the initialization object
         */
        destroy: function () {
          Utilities.timeout.cancel(this.loadTimer);
        }
      };
      return Load;
    }
  ]);