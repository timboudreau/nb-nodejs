/* Copyright (C) 2012 Tim Boudreau

 Permission is hereby granted, free of charge, to any person obtaining a copy 
 of this software and associated documentation files (the "Software"), to 
 deal in the Software without restriction, including without limitation the 
 rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 sell copies of the Software, and to permit persons to whom the Software is 
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package org.netbeans.modules.nodejs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.nodejs.api.NodeJSUtils;
import org.netbeans.modules.nodejs.api.Stubs;
import org.netbeans.modules.nodejs.node.LibrariesChildFactory;
import org.netbeans.modules.nodejs.node.ProjectNodeKey;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Utilities to allow external code completion and such to talk to this module
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = NodeJSUtils.class)
public class NodeJSUtilsImpl extends NodeJSUtils {
    private final NodeJSProjectFactory fac = Lookup.getDefault().lookup( NodeJSProjectFactory.class );

    @Override
    protected FileObject resolveImpl ( String name, FileObject relativeTo ) {
        try {
            NodeJSProject prj = fac.findOwner( relativeTo );
            if (name.startsWith( "." )) {
                if (name.startsWith( "./" )) {
                    name = name.substring( 2 );
                    if (name.isEmpty()) {
                        return null;
                    }
                    name += ".js";
                    System.out.println( "LOOK FOR " + name + " as local on " + relativeTo.getParent().getPath() );
                    FileObject parent = relativeTo.getParent();
                    return parent.getFileObject( name );
                } else if (name.startsWith( "../" )) {
                    FileObject parpar = relativeTo.getParent().getParent();
                    if (parpar != null) {
                        name = name.substring( 3 ) + ".js";
                        return parpar.getFileObject( name );
                    } else {
                        return null;
                    }
                }
            }
            if (prj != null) {
                LibrariesChildFactory f = new LibrariesChildFactory( prj );
                List<ProjectNodeKey> all = f.libraries();
                for (ProjectNodeKey key : all) {
                    if (name.equals( key.toString() )) {
                        if (key.isBuiltIn()) {
                            return findBuiltIn( name );
                        } else {
                            File libdir = key.toCanonoicalFile();
                            if (libdir != null) {
                                FileObject fo = FileUtil.toFileObject( libdir );
                                if (fac.isProject( fo )) {
                                    NodeJSProject libPrj = fac.find( fo );
                                    if (libPrj != null) {
                                        NodeJSProjectProperties props = libPrj.getLookup().lookup( NodeJSProjectProperties.class );
                                        if (props != null) {
                                            return props.getMainFile();
                                        }
                                    } else {
                                        FileObject theFile = FileUtil.toFileObject(libdir).getFileObject( name + ".js");
                                        return theFile;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // A standalone script - we can import relative paths or 
                // built in node modules only
                return findBuiltIn( name );
            }
            return null;
        } catch ( IOException ex ) {
            Logger.getLogger( NodeJSUtilsImpl.class.getName() ).log( Level.INFO, "Exception looking up project for " + relativeTo.getPath(), ex );
            return null;
        }
    }

    private FileObject findBuiltIn ( String name ) {
        String loc = DefaultExecutable.get().getSourcesLocation();
        if (loc != null && !loc.isEmpty() && new File( loc ).isDirectory()) {
            File sourcesRoot = new File( loc );
            FileObject src = FileUtil.toFileObject( sourcesRoot );
            if (src != null) {
                FileObject result = src.getFileObject( name + ".js" );
                if (result == null) {
                    result = src.getFileObject( "lib/" + name + ".js" );
                    return result;
                }
            }
        } else {
            Stubs stubs = Stubs.getDefault();
            if (stubs != null) {
                FileSystem fs = stubs.getStubs( null ); // PENDING GET NODE VERSION
                if (fs != null) {
                    return fs.getRoot().getFileObject( name + ".js" );
                }
            }
        }
        return null;
    }
}
