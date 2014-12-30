package org.netbeans.modules.nodejs.ui2;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.NodeJSProjectFactory;
import org.netbeans.spi.project.support.ProjectOperations;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tim Boudreau
 */
public class IgnoreProjectAction extends AbstractAction {
    private NodeJSProject prj;

    public IgnoreProjectAction ( NodeJSProject prj ) {
        this.prj = prj;
        putValue( NAME, NbBundle.getMessage( IgnoreProjectAction.class, "IGNORE_PROJECT" ) );
    }

    @Override
    public void actionPerformed ( ActionEvent ae ) {
        NodeJSProject prj;
        synchronized ( this ) {
            prj = this.prj;
        }
        if (prj == null) {
            return;
        }
        NodeJSProjectFactory factory = Lookup.getDefault().lookup( NodeJSProjectFactory.class );
        FileObject fo = prj.getProjectDirectory();
        if (fo.isValid() && factory != null) {
            synchronized ( this ) {
                this.prj = null;
            }
            factory.ignore( prj );
            OpenProjects.getDefault().close( new Project[]{prj} );
            try {
                ProjectOperations.notifyDeleted( prj );
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            } finally {
                RequestProcessor.Task task = NodeJSProject.NODE_JS_PROJECT_THREAD_POOL.create( new ProjectReopen( fo ) );
                task.schedule( 1500 );
            }
        }
    }

    static class ProjectReopen implements Runnable {
        private final FileObject fo;
        private volatile Project reopen;

        public ProjectReopen ( FileObject fo ) {
            this.fo = fo;
        }

        @Override
        public void run () {
            if (!EventQueue.isDispatchThread()) {
                if (!ProjectManager.mutex().isWriteAccess()) {
                    ProjectManager.mutex().writeAccess( this );
                } else {
                    try {
                        Project p = reopen = ProjectManager.getDefault().findProject( fo );
                        if (p != null) {
                            EventQueue.invokeLater( this );
                        }
                    } catch ( IOException ex ) {
                        Exceptions.printStackTrace( ex );
                    } catch ( IllegalArgumentException ex ) {
                        Exceptions.printStackTrace( ex );
                    }
                }
            } else {
                OpenProjects.getDefault().open( new Project[]{reopen}, false );
                if (reopen instanceof NodeJSProject) {
                    NotifyDescriptor des = new NotifyDescriptor.Confirmation( NbBundle.getMessage(
                            IgnoreProjectAction.class, "RESTART" ),
                            NbBundle.getMessage( IgnoreProjectAction.class, "TITLE_RESTART" ) );
                    if (NotifyDescriptor.OK_OPTION.equals( DialogDisplayer.getDefault().notify( des ) )) {
                        LifecycleManager.getDefault().markForRestart();
                        LifecycleManager.getDefault().exit();
                    }
                }
            }
        }
    }
}
