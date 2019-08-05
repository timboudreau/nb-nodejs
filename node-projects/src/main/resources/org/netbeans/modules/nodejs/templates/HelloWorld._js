<#assign licenseFirst = "/*">
<#assign licensePrefix = " * ">
<#assign licenseLast = " */">
<#include "../../Templates/Licenses/license-${project.license}.txt">

const http = require('http');
http.createServer((req, res) => {
    res.writeHead(200, {
        'Content-Type': 'text/plain; charset=UTF-8'
    });
    
    res.end('Hello from ${projectName}.\n');
    
}).listen(${port}, "");
