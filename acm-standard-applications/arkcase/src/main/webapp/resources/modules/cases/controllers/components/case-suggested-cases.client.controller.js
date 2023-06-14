'use strict';

angular.module('cases').controller('Cases.SuggestedCasesController', ['$scope', '$translate', '$stateParams', 'Helper.UiGridService', 'UtilService', 'Helper.ObjectBrowserService', 'Case.InfoService', 'SuggestedObjectsService',
    function ($scope, $translate, $stateParams, HelperUiGridService, Util, HelperObjectBrowserService, CaseInfoService, SuggestedObjectsService) {


        new HelperObjectBrowserService.Component({
            scope: $scope,
            stateParams: $stateParams,
            moduleId: "cases",
            componentId: "suggestedCases",
            retrieveObjectInfo: CaseInfoService.getCaseInfo,
            validateObjectInfo: CaseInfoService.validateCaseInfo,
            onConfigRetrieved: function (componentConfig) {
                return onConfigRetrieved(componentConfig);
            },
            onObjectInfoRetrieved: function(objectInfo) {
                onObjectInfoRetrieved(objectInfo);
            }
        });

        var gridHelper = new HelperUiGridService.Grid({
            scope: $scope
        });
        var onConfigRetrieved = function(config) {

            $scope.config = config;
            $scope.objectTypeValue = config.objectTypeValue;

            gridHelper.setColumnDefs(config);
            gridHelper.setBasicOptions(config);
            gridHelper.disableGridScrolling(config);

            $scope.gridOptions = {
                enableColumnResizing: true,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                noUnselect: false,
                columnDefs: $scope.config.columnDefs,
                paginationPageSizes: $scope.config.paginationPageSizes,
                paginationPageSize: $scope.config.paginationPageSize,
                totalItems: 0,
                data: []
            };
        };

        var isNullOrEmpty = function(s) {
            if((s != undefined) && (s != null) && (s != '')) {
                return false;
            }
            return true;
        }

        var getSanctionAssociateFullName = function() {
             var firstName = $scope.objectInfo.acmObjectOriginator.person.associateLastName;
             var lastName = $scope.objectInfo.acmObjectOriginator.person.associateFirstName;
            var fullName = '', comma = '';
            if(!isNullOrEmpty(firstName)) {
                fullName += firstName;
                comma = ' ';
            }
            if(!isNullOrEmpty(lastName)) {
                fullName += (comma + lastName);
            }
            return fullName.trim();
        }

        function retrieveGridData(){
            console.log($scope.objectInfo);

            var sanctionAssociatedTin = $scope.objectInfo.acmObjectOriginator.person.associateTIN;
            var sanctionAssociatedNpi = $scope.objectInfo.acmObjectOriginator.person.associateNPI;
            var sanctionAssociateLegalBusiness = $scope.objectInfo.acmObjectOriginator.person.associateLegalBusinessName;
            var sanctionAssociateFullName = getSanctionAssociateFullName();

            if(isNullOrEmpty($scope.objectInfo.caseConvictedIndividual) || isNullOrEmpty($scope.objectInfo.caseConvictedIndividualLastName)){
                var convictName = null;
            } else {
                var convictName = $scope.objectInfo.caseConvictedIndividual + " " + $scope.objectInfo.caseConvictedIndividualLastName;
            }

            var convictTin = $scope.objectInfo.caseConvictedIndividualTin; //
            var legalBusinessName = $scope.objectInfo.acmObjectOriginator.person.legalBusinessName;

            SuggestedObjectsService.getSimilarCases(
                $scope.objectInfo.acmObjectOriginator.person.ssn,
                $scope.objectInfo.acmObjectOriginator.person.npi,
                $scope.objectInfo.id, sanctionAssociatedTin, sanctionAssociatedNpi, sanctionAssociateLegalBusiness,
                sanctionAssociateFullName, convictName, convictTin, legalBusinessName).then(function (data) {

                    $scope.suggestedCases = data.data;
                    $scope.gridOptions = $scope.gridOptions || {};
                    $scope.gridOptions.data = $scope.suggestedCases;
                    $scope.gridOptions.totalItems = $scope.suggestedCases.length;
            }, function(error) {
                console.log(error);
            });
        }

        $scope.getObjectTypeValue = function (key) {
            return $scope.objectTypeValue[key];
        };

        var onObjectInfoRetrieved = function(objectInfo) {
            $scope.objectInfo = objectInfo;
            retrieveGridData();
        };

        $scope.onClickObjLink = function(event, rowEntity) {
            event.preventDefault();

            var targetType = Util.goodMapValue(rowEntity, "type");
            var targetId = Util.goodMapValue(rowEntity, "id");
            gridHelper.showObject(targetType, targetId);
        };
       
    }]);