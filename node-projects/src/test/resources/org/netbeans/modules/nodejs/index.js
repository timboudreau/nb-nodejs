var fs = require('fs'),
        http = require('http'),
        boo = require('boo'),
        other = require('./other');

exports.hello = function(what) {
    console.log('Hello ' + what);
}

exports.hello = function(what, a, b) {
    console.log('Goodbye ' + what + ' ' + a + ' ' + b);
}
