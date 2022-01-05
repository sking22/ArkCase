'use strict';

angular.module('services').factory('SuggestedObjectsService', ['$resource', '$http', 'UtilService', 'base64', function ($resource, $http, Util, base64) {

    return ({
        getSuggestedObjects: getSuggestedObjects,
        getSimilarCases: getSimilarCases
    });


    function getSimilarCases(ssn, npi, id) {
        var Service = $resource('api/latest/service/suggestion/assoc_cases', {}, {

            getAssociatedCases: {
                method: 'POST',
                url: 'api/latest/service/suggestion/assoc_cases',
                cache: false
            }
        });
        return Util.serviceCall({
            service: Service.getAssociatedCases,
            data: {
                objectId: id,
                ssn: ssn,
                npi: npi
            },
            onSuccess: function (data) {
                return data;
            },
            onError: function (errorData) {
                return errorData;
            }
        });
    }

    function getSuggestedObjects(title, type, id) {
        return $http({
            url: 'api/latest/service/suggestion/' + base64.urlencode(title),
            method: 'GET',
            params: {
                objectId: id,
                objectType: type
            }
        });
    }

}]);
