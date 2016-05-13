(function() {
  'use strict';
    function fetchFileFactory($http) {
      var _factory = {};

      _factory.fetchFile = function(file) {
        return $http.get('/api/resource?resource=' + encodeURIComponent(file));
      };

      return _factory;
    }
  
  var app = angular.module('stepNext');
  app.requires.push('jsTree.directive');
  app.requires.push('ui.ace');
  app.factory('FetchFileFactory', ['$http', fetchFileFactory ]);
})();
