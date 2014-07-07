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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.netbeans.modules.nodejs.api.LaunchSupport;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class Npm {
    public static final String NPM_EXECUTABLE = "npm";
    public static final String COMMON_PATHS[] = new String[]{
        "/usr/bin/npm",
        "/usr/local/bin/npm",
        "/opt/bin/npm",
        "/opt/local/bin/npm",
        "C:" + File.separatorChar + "Program Files" + File.separatorChar + "nodejs" + File.separatorChar + "npm",
        "C:" + File.separatorChar + "Program Files" + File.separatorChar + "nodejs" + File.separatorChar + "npm.cmd",
        System.getProperty( "user.home" ) + File.separatorChar + "AppData" + File.separatorChar + "Roaming" + File.separatorChar + "npm",
        "C:" + File.separatorChar + "Users" + File.separatorChar + System.getProperty( "user.name" ) + File.separatorChar + "AppData" + File.separatorChar + "Roaming" + File.separatorChar + "npm"
    };
    private String npm;

    private static final Npm INSTANCE = new Npm();

    public static Npm getDefault () {
        return INSTANCE;
    }

    private Preferences prefs () {
        return NbPreferences.forModule( Npm.class );
    }

    private File findAny ( String... paths ) {
        for (String s : paths) {
            File f = new File( s );
            if (f.exists() && f.canExecute()) {
                return f;
            }
        }
        return null;
    }

    public String exe () {
        String result = exePath( true );
        if (result == null) {
            result = "npm";
        }
        return result;
    }

    public Future<Integer> runWithOutputWindow ( File workingDir, String... cmd ) throws IOException {
        LaunchSupport supp = new LaunchSupport( NodeJSExecutable.getDefault() ) {
            @Override
            protected String[] getLaunchCommandLine ( boolean showDialog ) {
                return new String[]{exe()};
            }
        };
        List<String> l = new LinkedList<>();
        l.add( exe() );
        l.addAll( Arrays.asList( cmd ) );
        return supp.runWithOutputWindow( l.toArray( new String[l.size()] ), FileUtil.toFileObject( FileUtil.normalizeFile( workingDir ) ), "");
    }

    public String run ( File workingDir, String... cmd ) {
        String npm = exe();
        if (npm != null && new File( npm ).exists()) {
            String[] args = new String[cmd.length + 1];
            args[0] = npm;
            System.arraycopy( cmd, 0, args, 1, cmd.length );
            return DefaultExecutable.runExternal( workingDir, args );
        }
        return null;
    }

    public void setExePath ( String location ) {
        if (location != null && !location.isEmpty() && !location.equals( this.npm )) {
            if (new File( location ).exists() && new File( location ).canExecute()) {
                this.npm = location;
                Preferences prefs = prefs();
                prefs.put( NPM_EXECUTABLE, npm );
                try {
                    prefs.flush();
                } catch ( BackingStoreException ex ) {
                    Exceptions.printStackTrace( ex );
                }
            }
        }
    }

    public String exePath ( boolean locate ) {
        if (npm != null) {
            return npm;
        }
        Preferences prefs = prefs();
        String result = prefs.get( NPM_EXECUTABLE, null );
        if (result == null || (!new File( result ).exists())) {
            File f = findAny( COMMON_PATHS );
            if (f == null) {
                if (!Utilities.isWindows()) {
                    String pth = DefaultExecutable.runExternal( "which", "npm" );
                    if (pth != null) {
                        f = new File( pth );
                        if (f.exists() && f.canExecute()) {
                            result = f.getAbsolutePath();
                        }
                    }
                }
            } else {
                result = f.getAbsolutePath();
            }
        }
        if (result == null && locate) {
            result = askUser( true );
        }
        if (result != null && new File( result ).exists()) {
            prefs.put( NPM_EXECUTABLE, result );
        }
        return npm = result;
    }

    public String askUser ( boolean initialDialog ) {
        String msg = NbBundle.getMessage( Npm.class, "MSG_FIND_NPM" );
        String title = NbBundle.getMessage( Npm.class, "TTL_FIND_NPM" );
        if (!initialDialog || DialogDescriptor.OK_OPTION.equals( DialogDisplayer.getDefault().notify( new NotifyDescriptor.Confirmation( msg, title ) ) )) {
            String approve = NbBundle.getMessage( Npm.class, "APPROVE_FIND_NPM" );

            File[] roots = File.listRoots();

            FileChooserBuilder bldr = new FileChooserBuilder( Npm.class ).setApproveText( approve ).setFilesOnly( true ).setFileFilter( new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept ( File f ) {
                    return f.isDirectory() || f.isFile() && f.canExecute();
                }

                @Override
                public String getDescription () {
                    return null;
                }
            } ).setTitle( title );
            if (roots.length > 0) {
                bldr.setDefaultWorkingDirectory( roots[0] );
            }

            File f = bldr.showOpenDialog();
            return f == null || !f.exists() ? null : f.getAbsolutePath();
        }
        return null;
    }
}
