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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.queries.VisibilityQuery;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.NodeJSProjectFactory;
import org.netbeans.modules.nodejs.registry.FileChangeRegistry;
import org.netbeans.modules.nodejs.registry.FileChangeRegistry.EventType;
import org.netbeans.modules.nodejs.registry.FileChangeRegistry.FileObserver;
import org.netbeans.modules.nodejs.ui2.SourcesChildFactory.Entry;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public class SourcesChildFactory extends ChildFactory.Detachable<Entry> implements FileObserver {
    private final NodeJSProject project;

    public SourcesChildFactory ( NodeJSProject project ) {
        this.project = project;
    }

    boolean skip ( FileObject fo ) {
        boolean result = !fo.isValid()
                || fo.getNameExt().equals( NodeJSProjectFactory.NODE_MODULES_FOLDER )
                || fo.getNameExt().equals( NodeJSProjectFactory.PACKAGE_JSON )
                || fo.getNameExt().equals( NodeJSProjectFactory.PACKAGE_LOCK_JSON )
                || fo.getNameExt().equals( ".gitignore" )
                || fo.getNameExt().equals( "README.md" )
                || fo.getNameExt().equals( NodeJSProjectFactory.NB_METADATA )
                || !VisibilityQuery.getDefault().isVisible( fo );
        return result;
    }

    @Override
    protected boolean createKeys ( List<Entry> toPopulate ) {
        FileObject prj = project.getProjectDirectory();
        if (prj.isValid()) {
            for (FileObject fo : project.getProjectDirectory().getChildren()) {
                if (!skip( fo )) {
                    toPopulate.add( new Entry( fo, project ) );
                }
            }
        }
        Collections.sort( toPopulate );
        return true;
    }

    @Override
    protected Node createNodeForKey ( Entry key ) {
        return key.node();
    }

    @Override
    public void onEvent ( EventType type, String path ) {
        if (type == EventType.NEW_CHILD) {
            refresh( true );
        }
    }
    volatile boolean active;
    boolean registered = false;

    @Override
    protected void addNotify () {
        active = true;
        if (!registered) {
            registered = true;
            FileChangeRegistry fileChangeRegistry = project.getLookup().lookup( FileChangeRegistry.class );
            System.out.println( "Register on " + fileChangeRegistry );
            if (fileChangeRegistry != null) {
                fileChangeRegistry.registerInterest( "", observer );
            }
        }
        registered = true;
        super.addNotify();
    }

    @Override
    protected void removeNotify () {
        super.removeNotify();
        active = false;
    }
    FileObserver observer = new FileObserver() {
        @Override
        public void onEvent ( EventType type, String path ) {
            System.out.println( "onEvent " + type + " " + path + " for " + project.getDisplayName() );
            switch ( type ) {
                case NEW_CHILD:
                    if (path != null && path.indexOf( NodeJSProjectFactory.NODE_MODULES_FOLDER ) < 0) {
                        refresh( false );
                    }
                    break;
                case DELETED:
                    refresh( false );
                    break;
                case CHANGE:
                    break;
                default:
                    throw new AssertionError( type );
            }
        }
    };

    final class Entry implements FileObserver, Comparable<Entry> {
        private final FileObject fo;
        private Node node;

        public Entry ( FileObject fo, NodeJSProject project ) {
            this.fo = fo;
            FileChangeRegistry reg = project.getLookup().lookup( FileChangeRegistry.class );
            reg.registerInterest( fo, this );
        }

        private Node node () {
            synchronized ( this ) {
                if (node != null) {
                    return node;
                }
            }
            try {
                DataObject dob = DataObject.find( fo );
                Node n = dob.getNodeDelegate();
                synchronized ( this ) {
                    return node = new FilterNode( n );
                }
            } catch ( DataObjectNotFoundException ex ) {
                Logger.getLogger( SourcesChildFactory.class.getName() ).log(
                        Level.INFO,
                        "File disappeared before node could be created: {0}", fo ); //NOI18N
            }
            return null;
        }

        @Override
        public void onEvent ( EventType type, String path ) {
            System.out.println( "onEvent " + type + " " + path + " for " + fo.getNameExt() );
            if (type == EventType.DELETED && path == null) {
                Node n;
                synchronized ( this ) {
                    n = this.node;
                }
                if (n != null) {
                    try {
                        n.destroy();
                    } catch ( IOException ex ) {
                        Exceptions.printStackTrace( ex );
                    }
                }
            }
        }

        private boolean isJsOrJson ( FileObject a ) {
            boolean aIsJsOrJson = a.getExt().toLowerCase().equals( "js" ) || a.getExt().toLowerCase().equals( "json" ); //NOI18N
            if (!aIsJsOrJson) {
                aIsJsOrJson = a.getMIMEType().equals( "text/javascript" ) || a.getMIMEType().equals( "application/json" ); //NOI18N
            }
            return aIsJsOrJson;
        }

        @Override
        public int compareTo ( Entry o ) {
            FileObject a = this.fo;
            FileObject b = o.fo;
            boolean aIsFolder = a.isFolder();
            boolean bIsFolder = b.isFolder();
            boolean aIsJsOrJson = isJsOrJson( a );
            boolean bIsJsOrJson = isJsOrJson( b );
            if (aIsFolder == bIsFolder) {
                if (aIsJsOrJson == bIsJsOrJson) {
                    return a.getNameExt().compareToIgnoreCase( b.getNameExt() );
                } else {
                    if (aIsJsOrJson) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            } else {
                return aIsFolder ? 1 : -1;
            }
        }
    }
}
