var fs = require('fs'),
        http = require('http'),
        other = require('./other');

exports.hello = function(what) {
    console.log('Hello ' + what);
}

exports.hello = function(what, a, b) {
    console.log('Goodbye ' + what + ' ' + a + ' ' + b);
}
