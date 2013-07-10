package org.netbeans.modules.nodejs.code.completion;

import java.util.Collections;
import java.util.Set;
import org.netbeans.contrib.yenta.Yenta;

public class Installer extends Yenta {

    @Override protected Set<String> friends() {
        // Exposes an API, just not to us.
        return Collections.singleton("org.netbeans.modules.javascript2.editor");
    }

//    @Override protected Set<String> siblings() {
//        return new HashSet<String>(Arrays.asList(
//                // Exposes no public packages.
//                "org.netbeans.core.ui",
//                // Exposes some friend packages, but we want private packages.
//                "org.netbeans.core",
//                // Exposes some public packages, but we want private packages.
//                "org.openide.awt"));
//    }
}
