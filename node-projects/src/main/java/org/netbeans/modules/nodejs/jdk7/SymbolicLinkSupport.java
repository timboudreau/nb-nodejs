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
package org.netbeans.modules.nodejs.jdk7;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author tim
 */
public abstract class SymbolicLinkSupport {
    private static final SymbolicLinkSupport INSTANCE;

    static {
        SymbolicLinkSupport supp = Lookup.getDefault().lookup( SymbolicLinkSupport.class );
        if (supp == null) {
            supp = new DefaultImpl();
            try {
                Class<?> filesType = Class.forName( "java.nio.file.Files" );
                Class<?> pathType = Class.forName( "java.nio.file.Path" );
                Method isLink = filesType.getMethod( "isSymbolicLink", pathType );
                Method toPath = File.class.getMethod( "toPath" );
                Method readSymbolicLink = filesType.getMethod( "readSymbolicLink", pathType );
                Method pathToFileMethod = pathType.getMethod( "toFile" );
//                supp = new JDK7Impl(isLink, toPath, readSymbolicLink, pathToFileMethod);
                supp = new RealJDK7Impl(); //XXX for testing
            } catch ( Exception e ) {
                Logger.getLogger( SymbolicLinkSupport.class.getName() ).log(
                        Level.INFO, "Detecting if JDK 7 nio.file is present", e );
            }
        }
        INSTANCE = supp;
    }

    public static SymbolicLinkSupport getDefault () {
        return INSTANCE;
    }

    public boolean isSymlink ( FileObject fo ) {
        boolean result = fo != null;
        if (result) {
            File f = FileUtil.toFile( fo );
            result = f != null;
            if (result) {
                result = isSymlinkImpl( f );
            } else {
                result = false;
            }
        }
        return result;
    }

    public FileObject resolveSymlink ( FileObject fo ) {
        FileObject result = fo;
        File f = FileUtil.toFile( fo );
        if (f != null) {
            File old = f;
            f = resolveSymlinkImpl( f );
            if (old != f) {
                f = FileUtil.normalizeFile( f );
                result = FileUtil.toFileObject( f );
            }
        }
        return result;
    }

    protected abstract File resolveSymlinkImpl ( File file );

    protected abstract boolean isSymlinkImpl ( File f );

    private static final class RealJDK7Impl extends SymbolicLinkSupport {
        @Override
        protected File resolveSymlinkImpl ( final File file ) {
            if (file == null) {
                return null;
            }
            // See http://netbeans.org/bugzilla/show_bug.cgi?id=221419 for why
            // this nonsense is necessary
            return AccessController.doPrivileged( new PrivilegedAction<File>() {
                @Override
                public File run () {
                    Path p = file.toPath();
                    if (p != null) {
                        if (Files.isSymbolicLink( p )) {
                            try {
                                Path res = Files.readSymbolicLink( p );
                                if (res != null) {
                                    System.out.println( "Resolved " + res + " as target of " + file );
                                    File result = res.toFile();
                                    return result == null ? file : result;
                                }
                            } catch ( IOException ex ) {
                                Exceptions.printStackTrace( ex );
                            }
                        }
                    }
                    return file;
                }
            } );
        }

        @Override
        protected boolean isSymlinkImpl ( File file ) {
            if (file != null) {
                Path p = file.toPath();
                if (p != null) {
                    return Files.isSymbolicLink( p );
                }
            }
            return false;
        }
    }

    private static class ReflectionJDK7Impl extends SymbolicLinkSupport {
        private final Method isLink;
        private final Method toPath;
        private final Method readSymbolicLink;
        private final Method pathToFileMethod;

        private ReflectionJDK7Impl ( Method isLink, Method toPath, Method readSymbolicLink, Method pathToFileMethod ) {
            this.isLink = isLink;
            this.toPath = toPath;
            this.readSymbolicLink = readSymbolicLink;
            this.pathToFileMethod = pathToFileMethod;
        }

        @Override
        protected File resolveSymlinkImpl ( final File file ) {
            if (file == null) {
                return null;
            }
            // See http://netbeans.org/bugzilla/show_bug.cgi?id=221419 for why
            // this nonsense is necessary
            return AccessController.doPrivileged( new PrivilegedAction<File>() {
                @Override
                public File run () {
                    try {
                        Object path = toPath.invoke( null, file );
                        Boolean val = (Boolean) isLink.invoke( null, path );
                        boolean isAlink = val == null ? false : val.booleanValue();
                        if (isAlink) {
                            Object newPath = readSymbolicLink.invoke( null, path );
                            if (newPath != path) {
                                return (File) pathToFileMethod.invoke( newPath );
                            }
                        }
                    } catch ( IllegalAccessException ex ) {
                        Exceptions.printStackTrace( ex );
                    } catch ( IllegalArgumentException ex ) {
                        Exceptions.printStackTrace( ex );
                    } catch ( InvocationTargetException ex ) {
                        Throwable t = ex.getCause();
                        if (t != null && t.getClass().getSimpleName().equals( "NotLinkException" )) {
                            return file;
                        }
                        Exceptions.printStackTrace( ex );
                    }
                    return file;
                }
            } );
        }

        @Override
        protected boolean isSymlinkImpl ( File f ) {
            return false;
        }
    }

    private static class DefaultImpl extends SymbolicLinkSupport {
        @Override
        protected File resolveSymlinkImpl ( File file ) {
            return file;
        }

        @Override
        protected boolean isSymlinkImpl ( File f ) {
            return false;
        }
    }
}
