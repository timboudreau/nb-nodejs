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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.ui2.RootNode;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Tim Boudreau
 */
public class NodeJSLogicalViewProvider implements LogicalViewProvider {
    private final NodeJSProject project;
    private Reference<RootNode> rn = null;

    public NodeJSLogicalViewProvider ( NodeJSProject project ) {
        this.project = project;
    }

    public synchronized RootNode getView () {
        return rn == null ? null : rn.get();
    }

    @Override
    public Node createLogicalView () {
        RootNode n;
        synchronized ( this ) {
            n = rn == null ? null : rn.get();
            if (n == null) {
                n = new RootNode( project );
                rn = new WeakReference<RootNode>( n );
            }
        }
        return n;
    }

    @Override
    public Node findPath ( Node root, Object target ) {
        NodeJSProject p = root.getLookup().lookup( NodeJSProject.class );
        if (target instanceof FileObject && p != null) {
            FileObject t = (FileObject) target;
            if (t.getParent().equals( p.getProjectDirectory() )) {
                Children kids = root.getChildren();
                Node[] nodes = kids.getNodes( true );
                Set<Node> folders = new HashSet<Node>();
                for (Node node : nodes) {
                    DataObject dob = node.getLookup().lookup( DataObject.class );
                    if (dob != null) {
                        if (t.equals( dob.getPrimaryFile() )) {
                            return node;
                        }
                    }
                    if (dob instanceof DataFolder) {
                        folders.add( node );
                    }
                }
                // Could be more elegant - for now, handle 2-deep, which is
                // enough for any typical node project
                FileObject par = t.getParent();
                for (Node fld : folders) {
                    DataObject dob = fld.getLookup().lookup( DataObject.class );
                    if (dob != null) {
                        if (par.equals( dob.getPrimaryFile() )) {
                            for (Node nn : fld.getChildren().getNodes( true )) {
                                DataObject d1 = nn.getLookup().lookup( DataObject.class );
                                if (d1 != null && d1.getPrimaryFile().equals( target )) {
                                    return nn;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
