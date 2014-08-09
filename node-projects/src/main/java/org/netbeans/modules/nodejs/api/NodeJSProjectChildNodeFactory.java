package org.netbeans.modules.nodejs.api;

import java.util.List;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.openide.nodes.ChildFactory;

/**
 *
 * @author Tim Boudreau
 */
public interface NodeJSProjectChildNodeFactory extends KeyTypes {
    public ChildFactory createChildren ( Project project );
    public boolean isPresent (Project project);
    public void getActions(Project project, List<? super Action> actions);
}
