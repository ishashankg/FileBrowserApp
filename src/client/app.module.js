var app = angular.module("stepNext", ['ngRoute']);

app.config(["$routeProvider", function ($routeProvider) {
    $routeProvider.
        when('/modeler', {
            templateUrl: 'workflow/modeler.html',
            controller: 'modelerController'
        }).
        when('/fileBrowser',{
            templateUrl: 'workflow/fileBrowser.html',
            controller: 'fileBrowserController'
        }).
        otherwise({
            redirectTo: '/modeler'
        });   
}]);

