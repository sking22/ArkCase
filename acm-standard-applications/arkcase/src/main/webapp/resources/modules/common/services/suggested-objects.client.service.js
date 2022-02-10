'use strict';

angular.module('services').factory('SuggestedObjectsService', ['$resource', '$http', 'UtilService', 'base64', function ($resource, $http, Util, base64) {

    return ({
        getSuggestedObjects: getSuggestedObjects,
        getSimilarCases: getSimilarCases
    });


    function getSimilarCases(ssn, npi, id, sanctionAssociatedTin, sanctionAssociatedNpi, sanctionAssociateLegalBusiness, sanctionAssociateFullName, convictName, convictTin, providerLegalBusiness) {
        return $http({
            method: 'POST',
            url: 'api/latest/service/suggestion/assoc_cases',
            isArray: false,
            cache: false,
            headers: {
                "Content-Type": "application/json"
            },
            data: {
                objectId: id,
                ssn: ssn,
                npi: npi,
                sanctionAssociatedTin: sanctionAssociatedTin,
                sanctionAssociatedNpi: sanctionAssociatedNpi,
                sanctionAssociateLegalBusiness: sanctionAssociateLegalBusiness,
                sanctionAssociateFullName: sanctionAssociateFullName,
                convictName: convictName,
                convictTin: convictTin,
                providerLegalBusiness: providerLegalBusiness
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
