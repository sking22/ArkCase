'use strict';

angular.module('dashboard.associate-bot').controller('Dashboard.AssociateBotBtn', ['$scope', 'config', '$translate', 'Dashboard.DashboardService', 'ConfigService', 'params', 'UtilService', '$http',
    function($scope, config, $translate, DashboardService, ConfigService, params, Util, $http) {
        $scope.associateBotInfoText = "Click the button below to run the associate bot";
        $scope.btnDisabled = false;

        //function for calling the associate bot
        $scope.runBotFunc = function() {
            $scope.btnDisabled = true;
            $http.get('https://acmsrpa.apvit.com:8080/callBatch',
             { headers: {
              'Access-Control-Allow-Origin': 'https://acmsrpa.apvit.com/',
               'Access-Control-Allow-Headers': 'Content-Type, Authorization',
                'Access-Control-Allow-Credentials': true, }
              }).then(function(response) {
                    console.log('Endpoint called successfully');
                    $scope.associateBotInfoText = "Associate bot run successfully";
                 })
                .catch(function(error) {
                    console.error('Error calling endpoint:', error);
                    $scope.associateBotInfoText = "Error..... \n Associate bot did not run. Contact Support";
                });
        };
    }
]);
