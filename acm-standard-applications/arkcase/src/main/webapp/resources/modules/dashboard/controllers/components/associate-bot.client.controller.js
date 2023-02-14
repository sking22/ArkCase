'use strict';

angular.module('dashboard.associate-bot').controller('Dashboard.AssociateBotBtn', [ '$scope', 'config', '$translate', 'Dashboard.DashboardService', 'ConfigService', 'params', 'UtilService', '$http',
          function($scope, config, $translate, DashboardService, ConfigService, params, Util, $http) {
                $scope.count = "click the button";
                //function for calling the associate bot
                $scope.runBotFunc = function() {
                  $http.get('http://18.253.42.219:8080/callBatch')
                      .then(function(response) {
                        // Success callback
                        console.log('Endpoint called successfully');
                      })
                      .catch(function(error) {
                        // Error callback
                        console.error('Error calling endpoint:', error);
                      });
                };

            }
]);