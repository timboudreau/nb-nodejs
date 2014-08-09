/* Copyright (C) 2014 Tim Boudreau

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
package org.netbeans.modules.avatar.platform;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
public class JavaLibrariesChildren extends ChildFactory<JavaDependency> {

    private final Project project;

    public JavaLibrariesChildren(Project project) {
        this.project = project;
    }

    @Override
    protected boolean createKeys(List<JavaDependency> list) {
        JavaDependency.find(project, list);
        return true;
    }

    @Override
    protected Node createNodeForKey(JavaDependency key) {
        return new LibraryNode(key, project, this);
    }

    void ref() {
        super.refresh(true);
    }

    private static final class LibraryNode extends AbstractNode {

        private final JavaLibrariesChildren ch;

        public LibraryNode(JavaDependency dep, Project project, JavaLibrariesChildren ch) {
            super(Children.LEAF, Lookups.fixed(dep, project, ch));
            setIconBaseWithExtension("org/netbeans/modules/avatar/platform/jar.png");
            setDisplayName(dep.artifactId);
            setShortDescription(dep.groupId + " : " + dep.version);
            this.ch = ch;
        }

        public Action[] getActions(boolean ignored) {
            JavaDependency dep = getLookup().lookup(JavaDependency.class);
            Project project = getLookup().lookup(Project.class);
            JavaLibrariesChildren ch = getLookup().lookup(JavaLibrariesChildren.class);
            return new Action[]{new RemoveJavaLibraryAction(project, dep, ch)};
        }

        @Messages("REMOVE_JAVA_LIBRARY=&Remove Library")
        static class RemoveJavaLibraryAction extends AbstractAction {

            private final Project project;
            private final JavaDependency dep;
            private final JavaLibrariesChildren ch;

            RemoveJavaLibraryAction(Project project, JavaDependency dep, JavaLibrariesChildren ch) {
                putValue(NAME, NbBundle.getMessage(RemoveJavaLibraryAction.class, "REMOVE_JAVA_LIBRARY"));
                this.project = project;
                this.dep = dep;
                this.ch = ch;
            }

            @Override
            public void actionPerformed(ActionEvent ae) {
                JavaDependency.remove(project, dep);
                ch.ref();
            }
        }
    }
}
