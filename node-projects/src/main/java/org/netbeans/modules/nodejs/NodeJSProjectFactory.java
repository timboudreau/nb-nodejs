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
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = ProjectFactory.class, position = -200000000)
public class NodeJSProjectFactory implements ProjectFactory2 {
    public static final String PACKAGE_JSON = "package.json"; //NOI18N
    public static final String NODE_MODULES_FOLDER = "node_modules"; //NOI18N
    public static final String NB_METADATA = ".nbrun"; //NOI18N
    public static final String DOT_NPMIGNORE = ".npmignore"; //NOI18N
    private final Set<NodeJSProject> cache = new WeakSet<NodeJSProject>();

    @Override
    public boolean isProject ( FileObject fo ) {
        return fo.getFileObject( PACKAGE_JSON ) != null;
    }

    private FileObject resolve ( FileObject fo ) {
        if (fo == null) {
            return null;
        }
        File f = FileUtil.toFile( fo );
        if (f != null) {
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
            l = new ArrayList<NodeJSProject>( cache );
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
        if (!isProject( fo )) {
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
        if (isProject( fo )) {
            return new ProjectManager.Result( ImageUtilities.loadImageIcon(
                    "org/netbeans/modules/nodejs/resources/logo.png", false ) ); //NOI18N
        }
        return null;
    }

    void register ( NodeJSProject prj ) { //for tests
        cache.add( prj );
    }
}
