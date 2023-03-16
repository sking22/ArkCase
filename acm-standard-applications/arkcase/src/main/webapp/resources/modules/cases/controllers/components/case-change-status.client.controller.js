'use strict';

angular.module('cases').controller(
        'Cases.ChangeStatusController',
        [ '$scope', '$http', '$stateParams', '$translate', '$modalInstance', 'Complaint.InfoService', '$state', 'Object.LookupService', 'MessageService', 'UtilService', '$modal', 'ConfigService', 'ObjectService', 'modalParams', 'Case.InfoService', 'Object.ParticipantService','Admin.FormWorkflowsLinkService', 'Object.ModelService', 'Profile.UserInfoService','Helper.NoteService', 'Object.NoteService', 'Authentication',
                function($scope, $http, $stateParams, $translate, $modalInstance, ComplaintInfoService, $state, ObjectLookupService, MessageService, Util, $modal, ConfigService, ObjectService, modalParams, CaseInfoService, ObjectParticipantService, AdminFormWorkflowsLinkService, ObjectModelService, UserInfoService,  HelperNoteService, ObjectNoteService, Authentication) {

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
                        assignee: null,
                        description: "",
                        changeCaseStatusFlow: $scope.showApprover == 'true'
                    };

                    $scope.note = {};
                    $scope.note.note = "";

                    ConfigService.getComponentConfig("cases", "notes").then(function (config) {
                     $scope.notesInit = {
                            objectType: ObjectService.ObjectTypes.CASE_FILE,
                            currentObjectId: $stateParams.id
                         };
                     });

                      var noteHelper = new HelperNoteService.Note();
                     $scope.notesInit = {
                          noteTitle: $translate.instant("cases.comp.notes.title"),
                          objectType: ObjectService.ObjectTypes.CASE_FILE,
                          currentObjectId: $stateParams.id,
                          parentTitle: "",
                          noteType: "GENERAL"
                     };

                     //var userId = "";
                      Authentication.queryUserInfo().then(function(userInfo) {
                          var userId = userInfo.userId;
                          var info = $scope.notesInit;
                          $scope.note = noteHelper.createNote(info.currentObjectId, info.objectType, $scope.oInfo.caseNumber, info.tag, userId );

                         return userInfo;
                     });


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
                        $scope.statuses = [];
                        var defaultChangeCaseStatus = ObjectLookupService.getPrimaryLookup($scope.statuses);
                        if (defaultChangeCaseStatus && !$scope.changeCaseStatus.status) {
                            $scope.changeCaseStatus.status = defaultChangeCaseStatus.key;
                        }
                        UserInfoService.getUserInfo().then(function(infoData) {
                            $scope.currentUserProfile = infoData;
                            $scope.isCms = $scope.currentUserProfile.groups[0] === "CMS@APVITACMS.COM";
                            if($scope.isCms){
                                for(var i = 0; i < caseStatuses.length; i++){
                                    if(caseStatuses[i].key === 'CMS Approved'
                                        || caseStatuses[i].key === 'CMS Approved-Documentation Pending'
                                        || caseStatuses[i].key === 'CMS Pending- On Hold'
                                        || caseStatuses[i].key === 'CMS Requested Edits'){
                                        $scope.statuses.push(caseStatuses[i]);
                                    }
                                }
                            } else {
                                $scope.statuses = caseStatuses;
                            }
                        });
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
                            if(ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'analysttest@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'supervisor@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'cmsassignmentuser@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'cms_testaccount@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'qaassignmentuser@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'qacasearchiveuser@apvitacms.com') {
                                $scope.oInfo.casePrevAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                                ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                                ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                                $scope.updateParticipants();
                            }

                        } else if ($scope.changeCaseStatus.status === "Ready For Review" || $scope.changeCaseStatus.status === "Ready For Review II") {
                            if(ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'analysttest@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'supervisor@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'cmsassignmentuser@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'cms_testaccount@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'qaassignmentuser@apvitacms.com'
                            && ObjectModelService.getAssignee($scope.oInfo).toLowerCase() !== 'qacasearchiveuser@apvitacms.com') {
                                 $scope.oInfo.casePrevAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                            }
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
                             ObjectModelService.setAssignee($scope.oInfo, 'supervisor@apvitacms.com');
                             ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                             $scope.updateParticipants();
                        } else if ($scope.changeCaseStatus.status === "Submitted to CMS"
                                || $scope.changeCaseStatus.status === "Submitted To CMS II"
                                || $scope.changeCaseStatus.status === "Submitted to CMS-Documentation Pending" ) {
                            //$scope.oInfo.casePrevAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                            ObjectModelService.setAssignee($scope.oInfo, 'cmsassignmentuser@apvitacms.com');
                            ObjectModelService.setGroup($scope.oInfo, 'CMS@APVITACMS.COM');
                            $scope.oInfo.priority = "CMS";
                            $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "OPT - DEX SENT - Docket Requested") {
                           ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                           ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                           $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "Resubmitted To CMS") {
                           //The CMS analyst who is assigned the case
                           ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevCMSAnalyst);
                           ObjectModelService.setGroup($scope.oInfo, 'CMS@APVITACMS.COM');
                           $scope.oInfo.priority = "CMS";
                           $scope.updateParticipants();

                        }  else if ($scope.changeCaseStatus.status === "R&R On Pending Case" || $scope.changeCaseStatus.status === "R&R On Approved Case" || $scope.changeCaseStatus.status === "Submitted to CMS - ORR (Original Recommendation Retracted)") {
                             //The CMS analyst who is assigned the case
                             console.log("casePrevCMSAnalyst: " + $scope.oInfo.casePrevCMSAnalyst);
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

                        } else if ($scope.changeCaseStatus.status === "CMS Approved" || $scope.changeCaseStatus.status === "CMS Approved-Documentation Pending") {
                            $scope.oInfo.casePrevCMSAnalyst = ObjectModelService.getAssignee($scope.oInfo);
                            ObjectModelService.setAssignee($scope.oInfo, $scope.oInfo.casePrevAnalyst);
                            ObjectModelService.setGroup($scope.oInfo, 'ALA_ANALYST@APVITACMS.COM');
                            $scope.oInfo.priority = "N/A";
                            $scope.updateParticipants();

                        } else if ($scope.changeCaseStatus.status === "CASE_CLOSED") {
                              ObjectModelService.setAssignee($scope.oInfo, 'qaassignmentuser@apvitacms.com');
                              ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                              $scope.oInfo.priority = "N/A";
                              $scope.updateParticipants();

                        }  else if ($scope.changeCaseStatus.status === "Audit Completed" || $scope.changeCaseStatus.status === "Audit N/A") {
                            ObjectModelService.setAssignee($scope.oInfo, 'qacasearchiveuser@apvitacms.com');
                            ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                            $scope.oInfo.priority = "N/A";
                            $scope.updateParticipants();
                        } else if ($scope.changeCaseStatus.status === "Audit Assigned") {
                            ObjectModelService.setGroup($scope.oInfo, 'ALA_QA_ANALYST@APVITACMS.COM');
                            $scope.oInfo.priority = "N/A";
                            $scope.updateParticipants();
                        } else if ($scope.changeCaseStatus.status === "Case Deleted/Canceled") {
                              ObjectModelService.setAssignee($scope.oInfo, 'canceleddeletedcaseu@apvitacms.com');
                              ObjectModelService.setGroup($scope.oInfo, 'ALA_SUPERVISOR@APVITACMS.COM');
                              $scope.updateParticipants();
                        }

                        $scope.changeCaseStatus.assignee = ObjectModelService.getAssignee($scope.oInfo);
                        $scope.oInfo.status = $scope.changeCaseStatus.status;

                        
                        CaseInfoService.changeCaseFileState('change_case_status', $scope.changeCaseStatus).then(function(data) {
                            MessageService.info(data.info);
                            var caseInfo = Util.omitNg($scope.oInfo);
                            CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                                //success
                                if($scope.note.note){
                                    ObjectNoteService.saveNote($scope.note).then(function(note) {

                                    }, function() {

                                    });
                                }
                                $modalInstance.close(caseInfo);
                            });

                        });

                    }

                    function cancelModal() {
                        $modalInstance.dismiss();
                    }

                } ]);
