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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.NodeJSProjectFactory;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

/**
 *
 * @author tim
 */
public class ImportantFilesChildFactory extends ChildFactory.Detachable<FileObject> {
    private final NodeJSProject project;

    public ImportantFilesChildFactory ( NodeJSProject project ) {
        this.project = project;
    }

    @Override
    protected boolean createKeys ( List<FileObject> toPopulate ) {
        FileObject root = project.getProjectDirectory();
        if (root.isValid()) {
            FileObject fo = root.getFileObject( NodeJSProjectFactory.PACKAGE_JSON );
            if (fo != null && fo.isValid()) {
                toPopulate.add( fo );
            }
            fo = root.getFileObject( NodeJSProjectFactory.DOT_NPMIGNORE );
            if (fo != null && fo.isValid()) {
                toPopulate.add( fo );
            }
        }
        return true;
    }

    @Override
    protected Node createNodeForKey ( FileObject key ) {
        try {
            DataObject dob = DataObject.find( key );
            return new FilterNode( dob.getNodeDelegate() );
        } catch ( DataObjectNotFoundException ex ) {
            Logger.getLogger( ImportantFilesChildFactory.class.getName() )
                    .log( Level.INFO, "File disappeared before node could be created: {0}", key );
        }
        return null;
    }
}
