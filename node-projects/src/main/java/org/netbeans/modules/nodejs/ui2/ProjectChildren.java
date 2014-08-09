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
package org.netbeans.modules.nodejs.ui2;

import java.awt.Image;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.queries.VisibilityQuery;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.NodeJSProjectFactory;
import org.netbeans.modules.nodejs.api.NodeJSProjectChildNodeFactory;
import org.netbeans.modules.nodejs.node.AddLibraryAction;
import org.netbeans.modules.nodejs.node.LibrariesChildFactory;
import org.netbeans.modules.nodejs.node.NodeJSLogicalViewProvider;
import static org.netbeans.modules.nodejs.ui2.Key.IMPORTANT_FILES;
import static org.netbeans.modules.nodejs.ui2.Key.LIBRARIES;
import static org.netbeans.modules.nodejs.ui2.KeyType.SOURCES;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
final class ProjectChildren extends ChildFactory.Detachable<Key<?>> {
    private final NodeJSProject project;

    ProjectChildren ( NodeJSProject project ) {
        this.project = project;
    }
    private FileChangeAdapter fcl = new FileChangeAdapter() {
        @Override
        public void fileFolderCreated ( FileEvent fe ) {
            ProjectChildren.this.refresh( false );
        }

        @Override
        public void fileDataCreated ( FileEvent fe ) {
            ProjectChildren.this.refresh( false );
        }

        @Override
        public void fileDeleted ( FileEvent fe ) {
            ProjectChildren.this.refresh( false );
        }
    };

    @Override
    protected void addNotify () {
        project.getProjectDirectory().addFileChangeListener( fcl );
    }

    @Override
    protected void removeNotify () {
        project.getProjectDirectory().removeFileChangeListener( fcl );
    }

    @Override
    protected boolean createKeys ( List<Key<?>> toPopulate ) {
        VisibilityQuery vq = VisibilityQuery.getDefault();
        Set<FileObject> meta = new HashSet<FileObject>( project.getMetadataFiles() );
        for (FileObject fo : project.getDataFiles()) {
            if (NodeJSProjectFactory.NODE_MODULES_FOLDER.equals( fo.getName() ) && fo.isFolder()) {
                continue;
            }
            if (vq.isVisible( fo ) && !meta.contains( fo )) {
                toPopulate.add( new Key<FileObject>( fo ) );
            }
        }
        toPopulate.add( Key.IMPORTANT_FILES );
        toPopulate.add( Key.LIBRARIES );
        ChildNodeRegistry reg = Lookup.getDefault().lookup( ChildNodeRegistry.class );
        reg.populateKeys( project, toPopulate );
        Collections.sort( toPopulate );
        return true;
    }

    @Override
    protected Node createNodeForKey ( Key<?> key ) {
        ChildFactory<?> kids;
        if (key.type() instanceof KeyType) {
            KeyType kt = (KeyType) key.type();
            switch ( kt ) {
                case IMPORTANT_FILES:
                    kids = new ImportantFilesChildFactory( project );
                    break;
                case LIBRARIES:
                    kids = new LibrariesChildFactory( project );
                    break;
                case SOURCES:
                    try {
                        return createSourceNode( key );
                    } catch ( DataObjectNotFoundException ex ) {
                        Logger.getLogger( ProjectChildren.class.getName() ).log(
                                Level.FINE, "Data object disappeared", ex );
                        return Node.EMPTY;
                    }
                default:
                    throw new AssertionError( kt );
            }
        } else if (key.get() instanceof NodeJSProjectChildNodeFactory) {
            NodeJSProjectChildNodeFactory val = (NodeJSProjectChildNodeFactory) key.get();
            kids = val.createChildren( project );
        } else {
            throw new IllegalArgumentException( "Unknown key type " + key + " " + key.get().getClass().getName() ); //NOI18N
        }
        return new GenericNode( key, kids, project );
    }

