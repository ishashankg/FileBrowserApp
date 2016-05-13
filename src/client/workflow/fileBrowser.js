(function() {
  'use strict';

    function fileBrowserController($scope, $http,$sce,FetchFileFactory, pdfDelegate) {
      var editor = ace.edit("aceEditor");
      $scope.fileViewer = 'Please select a file to view its contents';
      $('.content').hide();
      $('.textContent').hide();
      $('#message').html("<h4>Please select a file to view its contents</h4>");

      var BpmnViewer = window.BpmnJS;

      $scope.tree_core = {
        
        multiple: false,

        check_callback: function (operation, node, node_parent, node_position, more) {
            if (operation === 'move_node') {
                return false;  
            }
            return true;
        }
      };

      $scope.nodeSelected = function(e, data) {
        var _l = data.node.li_attr;
        if (_l.isLeaf) {
            if(_l.id.indexOf(".pdf") > -1){
              $('.content').hide();
              $('.textContent').show();
              $http.get('/api/resource?resource=' + encodeURIComponent(_l.base), { responseType: 'arraybuffer' })
                .success(function (response) {                  
                    var file = new Blob([response], { type: 'application/pdf' });
                    var fileURL = URL.createObjectURL(file);
                    $scope.pdfUrl = $sce.trustAsResourceUrl(fileURL).toString();
                    pdfDelegate.$getByHandle( 'my-pdf-container' ).load( $scope.pdfUrl );
                    $('#save').hide();
                    $('#aceEditor').hide();
                    $('#message').html("");
                    $('#pdfViewer').show();
                }).error(function () {
                    console.log("Error in reading pdf");
                });
      	    }else if (_l.id.indexOf(".bpmn") > -1){
                $('.textContent').hide();
                $('.bjs-container').remove();
                $('#message').html("");
                $('.content').show();

                var container = angular.element('#js-drop-zone');
                var viewer = new BpmnViewer({ container: container});

                FetchFileFactory.fetchFile(_l.base).then(function(data) {
                    var _d = data.data;
                    if (typeof _d == 'object') {
                       _d = JSON.stringify(_d, undefined, 2);
                    }

					var xml = _d;

					viewer.importXML(xml, function(err) {
						var canvas = viewer.get('canvas');
						canvas.zoom('fit-viewport');
					});
				});

				$scope.saveBpmn = function saveBpmn(){  
                  
					var bpmnXml ;       
					viewer.saveXML({ format: true }, function(err, xml) {
						bpmnXml = xml;
					});
					
					$http.post('/save',{'content': bpmnXml}).success(function(response){
						if(response == 'Success'){
							$('#message').html("Workflow edited successfully");
						}
					});
				}
            }
            else{
                $('.content').hide();
				$('#message').html("");
				$('.textContent').show();
      			FetchFileFactory.fetchFile(_l.base).then(function(data) {
      				var _d = data.data;
      				if (typeof _d == 'object') {
      				_d = JSON.stringify(_d, undefined, 2);
      				}
      				$scope.fileViewer = _d;
      				$('#save').show();
      				$('#message').html("");
      				if(_l.id.indexOf(".xml") > -1 || _l.id.indexOf(".xsd") > -1 || _l.id.indexOf(".activiti") > -1){
      				   editor.session.setMode("ace/mode/xml");
      				}else if(_l.id.indexOf(".drl") > -1){
      				   editor.session.setMode("ace/mode/drlfile");
      				}else if(_l.id.indexOf(".html") > -1){
					   editor.session.setMode("ace/mode/html");
					}else if(_l.id.indexOf(".groovy") > -1){
					   editor.session.setMode("ace/mode/groovy");
					}else if(_l.id.indexOf(".properties") > -1){
					   editor.session.setMode("ace/mode/properties");
					}else{
      				   editor.session.setMode("ace/mode/text");
      				}
					$('#aceEditor').show();
					$('#pdfViewer').hide();
					editor.$blockScrolling = Infinity;
					editor.setReadOnly(false);
					editor.setValue(_d);
				});
      		}
        } else {
          $scope.$apply(function() {
            $scope.fileViewer = 'Please select a file to view its contents';
            $('.textContent').hide();
            $('.content').hide();
            $('#message').html("<h4>Please select a file to view its contents</h4>");
          });
        }
      };
    }

    var app = angular.module('stepNext');
    app.requires.push('jsTree.directive');
    app.requires.push('ui.ace');
    app.requires.push('pdf');
    app.controller('fileBrowserController', ["$scope","$http","$sce","FetchFileFactory","pdfDelegate", fileBrowserController]);
})();
