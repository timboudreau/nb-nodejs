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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.queries.VisibilityQuery;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.NodeJSProjectFactory;
import org.netbeans.modules.nodejs.Npm;
import org.netbeans.modules.nodejs.json.ObjectMapperProvider;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbCollections;

/**
 *
 * @author Tim Boudreau
 */
public final class LibrariesChildFactory extends ChildFactory.Detachable<ProjectNodeKey> {
    private final A a = new A();
    private FileObject modulesFolder;
    private FileObject prjFolder;
    private final NodeJSProject project;
    public static final String[] BUILT_IN_NODE_LIBS = new String[]{"assert", "buffer",
        "buffer_ieee754", "child_process", "cluster", "console", "constants", "crypto",
        "dgram", "dns", "events", "freelist", "fs", "http", "https", "module",
        "net", "os", "path", "punycode", "querystring", "readline", "repl", "stream",
        "string_decoder", "sys", "timers", "tls", "tty",
        "url", "util", "vm", "zlib"};
    public static final Pattern CHECK_FOR_REQUIRE = Pattern.compile(
            "require\\s??\\(\\s??['\"](.*?)['\"]\\s??\\)", 40 );

    public LibrariesChildFactory ( NodeJSProject project ) {
        this.project = project;
    }

    public void update () {
        super.refresh( false );
    }

    @Override
    protected void addNotify () {
        FileObject lprjFolder = project.getProjectDirectory();
        FileObject lmodsFld = null;
        if (lprjFolder != null && lprjFolder.isValid()) {
            if (lprjFolder != null && lprjFolder.isValid()) {
                lprjFolder.addFileChangeListener( a );
                lmodsFld = lprjFolder.getFileObject( NodeJSProjectFactory.NODE_MODULES_FOLDER );
                if (lmodsFld != null && lmodsFld.isValid()) {
                    lmodsFld.addFileChangeListener( a );
                }
            }
        }
        synchronized ( this ) {
            this.modulesFolder = lmodsFld;
            this.prjFolder = lprjFolder;
        }
    }

    @Override
    protected void removeNotify () {
        FileObject lmodulesFolder;
        FileObject lprjFolder;
        synchronized ( this ) {
            lmodulesFolder = this.modulesFolder;
            lprjFolder = this.prjFolder;
        }
        if (lmodulesFolder != null) {
            lmodulesFolder.removeFileChangeListener( a );
        }
        if (lprjFolder != null) {
            lprjFolder.removeFileChangeListener( a );
        }
    }

    @Override
    public boolean createKeys ( List<ProjectNodeKey> toPopulate ) {
        toPopulate.addAll( libraries() );
        return true;
    }

    @Override
    protected Node createNodeForKey ( ProjectNodeKey key ) {
        return node( key, null );
    }

    public Node node ( final ProjectNodeKey key, CountDownLatch latch ) {
        if (key.getFld() != null && !key.getFld().isValid()) {
            return null;
        }
        switch ( key.getType() ) {
            case LIBRARY:
                return new LibraryFilterNode( key, latch );
            case BUILT_IN_LIBRARY:
                if (key.getFld() != null && key.getFld().isValid()) {
                return new LibraryFilterNode( key, latch );
            } else {
                AbstractNode li = new AbstractNode( Children.LEAF ) {
                    @Override
                    public String getHtmlDisplayName () {
                        return "<font color=\"#22AA22\">" + key; //NOI18N
                    }
                };
                li.setName( key.toString() );
                li.setDisplayName( key.toString() );
                li.setShortDescription( "Built-in library '" + key + "'" );
                li.setIconBaseWithExtension( ProjectNodeKey.LIBRARY_ICON ); //NOI18N
                return li;
            }
            case MISSING_LIBRARY:
                if (key.getFld() != null && key.getFld().isValid()) {
                return new LibraryFilterNode( key, latch );
            } else {
                AbstractNode an = new AbstractNode( Children.LEAF ) {
                    @Override
                    public String getHtmlDisplayName () {
                        return "<font color=\"#CC0000\">" + key; //NOI18N
                    }
                };
                an.setName( key.toString() );
                an.setDisplayName( key.toString() );
                StringBuilder sb = new StringBuilder( "<html>Missing library <b><i>" + key + "</i></b>" );
                if (key instanceof ProjectNodeKey.MissingLibrary && ((ProjectNodeKey.MissingLibrary) key).references != null && !((ProjectNodeKey.MissingLibrary) key).references.isEmpty()) {
                    sb.append( "<p>Referenced By<br><ul>" );
                    for (String path : ((ProjectNodeKey.MissingLibrary) key).references) {
                        sb.append( "<li>" ).append( path ).append( "</li>\n" );
                    }
                    sb.append( "</ul></pre></blockquote></html>" );
                }
                an.setShortDescription( sb.toString() );
                an.setIconBaseWithExtension( ProjectNodeKey.LIBRARY_ICON ); //NOI18N
                return an;
            }
            default:
                throw new AssertionError();
        }
    }

