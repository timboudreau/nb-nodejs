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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.netbeans.modules.avatar.platform.api.BundledAvatarPlatform;
import org.netbeans.modules.avatar.platform.wizard.ChooseAvatarJSBinaryPanel;
import org.netbeans.modules.avatar.platform.wizard.DisplayNamePanel;
import org.netbeans.modules.avatar.platform.wizard.ValidateAvatarNodePanel;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import org.netbeans.modules.nodejs.api.NodeJSPlatformType;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Registers the type of NodeJS platforms
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = NodeJSPlatformType.class)
@Messages({"AVATAR_PLATFORM=Avatar Java NodeJS Platforms", "FIND_PLATFORM_TITLE=Find AvatarJS JAR", "UNKNOWN_VERSION=(unknown version)"})
public class AvatarPlatformType extends NodeJSPlatformType {

    private final Preferences prefs = NbPreferences.forModule(AvatarPlatformType.class).node("platforms"); //NOI18N

    @Override
    public String name() {
        return "avatar";
    }

    @Override
    public NodeJSExecutable find(String name) {
        for (BundledAvatarPlatform a : BundledAvatarPlatform.all()) {
            if (a.name().equals(name)) {
                return new AvatarPlatform(a);
            }
        }
        try {
            Preferences cn = prefs.nodeExists(name) ? prefs.node(name) : null;
            if (cn != null) {
                String dn = cn.get("displayName", null); //NOI18N
                String jar = cn.get("jar", null); //NOI18N
                String version = cn.get("version", Bundle.UNKNOWN_VERSION());
                return new AvatarPlatform(new FP(name, jar, dn, version));
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public void all(List<? super NodeJSExecutable> populate) {
        for (BundledAvatarPlatform a : BundledAvatarPlatform.all()) {
            populate.add(new AvatarPlatform(a));
        }
        try {
            for (String name : prefs.childrenNames()) {
                Preferences cn = prefs.node(name);
                if (cn != null) {
                    String dn = cn.get("displayName", null); //NOI18N
                    String jar = cn.get("jar", null); //NOI18N
                    String version = cn.get("version", Bundle.UNKNOWN_VERSION());
                    populate.add(new AvatarPlatform(new FP(name, jar, dn, version)));
                }
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public String displayName() {
        return Bundle.AVATAR_PLATFORM();
    }

    @Override
    public boolean canAdd() {
        return true;
    }

    @Override
    public String add() {
        File f = new FileChooserBuilder(AvatarPlatformType.class)
                .setTitle(Bundle.FIND_PLATFORM_TITLE())
                .setFilesOnly(true).showOpenDialog();
        if (f != null) {
            String file = f.getAbsolutePath();
            String name = AvatarPlatform.jarToName(f);
            Preferences p = prefs.node(name);
            p.put("jar", file); //NOI18N
            try {
                p.flush();
            } catch (BackingStoreException ex) {
                Exceptions.printStackTrace(ex);
            }
            return name;
        }
        return null;
    }

    @Override
    public String add(File f, Map<String, Object> props, String displayName) {
        String file = f.getAbsolutePath();
        String name = AvatarPlatform.jarToName(f);
        Preferences p = prefs.node(name);
        p.put("jar", file); //NOI18N
        p.put("displayName", displayName);
        if (props.containsKey("version") && props.get("version") instanceof String) {
            p.put("version", (String) props.get("version"));
        }
        return name;
    }

    public static AvatarPlatform create(File f) {
        return new AvatarPlatform(new FP("test", f.getAbsolutePath(), "Test", Bundle.UNKNOWN_VERSION()));
    }

    @Override
    public List<WizardDescriptor.Panel<WizardDescriptor>> getAddPlatformWizardPages() {
        List<WizardDescriptor.Panel<WizardDescriptor>> result = new LinkedList<>();
        result.add(new ChooseAvatarJSBinaryPanel());
        result.add(new ValidateAvatarNodePanel());
        result.add(new DisplayNamePanel());
        return result;
    }

    private static final class FP extends BundledAvatarPlatform {

        private final String name;
        private final String jar;
        private final String displayName;
        private final String version;

        public FP(String name, String jar, String displayName, String version) {
            this.name = name;
            this.jar = jar;
            this.displayName = displayName;
            this.version = version;
        }
        
        public String version() {
            return version;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String displayName() {
            return displayName == null ? name : displayName;
        }

        @Override
        public String jar() {
            return jar;
        }
    }
}
