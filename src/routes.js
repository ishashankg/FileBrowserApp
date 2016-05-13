(function() {

  'use strict';
  var express = require('express');
  var router = express.Router();
  var fs = require('fs');
  var path = require('path');
  var filePath;
  var result;

  /* GET home page. */
  router.get('/', function(req, res) {
    res.render('index');
  });

  /* Serve the Tree */
  router.get('/api/tree', function(req, res) {
    var _p;
    if (req.query.id == 1) {
      _p = path.resolve('', '', 'www/resources');
      processReq(_p, res);

    } else {
      if (req.query.id) {
        _p = req.query.id;
        processReq(_p, res);
      } else {
        res.json(['No valid data found']);
      }
    }
  });

  /* Serve a Resource */
  router.get('/api/resource', function(req, res) {
    filePath = req.query.resource;
    if(filePath.indexOf(".pdf") > -1){
       res.sendFile(filePath);
    }else if(filePath.indexOf(".png") > -1){
        var img = fs.readFileSync(filePath);
        res.writeHead(200, {'Content-Type': 'image/png' });
        res.end(img, 'binary');
    }
    else{
       res.send(fs.readFileSync(req.query.resource, 'UTF-8'));
     }
  });

  router.get("/test", function (req, res) {
      fs.readFile(path.join(__dirname, "newDiagram.bpmn"), function (err, data) {
          res.set({ "Content-Type": "text/xml" });
          res.send({'data': data.toString("utf8"), 'filePath' : filePath});
      }); 
  });

  router.post('/save', function(req,res){
     var content = req.body.content;
     if(filePath && content){
        fs.writeFile(filePath, content , function (err) {
        if (err) {
          console.error("Error writing to file");
        } else {
          console.log("Wrote to file");
        }
      })
       res.send("Success");
     }
  });

  function processReq(_p, res) {
    var resp = [];
    fs.readdir(_p, function(err, list) {
      for (var i = list.length - 1; i >= 0; i--) {
        resp.push(processNode(_p, list[i]));
      }
      console.log("Resp : "+JSON.stringify(resp));
      res.json(resp);
    });
  }

  function processNode(_p, f) {
    var s = fs.statSync(path.join(_p, f));
    return {
      "id": path.join(_p, f),
      "text": f,
      "icon" : s.isDirectory() ? 'jstree-custom-folder' : 'jstree-custom-file',
      "state": {
        "opened": false,
        "disabled": false,
        "selected": false
      },
      "li_attr": {
        "base": path.join(_p, f),
        "isLeaf": !s.isDirectory()
      },
      "children": s.isDirectory()
    };
  }

  module.exports = router;

}());
