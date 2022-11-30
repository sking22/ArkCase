'use strict';

angular.module('dashboard.ready-review-cases', [ 'adf.provider' ]).config(function(ArkCaseDashboardProvider) {
    ArkCaseDashboardProvider.widget('readyReviewCases', {
        title: 'dashboard.widgets.readyReviewCases.title',
        description: 'dashboard.widgets.readyReviewCases.description',
        controller: 'Dashboard.ReadyReviewCaseFilesController',
        controllerAs: 'readyReviewCases',
        reload: true,
        templateUrl: 'modules/dashboard/views/components/ready-review-cases.client.view.html',
        resolve: {
            params: function(config) {
                return config;
            }
        },
        edit: {
            templateUrl: 'modules/dashboard/views/components/ready-review-cases-edit.client.view.html'
        }
    });
});