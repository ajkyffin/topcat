(function() {
    'use strict';

    /**
     * deferred bootstrap to load main configuration to APP_CONFIG
     */
    window.deferredBootstrapper.bootstrap({
        element : document.body,
        module : 'angularApp',
        resolve : {
            APP_CONFIG : ['$http', function($http) {
                return $http.get('config/topcat_dls.json');
            } ],
            LANG : ['$http', function($http) {
                return $http.get('languages/en.json');
            } ],
            SMARTCLIENTPING : ['$http', '$q', function($http, $q) {
                var def = $q.defer();

                $http.get('https://localhost:8888/ping').then(function() {
                    def.resolve({ping: 'online'});
                }, function(error) { //jshint ignore: line
                    def.resolve({ping: 'offline'});
                });

                return def.promise;
            }]
        }
    });

    /**
     * @ngdoc overview
     * @name angularApp
     * @description
     * # angularApp
     *
     * Main module of the application.
     */
    angular
        .module('angularApp', [
            'ngResource',
            'ngSanitize',
            'ui.router',
            'ct.ui.router.extras.sticky',
            'ui.bootstrap',
            'truncate',
            'inform',
            'inform-exception',
            'prettyBytes',
            'ngStorage',
            'pascalprecht.translate',
            'ui.grid',
            'ui.grid.pagination',
            'ui.grid.infiniteScroll',
            'ui.grid.selection',
            'bytes',
            'angularSpinner',
            'ng.deviceDetector',
            'angularMoment',
            'emguo.poller',
            'angular-bind-html-compile'

        ])
        .constant('_', window._)
        .constant('APP_CONSTANT', {
            storageName: 'TopCAT_0.1',
            smartClientUrl: 'https://localhost:8888'
        })
        .config(['$translateProvider', 'LANG', function($translateProvider, LANG) {
            $translateProvider.translations('en', LANG);

            $translateProvider.useStaticFilesLoader({
                prefix: '/languages/',
                suffix: '.json'
            });
            $translateProvider.preferredLanguage('en');
         }])
        .config(['$httpProvider', function($httpProvider) {
            $httpProvider.interceptors.push('HttpErrorInterceptor');
            $httpProvider.interceptors.push('ICATRequestInterceptor');
        }])
        .config(function($stateProvider, $urlRouterProvider) {
            //workaround https://github.com/angular-ui/ui-router/issues/1022
            $urlRouterProvider.otherwise(function($injector) {
                var $state = $injector.get('$state');
                var RouteUtils = $injector.get('RouteUtils');
                var routeName = RouteUtils.getHomeRouteName();
                $state.go(routeName);
            });

            $stateProvider
                .state('home', {
                    abstract: true,
                    resolve: {
                        cartInit : ['Cart', function(Cart) {
                            return Cart.restore();
                        }],
                    },
                    //url: '',
                    templateUrl: 'views/abstract-home.html',
                    controller: 'HomeController'
                })
                .state('home.browse', {
                    abstract: true,
                    url: '/browse',
                    views: {
                      '': {
                        templateUrl: 'views/main-browse.html'
                      }
                    }/*,
                    sticky: true,
                    deepStateRedirect: true*/
                })
                .state('home.browse.facility', {
                    url: '/facilities',
                    resolve: {
                        authenticate : ['Authenticate', function(Authenticate) {
                            return Authenticate.authenticate();
                        }]
                    },
                    views: {
                        '' : {
                            templateUrl: 'views/partial-browse-panel.html',
                            controller: 'BrowseFacilitiesController'
                        },
                        'search-form-view': {
                            templateUrl: 'views/partial-search-form.html',
                            controller: 'SearchFormController as searchForm'
                        },
                        'meta-view@home.browse' : {
                            templateUrl: 'views/partial-meta-panel.html',
                            controller: 'MetaPanelController as meta'
                        }
                    }
                })

                .state('home.browse.facility.search', {
                    url: '^/search?data&meta&pagingType&query&type&facility&startDate&endDate',
                    views: {
                        '@home.browse' : {
                            templateUrl: 'views/search-result.html'
                        },
                        'meta-view@home.browse' : {
                            templateUrl: 'views/partial-meta-panel.html',
                            controller: 'MetaPanelController as meta'
                        }
                    }
                })
                .state('home.my-data', {
                    url: '/my-data', //?data&meta&pagingType&query&type&facility&startDate&endDate',
                    resolve: {
                        authenticate : ['Authenticate', function(Authenticate) {
                            return Authenticate.authenticate();
                        }]
                    },
                    views: {
                      'my-data': {
                        templateUrl: 'views/main-my-data.html',
                        controller: 'MyDataController as md'
                      }
                    },
                    /*sticky: true,
                    deepStateRedirect: true*/
                })
                .state('home.cart', {
                    url: '/cart', //?data&meta&pagingType&query&type&facility&startDate&endDate',
                    resolve: {
                        authenticate : ['Authenticate', function(Authenticate) {
                            return Authenticate.authenticate();
                        }]
                    },
                    views: {
                      'cart': {
                        templateUrl: 'views/main-cart.html',
                        controller: 'CartController'
                      }
                    },
                    /*sticky: true,
                    deepStateRedirect: true*/
                })
                .state('home.download', {
                    url: '/download',
                    resolve: {
                        authenticate : ['Authenticate', function(Authenticate) {
                            return Authenticate.authenticate();
                        }]
                    },
                    views: {
                      'download': {
                        templateUrl: 'views/main-download.html',
                        controller: 'DownloadController'
                      }
                    },
                    /*sticky: true,
                    deepStateRedirect: true*/
                })
                // ABOUT PAGE AND MULTIPLE NAMED VIEWS =================================
                .state('about', {
                    /*resolve : {
                        authenticate : ['Authenticate', function(Authenticate) {
                            return Authenticate.authenticate();
                        }]
                    },*/
                    url: '/about',
                    templateUrl: 'views/main-about.html'
                })
                .state('contact', {

                    url: '/contact',
                    templateUrl: 'views/main-contact.html'
                })
                .state('globus-faq', {
                    url: '/globus-faq',
                    templateUrl: 'views/main-globus-faq.html'
                })
                .state('test', {
                    url: '/test',
                    templateUrl: 'views/test.html',
                    controller: 'TestController as test'
                })
                .state('login', {
                    url: '/login',
                    templateUrl: 'views/login.html',
                    controller: 'LoginController as vm'
                })
                .state('logout', {
                    url: '/logout',
                    controller: 'LogoutController'
                })
                .state('logout.facility', {
                    url: '/:facilityName',
                    controller: 'LogoutController'
                })
                .state('homeRoute', {
                    url: '/',
                    controller: 'HomeRouteController'
                })
                ;

        })
        .config(['$logProvider', function($logProvider) {
            $logProvider.debugEnabled(true);
        }])
        .config(function (pollerConfig) {
            pollerConfig.neverOverwrite = true;
        })
    /*.config(function($stickyStateProvider) {
      $stickyStateProvider.enableDebug(true);
    })*/
        .run(['SquelCustomQuery', function(SquelCustomQuery){
            SquelCustomQuery.init();
        }])
        .run(['$rootScope', '$state', '$stateParams', function ($rootScope, $state, $stateParams) {
            //make $state and $stateParams available at rootscope.
            $rootScope.$state = $state;
            $rootScope.$stateParams = $stateParams;
        }])
        .run(['$rootScope', '$state', function ($rootScope, $state) {
            //watch for state change resolve authentication errors
            $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
                if (error && error.isAuthenticated === false) {
                    $state.go('login');
                }
            });

            //save last page to rootscope
            /*$rootScope.$on('$stateChangeStart', function() {
                $log.debug.log('previous state', $previousState.get() !== null ? $previousState.get().state.name : $previousState.get());
                $rootScope.previousState = $previousState.get();
            });*/

        }])
        .run(['SmartClientPollManager', function(SmartClientPollManager) {
            //run checking of smartclient
            SmartClientPollManager.runOnStartUp();
        }])
        .run(['RouteCreatorService', function(RouteCreatorService) {
            RouteCreatorService.createStates();
        }])
        //TODO controller is run before restore completes as it is an ajax call
        .run(['$rootScope', 'Cart', function($rootScope, Cart) {
            //init and restore cart when user refresh page
            Cart.init();
        }]);
})();
