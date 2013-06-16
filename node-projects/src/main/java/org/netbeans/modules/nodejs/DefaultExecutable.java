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

import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeListener;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.ExternalProcessBuilder;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.BuildExecutionSupport;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.util.WeakSet;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = NodeJSExecutable.class)
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
    
    private static final String[] COMMON_LOCATIONS = new String[] {
            "/usr/bin/node",
            "/usr/local/bin/node",
            "/opt/bin/node",
            "/opt/local/bin/node",
    };
    
    private String find(String... opts) {
        for (String s : opts) {
            File f = new File(s);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    @Override
    public String getNodeExecutable ( boolean showDialog ) {
        Preferences p = preferences();
        String loc = p.get( NODE_EXE_KEY, null );
        if (loc == null) {
            loc = find(COMMON_LOCATIONS);
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

    @Override
    public void setNodeExecutable ( String location ) {
        if (location != null && "".equals( location.trim() )) {
            location = null;
        }
        preferences().put( NODE_EXE_KEY, location );
    }

    public void stopRunningProcesses ( NodeJSProject p ) {
        FileObject f = p.getLookup().lookup( NodeJSProjectProperties.class ).getMainFile();
        if (f != null) {
            for (Rerunner r : runners) {
                if (f.equals( r.file )) {
                    r.stopOldProcessIfRunning();
                }
            }
        }
    }

    @Override
    protected Future<Integer> doRun ( final FileObject file, String args ) throws IOException {
        for (Rerunner r : runners) {
            if (file.equals( r.file )) {
                r.stopOldProcessIfRunning();
            }
        }
        File f = FileUtil.toFile( file );
        String executable = getNodeExecutable( true );
        if (executable == null) {
            StatusDisplayer.getDefault().setStatusText(
                    NbBundle.getMessage( DefaultExecutable.class, "NO_BINARY" ) );
            Toolkit.getDefaultToolkit().beep();
            return null;
        }
        ExternalProcessBuilder b = new ExternalProcessBuilder( executable )
                .addArgument( f.getAbsolutePath() )
                .workingDirectory( f.getParentFile() )
                .redirectErrorStream( true );

        if (args != null) {
            for (String arg : args.split( " " )) {
                b = b.addArgument( arg );
            }
        }
        Project p = FileOwnerQuery.getOwner( file );
        String displayName = file.getName();
        if (p != null && p.getLookup().lookup( NodeJSProject.class ) != null) {
            NodeJSProject info = p.getLookup().lookup( NodeJSProject.class );
            displayName = info.getDisplayName();
            if (!file.equals( info.getLookup().lookup( NodeJSProjectProperties.class ).getMainFile() )) {
                displayName += "-" + file.getName();
            }
        }

        Rerunner rerunner = new Rerunner( file, b, displayName );
        synchronized ( this ) {
            runners.add( rerunner );
        }
        return rerunner.launch();
    }
    private Set<Rerunner> runners = new WeakSet<Rerunner>();

    static class Rerunner implements ExecutionDescriptor.RerunCondition, Runnable, Callable<Process>, BuildExecutionSupport.Item {
        private final FileObject file;
        private volatile int prePost;
        private final ChangeSupport supp = new ChangeSupport( this );
        private final Callable<Process> processCreator;
        private final String displayName;

        public Rerunner ( FileObject file, ExternalProcessBuilder b, String displayName ) {
            this.file = file;
            this.processCreator = b;
            this.displayName = displayName;
        }

        public Future<Integer> launch () {
            ExecutionDescriptor des = new ExecutionDescriptor().controllable( true )
                    .showSuspended( true ).frontWindow( true ).outLineBased( true )
                    .controllable( true ).errLineBased( true )
                    .errConvertorFactory( new LineConverter() )
                    .outLineBased( true )
                    .outConvertorFactory( new LineConverter() )
                    .rerunCondition( this )
                    .preExecution( this )
                    .postExecution( this )
                    .optionsPath( "Advanced/Node" ); //NOI18N
            ExecutionService service = ExecutionService.newService( this, des,
                    displayName );
            return service.run();
        }

        @Override
        public void addChangeListener ( ChangeListener listener ) {
            supp.addChangeListener( listener );
        }

        @Override
        public void removeChangeListener ( ChangeListener listener ) {
            supp.removeChangeListener( listener );
        }

        @Override
        public boolean isRerunPossible () {
            return file.isValid();
        }

        @Override
        public synchronized void run () {
            boolean isPre = (prePost++ % 2) == 0;
            if (isPre) {
                supp.fireChange();
            }
            if (!isPre) {
                BuildExecutionSupport.registerFinishedItem( this );
            }
        }

        public void stopOldProcessIfRunning () {
            Process p;
            synchronized ( this ) {
                p = this.process;
            }
            if (p != null && (prePost % 2) != 0) {
                StatusDisplayer.getDefault().setStatusText( NbBundle.getMessage( Rerunner.class, "STOPPING", file.getName() ) );
                p.destroy();
                try {
                    p.waitFor();
                    //Give the OS a chance to release the socket
                    Thread.sleep( 300 );
                } catch ( InterruptedException ex ) {
                    Exceptions.printStackTrace( ex );
                }
            }
        }
        Process process;

        @Override
        public Process call () throws Exception {
            Process result = processCreator.call();
            synchronized ( this ) {
                process = result;
            }
            BuildExecutionSupport.registerRunningItem( this );
            return result;
        }

        @Override
        public String getDisplayName () {
            return displayName;
        }

        @Override
        public void repeatExecution () {
        }

        @Override
        public boolean isRunning () {
            Process p;
            synchronized ( this ) {
                p = this.process;
            }
            boolean running = p != null;
            if (running) {
                try {
                    p.exitValue();
                    running = false;
                } catch ( IllegalThreadStateException e ) {
                    // Only way to test if a process is running is to
                    // try to get its exit code and see if it throws an
                    // ITSE
                }
            }
            return running;
        }

        @Override
        public void stopRunning () {
            stopOldProcessIfRunning();
        }

        @Override
        public int hashCode () {
            return file.hashCode();
        }

        @Override
        public boolean equals ( Object o ) {
            return o instanceof Rerunner && ((Rerunner) o).file.equals( file );
        }

        @Override
        public String toString () {
            return file.getPath();
        }
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
        return runExternal(null, cmdline);
    }
    static String runExternal ( File dir, String... cmdline ) {
        ProcessBuilder b = new ProcessBuilder( cmdline );
        if (dir != null) {
            b.directory( dir );
        }
        try {
            Process p = b.start();
            try {
                InputStream in = p.getInputStream();
                try {
                    p.waitFor();
                    if (p.exitValue() == 0) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        FileUtil.copy( in, out );
                        String result = new String( out.toByteArray() ).trim(); //trim off \n
                        return result.length() == 0 ? null : result;
                    }
                } finally {
                    in.close();
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
