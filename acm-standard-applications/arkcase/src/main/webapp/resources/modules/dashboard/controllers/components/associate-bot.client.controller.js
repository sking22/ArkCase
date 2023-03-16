'use strict';

angular.module('dashboard.associate-bot').controller('Dashboard.AssociateBotBtn', [ '$scope', 'config', '$translate', 'Dashboard.DashboardService', 'ConfigService', 'params', 'UtilService', '$http',
          function($scope, config, $translate, DashboardService, ConfigService, params, Util, $http) {
                $scope.count = "click the button";
                //function for calling the associate bot
                $scope.runBotFunc = function() {
                  $http.get('https://18.253.42.219:8080/callBatch')
                      .then(function(response) {
                        console.log('Endpoint called successfully');
                      })
                      .catch(function(error) {
                        console.error('Error calling endpoint:', error);
                      });
                };

            }
]);