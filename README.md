nb-nodejs
=========

NodeJS support for NetBeans, originally hosted on netbeans.org.

Provides a project type and support for running [NodeJS](nodejs.org) 
projects and files in the [NetBeans](http://netbeans.org) IDE.

Features
--------

   * A NodeJS project type which uses Node's built-in
     metadata: Any folder with a  ``package.json`` file is a project
   * Support for running projects or individual files in Node
   * Detects dependencies by scanning sources, understands and can update 
     ``package.json`` metadata
   * Support for adding libraries to a project using ``npm`` under the hood
   * Clickable stack traces in the output window
       * Ability to download and open NodeJS's sources as links in a stack trace
   * Allows the IDE to recognize scripts beginning with
       ``#!/usr/bin/env node``
     as a Javascript source

See [this blog](http://timboudreau.com/blog/read/NetBeans_Tools_for_Node_js)
for a broader description of the project.

License
-------
Sources are licensed under the simple MIT license, which amounts to
do what you want with it but give credit where credit is due.

The original sources are available in the history of the 
[netbeans.org contrib repository](http://hg.netbeans.org/main/contrib).
They were moved here because netbeans.org's process for approving using 
third-party libraries was slowing down development.  Such restrictions
exist for good reason, but hosting elsewhere and using a more broadly
compatible license was the most expedient solution.
