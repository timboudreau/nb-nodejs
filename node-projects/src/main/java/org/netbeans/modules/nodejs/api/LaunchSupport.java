package org.netbeans.modules.nodejs.api;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public abstract class LaunchSupport {
    private final NodeJSExecutable exe;
    private final Set<Rerunner> runners = new WeakSet<>();

    public LaunchSupport ( NodeJSExecutable exe ) {
        this.exe = exe;
    }

    public Future<Integer> doRun ( final FileObject file, String args ) throws IOException {
        for (Rerunner r : runners) {
            if (file.equals( r.file )) {
                r.stopOldProcessIfRunning();
            }
        }
        File f = FileUtil.toFile( file );
        String[] cmdLineArgs = getLaunchCommandLine( true );
        if (cmdLineArgs == null) {
            StatusDisplayer.getDefault().setStatusText(
                    NbBundle.getMessage( DefaultExecutable.class, "NO_BINARY" ) );
            Toolkit.getDefaultToolkit().beep();
            return null;
        }
        ExternalProcessBuilder b = new ExternalProcessBuilder( cmdLineArgs[0] );
        for (int i = 0; i < cmdLineArgs.length; i++) {
            b.addArgument( cmdLineArgs[i] );
        }
        
        b.addArgument( f.getAbsolutePath() )
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
            ProjectInformation pi = p.getLookup().lookup( ProjectInformation.class );
            displayName = pi == null ? p.getProjectDirectory().getName() : pi.getDisplayName();
            MainFileProvider prov = p.getLookup().lookup( MainFileProvider.class );
            if (prov != null && !file.equals( prov.getMainFile() )) {
                displayName += "-" + file.getName();
            }
        }

        Rerunner rerunner = new Rerunner( exe, file, b, displayName );
        synchronized ( this ) {
            runners.add( rerunner );
        }
        return rerunner.launch();
    }

    protected abstract String[] getLaunchCommandLine ( boolean showDialog );

    public void stopRunningProcesses ( Lookup.Provider p ) {
        MainFileProvider mf = p.getLookup().lookup( MainFileProvider.class );
        // XXX this will leave behind running processes for invoking run from
        // the context menu of a non main js source - replace runners with a 
        // Map of ProjectPath:Reference<Rerunner> or something like that
        if (mf != null) {
            FileObject f = mf.getMainFile();
            if (f != null) {
                for (Rerunner r : runners) {
                    if (f.equals( r.file )) {
                        r.stopOldProcessIfRunning();
                    }
                }
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
            ExecutionDescriptor des = new ExecutionDescriptor().controllable( true )
                    .showSuspended( true ).frontWindow( true ).outLineBased( true )
                    .controllable( true ).errLineBased( true )
                    .errConvertorFactory( new LineConverter( exe ) )
                    .outLineBased( true )
                    .outConvertorFactory( new LineConverter( exe ) )
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
                StatusDisplayer.getDefault().setStatusText( 
                        NbBundle.getMessage( Rerunner.class, "STOPPING", file.getName() ) ); //NOI18N
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
}
