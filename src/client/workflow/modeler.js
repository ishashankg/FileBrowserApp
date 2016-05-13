(function () {
    function modelerController($scope, $http) {
        //$scope.name = "Nishant Chaturvedi";
        $scope.schema = "";
        (function(){
            // $http.get("http://localhost:4500/test").then(function(res){
            //     $scope.schema = res.data;
            //     console.log(res.data);
            // });
        })();
        
    }

    angular.module("stepNext").controller("modelerController", ["$scope","$http", modelerController]);
})();