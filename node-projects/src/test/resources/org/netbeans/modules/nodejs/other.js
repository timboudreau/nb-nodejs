var util = require('util');

function hey(foo) {
    console.log(util.inspect(foo));
}

exports.hey = hey;
