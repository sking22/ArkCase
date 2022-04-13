'use strict';

angular.module('cases').controller(
        'Cases.ChangeStatusController',
        [ '$scope', '$http', '$stateParams', '$translate', '$modalInstance', 'Complaint.InfoService', '$state', 'Object.LookupService', 'MessageService', 'UtilService', '$modal', 'ConfigService', 'ObjectService', 'modalParams', 'Case.InfoService', 'Object.ParticipantService','Admin.FormWorkflowsLinkService', 'Object.ModelService',
                function($scope, $http, $stateParams, $translate, $modalInstance, ComplaintInfoService, $state, ObjectLookupService, MessageService, Util, $modal, ConfigService, ObjectService, modalParams, CaseInfoService, ObjectParticipantService, AdminFormWorkflowsLinkService, ObjectModelService) {
                    console.log('modalParams: ' + JSON.stringify(modalParams));
                    $scope.modalParams = modalParams;
                    $scope.currentStatus = modalParams.info.status;
                    $scope.oInfo = modalParams.objectInfo;
                    $scope.approverName = "";
                    $scope.groupName = "";
                    //Functions
                    $scope.statusChanged = statusChanged;
                    $scope.save = save;
                    $scope.cancelModal = cancelModal;
                    //Objects
                    $scope.showCaseCloseStatus = false;
                    $scope.showApprover= modalParams.showApprover;
                    $scope.changeCaseStatus = {
                        caseId: modalParams.info.caseId,
                        status: "",
                        caseResolution: "",
                        objectType: "CHANGE_CASE_STATUS",
                        changeDate: new Date(),
                        participants: [],
                        created: null,
                        creator: null,
                        modified: null,
                        modifier: null,
                        description: "",
                        changeCaseStatusFlow: $scope.showApprover == 'true'
                    };

                    var participantTypeApprover = 'approver';
                    var participantTypeOwningGroup = "owning group";

                    ConfigService.getModuleConfig("cases").then(function(moduleConfig) {
                        $scope.futureTaskConfig = _.find(moduleConfig.components, {
                            id: "newFutureTask"
                        });
                    });

                    ConfigService.getComponentConfig("cases", "participants").then(function(componentConfig) {
                        $scope.participantsConfig = componentConfig;
                    });

                    ObjectLookupService.getLookupByLookupName("changeCaseStatuses").then(function(caseStatuses) {
                        $scope.statuses = caseStatuses;
                        var defaultChangeCaseStatus = ObjectLookupService.getPrimaryLookup($scope.statuses);
                        if (defaultChangeCaseStatus && !$scope.changeCaseStatus.status) {
                            $scope.changeCaseStatus.status = defaultChangeCaseStatus.key;
                        }
                    });

                    function statusChanged() {
                        $scope.showCaseCloseStatus = $scope.changeCaseStatus.status === "CLOSED";
                    }

                    // ---------------------------            approver         --------------------------------------
                    $scope.userOrGroupSearch = function() {
                        var params = {};
                        params.header = $translate.instant("cases.comp.change.status.approver.pickerModal.header");
                        params.filter = "fq=\"object_type_s\":(GROUP OR USER)&fq=\"status_lcs\":(ACTIVE OR VALID)";
                        params.extraFilter = "&fq=\"name\": ";
                        params.config = Util.goodMapValue($scope.participantsConfig, "dialogUserPicker");
                        params.secondGrid = 'true';

                        var modalInstance = $modal.open({
                            templateUrl: "directives/core-participants/participants-user-group-search.client.view.html",
                            controller: [ '$scope', '$modalInstance', 'params', function($scope, $modalInstance, params) {
                                $scope.modalInstance = $modalInstance;
                                $scope.header = params.header;
                                $scope.filter = params.filter;
                                $scope.config = params.config;
                                $scope.secondGrid = params.secondGrid;
                                $scope.extraFilter = params.extraFilter;
                            } ],
                            animation: true,
                            size: 'lg',
                            backdrop: 'static',
                            resolve: {
                                params: function() {
                                    return params;
                                }
                            }
                        });

                        modalInstance.result.then(function(selection) {
                            if (selection) {
                                var selectedObjectType = selection.masterSelectedItem.object_type_s;
                                if (selectedObjectType === 'USER') { // Selected user
                                    var selectedUser = selection.masterSelectedItem;
                                    var selectedGroup = selection.detailSelectedItems;

                                    $scope.approverName = selectedUser.name;
                                    addParticipantInChangeCase(participantTypeApprover, selectedUser.object_id_s);

                                    if (selectedGroup) {
                                        $scope.groupName = selectedGroup.name;
                                        addParticipantInChangeCase(participantTypeOwningGroup, selectedGroup.object_id_s);
                                    }

                                    return;
                                } else if (selectedObjectType === 'GROUP') { // Selected group
                                    var selectedUser = selection.detailSelectedItems;
                                    var selectedGroup = selection.masterSelectedItem;

                                    $scope.groupName = selectedGroup.name;
                                    addParticipantInChangeCase(participantTypeOwningGroup, selectedGroup.object_id_s);

                                    if (selectedUser) {
                                        $scope.approverName = selectedUser.name;
                                        addParticipantInChangeCase(participantTypeApprover, selectedUser.object_id_s);
                                    }

                                    return;
                                }
                            }

                        }, function() {
                            // Cancel button was clicked.
                            return [];
                        });

                    };

                    $scope.updateParticipants = function() {
                        var len = $scope.oInfo.participants.length;
                        for (var i = 0; i < len; i++) {
                            if($scope.oInfo.participants[i].participantType =='assignee' || $scope.oInfo.participants[i].participantType =='owning group'){
                                $scope.oInfo.participants[i].replaceChildrenParticipant = true;
                            }
                        }
                    }

                    function addParticipantInChangeCase(participantType, participantLdapId){
                        var newParticipant = {};
                        newParticipant.className = $scope.participantsConfig.className;
                        newParticipant.participantType = participantType;
                        newParticipant.participantLdapId = participantLdapId;

                        if (ObjectParticipantService.validateParticipants([newParticipant], true)) {
                            var participantExists = false;
                            _.forEach($scope.changeCaseStatus.participants, function (participant) {
                                if(participant.participantType == participantType){
                                    participantExists = true;
                                    participant.participantLdapId = newParticipant.participantLdapId;
                                    participant.replaceChildrenParticipant = true;
                                    return false;
                                }
                            });
                            if(!participantExists){
                                $scope.changeCaseStatus.participants.push(newParticipant);
                            }
                        }
                    }

                    function save() {
                        $scope.loading = true;
                        $scope.loadingIcon = "fa fa-circle-o-notch fa-spin";
                       // $modalInstance.close($scope.changeCaseStatus);
                        
                        if ($scope.changeCaseStatus.status === "Assigned" || $scope.changeCaseStatus.status === "In Process" || $scope.changeCaseStatus.status === "Documentation Requested" ) {
                              $scope.oInfo.casePrevAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                              ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                              ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                              $scope.updateParticipants();
                        } else if ($scope.changeCaseStatus.status === "Ready For Review" || $scope.changeCaseStatus.status === "Ready For Review II") {
                             $scope.oInfo.casePrevAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                             ObjectModelService.setAssignee($scope.oInfo, 'supervisor@apvitacms.com');
                             ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                             $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "Returned For Revision" || $scope.changeCaseStatus.status === "OPT Case - Non-Actionable"
                                    || $scope.changeCaseStatus.status === "Returned For Revision II" ) {
                             ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                             ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                             $scope.updateParticipants();

                        } else if ( $scope.changeCaseStatus.status === "NON-OPT Case - Non-Actionable" ) {
                           //assign to system
                           ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                           ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                           $scope.updateParticipants();
                        }
                        else  if ($scope.changeCaseStatus.status === "Review Approved" || $scope.changeCaseStatus.status === "Review Approved II" ) {
                             ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                             ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                             $scope.updateParticipants();
                        } else if ($scope.changeCaseStatus.status === "Submitted to CMS"
                                || $scope.changeCaseStatus.status === "Submitted To CMS II"
                                || $scope.changeCaseStatus.status === "Submitted to CMS-Documentation Pending" ) {
                            //$scope.oInfo.casePrevAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                            ObjectModelService.setAssignee($scope.oInfo, 'cmsassignmentuser@apvitacms.com');
                            ObjectModelService.setGroup($scope.oInfo, 'CMS@APVITACMS.COM');
                            $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "OPT - DEX SENT - Docket Requested") {
                           ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                           ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                           $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "Resubmitted To CMS") {
                           //The CMS analyst who is assigned the case
                           ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevCMSAnalyst);
                           ObjectModelService.setGroup($scope.oInfo, 'CMS@APVITACMS.COM');
                           $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "CMS Pending- On Hold" ){
                            ObjectModelService.setAssignee($scope.oInfo, 'cms_testaccount@apvitacms.com');
                            ObjectModelService.setGroup($scope.oInfo, 'CMS@APVITACMS.COM');
                            $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "CMS Requested Edits") {

                            var holidays = ["12/31", "1/17", "2/21", "5/30", "6/20","7/4", "9/15", "10/10", "11/11", "11/24", "12/25"];

                            if($scope.oInfo.caseType === "OPT" || $scope.oInfo.caseType === "MED"){
                                var numberOfDaysToAdd = 3;
                            } else {
                                var numberOfDaysToAdd = 5;
                            }
                            var newDate = new Date();
                            var Sunday = 0;
                            var Saturday = 6;
                            var daysRemaining = numberOfDaysToAdd;
                            var counter = 0;

                            while (daysRemaining > 0) {
                                 var someDate = new Date();
                                 someDate.setDate(someDate.getDate() + counter);
                                 var month = someDate.getUTCMonth() + 1;
                                 var day = someDate.getUTCDate();
                                 var test = month +  "/" + day;
                                 var dayOfWeek = someDate.getDay();
                                 var isWeekend = (dayOfWeek === 6) || (dayOfWeek  === 0);
                                 if (!isWeekend && !holidays.includes(test)) {
                                       daysRemaining--;
                                 }
                                 counter++;
                            }

                            var result = newDate.setDate(newDate.getDate() + counter);

                            $scope.oInfo.caseResubDueDate = newDate;
                            $scope.oInfo.casePrevCMSAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                            ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                            ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                            $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "CMS Approved" && $scope.changeCaseStatus.status === "CMS Approved-Documentation Pending") {
                            $scope.oInfo.casePrevCMSAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                            ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                            ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                            $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "CASE_CLOSED") {
                              ObjectModelService.setAssignee($scope.oInfo, 'qaassignmentuser@apvitacms.com');
                              ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                              $scope.updateParticipants();

                        }  else if ($scope.changeCaseStatus.status === "Audit Completed" || $scope.changeCaseStatus.status === "Audit N/A") {
                            ObjectModelService.setAssignee($scope.oInfo, 'qacasearchiveuser@apvitacms.com');
                            ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                            $scope.updateParticipants();
                        } else if ($scope.changeCaseStatus.status === "Audit Assigned") {
                            ObjectModelService.setGroup($scope.oInfo, 'ALA_QA_ANALYST@APVITACMS.COM');
                            $scope.updateParticipants();
                        } else if ($scope.changeCaseStatus.status === "Case Deleted/Canceled") {
                              ObjectModelService.setAssignee($scope.oInfo, 'qaassignmentuser@apvitacms.com');
                              ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                              $scope.updateParticipants();
                         }


                        $scope.oInfo.status = $scope.changeCaseStatus.status;


                        
                        CaseInfoService.changeCaseFileState('change_case_status', $scope.changeCaseStatus).then(function(data) {
                            MessageService.info(data.info);
                          /* if($scope.changeCaseStatus.changeCaseStatusFlow){
                                $scope.changeCaseStatus.status = 'IN APPROVAL';
                            }*/
                            var caseInfo = Util.omitNg($scope.oInfo);
                            CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                                //success
                               // $scope.refresh();
                                //return caseInfo;
                                $modalInstance.close(caseInfo);
                            });

                        });

                    }

                    function cancelModal() {
                        $modalInstance.dismiss();
                    }

                } ]);
