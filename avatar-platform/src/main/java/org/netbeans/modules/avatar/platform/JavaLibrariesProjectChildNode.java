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
import static javax.swing.Action.NAME;
import org.netbeans.api.project.Project;
import org.netbeans.modules.nodejs.api.NodeJSProjectChildNodeFactory;
import org.netbeans.modules.nodejs.api.ProjectMetadata;
import org.openide.nodes.ChildFactory;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 * Allows this platform to provide a subnode of a project for Java libraries
 *
 * @author Tim Boudreau
 */
@Messages(value = {"JAVA_LIBRARIES=Java Libraries", "ADD_LIBRARY=Add &Java Library"})
@ServiceProvider(service = NodeJSProjectChildNodeFactory.class)
public class JavaLibrariesProjectChildNode implements NodeJSProjectChildNodeFactory {

    @Override
    public ChildFactory createChildren(Project project) {
        return new JavaLibrariesChildren(project);
    }

    @Override
    public boolean isPresent(Project project) {
//        ProjectMetadata md = project.getLookup().lookup(ProjectMetadata.class);
//        return md != null && md.getMap("java") != null; //NOI18N
        return true;
    }

    @Override
    public int ordinal() {
        return 1;
    }

    @Override
    public String name() {
        return "JAVA_LIBRARIES";
    }

    @Override
    public Image getIcon() {
        return ImageUtilities.loadImage("org/netbeans/modules/avatar/platform/duke.png");
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(JavaLibrariesProjectChildNode.class, "JAVA_LIBRARIES"); //NOI18N
    }

    @Override
    public void getActions(Project project, List<? super Action> actions) {
        actions.add(new AddJavaLibraryAction(project));
    }

    private static class AddJavaLibraryAction extends AbstractAction {

        private final Project project;

        AddJavaLibraryAction(Project project) {
            this.project = project;
            putValue(NAME, NbBundle.getMessage(AddJavaLibraryAction.class, "ADD_LIBRARY"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            JavaDependency dep = new JavaLibraryPanel().showDialog();
            if (dep != null) {
                JavaDependency.add(project, dep);
            }
        }
    }
}
