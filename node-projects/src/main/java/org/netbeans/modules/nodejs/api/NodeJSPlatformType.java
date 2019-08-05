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
package org.netbeans.modules.nodejs.api;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.nodejs.json.ObjectMapperProvider;
import static org.netbeans.modules.nodejs.json.ObjectMapperProvider.STRING_OBJECT_MAP;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author tim
 */
public abstract class NodeJSPlatformType implements Comparable<NodeJSPlatformType> {

    public abstract String name ();

    public static Collection<? extends NodeJSPlatformType> allTypes () {
        return Lookup.getDefault().lookupAll( NodeJSPlatformType.class );
    }

    public abstract String add ( File f, Map<String, Object> props, String displayName );

    public abstract NodeJSExecutable find ( String name );

    public abstract void all ( List<? super NodeJSExecutable> populate );

    public abstract String displayName ();

    public boolean canAdd () {
        return false;
    }

    public String add () {
        throw new UnsupportedOperationException();
    }

    public boolean canRemove ( NodeJSExecutable exe ) {
        return false;
    }

    public void remove ( NodeJSExecutable exe ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo ( NodeJSPlatformType t ) {
        return displayName().compareTo( t.displayName() );
    }

    public boolean equals ( Object o ) {
        return o instanceof NodeJSPlatformType && name().equals( ((NodeJSPlatformType) o).name() );
    }

    public int hashCode () {
        return name().hashCode();
    }

    public String toString () {
        return name();
    }

    public List<WizardDescriptor.Panel<WizardDescriptor>> getAddPlatformWizardPages () {
        return Collections.emptyList();
    }

    public static Map<String, Object> toJson ( File outFile ) throws IOException {
        FileObject fo = FileUtil.toFileObject( FileUtil.normalizeFile( outFile ) );
        Map<String, Object> m = ObjectMapperProvider.newObjectMapper().readValue( fo.asText(), STRING_OBJECT_MAP);
        return m;
    }
}
