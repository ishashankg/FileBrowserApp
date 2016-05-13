var app = angular.module('stepNext');
function bpmnModeler($http) {

    return {
        restrict: 'A',
        scope: {
            data: "=bpmnModeler"
        },
        link: function (scope, element, attrs) {
            var BpmnModeler = window.BpmnJS;

            var container = angular.element('#js-drop-zone');

            //var canvas = angular.element('#js-canvas');

            var renderer = new BpmnModeler({ container: element });
            
            $http.get("http://localhost:4500/test").then(function (res) {
                renderer.importXML(res.data.data, function (err) {
                    var canvas = renderer.get('canvas');
                    canvas.zoom('fit-viewport');
                    
//                     if (err) {
//                         container
//                             .removeClass('with-diagram')
//                             .addClass('with-error');
// 
//                         container.find('.error pre').text(err.message);
// 
//                         console.error(err);
//                     } else {
//                         container
//                             .removeClass('with-error')
//                             .addClass('with-diagram');
//                             
//                         
//                     }
                });
            });

        }
    }
}

app.directive('bpmnModeler', ['$http', bpmnModeler]);