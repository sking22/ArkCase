'use strict';

angular.module('cases').controller(
        'Cases.NotesModalController',
        [ '$scope',
        '$http',
        '$stateParams',
        '$translate',
        '$modalInstance',
        'Complaint.InfoService',
        '$state',
        'Object.LookupService',
        'MessageService',
        'UtilService',
        '$modal',
        'ConfigService',
        'ObjectService',
        'modalParams',
        'Case.InfoService',
        'Object.ParticipantService',
        'Admin.FormWorkflowsLinkService',
        'Object.NoteService',
        'Helper.NoteService',
        '$config',
        function($scope, $http, $stateParams, $translate, $modalInstance, ComplaintInfoService, $state, ObjectLookupService, MessageService, Util, $modal, ConfigService, ObjectService, modalParams, CaseInfoService, ObjectParticipantService, AdminFormWorkflowsLinkService, ObjectNoteService, HelperNoteService, $config) {



            /* modalScope.note = note || {};
             modalScope.isEdit = false;*/

            $scope.onClickOk = function() {
               console.log('!!!! before savenote ');
                ObjectNoteService.saveNote(note).then(function(data) {
                    /*if (scope.mentionInfo) {
                        var url = "/home.html#!/viewer/" + scope.mentionInfo.fileId + "/" + scope.mentionInfo.containerObjectId + "/" + scope.mentionInfo.containerObjectType + "/" + encodeURIComponent(scope.mentionInfo.fileName) + "/" + scope.mentionInfo.fileId;
                        MentionsService.sendEmailToMentionedUsersWithUrl(scope.params.emailAddresses, scope.params.usersMentioned, scope.mentionInfo.containerObjectType, scope.mentionInfo.containerObjectId, url, note.note);
                    } else {
                        var noteParentType = "";
                        if (note.type == "REJECT_COMMENT") {
                            noteParentType = "TASK_REJECT_COMMENT"
                        } else if (note.type == "REJECT_COMMENT") {
                            noteParentType = "TASK_REJECT_COMMENT"
                        } else {
                            noteParentType = note.parentType;
                        }
                        MentionsService.sendEmailToMentionedUsers(scope.params.emailAddresses, scope.params.usersMentioned, "NOTE", noteParentType, note.parentId, note.note);
                    }
                    scope.retrieveGridData();*/
                    console.log('!!!! savenote ');
                     $modalInstance.close({
                        note: $scope.note
                    });
                });

            };
            $scope.onClickCancel = function() {
                $modalInstance.dismiss('cancel');
            };

           /* console.log('modalParams: ' + JSON.stringify(modalParams));
            $scope.modalParams = modalParams;*/

/*

            var caseInfo = Util.omitNg($scope.modalParams.info);
            CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
                MessageService.info("Note Saved.");
                $modalInstance.close(caseInfo);
            });
*/
       /* $scope.onClickOk = function() {
            $modalInstance.close({
                note: $scope.note,
                isEdit: $scope.isEdit
            });
        };*/
        $scope.onClickCancel = function() {
            $modalInstance.dismiss('cancel');
        }


        } ]);