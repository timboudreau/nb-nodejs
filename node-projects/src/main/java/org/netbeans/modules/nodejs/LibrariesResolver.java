package org.netbeans.modules.nodejs;

import javax.swing.event.ChangeListener;

/**
 * Utility in the project's lookup for detecting libraries which are listed
 * in the package.json dependencies section, but for which there are no
 * corresponding folders under node_modules.
 *
 * @author Tim Boudreau
 */
public interface LibrariesResolver {
    void install ();

    void addChangeListener ( ChangeListener cl );

    void removeChangeListener ( ChangeListener cl );

    boolean hasMissingLibraries ();
}
