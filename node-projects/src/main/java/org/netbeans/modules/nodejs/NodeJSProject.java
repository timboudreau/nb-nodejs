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

import org.netbeans.modules.nodejs.api.NodeJSPlatformProvider;
import org.netbeans.modules.nodejs.api.ProjectMetadata;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.api.validation.adapters.DialogBuilder;
import org.netbeans.api.validation.adapters.DialogBuilder.DialogType;
import org.netbeans.modules.nodejs.api.MainFileProvider;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import org.netbeans.modules.nodejs.libraries.LibrariesPanel;
import org.netbeans.modules.nodejs.node.NodeJSLogicalViewProvider;
import org.netbeans.modules.nodejs.platform.NodeJSPlatforms;
import org.netbeans.modules.nodejs.registry.FileChangeRegistry;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.CopyOperationImplementation;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.MoveOrRenameOperationImplementation;
import org.netbeans.spi.project.ProjectConfiguration;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.RecommendedTemplates;
import org.netbeans.spi.project.ui.support.DefaultProjectOperations;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.netbeans.validation.api.ui.swing.ValidationPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.explorer.ExplorerManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.CreateFromTemplateAttributesProvider;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author Tim Boudreau
 */
public class NodeJSProject implements Project, ProjectConfiguration, ActionProvider,
        Comparable<NodeJSProject>, ProjectInformation,
        PrivilegedTemplates, RecommendedTemplates, CreateFromTemplateAttributesProvider,
        PropertyChangeListener, MoveOrRenameOperationImplementation,
        DeleteOperationImplementation, CopyOperationImplementation, MainFileProvider {
    public static final String NBRUN = ".nbrun"; //NOI18N
    private final FileObject dir;
    private final ProjectState state;
    public static final String MAIN_FILE_COMMAND = "set_main_file"; //NOI18N
    public static final String PROPERTIES_COMMAND = "project_properties"; //NOI18N
    public static final String LIBRARIES_COMMAND = "libs"; //NOI18N
    public static final String CLOSE_COMMAND = "close"; //NOI18N
    static final Logger LOGGER = Logger.getLogger( NodeJSProject.class.getName() );
    private final ProjectMetadataImpl metadata = new ProjectMetadataImpl( this );
    private final NodeJsClassPathProvider classpath = new NodeJsClassPathProvider();
    private final PropertyChangeSupport supp = new PropertyChangeSupport( this );
    private final Sources sources = new NodeJSProjectSources( this );
    private final NodeJSLogicalViewProvider logicalView = new NodeJSLogicalViewProvider( this );
    private final FileChangeRegistry registry = new FileChangeRegistry( this );
    public static final RequestProcessor NODE_JS_PROJECT_THREAD_POOL = new RequestProcessor( "NodeJS", 3 ); //NOI18N
    private final Lookup lookup = Lookups.fixed( this, logicalView,
            new NodeJSProjectProperties( this ), classpath, sources,
            new NodeJsEncodingQuery(), registry, metadata,
            new PlatformProvider(), new LibrariesResolverImpl(),
            NODE_JS_PROJECT_THREAD_POOL );

    @SuppressWarnings ("LeakingThisInConstructor")
    NodeJSProject ( FileObject dir, ProjectState state ) {
        this.dir = dir;
        this.state = state;
        metadata.addPropertyChangeListener( this );
    }

    private class LibrariesResolverImpl implements LibrariesResolver, Runnable, FileChangeRegistry.FileObserver, PropertyChangeListener {
        private final ChangeSupport supp = new ChangeSupport( this );
        private final RequestProcessor.Task installTask = NODE_JS_PROJECT_THREAD_POOL.create( this );
        private final Checker checker = new Checker();
        private final RequestProcessor.Task checkTask = NODE_JS_PROJECT_THREAD_POOL.create( checker );
        private Boolean hasMissing;

        @Override
        public void install () {
            installTask.schedule( 100 );
        }

        void init () {
            FileChangeRegistry reg = getLookup().lookup( FileChangeRegistry.class );
            reg.registerInterest( NodeJSProjectFactory.NODE_MODULES_FOLDER, this );
            ProjectMetadata md = getLookup().lookup( ProjectMetadata.class );
            md.addPropertyChangeListener( WeakListeners.propertyChange( this, md ) );
        }

        private void setHasMissing ( boolean nue ) {
            Boolean old;
            synchronized ( this ) {
                old = hasMissing;
                hasMissing = nue;
            }
            if (old == null) {
                init();
            }
            if (old == null || !old.equals( nue )) {
                EventQueue.invokeLater( new Runnable() {

                    @Override
                    public void run () {
                        supp.fireChange();
                    }

                } );
            }
        }

        private synchronized Boolean hasMissing () {
            return hasMissing;
        }

        @Override
        public void onEvent ( FileChangeRegistry.EventType type, String path ) {
            checkTask.schedule( 2000 );
        }

        @Override
        public void propertyChange ( PropertyChangeEvent evt ) {
            if (null == evt.getPropertyName() || "dependencies".equals( evt.getPropertyName() )) {
                checkTask.schedule( 2000 );
            }
        }

        private class Checker implements Runnable {

            @Override
            public void run () {
                Map<String, Object> deps = metadata.getMap( "dependencies" ); //NOI18N
                if (deps == null || deps.isEmpty()) {
                    setHasMissing( false );
                    return;
                }
                FileObject node_modules = getProjectDirectory().getFileObject( NodeJSProjectFactory.NODE_MODULES_FOLDER );
                if (node_modules == null) {
                    setHasMissing( !deps.isEmpty() );
                    return;
                }
                Set<String> names = new HashSet<>();
                for (FileObject kid : node_modules.getChildren()) {
                    if (kid.isFolder()) {
                        names.add( kid.getNameExt() );
                    }
                }
                Set<String> declared = new HashSet<>( deps.keySet() );
                declared.removeAll( names );
                setHasMissing( !declared.isEmpty() );
            }
        }

        @Override
        public void addChangeListener ( ChangeListener cl ) {
            supp.addChangeListener( cl );
        }

        @Override
        public void removeChangeListener ( ChangeListener cl ) {
            supp.removeChangeListener( cl );
        }

        @Override
        public boolean hasMissingLibraries () {
            Boolean result = hasMissing();
            if (result == null) {
                checkTask.schedule( 10 );
            }
            return result == null ? false : result;
        }

        @Override
        public synchronized void run () {
            ProgressHandle handle = ProgressHandleFactory.createHandle(
                    NbBundle.getMessage( NodeJSProject.class, "RUNNING_NPM_INSTALL", getName() ) ); //NOI18N
            handle.start();
            try {
                String result = Npm.getDefault().run( FileUtil.toFile( getProjectDirectory() ), "install" ); //NOI18N
                InputOutput io = IOProvider.getDefault().getIO( getName() + " - npm install", true ); //NOI18N
                io.select();
                io.getOut().print( result );
                io.getOut().close();
                checkTask.schedule( 3000 );
            } finally {
                handle.finish();
            }
        }
    }

    @Override
    public FileObject getMainFile () {
        return getLookup().lookup( NodeJSProjectProperties.class ).getMainFile();
    }

    private final class PlatformProvider extends NodeJSPlatformProvider {

        @Override
        public NodeJSExecutable get () {
            NodeJSProjectProperties props = NodeJSProject.this.getLookup().lookup( NodeJSProjectProperties.class );
            String name = props.getPlatformName();
            NodeJSExecutable result = null;
            if (name != null) {
                result = NodeJSPlatforms.find( name, true );
            } else {
                result = NodeJSExecutable.getDefault();
            }
            return result;
        }
    }

    ProjectState state () {
        return state;
    }

    public ProjectMetadataImpl metadata () {
        return metadata;
    }

    public ProjectMetadata getMetadata () {
        return metadata;
    }

    @Override
    public FileObject getProjectDirectory () {
        return dir;
    }

    @Override
    public Lookup getLookup () {
        return lookup;
    }

    @Override
    public String getDisplayName () {
        String result = getLookup().lookup( NodeJSProjectProperties.class ).getDisplayName();
        if (result == null || "".equals( result )) {
            result = getProjectDirectory().getName();
        }
        return result;
    }

    @Override
    public String[] getSupportedActions () {
        return ALWAYS_ENABLED.toArray( new String[ALWAYS_ENABLED.size()] );
    }

    @Override
    public void invokeAction ( String string, Lookup lkp ) throws IllegalArgumentException {
        if (COMMAND_RUN.equals( string )) {
            NodeJSPlatformProvider platformP = getLookup().lookup( NodeJSPlatformProvider.class );
            final NodeJSExecutable exe = platformP.get();
            FileObject main = getLookup().lookup( NodeJSProjectProperties.class ).getMainFile();
            if (main == null) {
                main = showSelectMainFileDialog();
                if (main != null) {
                    NodeJSProjectProperties props = getLookup().lookup( NodeJSProjectProperties.class );
                    props.setMainFile( main );
                    StatusDisplayer.getDefault().setStatusText( NbBundle.getMessage( NodeJSProject.class,
                            "MSG_MAIN_FILE_SET", getName(), main.getName() ) );
                } else {
                    return;
                }
            }
            final FileObject toRun = main;
            if (toRun != null && toRun.isValid()) {
                final Runnable runIt = new Runnable() {
                    @Override
                    public void run () {
                        try {
                            exe.run( toRun, getLookup().lookup( NodeJSProjectProperties.class ).getRunArguments() );
                        } catch ( IOException ex ) {
                            throw new IllegalArgumentException( ex );
                        }
                    }
                };
                Object o = metadata.getMap().get( "dependencies" ); //NOI18N
                boolean needRunNPMInstall = false;
                if (o instanceof Map<?, ?>) {
                    FileObject nodeModules = getProjectDirectory().getFileObject( NodeJSProjectFactory.NODE_MODULES_FOLDER );
                    needRunNPMInstall = nodeModules == null;
                    if (needRunNPMInstall) {
                        String msg = NbBundle.getMessage( NodeJSProject.class, "DETAIL_RUN_NPM_INSTALL" ); //NOI18N
                        String title = NbBundle.getMessage( NodeJSProject.class, "TITLE_RUN_NPM_INSTALL" ); //NOI18N
                        NotifyDescriptor nd = new NotifyDescriptor.Confirmation( msg, title, NotifyDescriptor.YES_NO_CANCEL_OPTION );
                        Object dlgResult = DialogDisplayer.getDefault().notify( nd );
                        needRunNPMInstall = NotifyDescriptor.YES_OPTION.equals( dlgResult );
                        if (NotifyDescriptor.CANCEL_OPTION.equals( dlgResult )) {
                            return;
                        }
                    }
                }
                if (needRunNPMInstall) {
                    NODE_JS_PROJECT_THREAD_POOL.post( new Runnable() {
                        @Override
                        public void run () {
                            try {
                                Future<Integer> f = Npm.getDefault().runWithOutputWindow( FileUtil.toFile( getProjectDirectory() ), "install" ); //NOI18N
                                int exitCode = f.get();
                                if (exitCode == 0) {
                                    runIt.run();
                                } else {
                                    StatusDisplayer.getDefault().setStatusText( NbBundle.getMessage( NodeJSProject.class, "NPM_INSTALL_FAILED" ) ); //NOI18N
                                }
                            } catch ( IOException ex ) {
                                Exceptions.printStackTrace( ex );
                            } catch ( InterruptedException ex ) {
                                StatusDisplayer.getDefault().setStatusText( NbBundle.getMessage( NodeJSProject.class, "RUN_CANCELLED" ) ); //NOI18N
                                Logger.getLogger( NodeJSProject.class.getName() ).log( Level.INFO, "Interrupted running NPM", ex ); //NOI18N
                            } catch ( ExecutionException ex ) {
                                Exceptions.printStackTrace( ex );
                            }
                        }
                    } );
                } else {
                    NODE_JS_PROJECT_THREAD_POOL.post( runIt );
                }
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } else if (MAIN_FILE_COMMAND.equals( string )) {
            FileObject main = showSelectMainFileDialog();
            if (main != null) {
                NodeJSProjectProperties props = getLookup().lookup( NodeJSProjectProperties.class );
                props.setMainFile( main );
            }
        } else if (PROPERTIES_COMMAND.equals( string )) {
        } else if (CLOSE_COMMAND.equals( string )) {
            OpenProjects.getDefault().close( new Project[]{this} );
        } else if (LIBRARIES_COMMAND.equals( string )) {
            LibrariesPanel pn = new LibrariesPanel( this );
            DialogDescriptor dd = new DialogDescriptor( pn, NbBundle.getMessage( NodeJSProject.class, "SEARCH_FOR_LIBRARIES" ) ); //NOI18N
            DialogDisplayer.getDefault().notify( dd );
        } else if (COMMAND_DELETE.equals( string )) {
            DefaultProjectOperations.performDefaultDeleteOperation( this );
        } else if (COMMAND_MOVE.equals( string )) {
            DefaultProjectOperations.performDefaultMoveOperation( this );
        } else if (COMMAND_RENAME.equals( string )) {
            String label = NbBundle.getMessage( NodeJSProject.class, "LBL_PROJECT_RENAME" ); //NOI18N
            NotifyDescriptor.InputLine l = new NotifyDescriptor.InputLine( label, NbBundle.getMessage( NodeJSProject.class, "TTL_PROJECT_RENAME" ) ); //NOI18N
            if (DialogDisplayer.getDefault().notify( l ).equals( NotifyDescriptor.OK_OPTION )) {
                String txt = l.getInputText();
                Validator<String> v = ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING, StringValidators.REQUIRE_VALID_FILENAME );
                Problems p = new Problems();
                v.validate( p, label, txt );
                if (p.hasFatal()) {
                    NotifyDescriptor.Message msg = new NotifyDescriptor.Message( p.getLeadProblem().getMessage(), NotifyDescriptor.ERROR_MESSAGE );
                    DialogDisplayer.getDefault().notify( msg );
                    return;
                }
                DefaultProjectOperations.performDefaultRenameOperation( this, l.getInputText() );
            }
        } else if (COMMAND_COPY.equals( string )) {
            DefaultProjectOperations.performDefaultCopyOperation( this );
        } else {
            throw new AssertionError( string );
        }
    }
    private static final Set<String> ALWAYS_ENABLED = new HashSet<>( Arrays.asList(
            LIBRARIES_COMMAND, COMMAND_DELETE, COMMAND_MOVE, COMMAND_COPY,
            COMMAND_RENAME, MAIN_FILE_COMMAND, CLOSE_COMMAND, COMMAND_RUN ) );

    @Override
    public boolean isActionEnabled ( String string, Lookup lkp ) throws IllegalArgumentException {
        if (COMMAND_RUN.equals( string )) {
            return getLookup().lookup( NodeJSProjectProperties.class ).getMainFile() != null;
        }
        boolean result = ALWAYS_ENABLED.contains( string );
        return result;
    }

    @Override
    public int compareTo ( NodeJSProject t ) {
        int myPathLength = getProjectDirectory().getPath().length();
        int otherPathLength = t.getProjectDirectory().getPath().length();
        return myPathLength > otherPathLength ? -1 : myPathLength < otherPathLength ? 1 : 0;
    }

    @Override
    public void propertyChange ( PropertyChangeEvent evt ) {
        if (ProjectMetadata.PROP_NAME.equals( evt.getPropertyName() )) {
            Node n = logicalView.getView();
            if (n != null) {
                n.setDisplayName( evt.getNewValue() + "" );
            }
        }
    }

    private static final class AllJSFiles extends ChildFactory<FileObject> implements Comparator<FileObject> {
        private final FileObject root;

        public AllJSFiles ( FileObject root ) {
            this.root = root;
        }

        @Override
        protected boolean createKeys ( List<FileObject> toPopulate ) {
            createKeys( root, toPopulate );
            Collections.sort( toPopulate, this );
            return true;
        }

        private void createKeys ( FileObject fo, List<FileObject> toPopulate ) {
            if (fo == null || !fo.isValid()) {
                return;
            }
            for (FileObject file : fo.getChildren()) {
                if (file.getExt().equals( "js" ) && file.isData()) { //NOI18N
                    toPopulate.add( file );
                } else if (file.isFolder()) {
                    createKeys( file, toPopulate );
                }
            }
        }

        @Override
        protected Node createNodeForKey ( FileObject key ) {
            if (!key.isValid()) {
                return null;
            }
            try {
                DataObject dob = DataObject.find( key );
                return new JSFileFilterNode( dob.getNodeDelegate() );
            } catch ( DataObjectNotFoundException ex ) {
                Exceptions.printStackTrace( ex );
                return Node.EMPTY;
            }
        }

        @Override
        public int compare ( FileObject o1, FileObject o2 ) {
            if (!o1.isValid() || !o2.isValid()) {
                return 1;
            }
            if (o1.getParent() != o2.getParent()) {
                if (o1.getParent().equals( root )) {
                    return -1;
                } else if (o2.getParent().equals( root )) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                FileObject p1 = o1.getParent();
                FileObject p2 = o2.getParent();
                return p1.getName().compareToIgnoreCase( p2.getName() );
            }
        }

        private final class JSFileFilterNode extends FilterNode {
            public JSFileFilterNode ( Node original ) {
                super( original, Children.LEAF );
            }

            public Action[] getActions ( boolean ignored ) {
                return new Action[0];
            }

            private String getRelativePath () {
                FileObject fo = getLookup().lookup( DataObject.class ).getPrimaryFile();
                if (fo != null && fo.isValid() && !fo.getParent().equals( root )) {
                    String s = FileUtil.getRelativePath( root, fo );
                    int ix = s.lastIndexOf( '/' ); //NOI18N
                    if (ix > 0 && ix < s.length() - 1) {
                        s = s.substring( 0, ix );
                    }
                    return s;
                }
                return null;
            }

            @Override
            public Image getIcon ( int type ) {
                if (getRelativePath() != null) {
                    return ImageUtilities.createDisabledImage( super.getIcon( type ) );
                }
                return super.getIcon( type );
            }

            @Override
            public Image getOpenedIcon ( int type ) {
                return getIcon( type );
            }

            @Override
            public String getHtmlDisplayName () {
                String rp = getRelativePath();
                if (rp != null) {
                    return super.getDisplayName() + " <font color='!controlShadow'>(" + rp + ")"; //NOI18N
                }
                return super.getHtmlDisplayName();
            }
        }
    }

    FileObject showSelectMainFileDialog () {
        ExplorerPanel ep = new ExplorerPanel();
        final ExplorerManager mgr = ep.getExplorerManager();
        ChildFactory<?> kids = new AllJSFiles( getProjectDirectory() );
        Children ch = Children.create( kids, true );
        Node root = new AbstractNode( ch );
        mgr.setRootContext( root );
        final SwingValidationGroup grp = SwingValidationGroup.create();
        ValidationPanel pnl = new ValidationPanel( grp );
        pnl.setInnerComponent( ep );
        class X extends ValidationListener<Void> implements PropertyChangeListener {
            X () {
                super( Void.class, ValidationUI.NO_OP, null );
            }

            @Override
            public void propertyChange ( PropertyChangeEvent pce ) {
                grp.performValidation();
            }

            @Override
            protected void performValidation ( Problems prblms ) {
                Node[] selection = mgr.getSelectedNodes();
                if (selection != null && selection.length == 1) {
                    return;
                }
                prblms.add( new Problem( NbBundle.getMessage( NodeJSProject.class, "PROBLEM_NO_MAIN_FILE" ), Severity.FATAL ) ); //NOI18N
            }
        }
        X x = new X();
        mgr.addPropertyChangeListener( x );
        grp.addItem( x, true );
        DialogBuilder b = new DialogBuilder( NodeJSProject.class ).setModal( true ).setContent( ep ).setValidationGroup( grp ).setTitle( NbBundle.getMessage( NodeJSProject.class, "CHOOSE_NO_MAIN_FILE" ) ).setDialogType( DialogType.QUESTION );
        if (b.showDialog( NotifyDescriptor.OK_OPTION ) && mgr.getSelectedNodes().length == 1) {
            Node n = mgr.getSelectedNodes()[0];
            FileObject fo = n.getLookup().lookup( DataObject.class ).getPrimaryFile();
            return fo;
        }
        return null;
    }

    @Override
    public String[] getPrivilegedTemplates () {
        return new String[]{
            "Templates/javascript/Empty.js", //NOI18N
            "Templates/javascript/Module.js", //NOI18N
            "Templates/javascript/HelloWorld.js", //NOI18N
            "Templates/Other/javascript.js", //NOI18N
            "Templates/Other/file", //NOI18N
            "Templates/Web/Html.html", //NOI18N
            "Templates/Web/Xhtml.html", //NOI18N
            "Templates/Web/CascadingStyleSheet.css", //NOI18N
            "Templates/Other/json.json", //NOI18N
            "Templates/Other/Folder", //NOI18N
            "Templates/javscript/package.json" //NOI18N
        };
    }

    @Override
    public String[] getRecommendedTypes () {
        return new String[]{"javascript", "Other", "Web"}; //NOI18N
    }

    @Override
    public Map<String, ?> attributesFor ( DataObject template, DataFolder target, String name ) {
        String license = getLookup().lookup( NodeJSProjectProperties.class ).getLicenseType();
        Map<String, Object> result = new HashMap<String, Object>();
        if (license != null) {
            result.put( "project.license", license ); //NOI18N
            result.put( "license", license ); //NOI18N
        }
        result.put( "port", "8080" /* XXX GET RID OF THIS */ ); //NOI18N
        return result;
    }

    @Override
    public String getName () {
        return getProjectDirectory().getNameExt();
    }

    @Override
    public Icon getIcon () {
        return ImageUtilities.loadImageIcon( NodeJSProject.class.getPackage().getName().replace( '.', '/' ) + "/project.png", true );
    }

    @Override
    public Project getProject () {
        return this;
    }

    @Override
    public void addPropertyChangeListener ( PropertyChangeListener listener ) {
        supp.addPropertyChangeListener( listener );
    }

    @Override
    public void removePropertyChangeListener ( PropertyChangeListener listener ) {
        supp.removePropertyChangeListener( listener );
    }

    public NodeJSExecutable exe () {
        NodeJSPlatformProvider exe = getLookup().lookup( NodeJSPlatformProvider.class );
        return exe.get();
    }

    @Override
    public void notifyRenaming () throws IOException {
        exe().stopRunningProcesses( this );
    }

    @Override
    public void notifyRenamed ( String nueName ) throws IOException {
        //do nothing
    }

    @Override
    public void notifyMoving () throws IOException {
        exe().stopRunningProcesses( this );
    }

    @Override
    public void notifyMoved ( Project original, File originalPath, String nueName ) throws IOException {
        getLookup().lookup( NodeJSProjectProperties.class ).setDisplayName( nueName );
    }

    @Override
    public List<FileObject> getMetadataFiles () {
        List<FileObject> result = new ArrayList<>();
        FileObject projectProps = getProjectDirectory().getFileObject( NodeJSProjectFactory.PACKAGE_JSON );
        if (projectProps != null) {
            result.add( projectProps );
        }
        FileObject runProps = getProjectDirectory().getFileObject( NBRUN );
        if (runProps != null) {
            result.add( runProps );
        }
        return result;
    }

    @Override
    public List<FileObject> getDataFiles () {
        return Arrays.asList( getProjectDirectory().getChildren() );
    }

    @Override
    public void notifyDeleting () throws IOException {
        exe().stopRunningProcesses( this );
    }

    @Override
    public void notifyDeleted () throws IOException {
        //wipe project props?
    }

    @Override
    public void notifyCopying () throws IOException {
        //do nothing
    }

    @Override
    public void notifyCopied ( Project original, File originalPath, String nueName ) throws IOException {
        //do nothing
    }

    @Override
    public String toString () {
        FileObject fo = getProjectDirectory();
        if (fo == null) {
            return "Unintitialized project";
        }
        return fo.getName();
    }
}
