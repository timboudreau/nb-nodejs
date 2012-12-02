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
package org.netbeans.modules.nodejs.node;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.ExternalProcessBuilder;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.NodeJSProjectFactory;
import org.netbeans.modules.nodejs.ProjectMetadataImpl;
import org.netbeans.modules.nodejs.json.ObjectMapperProvider;
import org.netbeans.modules.nodejs.libraries.LibrariesPanel;
import org.netbeans.modules.nodejs.ui2.RootNode;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author tim
 */
public class AddLibraryAction extends AbstractAction {
    private final NodeJSProject project;
    private final RootNode root;

    public AddLibraryAction ( ResourceBundle bundle, NodeJSProject project, RootNode root ) {
        putValue( NAME, bundle.getString( "LBL_AddLibrary_Name" ) ); //NOI18N
        this.project = project;
        this.root = root;
    }

    @Override
    public void actionPerformed ( ActionEvent e ) {
        LibrariesPanel pn = new LibrariesPanel( project );
        DialogDescriptor dd = new DialogDescriptor( pn, NbBundle.getMessage( NodeJSProject.class, "SEARCH_FOR_LIBRARIES" ) ); //NOI18N
        if (DialogDisplayer.getDefault().notify( dd ).equals( DialogDescriptor.OK_OPTION )) {
            final Set<String> libraries = new HashSet<String>( pn.getLibraries() );
            if (libraries.size() > 0) {
                final AtomicInteger jobs = new AtomicInteger();
                final ProgressHandle h = ProgressHandleFactory.createHandle( NbBundle.getMessage( AddLibraryAction.class,
                        "MSG_RUNNING_NPM", libraries.size(), project.getDisplayName() ) ); //NOI18N
                RequestProcessor.getDefault().post( new Runnable() {
                    @Override
                    public void run () {
                        final int totalLibs = libraries.size();
                        try {
                            h.start( totalLibs * 2 );
                            for (String lib : libraries) {
                                int job = jobs.incrementAndGet();
                                ExternalProcessBuilder epb = new ExternalProcessBuilder( "npm" ) //NOI18N
                                        .addArgument( "install" ) //NOI18N
                                        .addArgument( lib )
                                        .workingDirectory( FileUtil.toFile( project.getProjectDirectory() ) );
                                final String libraryName = lib;
                                ExecutionDescriptor des = new ExecutionDescriptor().controllable( true ).showProgress( true ).showSuspended( true ).frontWindow( false ).controllable( true ).optionsPath( "Advanced/Node" ).postExecution( new Runnable() {
                                    @Override
                                    public void run () {
                                        try {
                                            int ct = jobs.decrementAndGet();
                                            if (ct == 0) {
                                                try {
                                                    project.getProjectDirectory().refresh();
                                                    FileObject fo = project.getProjectDirectory().getFileObject( NodeJSProjectFactory.NODE_MODULES_FOLDER );
                                                    if (fo != null && fo.isValid()) {
                                                        fo.refresh();
                                                    }
                                                    root.updateChildren();
                                                } finally {
                                                    h.finish();
                                                }
                                            } else {
                                                h.progress( NbBundle.getMessage( ProjectNodeKey.class,
                                                        "PROGRESS_LIBS_REMAINING", totalLibs - ct ), totalLibs - ct ); //NOI18N
                                                h.setDisplayName( libraryName );
                                            }
                                        } finally {
                                            List<LibraryAndVersion> l = libraries( project );
                                            updateDependencies( project, l, libraryName );
                                        }
                                    }
                                } ).charset( Charset.forName( "UTF-8" ) ).frontWindowOnError( true ); //NOI18N
                                ExecutionService service = ExecutionService.newService( epb, des, lib );
                                service.run();
                            }
                        } finally {
                            h.finish();
                        }
                    }
                } );
            }
        }
    }

