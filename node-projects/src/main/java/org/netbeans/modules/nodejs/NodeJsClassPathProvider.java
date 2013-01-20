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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 * Borrowed from javascript.editor
 *
 * @author vita, Tim Boudreau
 */
@ServiceProvider (service = ClassPathProvider.class)
public final class NodeJsClassPathProvider implements ClassPathProvider {
    public static final String BOOT_CP = "NodeJsBootClassPath"; //NOI18N
    private static FileObject jsStubsFO;
    private static FileObject nodeStubsFO;
    private static ClassPath bootClassPath;

    @Override
    public ClassPath findClassPath ( FileObject file, String type ) {
        if (!file.isValid()) {
            return null;
        }
        NodeJSProjectFactory pf = new NodeJSProjectFactory();
        if (type.equals( BOOT_CP ) || ClassPath.SOURCE.equals( type )) {
            try {
                if (pf.findOwner( file ) != null) {
                    return getBootClassPath();
                }
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            }
        }
        return null;
    }

    public static synchronized ClassPath getBootClassPath () {
        if (bootClassPath == null) {
            FileObject jsstubs = getJsStubs();
            if (jsstubs != null) {
                bootClassPath = ClassPathSupport.createClassPath( getJsStubs(), getNodeJsSources() );
            }
        }
        return bootClassPath;
    }

    private static FileObject getNodeJsSources () {
        if (nodeStubsFO == null) {
            String loc = DefaultExectable.get().getSourcesLocation();
            if (loc != null) {
                File dir = new File( loc );
                return nodeStubsFO = FileUtil.toFileObject( dir );
            }
        }
        return nodeStubsFO;

    }

    // TODO - add classpath recognizer for these ? No, don't need go to declaration inside these files...
    @SuppressWarnings ({"null", "ConstantConditions"})
    private static FileObject getJsStubs () {
        if (jsStubsFO == null) {
            // Core classes: Stubs generated for the "builtin" Ruby libraries.
            File allstubs = InstalledFileLocator.getDefault().locate( "jsstubs/allstubs.zip", "org.netbeans.modules.javascript.editing", false );
            if (allstubs == null) {
                // Probably inside unit test.
                try {
                    URI u = NodeJsClassPathProvider.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                    try {
                        File moduleJar = Utilities.toFile( u );
                        allstubs = new File( moduleJar.getParentFile().getParentFile(), "jsstubs/allstubs.zip" );
                        if (allstubs == null) {
                            return null;
                        }
                    } catch ( IllegalArgumentException e ) {
                        // Can happen with an unresolvable symlink in the project dir
                        Logger.getLogger( NodeJsClassPathProvider.class.getName() ).log( Level.FINE, "" + u, e );
                        return null;
                    }
                } catch ( URISyntaxException x ) {
                    assert false : x;
                    return null;
                }
            }
            assert allstubs.isFile() : allstubs;
            jsStubsFO = FileUtil.getArchiveRoot( FileUtil.toFileObject( allstubs ) );
        }
        return jsStubsFO;
    }
}
