(function() {
    'use strict';

    var app = angular.module('angularApp');

    app.controller('SearchResultsController', function($stateParams, $scope, $q, $sessionStorage, $timeout, ICATSearchService, IdsManager, APP_CONFIG, Cart){
    	var facilities = $stateParams.facilities ? JSON.parse($stateParams.facilities) : [];
    	var text = $stateParams.text;
    	var type = $stateParams.type;
    	var startDate = $stateParams.startDate;
    	var endDate = $stateParams.endDate;
        var parameters = $stateParams.parameters ? JSON.parse($stateParams.parameters) : [];
        var samples = $stateParams.samples ? JSON.parse($stateParams.samples) : [];
        var gridApi;

        var canceler = $q.defer();
        $scope.$on('$destroy', function(){ canceler.resolve(); });

        var gridOptions = {data: []};
        _.merge(gridOptions, APP_CONFIG.site.searchGridOptions[type]);
        _.each(gridOptions.columnDefs, function(column){
            if(column.field == 'size'){
                column.cellTemplate = '<div class="ui-grid-cell-contents"><span us-spinner="{radius:2, width:2, length: 2}"  spinner-on="row.entity.size === undefined" class="grid-cell-spinner"></span><span>{{row.entity.size|bytes}}</span></div>';
            }
        });
        this.gridOptions = gridOptions;

     	var query = {target: type}
     	if(text) query.text = text;
     	if(startDate) query.lower = startDate.replace(/-/g, '') + "0000";
     	if(endDate) query.upper = endDate.replace(/-/g, '') + "0000";
        if(parameters.length > 0){
            query.parameters = _.map(parameters, function(parameter){
                var out = {};
                out.name = parameter.name;
                if(parameter.valueType === 'text'){
                    out.stringValue = parameter.value;
                } else if(parameter.valueType === 'number'){
                    out.lowerNumericValue = parameter.value;
                    out.upperNumericValue = parameter.value;
                } else if(parameter.valueType === 'date'){
                    var date = parameter.value.replace(/-/g, '') + "0000";
                    out.lowerDateValue = date;
                    out.upperDateValue = date;
                }
                return out;
            });
        }
        if(samples.length > 0) query.samples = samples;

        ICATSearchService.search(facilities, query, function(results){
        	gridOptions.data = results;
            $timeout(function(){
                _.each(results, function(row){
                    if (Cart.hasItem(row.facilityName, type.toLowerCase(), row.id)) {
                        gridApi.selection.selectRow(row);
                    } else {
                        gridApi.selection.unSelectRow(row);
                    }
                });
            });
        }, canceler.promise).then(function(){
            _.each(gridOptions.data, function(entity){
                getSize(entity.facilityName, entity.id).then(function(data){
                    entity.size = parseInt(data);
                });
            });
        });

        //proxy to simplify later refactor
        function getSize(facilityName, id){
            var params = {};
            params[type.replace(/^./, function myFunction(x){return x.toLowerCase();})  + 'Ids'] = id;
            params.canceler = canceler.promise;
            return IdsManager.getSize($sessionStorage.sessions, APP_CONFIG.facilities[facilityName], params);
        }

        function addItem(row){
            Cart.addItem(row.facilityName, type.toLowerCase(), row.id, row.name, []);
        }

        function removeItem(row){
            Cart.removeItem(row.facilityName, type.toLowerCase(), row.id);
        }

        gridOptions.onRegisterApi = function(_gridApi) {
            gridApi = _gridApi;

            gridApi.selection.on.rowSelectionChanged($scope, function(row) {
                if(_.find(gridApi.selection.getSelectedRows(), _.pick(row.entity, ['facilityName', 'id']))){
                    addItem(row.entity);
                } else {
                    removeItem(row.entity);
                }
            });

            gridApi.selection.on.rowSelectionChangedBatch($scope, function(rows) {
                _.each(rows, function(row){
                    if(_.find(gridApi.selection.getSelectedRows(), _.pick(row.entity, ['facilityName', 'id']))){
                        addItem(row.entity.entity);
                    } else {
                        removeItem(row.entity);
                    }
                });
            });
        };

    });

})();