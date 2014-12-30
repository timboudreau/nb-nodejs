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
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import org.openide.util.WeakSet;
import org.openide.util.lookup.ServiceProvider;
import org.netbeans.api.project.ProjectManager.Result;
import org.netbeans.spi.project.ProjectFactory2;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = ProjectFactory.class, position = -2147483648)
public class NodeJSProjectFactory implements ProjectFactory2 {
    public static final String PACKAGE_JSON = "package.json"; //NOI18N
    public static final String NODE_MODULES_FOLDER = "node_modules"; //NOI18N
    public static final String NB_METADATA = ".nbrun"; //NOI18N
    public static final String DOT_NPMIGNORE = ".npmignore"; //NOI18N
    private final Set<NodeJSProject> cache = new WeakSet<NodeJSProject>();
    private static final String PREFS_KEY_IGNORED_PROJECTS = "ignore";
    private final RequestProcessor.Task task;
    private static final int CACHE_CLEAN_DELAY = 1000 * 60;

    private Set<String> cachedIgnoredPaths;

    public NodeJSProjectFactory () {
        task = NodeJSProject.NODE_JS_PROJECT_THREAD_POOL.create( new CacheCleaner( this ) );
    }

    static final class CacheCleaner implements Runnable {
        private final WeakReference<NodeJSProjectFactory> factory;

        CacheCleaner ( NodeJSProjectFactory factory ) {
            this.factory = new WeakReference<>( factory );
        }

        @Override
        public void run () {
            NodeJSProjectFactory f = factory.get();
            if (f != null) {
                f.clearCaches();
            }
        }
    }

    synchronized void clearCaches () {
        cachedIgnoredPaths = null;
    }

    private Set<String> ignoredPaths () {
        Set<String> result = null;
        synchronized ( this ) {
            result = cachedIgnoredPaths;
        }
        if (result == null) {
            Preferences prefs = NbPreferences.forModule( NodeJSProjectFactory.class );
            String[] paths = prefs.get( PREFS_KEY_IGNORED_PROJECTS, "" ).split( "," );
            synchronized ( this ) {
                result = cachedIgnoredPaths = new HashSet<>( Arrays.asList( paths ) );
            }
            task.schedule( CACHE_CLEAN_DELAY );
        }
        return result;
    }

    public void ignore ( NodeJSProject project ) {
        ignore( project.getProjectDirectory() );
    }

    private boolean isIgnored ( FileObject prj ) {
        File f = FileUtil.toFile( prj );
        if (f != null && ignoredPaths().contains( f.getAbsolutePath() )) {
            return true;
        }
        return false;
    }

    private void ignore ( FileObject fo ) {
        File f = FileUtil.toFile( fo );
        if (f != null) {
            String path = f.getAbsolutePath();
            Set<String> all = new HashSet<>( ignoredPaths() );
            if (!all.contains( path )) {
                all.add( path );
                StringBuilder sb = new StringBuilder();
                for (Iterator<String> iter = all.iterator(); iter.hasNext();) {
                    sb.append( iter.next() );
                    if (iter.hasNext()) {
                        sb.append( ',' );
                    }
                }
                Preferences prefs = NbPreferences.forModule( NodeJSProjectFactory.class );
                prefs.put( PREFS_KEY_IGNORED_PROJECTS, sb.toString() );
                Set<NodeJSProject> cached = new HashSet<>();
                synchronized ( this ) {
                    cached.addAll( cache );
                    cachedIgnoredPaths = all;
                }
                for (NodeJSProject p : cached) {
                    if (p != null && fo.equals( p.getProjectDirectory() )) {
                        cache.remove( p );
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isProject ( FileObject fo ) {
        return isIgnored( fo ) ? false : fo.getFileObject( PACKAGE_JSON ) != null;
    }

    private FileObject resolve ( FileObject fo ) {
        if (fo == null) {
            return null;
        }
        File f = FileUtil.toFile( fo );
        if (f != null) {
            if (ignoredPaths().contains( f.getAbsolutePath() )) {
                return null;
            }
            try {
                f = f.getCanonicalFile();
                if (f != null) {
                    FileObject res = FileUtil.toFileObject( FileUtil.normalizeFile( f ) );
                    return res;
                }
            } catch ( IOException ioe ) {
                Logger.getLogger( NodeJSProjectFactory.class.getName() ).log( Level.INFO, null, ioe );
            }
        }
        return fo;
    }

    NodeJSProject findOwner ( FileObject fo ) throws IOException {
        List<NodeJSProject> l;
        synchronized ( this ) {
            l = new ArrayList<>( cache );
        }
        //Sort by longest-path first, so the deepest directory which is 
        //a project gets the first chance to claim it in the case of nested
        //projects
        Collections.sort( l );
        for (NodeJSProject cached : l) {
            if (FileUtil.isParentOf( cached.getProjectDirectory(), fo )) {
                return cached;
            }
        }
        FileObject projectDir = fo;
        while ( projectDir != null && (!projectDir.isFolder() || projectDir.getFileObject( PACKAGE_JSON ) == null) ) {
            projectDir = projectDir.getParent();
        }
        projectDir = resolve( projectDir );
        if (projectDir != null && projectDir.getFileObject( PACKAGE_JSON ) != null) {
            Project p = ProjectManager.getDefault().findProject( projectDir );
            if (p != null) {
                return p.getLookup().lookup( NodeJSProject.class );
            }
        }
        return null;
    }

    NodeJSProject find ( FileObject fo ) {
        fo = resolve( fo );
        Iterator<NodeJSProject> i;
        synchronized ( this ) {
            i = new HashSet<>( cache ).iterator();
        }
        while ( i.hasNext() ) {
            NodeJSProject prj = i.next();
            if (fo.equals( prj.getProjectDirectory() )) {
                return prj;
            }
        }
        return null;
    }

    @Override
    public Project loadProject ( FileObject fo, ProjectState ps ) throws IOException {
        if (!isProject( fo ) || isIgnored( fo )) {
            return null;
        }
        Iterator<NodeJSProject> i;
        synchronized ( this ) {
            i = new HashSet<>( cache ).iterator();
        }
        fo = resolve( fo );
        if (fo == null) {
            // load in progress?
            return null;
        }
        NodeJSProject result = null;
        while ( i.hasNext() ) {
            NodeJSProject p = i.next();
            if (fo.equals( p.getProjectDirectory() )) {
                result = p;
            }
        }
        if (result == null) {
            result = new NodeJSProject( fo, ps );
            synchronized ( this ) {
                cache.add( result );
            }
        }
        return result;
    }

    @Override
    public void saveProject ( Project prjct ) throws IOException, ClassCastException {
        NodeJSProject project = prjct.getLookup().lookup( NodeJSProject.class );
        if (project != null) {
            project.metadata().save();
        }
    }

    @Override
    public Result isProject2 ( FileObject fo ) {
        if (isProject( fo ) && !isIgnored( fo )) {
            return new ProjectManager.Result( ImageUtilities.loadImageIcon(
                    "org/netbeans/modules/nodejs/resources/logo.png", false ) ); //NOI18N
        }
        return null;
    }

    void register ( NodeJSProject prj ) { //for tests
        cache.add( prj );
    }
}
