'use strict';
/**
 * @ngdoc controller
 * @name dashboard.active-case-files.controller:Dashboard.ActiveCaseFilesController
 *
 * @description
 *
 * {@link /acm-standard-applications/arkcase/src/main/webapp/resources/modules/dashboard/controllers/components/active-case-files.client.controller.js modules/dashboard/controllers/components/active-case-files.client.controller.js}
 *
 * Loads cases in the "Active Case Files" widget.
 */
angular.module('dashboard.active-case-files').controller('Dashboard.ActiveCaseFilesController', [ '$scope', '$translate', 'config', 'Authentication', 'Dashboard.DashboardService', 'Task.AlertsService', 'Util.DateService', 'ConfigService', 'params', 'UtilService', '$timeout', 'uiGridConstants', function($scope, $translate, config, Authentication, DashboardService, TaskAlertsService, UtilDateService, ConfigService, params, Util, $timeout, uiGridConstants) {
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

    // Timeout id. Used to prevent too frequent filter requests
    var filterTimeout = null;

    // Be sure that filterTimeout is canceled on destroy
    $scope.$on('$destroy', function() {
        if (angular.isDefined(filterTimeout)) {
            $timeout.cancel(filterTimeout);
        }
    });

    ConfigService.getComponentConfig("dashboard", "activeCaseFiles").then(function(config) {
        vm.config = config;
        vm.gridOptions.columnDefs = config.columnDefs;
        vm.gridOptions.enableFiltering = config.enableFiltering;
        vm.gridOptions.paginationPageSizes = config.paginationPageSizes;
        vm.gridOptions.paginationPageSize = paginationOptions.pageSize;

        Authentication.queryUserInfo().then(function(responseUserInfo) {
            userInfo = responseUserInfo;
            var userGroups = _.filter(responseUserInfo.authorities, function(userGroup) {
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
        pageSize: 25,
        sortBy: 'id',
        sortDir: 'desc'
    };

    //Get the user's defined options from the Config.
    if (config.paginationPageSize) {
        paginationOptions.pageSize = 25;
        //paginationOptions.pageSize = parseInt(config.paginationPageSize);
    } else {
        //defaults the dropdown value on edit UI to the default pagination options
        config.paginationPageSize = "" + paginationOptions.pageSize + "";
    }

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
    vm.openViewer = function(rowData) {
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
        onRegisterApi: function(gridApi) {
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
                        console.log("!!!! column: ", column);
                        console.log("!!!! columnFilter: ", columnFilter);

                        if (!_.isUndefined(columnFilter.term)) {
                            var filterOption = {
                                column: column.name
                            };

                            // Parese date filter and try to create Date object.
                            // If error happens then don't add it to the filter
                            if (column.name == 'dueDate_tdt' || column.name == 'queue_enter_date_tdt') {
                                var dateObj = moment(columnFilter.term, $scope.config['dateFormat']);
                                if (dateObj.isValid()) {
                                    filterOption.value = dateObj.toDate();
                                } else {
                                    return;
                                }
                            } else {
                                filterOption.value = columnFilter.term
                            }

                            if (columnFilter.condition == uiGridConstants.filter.GREATER_THAN_OR_EQUAL) {
                                filterOption.condition = 'from';
                            } else if (columnFilter.condition == uiGridConstants.filter.LESS_THAN_OR_EQUAL) {
                                filterOption.condition = 'to';
                            }
                            if(filterOption.value !== ""){
                                filters.push(filterOption);
                            }

                            console.log("!!!! filterOption: ", filterOption);

                        }
                    })
                });
                paginationOptions.filters = filters;
                console.log("!!!! paginationOptions.filters: ", paginationOptions.filters);
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
                        paginationOptions.pageSize = vm.gridOptions.totalItems;
                        vm.gridOptions.paginationPageSizes.push(paginationOptions.pageSize);
                        vm.gridOptions.paginationPageSize = paginationOptions.pageSize;
                    }
                }
            }, 500);
        });
            gridApi.core.on.sortChanged($scope, function(grid, sortColumns) {
                if (sortColumns.length == 0) {
                    paginationOptions.sort = null;
                } else {
                    paginationOptions.sortBy = sortColumns[0].name;
                    paginationOptions.sortDir = sortColumns[0].sort.direction;
                }
                getPage();
            });
            gridApi.pagination.on.paginationChanged($scope, function(newPage, pageSize) {
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
            pageSize: paginationOptions.pageSize,
            filters: paginationOptions.filters
        }, function(data) {
            vm.gridOptions.data = [];
            vm.gridOptions.totalItems = data.response.numFound;

            _.forEach(data.response.docs, function(value) {
                value.status_lcs = value.status_lcs.toUpperCase();

                if ((userGroupList.includes(value.owning_group_id_lcs)) == true) {
                    data.response.docs.getElementById(value.id).style.display = "none";
                }

                if (Util.goodValue(value.dueDate_tdt)) {
                    value.dueDate_tdt = UtilDateService.isoToLocalDateTime(value.dueDate_tdt);

                    //calculate to show alert icons if cases is in overdue or deadline is approaching
                    value.isOverdue = TaskAlertsService.calculateOverdue(value.dueDate_tdt);
                    value.isDeadline = TaskAlertsService.calculateDeadline(value.dueDate_tdt);
                }

                vm.gridOptions.data.push(value);
                console.log("!!! data: ", data);
                console.log("!!! value: ", value);
                console.log("!!! paginationOptions.filters: ", paginationOptions.filters);
               // console.log("!!! filters: ", filters);


            });

        });
    }
} ]);
