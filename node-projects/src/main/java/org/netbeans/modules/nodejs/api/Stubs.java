package org.netbeans.modules.nodejs.api;

import org.openide.filesystems.FileSystem;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Lookup;

/**
 * Provider of stub versions of NodeJS's built in API classes - enough to allow
 * code completion to work minimally.
 * <p/>
 * If the user has specified a source location in the options dialog, that
 * will be used in preference.
 *
 * @author Tim Boudreau
 */
public abstract class Stubs {
    public abstract FileSystem getStubs(SpecificationVersion ver);
    
    public static Stubs getDefault() {
        return Lookup.getDefault().lookup(Stubs.class);
    }
}
