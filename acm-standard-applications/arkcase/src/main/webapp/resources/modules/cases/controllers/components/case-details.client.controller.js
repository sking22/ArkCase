'use strict';

angular.module('cases').controller(
        'Cases.DetailsController',
        [
            '$scope',
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
                function($scope, $stateParams, $translate, Util, ConfigService, CaseInfoService, CaseLookupService, MessageService, HelperObjectBrowserService, MentionsService, ObjectService,  SuggestedObjectsService) {

                    new HelperObjectBrowserService.Component({
                        scope: $scope,
                        stateParams: $stateParams,
                        moduleId: "cases",
                        componentId: "details",
                        retrieveObjectInfo: CaseInfoService.getCaseInfo,
                        validateObjectInfo: CaseInfoService.validateCaseInfo,
                        onObjectInfoRetrieved: function (objectInfo) {
                            onObjectInfoRetrieved(objectInfo);
                        }
                    });

                    // ---------------------   mention   ---------------------------------
                    $scope.paramsSummernote = {
                        emailAddresses: [],
                        usersMentioned: []
                    };

                    $scope.saveDetailsSummary = function() {
                        var caseInfo = Util.omitNg($scope.objectInfo);
                        CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                            MessageService.info($translate.instant("cases.comp.details.caseSummary.informSaved"));
                            return caseInfo;
                        });
                    };

                    $scope.saveOptMedicaid = function() {
                        var caseInfo = Util.omitNg($scope.objectInfo);
                        CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                            MessageService.info($translate.instant("cases.comp.details.caseSummary.informSaved"));
                            return caseInfo;
                        });
                    };


                    $scope.saveDetails = function() {
                        var caseInfo = Util.omitNg($scope.objectInfo);
                        CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                            MentionsService.sendEmailToMentionedUsers($scope.paramsSummernote.emailAddresses, $scope.paramsSummernote.usersMentioned, ObjectService.ObjectTypes.CASE_FILE, "DETAILS", caseInfo.id, caseInfo.details);
                            MessageService.info($translate.instant("cases.comp.details.informSaved"));
                            return caseInfo;
                        });
                    };

                    var onObjectInfoRetrieved = function(data) {
                        /*console.log("data: " + data);
                        console.log("data js: " + JSON.stringify(data));*/
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
                            if (item.idDispositionDate !== null) {
                                $scope.dispositionDate = item.idDispositionDate;

                                var addDays = function(str, days) {
                                    var myDate = new Date(str);
                                    myDate.setDate(myDate.getDate() + parseInt(days));
                                    return myDate;
                                }

                                var tycd = addDays(item.idDispositionDate, 3650);
                                var m = tycd.getMonth();
                                var d = tycd.getDay();
                                var y = tycd.getFullYear();
                                $scope.tenYearsConvDate = m+"/"+ d+"/"+y;

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

                    };

                } ]);