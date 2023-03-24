'use strict';

angular.module('dashboard.associate-bot').config(function($httpProvider) {
        $httpProvider.defaults.xsrfTrustedOrigins = ['https://acmsrpa.apvit.com'];
})
.controller('Dashboard.AssociateBotBtn', ['$scope', 'config', '$translate', 'Dashboard.DashboardService', 'ConfigService', 'params', 'UtilService', '$http',
    function($scope, config, $translate, DashboardService, ConfigService, params, Util, $http) {
        $scope.count = "click the button";

        //function for calling the associate bot
        $scope.runBotFunc = function() {
            $http.get('https://acmsrpa.apvit.com:8080/callBatch')
                .then(function(response) {
                    console.log('Endpoint called successfully');
                })
                .catch(function(error) {
                    console.error('Error calling endpoint:', error);
                });
        };
    }
]);
