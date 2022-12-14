'use strict';

angular.module('dashboard.associate-bot', [ 'adf.provider' ]).config(function(ArkCaseDashboardProvider) {
    ArkCaseDashboardProvider.widget('associateBotBtn', {
        title: 'dashboard.widgets.associateBotBtn.title',
        description: 'dashboard.widgets.associateBotBtn.description',
        controller: 'Dashboard.AssociateBotBtn',
        controllerAs: 'associateBotBtn',
        reload: true,
        templateUrl: 'modules/dashboard/views/components/associate-bot.client.view.html',
        resolve: {
            params: function(config) {
                return config;
            }
        }
//        },
//        edit: {
//            templateUrl: 'modules/dashboard/views/components/team-workload-edit.client.view.html'
//        }
    });
});
