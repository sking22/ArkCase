'use strict';

angular.module('cases').controller(
        'Cases.CaseSaveModalController',
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
        'Mentions.Service',
        function($scope, $http, $stateParams, $translate, $modalInstance, ComplaintInfoService, $state, ObjectLookupService, MessageService, Util, $modal, ConfigService, ObjectService, modalParams, CaseInfoService, ObjectParticipantService, AdminFormWorkflowsLinkService, MentionsService) {


            console.log('modalParams: ' + JSON.stringify(modalParams));
            $scope.modalParams = modalParams;

            var caseInfo = Util.omitNg($scope.modalParams.info);
            CaseInfoService.saveCaseInfo(caseInfo).then(function(caseInfo) {
             //   MentionsService.sendEmailToMentionedUsers($scope.paramsSummernote.emailAddresses, $scope.paramsSummernote.usersMentioned, ObjectService.ObjectTypes.CASE_FILE, "DETAILS", caseInfo.id, caseInfo.details);
                MessageService.info($translate.instant("Case Details Saved."));
                $modalInstance.close(caseInfo);
            });



        } ]);