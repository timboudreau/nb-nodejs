<#assign licenseFirst = "/*">
<#assign licensePrefix = " * ">
<#assign licenseLast = " */">
<#include "../../Templates/Licenses/license-${project.license}.txt">

var http = require('http');
http.createServer(function (req, res) {
    res.writeHead(200, {
        'Content-Type': 'text/plain'
    });
    
    res.end('Hello World.\n');
    
}).listen(${port}, "");
