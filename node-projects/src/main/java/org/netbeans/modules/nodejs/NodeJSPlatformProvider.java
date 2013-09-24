package org.netbeans.modules.nodejs;

import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import org.netbeans.api.project.Project;

/**
 *
 * @author Tim Boudreau
 */
public abstract class NodeJSPlatformProvider {
    public abstract NodeJSExecutable get ();

    public static NodeJSExecutable get ( Project project ) {
        NodeJSPlatformProvider prov = project == null ? null : project.getLookup().lookup( NodeJSPlatformProvider.class );
        if (prov == null) {
            prov = DEFAULT;
        }
        return prov.get();
    }

    private static NodeJSPlatformProvider DEFAULT = new NodeJSPlatformProvider() {

        @Override
        public NodeJSExecutable get () {
            return NodeJSExecutable.getDefault();
        }

    };
}
