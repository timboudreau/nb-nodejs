var util = require('util'), fs = require('fs');

var args = process.argv.slice(2);
if (args.length === 0) {
    console.log('First argument should be output file.');
    process.exit(1);
}
var outfile = args[0];

var data = {
	arch : process.arch,
	version: process.version,
	versions: process.versions,
	platform: process.platform,
	features: process.features
};

console.log(util.inspect(data, null, 100));

fs.writeFile(outfile, JSON.stringify(data), { encoding : 'utf8'}, function(err) {
    if (err) throw err;
});