    static final Node nodeFromKey ( ProjectNodeKey key ) {
        try {
            if (key.getFld() != null && key.getFld().isValid()) {
                return DataObject.find( key.getFld() ).getNodeDelegate();
            }
        } catch ( DataObjectNotFoundException ex ) {
            Logger.getLogger( LibrariesChildFactory.class.getName() ).log(
                    Level.FINE, "File disappeared before node could be created for {0}", key );
        }
        return null;
    }

    class A extends FileChangeAdapter {
        @Override
        public void fileFolderCreated ( FileEvent fe ) {
            refresh( false );
        }

        @Override
        public void fileDataCreated ( FileEvent fe ) {
            refresh( false );
        }

        @Override
        public void fileChanged ( FileEvent fe ) {
            refresh( false );
        }

        @Override
        public void fileDeleted ( FileEvent fe ) {
            refresh( false );
        }
    }

    public List<ProjectNodeKey> libraries () {
        VisibilityQuery q = VisibilityQuery.getDefault();
        List<ProjectNodeKey> keys = new ArrayList<>();
        Map<String, List<FileObject>> otherLibs = findOtherModules( project.getProjectDirectory() );
        FileObject libFolder = project.getProjectDirectory().getFileObject( NodeJSProjectFactory.NODE_MODULES_FOLDER );
        if (libFolder != null) {
            Set<ProjectNodeKey> libFolders = new HashSet<>();
            Set<FileObject> childFolders = new LinkedHashSet<>();
            for (FileObject lib : libFolder.getChildren()) {
                File f = FileUtil.toFile( lib );
                try {
                    f = f.getCanonicalFile();
                    lib = FileUtil.toFileObject( FileUtil.normalizeFile( f ) );
                } catch ( IOException ex ) {
                    Logger.getLogger( LibrariesChildFactory.class.getName() ).log( Level.FINER,
                            "No canonical file for " + lib.getPath(), ex ); //NOI18N
                }
                childFolders.add( lib );
            }
            for (FileObject lib : childFolders) {
                boolean visible = q.isVisible( lib );
                if ((visible) && (!NodeJSProjectFactory.NODE_MODULES_FOLDER.equals( lib.getName() ))
                        && (!"nbproject".equals( lib.getName() )) && (lib.isFolder())) { //NOI18N
                    if (otherLibs.containsKey( lib.getName() )) {
                        otherLibs.remove( lib.getName() );
                    }
                    ProjectNodeKey key = new ProjectNodeKey( ProjectNodeKeyTypes.LIBRARY, lib );
                    key.direct = true;
                    keys.add( key );
                    recurseLibraries( lib, libFolders );
                }
            }
            keys.addAll( libFolders );
        }
        File home = new File(System.getProperty("user.home"));
        File userHomeModules = new File( home, NodeJSProjectFactory.NODE_MODULES_FOLDER ); //NOI18N
        userHomeModules = (userHomeModules.exists()) && (userHomeModules.isDirectory()) ? userHomeModules : null;
        if (userHomeModules == null) {
            String s = Npm.getDefault().run( home, "root");
            if (s != null) {
                userHomeModules = new File(s);
            }
        }
        File libModules = new File( "/usr/local/lib/node_modules" ); //NOI18N
        if (!libModules.exists()) {
            libModules = new File( "/usr/lib/node_modules" ); //NOI18N
        }
        if (!libModules.exists()) {
            libModules = new File( "/opt/lib/node_modules" ); //NOI18N
        }
        if (!libModules.exists()) {
            libModules = new File( "/opt/local/lib/node_modules" ); //NOI18N
        }
        if (!libModules.exists()) {
            String s = Npm.getDefault().run( new File( System.getProperty( "user.home" ) ), "root", "-g" );
            if (s != null) {
                libModules = new File( s );
            }
        }

        libModules = (libModules.exists()) && (libModules.isDirectory()) ? libModules : null;
        String src = project.exe().getSourcesLocation();
        File nodeSources = src == null ? null : new File( src );
        File libDir = nodeSources == null ? null : new File( nodeSources, "lib" );
        for (String lib : otherLibs.keySet()) {
            if ("./".equals( lib )) {
                continue;
            }
            if (userHomeModules != null) {
                File f = new File( userHomeModules, lib );
                if ((f.exists()) && (f.isDirectory())) {
                    ProjectNodeKey key = new ProjectNodeKey( ProjectNodeKeyTypes.LIBRARY, FileUtil.toFileObject( FileUtil.normalizeFile( f ) ) );
                    key.direct = true;
                    keys.add( key );
                    continue;
                }
            }
            if (libModules != null) {
                File f = new File( libModules, lib );
                if ((f.exists()) && (f.isDirectory())) {
                    ProjectNodeKey key = new ProjectNodeKey( ProjectNodeKeyTypes.LIBRARY, FileUtil.toFileObject( FileUtil.normalizeFile( f ) ) );
                    keys.add( key );
                    continue;
                }
            }
            if (libDir != null) {
                File f = new File( libDir, lib + ".js" ); //NOI18N
                if ((f.exists()) && (f.isFile()) && (f.canRead())) {
                    ProjectNodeKey key = new ProjectNodeKey( ProjectNodeKeyTypes.BUILT_IN_LIBRARY, FileUtil.toFileObject( FileUtil.normalizeFile( f ) ) );
                    keys.add( key );
                    continue;
                }
            }
            if (Arrays.binarySearch( BUILT_IN_NODE_LIBS, lib ) >= 0) {
                ProjectNodeKey key = new ProjectNodeKey.BuiltInLibrary( lib );
                keys.add( key );
                key.direct = true;
                continue;
            }
            if (lib.startsWith( "./" ) || lib.startsWith( "../" )) { //NOI18N
                //                FileObject fo = project.getProjectDirectory().getFileObject(lib + ".js");
                //                if (fo != null) {
                continue;
                //                }
            }
            ProjectNodeKey.MissingLibrary key = new ProjectNodeKey.MissingLibrary( lib );
            List<FileObject> referencedBy = otherLibs.get( lib );
            List<String> paths = new LinkedList<>();
            for (FileObject fo : referencedBy) {
                if (FileUtil.isParentOf( project.getProjectDirectory(), fo )) {
                    paths.add( FileUtil.getRelativePath( project.getProjectDirectory(), fo ) );
                } else {
                    paths.add( fo.getPath() );
                }
            }
            key.references = paths;
            keys.add( key );
        }
        outer:
        for (Iterator<ProjectNodeKey> it = keys.iterator(); it.hasNext();) {
            ProjectNodeKey k = it.next();
            if (k.getType() == ProjectNodeKeyTypes.MISSING_LIBRARY) {
                for (ProjectNodeKey k1 : new LinkedList<>( keys )) {
                    if (k == k1) {
                        continue;
                    }
                    if (k.toString().equals( k1.toString() ) && k1.getType() != ProjectNodeKeyTypes.MISSING_LIBRARY) {
                        it.remove();
                        break outer;
                    } else if (k.getType() == k1.getType() && k.toString().equals( k1.toString() )) {
                        it.remove();
                        break outer;
                    }
                    if (k.getFld() != null && k1.getFld() != null && k.getFld().equals( k1.getFld() )) {
                        it.remove();
                        break outer;
                    }
                }
            }
        }
        Collections.sort( keys );
        return keys;
    }

