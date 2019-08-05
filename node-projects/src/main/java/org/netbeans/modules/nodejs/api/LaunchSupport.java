package org.netbeans.modules.nodejs.api;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.ExternalProcessBuilder;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.modules.nodejs.DefaultExecutable;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.spi.project.ui.support.BuildExecutionSupport;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * Makes it easy to wire up a node executable to the output window with stack
 * trace linking to project or nodejs sources.
 *
 * @author Tim Boudreau
 */
public abstract class LaunchSupport {
    private final NodeJSExecutable exe;
    private final Map<Project, Rerunner> rerunners = new HashMap<>();

    public LaunchSupport ( NodeJSExecutable exe ) {
        this.exe = exe;
    }

    public Future<Integer> doRun ( final FileObject file, String args ) throws IOException {
        Map<String, String> envToPopulate = new HashMap<>();
        populateEnv( envToPopulate, file, args );
        String[] cmdLineArgs = getLaunchCommandLine( true, envToPopulate );
        return runWithOutputWindow( cmdLineArgs, file, envToPopulate, args );
    }

    protected void populateEnv ( Map<String, String> env, FileObject toRun, String args ) {

    }

    public Future<Integer> runWithOutputWindow ( String[] cmdLineArgs, final FileObject file, Map<String, String> env, String args ) throws IOException {
        for (Rerunner r : rerunners.values()) {
            if (file.equals( r.file )) {
                r.stopOldProcessIfRunning();
            }
        }
        File f = FileUtil.toFile( file );
        if (cmdLineArgs == null) {
            StatusDisplayer.getDefault().setStatusText(
                    NbBundle.getMessage( DefaultExecutable.class, "NO_BINARY" ) ); //NOI18N
            Toolkit.getDefaultToolkit().beep();
            return null;
        }
        ExternalProcessBuilder b = new ExternalProcessBuilder( cmdLineArgs[0] );
        for (Map.Entry<String, String> e : env.entrySet()) {
            String value = e.getValue();
            String orig = System.getenv( e.getKey() );
            if (orig != null) {
                char separator = File.pathSeparatorChar;
                b = b.addEnvironmentVariable( e.getKey(), orig + separator + value );
            } else {
                b = b.addEnvironmentVariable( e.getKey(), value );
            }
        }
        for (int i = 1; i < cmdLineArgs.length; i++) {
            b = b.addArgument( cmdLineArgs[i] );
        }

        if (f.isFile()) {
            b = b.addArgument( f.getAbsolutePath() )
                    .workingDirectory( f.getParentFile() );
        } else {
            b = b.workingDirectory( f );
        }
        b = b.redirectErrorStream( true );

        if (args != null) {
            for (String arg : args.split( "\\s" )) { //NOI18N
                b = b.addArgument( arg );
            }
        }
        Project p = FileOwnerQuery.getOwner( file );
        String displayName = file.getName();
        if (p != null && p.getLookup().lookup( NodeJSProject.class ) != null) {
            ProjectInformation pi = p.getLookup().lookup( ProjectInformation.class );
            displayName = pi == null ? p.getProjectDirectory().getName() : pi.getDisplayName();
            MainFileProvider prov = p.getLookup().lookup( MainFileProvider.class );
            if (prov != null && !file.equals( prov.getMainFile() )) {
                displayName += "-" + file.getName(); //NOI18N
            }
        }

        Rerunner rerunner = new Rerunner( exe, file, b, displayName );
        synchronized ( this ) {
            rerunners.put( p, rerunner );
        }
        return rerunner.launch();
    }

    protected abstract String[] getLaunchCommandLine ( boolean showDialog, Map<String, String> env );

    public void stopRunningProcesses ( Lookup.Provider p ) {
        Project prj = p.getLookup().lookup( Project.class );
        if (prj != null) {
            Rerunner r = rerunners.get( prj );
            if (r != null) {
                r.stopOldProcessIfRunning();
                rerunners.remove( prj );
            }
        }
    }

    static class Rerunner implements ExecutionDescriptor.RerunCondition, Runnable, Callable<Process>, BuildExecutionSupport.Item {
        private final NodeJSExecutable exe;
        private final FileObject file;
        private volatile int prePost;
        private final ChangeSupport supp = new ChangeSupport( this );
        private final Callable<Process> processCreator;
        private final String displayName;

        public Rerunner ( NodeJSExecutable exe, FileObject file, ExternalProcessBuilder b, String displayName ) {
            this.exe = exe;
            this.file = file;
            this.processCreator = b;
            this.displayName = displayName;
        }

        public Future<Integer> launch () {
            ExecutionDescriptor.LineConvertorFactory converter = exe.newLineConverter();
            ExecutionDescriptor des = new ExecutionDescriptor().controllable( true )
                    .showSuspended( true ).frontWindow( true ).outLineBased( true )
                    .controllable( true ).errLineBased( true )
                    .errConvertorFactory( converter )
                    .outLineBased( true )
                    .outConvertorFactory( converter )
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

        public static final int PROCESS_QUIET_MILLISECONDS = 150;

        public void stopOldProcessIfRunning () {
            Process p;
            synchronized ( this ) {
                p = this.process;
            }
            if (p != null && (prePost % 2) != 0) {
                StatusDisplayer.getDefault().setStatusText(
                        NbBundle.getMessage( Rerunner.class, "STOPPING", file.getName() ) ); //NOI18N
                p.destroy();
                try {
                    p.waitFor();
                    //Give the OS a chance to release the socket
                    Thread.sleep( PROCESS_QUIET_MILLISECONDS );
                } catch ( InterruptedException ex ) {
                    Logger.getLogger( Rerunner.class.getName() ).log( Level.INFO,
                            "Exception in quiet period before rerun" ); //NOI18N
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
}
