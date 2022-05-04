'use strict';

angular.module('cases').controller(
        'Cases.DetailsController',
        [
            '$scope',
            '$modal',
            '$stateParams',
            '$translate',
            'UtilService',
            'ConfigService',
            'Case.InfoService',
            'Case.LookupService',
            'MessageService',
            'Helper.ObjectBrowserService',
            'Mentions.Service',
            'ObjectService',
            'SuggestedObjectsService',
            'Profile.UserInfoService',
            'Object.LookupService',
                function($scope, $modal, $stateParams, $translate, Util, ConfigService, CaseInfoService, CaseLookupService, MessageService, HelperObjectBrowserService, MentionsService, ObjectService, SuggestedObjectsService, UserInfoService, ObjectLookupService) {

                    new HelperObjectBrowserService.Component({
                        scope: $scope,
                        stateParams: $stateParams,
                        moduleId: "cases",
                        componentId: "details",
                        retrieveObjectInfo: CaseInfoService.getCaseInfo,
                        validateObjectInfo: CaseInfoService.validateCaseInfo,
                        onObjectInfoRetrieved: function (objectInfo) {
                            onObjectInfoRetrieved(objectInfo);
                            UserInfoService.getUserInfo().then(function(infoData) {
                                $scope.currentUserProfile = infoData;
                                $scope.disableField = $scope.currentUserProfile.groups[0] === "CMS@APVITACMS.COM" &&
                                                      ($scope.objectInfo.status === "CASE_CLOSED"
                                                    || $scope.objectInfo.status === "Audit Assigned"
                                                    || $scope.objectInfo.status === "Audit N/A"
                                                    || $scope.objectInfo.status === "Audit Completed");
                                $scope.isAnalyst = $scope.currentUserProfile.groups[0] === "ALA_ANALYST@APVITACMS.COM";
                                $scope.disableDexVer = $scope.disableField || $scope.isAnalyst;
                            });
                        }
                    });

                    $scope.maxLen300 = 300;
                    $scope.maxLen280 = 280;
                    $scope.maxLen150 = 150;
                    $scope.maxLen128 = 128;
                    $scope.maxLen64 = 64;
                    $scope.maxLen50 = 50;
                    $scope.maxLen40 = 40;
                    $scope.maxLen35 = 35;
                    $scope.maxLen32 = 32;
                    $scope.maxLen30 = 30;
                    $scope.maxLen25 = 25;
                    $scope.maxLen20 = 20;
                    $scope.maxLen16 = 16;
                    $scope.maxLen15 = 15;
                    $scope.maxLen11 = 11;
                    $scope.maxLen10 = 10;
                    $scope.maxLen5 = 5;
                    $scope.maxLen4 = 4;

                    // ---------------------   mention   ---------------------------------
                    $scope.paramsSummernote = {
                        emailAddresses: [],
                        usersMentioned: []
                    };

                    $scope.refresh = function() {
                        $scope.$emit('report-object-refreshed', $stateParams.id);
                    };

                     $scope.updateContractName = function() {
                        var idList = $scope.objectInfo.acmObjectOriginator.person.identifications;
                         idList.forEach(function (item) {
                             if (item.identificationType === "Contractor ID/Contractor Name") {
                                 $scope.ContractortID = item.identificationNumber;
                             }
                        });
                        if($scope.contractTypes) {
                            for(var i=0; i< $scope.contractTypes.length; i++) {
                                 for (var key in  $scope.contractTypes[i]) {
                                     if($scope.contractTypes[i][key]) {
                                          if($scope.ContractortID === $scope.contractTypes[i][key].toString().substring(0, $scope.contractTypes[i][key].toString().indexOf('-'))) {
                                              $scope.objectInfo.acmObjectOriginator.person.providerContractorName = $scope.contractTypes[i][key].toString();
                                          }
                                     }
                                 }
                            }
                        }
                     };


                    $scope.saveDetailsSummary = function() {
                        var caseInfo = Util.omitNg($scope.objectInfo);
                        CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                            MessageService.info($translate.instant("cases.comp.details.caseSummary.informSaved"));
                            return caseInfo;
                        });
                    };

                    $scope.saveAll = function() {

                        var params = {
                            "info": $scope.objectInfo,
                        };
                        var modalInstance = $modal.open({
                            animation: true,
                            templateUrl: 'modules/cases/views/components/case-save-modal.client.view.html',
                            controller: 'Cases.CaseSaveModalController',
                            size: 'sm',
                            backdrop: 'static',
                            resolve: {
                                modalParams: function() {
                                    return params;
                                }
                            }
                        });

                        modalInstance.result.then(function(data) {
                            $scope.refresh();
                        }, function() {
                            console.log("error");
                        });
                    };

                    ObjectLookupService.getLookupByLookupName('states').then(function (states) {
                        if(states){
                            var clear = { "readonly":null,"description":null,"value":"","key":"","primary":null,"order":0} ;
                            states.unshift(clear);
                            $scope.idStates = states;
                        }
                    });

                    ObjectLookupService.getLookupByLookupName('notActionReasons').then(function (notActionReasons) {
                    if(notActionReasons){
                        var clear = { "readonly":null,"description":null,"value":"","key":"","primary":null,"order":0} ;
                        notActionReasons.unshift(clear);
                        $scope.caseNAR = notActionReasons;
                        }
                    });

                    ObjectLookupService.getLookupByLookupName('outcomeRevRei').then(function (outcomeRevRei) {
                      if(outcomeRevRei){
                        var clear = { "readonly":null,"description":null,"value":"","key":"","primary":null,"order":0} ;
                        outcomeRevRei.unshift(clear);
                        $scope.caseORR = outcomeRevRei;
                        }
                    });

                    ObjectLookupService.getLookupByLookupName('caseAdminActionsOutcomes').then(function (caseAdminActionsOutcomes) {
                       if(caseAdminActionsOutcomes){
                        var clear = { "readonly":null,"description":null,"value":"","key":"","primary":null,"order":0} ;
                        caseAdminActionsOutcomes.unshift(clear);
                        $scope.caseAAO = caseAdminActionsOutcomes;
                        }
                    });

                    ObjectLookupService.getLookupByLookupName('caseTerminationTypes').then(function (caseTerminationTypes) {
                        if(caseTerminationTypes){
                        var clear = { "readonly":null,"description":null,"value":"","key": "","primary":null,"order":0} ;
                        caseTerminationTypes.unshift(clear);
                        $scope.caseTerminationTypes = caseTerminationTypes;
                        }
                    });

                    ObjectLookupService.getLookupByLookupName('caseOptCmsDecisionTypes').then(function (caseOptCmsDecisionTypes) {
                       if(caseOptCmsDecisionTypes){
                        var clear = { "readonly":null,"description":null,"value":"","key":"","primary":null,"order":0} ;
                        caseOptCmsDecisionTypes.unshift(clear);
                        $scope.caseOptCmsDecisionTypes = caseOptCmsDecisionTypes;
                        }
                    });

                    ObjectLookupService.getLookupByLookupName('caseMultipleAlertTypes').then(function (caseMultipleAlertTypes) {
                       if(caseMultipleAlertTypes){
                        var clear = { "readonly":null,"description":null,"value":"","key":"","primary":null,"order":0} ;
                        caseMultipleAlertTypes.unshift(clear);
                        $scope.caseMultipleAlertTypes = caseMultipleAlertTypes;
                        }
                    });

                    ObjectLookupService.getLookupByLookupName('contractTypes').then(function (contractTypes) {
                      if(contractTypes){
                        var clear = { "readonly":null,"description":null,"value":"","key":"","primary":null,"order":0} ;
                        contractTypes.unshift(clear);
                        $scope.contractTypes = contractTypes;
                      }
                    });

                    $scope.saveDetails = function() {
                        var caseInfo = Util.omitNg($scope.objectInfo);
                        CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                            MentionsService.sendEmailToMentionedUsers($scope.paramsSummernote.emailAddresses, $scope.paramsSummernote.usersMentioned, ObjectService.ObjectTypes.CASE_FILE, "DETAILS", caseInfo.id, caseInfo.details);
                            MessageService.info($translate.instant("cases.comp.details.informSaved"));
                            return caseInfo;
                        });
                    };

                    UserInfoService.getUserInfo().then(function(infoData) {
                        $scope.currentUserProfile = infoData;
                    });

                    var onObjectInfoRetrieved = function(data) {
                        $scope.providerFullName = data.acmObjectOriginator.person.givenName + " " + data.acmObjectOriginator.person.familyName;
                        $scope.caseFileType = data.caseType;
                        $scope.providerSpecialty = data.acmObjectOriginator.person.providerSpecialty;
                        $scope.associateLastName = data.acmObjectOriginator.person.associateLastName;
                        $scope.associateFirstName = data.acmObjectOriginator.person.associateFirstName;
                        $scope.associateLegalBusinessName = data.acmObjectOriginator.person.associateLegalBusinessName;
                        $scope.associateEnrollmentId = data.acmObjectOriginator.person.associateEnrollmentId;
                        $scope.associateNPI = data.acmObjectOriginator.person.associateNPI;
                        $scope.associateTIN =  data.acmObjectOriginator.person.associateTIN;
                        $scope.associateTinType =  data.acmObjectOriginator.person.associateTinType;
                        $scope.associateRole = data.acmObjectOriginator.person.associateRole;
                        $scope.associateSanctionCode = data.acmObjectOriginator.person.associateSanctionCode;
                        $scope.associateSanctionDate = data.acmObjectOriginator.person.associateSanctionDate;
                        var idList = data.acmObjectOriginator.person.identifications;

                        idList.forEach(function (item) {
                            if (item.identificationType === "PECOS Enrollment ID") {
                                $scope.PECOSEnrollmentID = item.identificationNumber;
                                $scope.PECOSEnrollmenState = item.idState;
                                $scope.PECOSEnrollmenStatus = item.idStatus;
                            }
                            if (item.identificationType === "Contractor ID/Contractor Name") {
                                $scope.ContractortID = item.identificationNumber;
                            }
                            if (item.identificationType === "NPI") {
                                $scope.NPINumber = item.identificationNumber;
                            }
                            if (item.identificationType === "SSN/EIN") {
                                $scope.SSN_EIN = item.identificationNumber;
                            }
                            if (item.identificationType === "PTAN") {
                                $scope.ptanId = item.identificationNumber;
                            }
                            if (item.identificationType === "TIN") {
                                $scope.tinId = item.identificationNumber;
                            }
                            if (item.identificationType === "Enrollment ID") {
                                $scope.enrollmentID = item.identificationNumber;
                            }
                            if (item.identificationType === "License Number") {
                                $scope.licenseNumber = item.identificationNumber;
                                $scope.licenseStatus = item.idStatus;
                                $scope.licenseExp = item.idExpirationDate;
                                $scope.licenseQualifierSanction = item.idQualifierSanction;
                                $scope.licenseAlertDate  = item.idAlertDate;
                            }
                            if (item.idConvictionDate !== null) {
                                $scope.dispositionDate = item.idConvictionDate;

                               var tycd = moment.utc(new Date(item.idConvictionDate));
                               var m = moment(tycd).get('month') + 1;
                               var d = moment(tycd).get('date');
                               var y = moment(tycd).get('year') + 10;
                               $scope.objectInfo.caseTenYearsConvDate = m+"/"+ d+"/"+y;

                            }
                            if (item.idDocketRequestDate !== null) {
                                $scope.docketRequestDate  = item.idDocketRequestDate;
                            }
                            if (item.idDocketResponseDate !== null) {
                                $scope.docketResponseDate  = item.idDocketResponseDate;
                            }
                            if (item.idDocketStatus !== null) {
                                $scope.docketStatus = item.idDocketStatus;
                            }

                            if (item.idOffenseType !== null) {
                                $scope.offenseType = item.idOffenseType;
                            }
                            if (item.idCaseNumber !== null) {
                                $scope.caseFileNumber = item.idCaseNumber;
                            }

                            if (item.idCourtName !== null) {
                                $scope.courtName = item.idCourtName;
                            }
                            if (item.idExclusionType !== null) {
                                $scope.idExclusionType = item.idExclusionType;
                            }

                        });

                        if(data.acmObjectOriginator.person.providerContractorName == null || data.acmObjectOriginator.person.providerContractorName == '') {
                           $scope.updateContractName();
                        }

                    };

                } ]);