    private synchronized List<LibraryAndVersion> updateDependencies ( NodeJSProject prj, List<LibraryAndVersion> onDisk, String added ) {
        System.out.println( "UPDATE DEPENDENCIES FOR " + added );
        List<LibraryAndVersion> l = new ArrayList<LibraryAndVersion>();
        ProjectMetadataImpl metadata = prj.metadata();
        if (metadata != null) {
            Map<String, Object> deps = metadata.getMap( "dependencies" );
            if (deps != null) {
                for (Map.Entry<String, Object> e : deps.entrySet()) {
                    if (e.getValue() instanceof String) {
                        String ver = (String) e.getValue();
                        LibraryAndVersion dep = new LibraryAndVersion( e.getKey(), ver );
                        l.add( dep );
                    }
                }
            }
            System.out.println( "Dependencies from project.json: " + l );
            System.out.println( "Dependencies from disk: " + onDisk );
            for (LibraryAndVersion v : onDisk) {
                if (!l.contains( v )) {
                    if (v.version != null) {
                        if (!v.version.startsWith( ">" ) && !v.version.startsWith( "=" ) && !v.version.startsWith( "<" ) && !v.version.startsWith( "~" )) {
                            v.version = ">=" + v.version;
                        }
                        l.add( v );
                    }
                }
            }
            Collections.sort( l );
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            for (LibraryAndVersion lib : l) {
                map.put( lib.name, lib.version );
            }
            Map m = metadata.getMap();
            m.put( "dependencies", map );
            try {
                // XXX figure out why queueSave isn't working
                String out = ObjectMapperProvider.newObjectMapper().writeValueAsString( m );
                FileObject fo = project.getProjectDirectory().getFileObject( NodeJSProjectFactory.PACKAGE_JSON );
                if (fo == null) {
                    fo = project.getProjectDirectory().createData( NodeJSProjectFactory.PACKAGE_JSON );
                }
                OutputStream o = fo.getOutputStream();
                try {
                    FileUtil.copy( new ByteArrayInputStream( out.getBytes( "UTF-8" ) ), o );;
                } finally {
                    o.close();
                }
                //            if (!l.equals(metadata.getMap("dependencies"))) {
                //                System.out.println("ADD MAP: " + map);
                //                metadata.addMap("dependencies", map);
                //                try {
                //                    metadata.save();
                //                    System.out.println("Saved metdata - dependencies: " + metadata.getMap("dependencies"));
                //                } catch (IOException ex) {
                //                    Logger.getLogger(AddLibraryAction.class.getName()).log(
                //                            Level.INFO, "Failed to save metadata", ex);
                //            }
                //            }
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            }
        } else {
            System.out.println( "project metadata was null" );
        }
        return l;
    }

    public class LibraryAndVersion implements Comparable<LibraryAndVersion> {
        public String name;
        public String version;

        public LibraryAndVersion () {
        }

        public LibraryAndVersion ( String name, String version ) {
            this.name = name;
            this.version = version;
        }

        @Override
        public int compareTo ( LibraryAndVersion o ) {
            return name.compareToIgnoreCase( o.name );
        }

        @Override
        public boolean equals ( Object o ) {
            return o instanceof LibraryAndVersion && name.equals( ((LibraryAndVersion) o).name );
        }

        @Override
        public int hashCode () {
            return name.hashCode();
        }

        public String toString () {
            return name + " " + version;
        }

        public void writeInto ( Map<String, Object> m ) {
            m.put( name, version );
        }
    }

    private List<LibraryAndVersion> libraries ( NodeJSProject prj ) {
        // This is really backwards, and we should have a model of libraries
        // represented by nodes, rather than a model of nodes from which we
        // derive libraries.  Ah, expediency.
        List<LibraryAndVersion> result = new ArrayList<LibraryAndVersion>();
        List<ProjectNodeKey> keys = new ArrayList<ProjectNodeKey>();
        LibrariesChildFactory f = new LibrariesChildFactory( prj );
        f.createKeys( keys );
        Map<LibraryFilterNode, CountDownLatch> latches = new LinkedHashMap<LibraryFilterNode, CountDownLatch>();
        for (ProjectNodeKey k : keys) {
            CountDownLatch latch = new CountDownLatch( 1 );
            Node n = f.node( k, latch );
            if (n instanceof LibraryFilterNode) {
                LibraryFilterNode ln = (LibraryFilterNode) n;
                if (ln.getKey().getType() == ProjectNodeKeyTypes.LIBRARY && ln.getKey().isDirect()) {
                    latches.put( ln, latch );
                }
            }
        }
        for (Map.Entry<LibraryFilterNode, CountDownLatch> e : latches.entrySet()) {
            try {
                e.getValue().await();
                String name = e.getKey().getName();
                String ver = e.getKey().getVersion();
                if (name != null && ver != null) {
                    result.add( new LibraryAndVersion( name, ver ) );
                }
            } catch ( InterruptedException ex ) {
                Logger.getLogger( AddLibraryAction.class.getName() ).log( Level.FINE, "Interrupted waiting", ex );
            }
        }
        return result;
    }
}
