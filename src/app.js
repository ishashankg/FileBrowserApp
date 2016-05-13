var express = require("express");
var fs = require("fs");
var path = require("path");
var bodyParser = require("body-parser");
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var routes = require('./routes.js');

var app = express();
app.use(express.static(path.join(__dirname, "../www")));

app.set('views', path.join(__dirname, 'views'));
app.engine('html', require('ejs').renderFile);
app.set('view engine', 'html');

app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
    extended: true
}));
app.use(cookieParser());
app.use('/', routes);

/* app.get("/test", function (req, res) {
    fs.readFile(path.join(__dirname, "newDiagram.bpmn"), function (err, data) {
        res.set({ "Content-Type": "text/xml" });
        res.send(data.toString("utf8"));
    });
}); */

var port = process.PORT || 4500;
app.listen(port, function () {
    console.log("Started listening on port", port);
});