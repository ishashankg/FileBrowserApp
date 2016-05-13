var gulp = require("gulp");
var concat = require("gulp-concat");
var stripDebug = require("gulp-strip-debug");
var uglify = require("gulp-uglify");
var rename = require("gulp-rename");
var inject = require("gulp-inject");
var sourcemaps = require("gulp-sourcemaps");

var gulpConfig = {

}

gulp.task("add-diagram-assets", function () {
    gulp.src(["./node_modules/bpmn-js/assets/**/*.*", "./node_modules/diagram-js/assets/*.css"])
        .pipe(gulp.dest("./www/css"));
});

gulp.task('move', function(){
    gulp.src(['./src/client/**/*.html'])
    .pipe(gulp.dest("./www"));
})

gulp.task('bundle-scripts', function () {
    var jsPath = {
        jsSrc: ['./src/client/app.module.js','./src/client/directives/*.js','./src/client/workflow/*.js'],
        jsDest: './www'
    };
    return gulp.src(jsPath.jsSrc)
        .pipe(sourcemaps.init())
        .pipe(concat('step.js'))
        //.pipe(stripDebug())
        .pipe(uglify())
        //.pipe(rename({ suffix: '-' + getDateTimeStamp() + '.min' }))
        .pipe(rename({ suffix: '.min' }))
        .pipe(sourcemaps.write())
        .pipe(gulp.dest(jsPath.jsDest));
});

gulp.task('update-references', ['bundle-scripts'], function () {
    var target = gulp.src('./src/client/index.html');
    var sources = gulp.src(['./www/*.js'], { read: false});

    return target.pipe(inject(sources,{relative: false, transform:function(filepath){
        return '<script src="step.min.js"></script>';
    }}))
       .pipe(gulp.dest('./www'));
});


gulp.task('copy', function () {
    return gulp.src(['./src/client/imgs/**/*'])
          .pipe(gulp.dest('./www/imgs'));
});

gulp.task('copyCss', function () {
    return gulp.src(['./src/client/css/**/*'])
          .pipe(gulp.dest('./www/css'));
});

gulp.task('copyJs', function () {
    return gulp.src(['./src/client/js/**/*'])
          .pipe(gulp.dest('./www/js'));
});

gulp.task("default", ["add-diagram-assets","move", "bundle-scripts","update-references","copy","copyCss","copyJs"]);

function getDateTimeStamp() {
    var currentDate = new Date();
    return currentDate.getDate().toString() + currentDate.getHours().toString() + currentDate.getMinutes().toString() + currentDate.getSeconds().toString();
};