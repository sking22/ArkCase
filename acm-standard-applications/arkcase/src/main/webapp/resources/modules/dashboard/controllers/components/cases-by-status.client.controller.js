'use strict';

angular.module('dashboard.cases-by-status').controller('Dashboard.CasesByStatusController', [
    '$scope', 'config', '$translate', '$modal', 'Authentication', 'Dashboard.DashboardService', 'Case.InfoService',
    'Object.LookupService', 'Object.ModelService', 'Task.AlertsService', 'Util.DateService', 'ConfigService',
    'params', 'UtilService', '$timeout', 'uiGridConstants',
    function ($scope, config, $translate, $modal, Authentication, DashboardService, CaseInfoService,
        ObjectLookupService, ObjectModelService, TaskAlertsService, UtilDateService, ConfigService,
        params, Util,  $timeout, uiGridConstants) {

        var vm = this;
        vm.config = null;
        var userInfo = null;
        //var userGroups = null;
        var userGroupList = null;

        if (!Util.isEmpty(params.description)) {
            $scope.$parent.model.description = " - " + params.description;
        } else {
            $scope.$parent.model.description = "";
        }

        ConfigService.getModuleConfig("common").then(function (moduleConfig) {
            $scope.userOrGroupSearchConfig = _.find(moduleConfig.components, {
                id: "userOrGroupSearch"
            });
        });
        ConfigService.getModuleConfig("common").then(function (moduleConfig) {
            $scope.userOrGroupSearchConfig = _.find(moduleConfig.components, {
                id: "userOrGroupSearch"
            });
        });

        ObjectLookupService.getGroups().then(function (groups) {
            var options = [];
            _.each(groups, function (group) {
                options.push({
                    value: group.name,
                    text: group.name
                });
            });
            $scope.owningGroups = options;
            return groups;
        });

        // Timeout id. Used to prevent too frequent filter requests
        var filterTimeout = null;

        // Be sure that filterTimeout is canceled on destroy
        $scope.$on('$destroy', function() {
            if (angular.isDefined(filterTimeout)) {
                $timeout.cancel(filterTimeout);
            }
        });

        ConfigService.getComponentConfig("dashboard", "casesByStatus").then(function (config) {
            // Load Cases info and render chart
            vm.config = config;
            vm.gridOptions.columnDefs = config.columnDefs;
            vm.gridOptions.enableFiltering = config.enableFiltering;
            vm.gridOptions.paginationPageSizes = config.paginationPageSizes;
            vm.gridOptions.paginationPageSize = config.paginationPageSize;
            paginationOptions.pageSize = config.paginationPageSize;

            Authentication.queryUserInfo().then(function (responseUserInfo) {
                userInfo = responseUserInfo;
                var userGroups = _.filter(responseUserInfo.authorities, function (userGroup) {
                    return _.startsWith(userGroup, 'ROLE') == false;
                });
                userGroupList = userGroups.join("\" OR \"");
                userGroupList = "(\"" + userGroupList + "\")";
                userGroupList = encodeURIComponent(userGroupList);

                getPage();
                return userInfo;
            });
        });

        var paginationOptions = {
            pageNumber: 1,
            pageSize: 5,
            sortBy: 'id',
            sortDir: 'desc'
        };
        /**
         * @ngdoc method
         * @name openViewer
         * @methodOf dashboard.active-case-files.controller:Dashboard.ActiveCaseFilesController
         *
         *
         * @description
         * This method opens the selected file in the snowbound viewer
         * @param rowData
         */
        vm.openViewer = function (rowData) {
            if (rowData && rowData.entity.object_id_s) {
                window.open(window.location.href.split('!')[0] + '!/cases/' + rowData.entity.object_id_s + '/main', '_self');
            }
        };

        var rowTmpl = '<div ng-class="{\'overdue\':row.entity.isOverdue, \'deadline\':row.entity.isDeadline}"><div ng-repeat="(colRenderIndex, col) in colContainer.renderedColumns track by col.colDef.name" class="ui-grid-cell" ng-class="{ \'ui-grid-row-header-cell\': col.isRowHeader }" ui-grid-cell></div></div>';

        vm.gridOptions = {
            enableColumnResizing: true,
            enableRowSelection: true,
            enableSelectAll: false,
            enableRowHeaderSelection: false,
            useExternalPagination: true,
            useExternalSorting: true,
            multiSelect: false,
            noUnselect: false,
            columnDefs: [],
            rowTemplate: rowTmpl,
            onRegisterApi: function (gridApi) {
                vm.gridApi = gridApi;

                gridApi.core.on.filterChanged($scope, function() {
                    var context = this;

                    // Prevent frequent filters requests
                    if (angular.isDefined(filterTimeout)) {
                        $timeout.cancel(filterTimeout);
                    }

                    filterTimeout = $timeout(function() {
                        var filters = [];

                        // Find filter
                        _.forEach(context.grid.columns, function(column) {
                            _.forEach(column.filters, function(columnFilter) {
                                /*console.log("!!!! column: ", column);
                                console.log("!!!! columnFilter: ", columnFilter);*/

                                if (!_.isUndefined(columnFilter.term)) {
                                    var filterOption = {
                                        column: column.name
                                    };

                                    // Parese date filter and try to create Date object.
                                    // If error happens then don't add it to the filter
                                    /*if (column.name == 'dueDate_tdt' || column.name == 'queue_enter_date_tdt') {
                                        var dateObj = moment(columnFilter.term, $scope.config['dateFormat']);
                                        if (dateObj.isValid()) {
                                            filterOption.value = dateObj.toDate();
                                        } else {
                                            return;
                                        }
                                    } else {*/

                                    filterOption.value = columnFilter.term

                                    if(filterOption.value !== ""){
                                        filters.push(filterOption);
                                    }

                                    /*console.log("!!!! filterOption: ", filterOption);*/

                                }
                            })
                        });
                        paginationOptions.filters = filters;
                        /*console.log("!!!! paginationOptions.filters: ", paginationOptions.filters);*/
                        if(paginationOptions.filters){
                            if(paginationOptions.filters.length === 0) {
                                for( var i = 0; i <  vm.gridOptions.paginationPageSizes.length; i++){
                                    if ( vm.gridOptions.paginationPageSizes[i] === vm.gridOptions.totalItems) {
                                        vm.gridOptions.paginationPageSizes.splice(i, 1);
                                    }
                                }
                                paginationOptions.pageSize = 25;
                                vm.gridOptions.paginationPageSize = 25;
                            } else {
                                if(!vm.gridOptions.paginationPageSizes.includes(vm.gridOptions.totalItems)) {
                                    paginationOptions.pageSize = vm.gridOptions.totalItems;
                                    vm.gridOptions.paginationPageSizes.push(paginationOptions.pageSize);
                                    vm.gridOptions.paginationPageSize = paginationOptions.pageSize;
                                }
                            }
                        }
                    }, 500);
                });

                gridApi.core.on.sortChanged($scope, function (grid, sortColumns) {
                    if (sortColumns.length == 0) {
                        paginationOptions.sort = null;
                    } else {
                        paginationOptions.sortBy = sortColumns[0].name;
                        paginationOptions.sortDir = sortColumns[0].sort.direction;
                    }
                    getPage();
                });
                gridApi.pagination.on.paginationChanged($scope, function (newPage, pageSize) {
                    paginationOptions.pageNumber = newPage;
                    paginationOptions.pageSize = pageSize;
                    getPage();
                });
            }
        };

        function getPage() {
            DashboardService.queryActiveCaseFiles({
                userId: userInfo.userId,
                userGroupList: userGroupList,
                sortBy: paginationOptions.sortBy,
                sortDir: paginationOptions.sortDir,
                startWith: (paginationOptions.pageNumber - 1) * paginationOptions.pageSize,
                pageSize: paginationOptions.pageSize
            }, function (data) {
                vm.gridOptions.data = [];
                vm.gridOptions.totalItems = data.response.numFound;

                _.forEach(data.response.docs, function (value) {
                    value.status_lcs = value.status_lcs.toUpperCase();

                    if (Util.goodValue(value.dueDate_tdt)) {
                        value.dueDate_tdt = UtilDateService.isoToLocalDateTime(value.dueDate_tdt);

                        //calculate to show alert icons if cases is in overdue or deadline is approaching
                        value.isOverdue = TaskAlertsService.calculateOverdue(value.dueDate_tdt);
                        value.isDeadline = TaskAlertsService.calculateDeadline(value.dueDate_tdt);
                    }


                    if(value.status_lcs === "AUDIT N/A" || value.status_lcs === "CASE_CLOSED"
                        || value.status_lcs ===  "CMS APPROVED" || value.status_lcs === "AUDIT ASSIGNED"
                        || value.status_lcs ===  "AUDIT COMPLETED"){

                        value.isOverdue = false;
                        value.isDeadline = false;
                    }

                    vm.gridOptions.data.push(value);
                });
            });

        }

        $scope.saveCase = function (row, selectedUser, selectedGroup) {
            var promiseSaveInfo = Util.errorPromise($translate.instant("common.service.error.invalidData"));
            if (CaseInfoService.validateCaseInfo($scope.objectInfo)) {
                var objectInfo = Util.omitNg($scope.objectInfo);
                promiseSaveInfo = CaseInfoService.saveCaseInfo(objectInfo);
                promiseSaveInfo.then(function (caseInfo) {
                    //$scope.$emit("report-object-updated", caseInfo);
                    return caseInfo;
                }, function (error) {
                    //$scope.$emit("report-object-update-failed", error);
                    return error;
                });
            }
            return promiseSaveInfo;
        };
        $scope.updateOwningGroup = function (row) {
            ObjectModelService.setGroup($scope.objectInfo, $scope.owningGroup);
        };
        $scope.retrieveCaseInfo = function (id) {
            CaseInfoService.getCaseInfo(id).then(function (caseInfo) {
                $scope.objectInfo = caseInfo;
            });
        }
        $scope.updateAssignee = function (row, selectedUser, selectedGroup) {
            $scope.assignee = selectedUser.object_id_s;
            ObjectModelService.setAssignee($scope.objectInfo, $scope.assignee);

            //set for AFDP-6831 to inheritance in the Folder/file participants
            var len = $scope.objectInfo.participants.length;
            for (var i = 0; i < len; i++) {
                if ($scope.objectInfo.participants[i].participantType == 'assignee' || $scope.objectInfo.participants[i].participantType == 'owning group') {
                    $scope.objectInfo.participants[i].replaceChildrenParticipant = true;
                }
            }
            if (selectedGroup) {
                $scope.owningGroup = selectedGroup.object_id_s;
                $scope.updateOwningGroup(row);
                $scope.saveCase(row, selectedUser, selectedGroup);
                row.entity.assignee_full_name_lcs = selectedUser.name_lcs;
            } else {
                $scope.saveCase(row, selectedUser, selectedGroup);
                row.entity.assignee_full_name_lcs = selectedUser.name_lcs;
            }

        };

        $scope.userOrGroupSearch = function (row) {

            var assigneUserName = _.find($scope.userFullNames, function (user) {
                return user.id === $scope.assignee
            });
            var params = {
                owningGroup: $scope.owningGroup,
                assignee: assigneUserName
            };
            var modalInstance = $modal.open({
                animation: $scope.animationsEnabled,
                templateUrl: 'modules/common/views/user-group-picker-modal.client.view.html',
                controller: 'Common.UserGroupPickerController',
                size: 'lg',
                backdrop: 'static',
                resolve: {
                    $filter: function () {
                        return $scope.userOrGroupSearchConfig.userOrGroupSearchFilters.userOrGroupFacetFilter;
                    },
                    $extraFilter: function () {
                        return $scope.userOrGroupSearchConfig.userOrGroupSearchFilters.userOrGroupFacetExtraFilter;
                    },
                    $config: function () {
                        return $scope.userOrGroupSearchConfig;
                    },
                    $params: function () {
                        return params;
                    }
                }
            });

            modalInstance.result.then(function (selection) {

                if (selection) {

                    var selectedObjectType = selection.masterSelectedItem.object_type_s;
                    if (selectedObjectType === 'USER') { // Selected user
                        var selectedUser = selection.masterSelectedItem;
                        var selectedGroup = selection.detailSelectedItems;
                        var id = row.entity.object_id_i;
                        CaseInfoService.getCaseInfo(id).then(function (caseInfo) {
                            $scope.objectInfo = caseInfo;
                            $scope.updateAssignee(row, selectedUser, selectedGroup);
                        });

                        return;
                    } 
                }
            });
        };
    }]);
