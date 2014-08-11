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
package org.netbeans.modules.nodejs.platform;

import java.io.File;
import org.netbeans.modules.nodejs.api.NodeJSPlatformType;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = NodeJSPlatformType.class)
@Messages ({"PLATFORM_TYPE=Native NodeJS", "FIND_PLATFORM_TITLE=Find NodeJS Binary"})
public class NativePlatformType extends NodeJSPlatformType {
    private final Preferences prefs = NbPreferences.forModule( NodeJSPlatforms.class ).node( "platforms" );

    @Override
    public NodeJSExecutable find ( String name ) {
        if ("default".equals( name )) {
            return NodeJSExecutable.getDefault();
        }
        try {
            Preferences p = prefs.nodeExists( name ) ? prefs.node( name ) : null;
            if (p != null) {
                String path = p.get( "path", null ); //NOI18N
                String sources = p.get( "sources", null ); //NOI18N
                return new NativeNodeJS( name, sources, path );
            }
        } catch ( BackingStoreException ex ) {
            Exceptions.printStackTrace( ex );
        }
        return null;
    }

    @Override
    public String name () {
        return "nodejs";
    }

    @Override
    public void all ( List<? super NodeJSExecutable> populate ) {
        populate.add(NodeJSExecutable.getDefault());
        try {
            for (String name : prefs.childrenNames()) {
                NodeJSExecutable ex = find( name );
                if (ex != null) {
                    populate.add( ex );
                }
            }
        } catch ( BackingStoreException ex ) {
            Exceptions.printStackTrace( ex );
        }
    }

    @Override
    public String displayName () {
        return NbBundle.getMessage( NativePlatformType.class, "PLATFORM_TYPE" ); //NOI18N
    }

    @Override
    public boolean canAdd () {
        return true;
    }

    @Override
    public String add () {
        File f = new FileChooserBuilder( NativeNodeJS.class )
                .setTitle( NbBundle.getMessage( NativeNodeJS.class, "FIND_PLATFORM_TITLE" ) ) //NOI18N
                .setFilesOnly( true ).showOpenDialog();
        if (f != null) {
            String file = f.getAbsolutePath();
            String name = name() + "-" + file.replace('/', '-');
            Preferences p = prefs.node( name );
            p.put( "path", file );
            try {
                p.flush();
            } catch ( BackingStoreException ex ) {
                Exceptions.printStackTrace( ex );
            }
            return name;
        }
        return null;
    }

    public boolean canRemove ( NodeJSExecutable exe ) {
        return exe instanceof NativeNodeJS;
    }

    public void remove ( NodeJSExecutable exe ) {
        if (exe instanceof NativeNodeJS) {
            String name = ((NativeNodeJS) exe).name();
            prefs.remove( name );
        }
    }
}