    private void recurseLibraries ( FileObject libFolder, Set<ProjectNodeKey> keys ) {
        FileObject libs = libFolder.getFileObject( NodeJSProjectFactory.NODE_MODULES_FOLDER );
        ObjectMapper mapper = ObjectMapperProvider.newObjectMapper();
        if (libs != null) {
            for (FileObject fo : libFolder.getChildren()) {
                for (FileObject lib : fo.getChildren()) {
                    if ((!NodeJSProjectFactory.NODE_MODULES_FOLDER.equals( lib.getName() )) && (!"nbproject".equals( lib.getName() )) && (lib.isFolder())) {
                        File f = FileUtil.toFile( lib );
                        if (f != null) {
                            try {
                                File canon = f.getCanonicalFile();
                                if (canon != null && !canon.equals( f )) {
                                    lib = FileUtil.toFileObject( canon );
                                }
                            } catch ( IOException ex ) {
                                Exceptions.printStackTrace( ex );
                            }
                        }
                        boolean jsFound = false;
                        FileObject pkgJson = lib.getFileObject( NodeJSProjectFactory.PACKAGE_JSON );
                        if (pkgJson != null && pkgJson.isValid()) {
                            File pkgFile = FileUtil.toFile( pkgJson );
                            if (pkgFile != null && pkgJson.isValid()) {
                                try {
                                    Map<String, Object> m = mapper.readValue( pkgFile, Map.class );
                                    Object mainO = m.get( "main" ); //NOI18N
                                    if (mainO instanceof String) {
                                        jsFound = lib.getFileObject( mainO.toString() ) != null;
                                    }
                                    if (!jsFound) {
                                        jsFound = lib.getFileObject( "index.js" ) != null; //NOI18N
                                    }
                                } catch ( FileNotFoundException ex ) {
                                    Logger.getLogger( LibrariesChildFactory.class.getName() ).log( Level.WARNING,
                                            "File disappeared: {0}", pkgFile.getPath() ); //NOI18N
                                } catch ( IOException ex ) {
                                    Exceptions.printStackTrace( ex );
                                }
                            }
                        } else {
                            for (FileObject kid : lib.getChildren()) {
                                if (!kid.isValid()) {
                                    continue;
                                }
                                jsFound = "js".equals( kid.getExt() );
                                if (jsFound) {
                                    break;
                                }
                            }
                        }
                        if (jsFound) {
                            ProjectNodeKey key = new ProjectNodeKey(
                                    ProjectNodeKeyTypes.LIBRARY, lib );
                            key.direct = false;
                            keys.add( key );
                            recurseLibraries( lib, keys );
                        }
                    }
                }
            }
        }
    }

    private Map<String, List<FileObject>> findOtherModules ( FileObject fld ) {
        Map<String, List<FileObject>> libs = new HashMap<>();
        assert (!EventQueue.isDispatchThread());
        for (FileObject fo : NbCollections.iterable( fld.getChildren( true ) )) {
            if (("js".equals( fo.getExt() )) && (fo.isData()) && (fo.canRead())) {
                checkForLibraries( fo, libs );
            }
        }
        return libs;
    }

    private void checkForLibraries ( FileObject jsFile, Map<String, List<FileObject>> all ) {
        try {
            String text = jsFile.asText();
            Matcher m = CHECK_FOR_REQUIRE.matcher( text );
            while ( m.find() ) {
                List<FileObject> l = all.get( m.group( 1 ) );
                if (l == null) {
                    l = new LinkedList<FileObject>();
                    all.put( m.group( 1 ), l );
                }
                l.add( jsFile );
            }
        } catch ( IOException ex ) {
            Logger.getLogger( LibrariesChildFactory.class.getName() ).log( Level.INFO, jsFile.getPath(), ex );
        }
    }
}