    private Node createSourceNode ( Key<?> k ) throws DataObjectNotFoundException {
        FileObject of = (FileObject) k.get();
        if (of.isFolder() && of.getFileObject( NodeJSProjectFactory.PACKAGE_JSON ) != null) {
            try {
                Project proj = ProjectManager.getDefault().findProject( of );
                if (proj != null) {
                    LogicalViewProvider vp = proj.getLookup().lookup( LogicalViewProvider.class );
                    if (vp != null) {
                        return new FilterNode( vp.createLogicalView() );
                    }
                }
            } catch ( IOException ex ) {
                Logger.getLogger( ProjectChildren.class.getName() ).log(
                        Level.FINE, "Error opening project disappeared", ex );
            } catch ( IllegalArgumentException ex ) {
                Exceptions.printStackTrace( ex );
            }
        }
        DataObject dob = DataObject.find( of );
        return new FilterNode( dob.getNodeDelegate() );
    }

    private static final class GenericNode extends AbstractNode {
        private final Key key;

        public GenericNode ( Key key, ChildFactory<?> factory, NodeJSProject project ) {
            this( key, factory, project, new InstanceContent() );
        }

        public GenericNode ( Key key, ChildFactory<?> factory, NodeJSProject project, InstanceContent content ) {
            this( key, factory, project, content, new Mut( Lookups.fixed( project, key ), new AbstractLookup( content ) ) );
        }

        @SuppressWarnings ("LeakingThisInConstructor")
        public GenericNode ( Key key, ChildFactory<?> factory, NodeJSProject project, InstanceContent content, Mut lkp ) {
            super( Children.create( factory, true ), lkp );
            content.add( this );
            if (key.type() == KeyType.SOURCES) {
                try {
                    DataObject dob = DataObject.find( project.getProjectDirectory() );
                    lkp.add( dob.getNodeDelegate().getLookup() );
                } catch ( DataObjectNotFoundException ex ) {
                    Exceptions.printStackTrace( ex );
                }

            }
            this.key = key;
            setDisplayName( key.toString() );
        }

        @Override
        public Action[] getActions ( boolean ignored ) {
            DataObject dataObject = this.getLookup().lookup( DataObject.class );
            if (dataObject != null) {
                Node n = dataObject.getNodeDelegate();
                return n.getActions( ignored );
            }
            if (key.type() == KeyType.LIBRARIES) {
                NodeJSProject project = getLookup().lookup( NodeJSProject.class );
                assert project != null;
                NodeJSLogicalViewProvider l = project.getLookup().lookup( NodeJSLogicalViewProvider.class );
                return new Action[]{new AddLibraryAction( NbBundle.getBundle( GenericNode.class ), project, l.getView() )};
            } else if (key.get() instanceof NodeJSProjectChildNodeFactory) {
                List<Action> l = new LinkedList<>();
                NodeJSProjectChildNodeFactory f = (NodeJSProjectChildNodeFactory) key.get();
                f.getActions( getLookup().lookup( Project.class ), l );
                return l.toArray( new Action[l.size()] );
            }
            return new Action[0];
        }

        @Override
        public Action getPreferredAction () {
            DataObject dataObject = this.getLookup().lookup( DataObject.class );
            if (dataObject != null) {
                Node n = dataObject.getNodeDelegate();
                return n.getPreferredAction();
            }
            return super.getPreferredAction();
        }

        @Override
        public Image getIcon ( int type ) {
            return key.getIcon();
        }

        @Override
        public Image getOpenedIcon ( int type ) {
            return key.getIcon();
        }

        static class Mut extends ProxyLookup {
            public Mut ( Lookup... lookups ) {
                super( lookups );
            }

            public void set ( Lookup... lkps ) {
                super.setLookups( lkps );
            }

            public Lookup[] get () {
                return super.getLookups();
            }

            public void add ( Lookup lkp ) {
                Lookup[] old = get();
                Lookup[] nue = new Lookup[old.length + 1];
                System.arraycopy( old, 0, nue, 0, old.length );
                nue[old.length] = lkp;
                set( nue );
            }
        }
    }
}
