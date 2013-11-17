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

import org.netbeans.modules.nodejs.api.LaunchSupport;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = NodeJSExecutable.class, position = Integer.MIN_VALUE)
public final class DefaultExecutable extends NodeJSExecutable {
    private static final String NODE_EXE_KEY = "nodejs_binary";
    private static final String PORT_KEY = "port";
    private static DefaultExecutable instance;

    @SuppressWarnings ("LeakingThisInConstructor")
    public DefaultExecutable () {
        assert instance == null;
        instance = this;
    }

    public static DefaultExecutable get () {
        if (instance == null) {
            NodeJSExecutable e = NodeJSExecutable.getDefault();
            if (e instanceof DefaultExecutable) {
                instance = (DefaultExecutable) e;
            } else {
                instance = new DefaultExecutable();
            }
        }
        return instance;
    }

    private Preferences preferences () {
        return NbPreferences.forModule( NodeJSExecutable.class );
    }

    private static final String[] COMMON_LOCATIONS = new String[]{
        "/usr/bin/node",
        "/usr/local/bin/node",
        "/opt/bin/node",
        "/opt/local/bin/node",
        "C:" + File.separatorChar + "Program Files" + File.separatorChar + "nodejs" + File.separatorChar + "node"
    };

    private String find ( String... opts ) {
        for (String s : opts) {
            File f = new File( s );
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    public String getNodeExecutable ( boolean showDialog ) {
        Preferences p = preferences();
        String loc = p.get( NODE_EXE_KEY, null );
        if (loc == null) {
            loc = find( COMMON_LOCATIONS );
        }
        if (loc == null) {
            loc = lookForNodeExecutable( showDialog );
        }
        return loc;
    }

    public int getDefaultPort () {
        return preferences().getInt( PORT_KEY, 9080 );
    }

    public void setDefaultPort ( int val ) {
        assert val > 0 && val < 65536;
        preferences().putInt( PORT_KEY, val );
    }

    public void setNodeExecutable ( String location ) {
        if (location != null && "".equals( location.trim() )) {
            location = null;
        }
        preferences().put( NODE_EXE_KEY, location );
    }

    public void stopRunningProcesses ( Lookup.Provider p ) {
        ls.stopRunningProcesses( p );
    }

    private final LaunchSupport ls = new LaunchSupport( this ) {
        @Override
        protected String[] getLaunchCommandLine ( boolean showDialog ) {
            return new String[]{DefaultExecutable.this.getNodeExecutable( showDialog )};
        }
    };

    @Override
    protected Future<Integer> doRun ( final FileObject file, String args ) throws IOException {
        return ls.doRun( file, args );
    }

    private String lookForNodeExecutable ( boolean showDialog ) {
        StatusDisplayer.getDefault().setStatusText( NbBundle.getMessage(
                DefaultExecutable.class, "LOOK_FOR_EXE" ) ); //NOI18N
        if (!Utilities.isWindows()) {
            String pathToBinary = runExternal( "which", "node" ); //NOI18N
            if (pathToBinary == null) {
                pathToBinary = runExternal( "which", "nodejs" ); //NOI18N
            }
            if (pathToBinary == null && showDialog) {
                pathToBinary = askUserForExecutableLocation();
            }
            if (pathToBinary != null) {
                preferences().put( NODE_EXE_KEY, pathToBinary );
            }
            return pathToBinary;
        } else {
            String pathToBinary = null;
            if (pathToBinary == null && showDialog) {
                pathToBinary = askUserForExecutableLocation();
            }
            if (pathToBinary != null) {
                preferences().put( NODE_EXE_KEY, pathToBinary );
            }
            return pathToBinary;
        }
    }

    static String runExternal ( String... cmdline ) {
        return runExternal( null, cmdline );
    }

    static String runExternal ( File dir, String... cmdline ) {
        ProcessBuilder b = new ProcessBuilder( cmdline );
        if (dir != null) {
            b.directory( dir );
        }
        try {
            Process p = b.start();
            try {
                try (InputStream in = p.getInputStream()) {
                    p.waitFor();
                    if (p.exitValue() == 0) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        FileUtil.copy( in, out );
                        String result = new String( out.toByteArray() ).trim(); //trim off \n
                        return result.length() == 0 ? null : result;
                    }
                }
            } catch ( InterruptedException ex ) {
                Exceptions.printStackTrace( ex );
                Thread.currentThread().interrupt(); //reset the flag
                return null;
            }
        } catch ( IOException ex ) {
            Exceptions.printStackTrace( ex );
            return null;
        }
        return null;
    }

    public String askUserForExecutableLocation () {
        File f = new FileChooserBuilder( DefaultExecutable.class ).setTitle( NbBundle.getMessage( DefaultExecutable.class, "LOCATE_EXECUTABLE" ) ).setFilesOnly( true ).setApproveText( NbBundle.getMessage( DefaultExecutable.class, "LOCATE_EXECUTABLE_APPROVE" ) ).showOpenDialog();
        return f == null ? null : f.getAbsolutePath();
    }

    public void setSourcesLocation ( String location ) {
        if (location != null && "".equals( location.trim() )) {
            location = null;
        }
        if (location != null) {
            Preferences p = preferences();
            p.put( "sources", location );
            try {
                p.flush();
            } catch ( BackingStoreException ex ) {
                Exceptions.printStackTrace( ex );
            }
        }
    }

    public String getSourcesLocation () {
        return preferences().get( "sources", null );
    }
}
