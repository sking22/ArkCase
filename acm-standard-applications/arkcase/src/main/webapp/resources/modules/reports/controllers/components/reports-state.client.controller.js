'use strict';

angular.module('reports').controller('Reports.StateController', [ '$scope', 'ConfigService', 'Object.LookupService', function($scope, ConfigService, ObjectLookupService) {
    //the case type for inline report
         $scope.caseTypeInlineReport = false;
         $scope.caseTypeLabel= "Case Type";
    // This configuration can be moved to ~/.arkcase/acm/acm-reports-parameters.json
        ConfigService.getComponentConfig("reports", "caseStates").then(function(config) {


        $scope.config = config;
        $scope.$watchCollection('data.reportSelected', function(newValue, oldValue) {
             if($scope.data.reportSelected === "INLINE_REVIEW_REPORT"){
                     $scope.caseTypeInlineReport = true;
             }
             else{
                     $scope.caseTypeInlineReport = false;
             }
            if (newValue) {
                 console.log("this should be the report selected" + newValue)
                $scope.data.reportSelected = newValue;
                if ($scope.config.resetCaseStateValues.indexOf($scope.data.reportSelected) > -1) {
                    $scope.data.caseStateSelected = '';
                }

            }
        });
        return config;
    });

    //gets the case type
    ObjectLookupService.getCaseFileTypes().then(function(caseTypes) {
                        $scope.caseCategory = caseTypes;
                        $scope.data.caseType = ObjectLookupService.getPrimaryLookup($scope.caseCategory);
     });

    ObjectLookupService.getLookupByLookupName("reportStates").then(function(reportStates) {
        $scope.reportStates = reportStates;
        $scope.data.stateSelected = ObjectLookupService.getPrimaryLookup($scope.reportStates);
        return reportStates;
    });

} ]);