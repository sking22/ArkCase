'use strict';

angular.module('reports').factory('Reports.Data', [ function() {
    var data = {
        reportSelected: '',
        caseStateSelected: '',
        caseType: '',
        endDate: new Date(),
        startDate: new Date()
    };

    return {
        getData: function() {
            return data;
        },

        setData: function(newData) {
            data = newData;
        }
    }
} ]);