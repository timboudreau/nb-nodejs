/* Copyright (C) 2012 Tim Boudreau

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
package org.netbeans.modules.nodejs.node;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author tim
 */
public class ProjectNodeKey implements Comparable<ProjectNodeKey> {
    public static final String LIBRARY_ICON = "org/netbeans/modules/nodejs/resources/libs.png"; //NOI18N
    public static final String LOGO_ICON = "org/netbeans/modules/nodejs/resources/logo.png"; //NOI18N
    private final ProjectNodeKeyTypes type;
    private final FileObject fld;
    private final File file;
    boolean direct;
    private final String path; //just something to compute a hash code that won't change if deleted from

    public ProjectNodeKey ( ProjectNodeKeyTypes type, FileObject fld ) {
        this.type = type;
        if (fld != null) {
            FileObject fo = fld;
            File file = FileUtil.toFile( fld );
            if (file != null) {
                try {
                    file = file.getCanonicalFile();
                    if (file != null) {
                        FileObject nue = FileUtil.toFileObject( FileUtil.normalizeFile( file ) );
                        if (nue != null) {
                            fo = nue;
                        }
                    }
                } catch ( IOException ex ) {
                    Logger.getLogger( ProjectNodeKeyTypes.class.getName() ).log(
                            Level.FINE, null, ex );
                }
            }
            this.fld = fo;
            this.file = FileUtil.normalizeFile( FileUtil.toFile( fo ) );
            path = file.getPath();
        } else {
            this.fld = fld;
            this.file = null;
            path = "" + System.currentTimeMillis() + "" + new Random().nextLong();
        }
    }

    @Override
    public String toString () {
        return getFld().getName();
    }

    public boolean isBuiltIn () {
        return type == ProjectNodeKeyTypes.BUILT_IN_LIBRARY ? true : false;
    }

    @Override
    public int compareTo ( ProjectNodeKey o ) {
        if (o.isDirect() != isDirect()) {
            if (o.isBuiltIn() != isBuiltIn()) {
                return isBuiltIn() ? 1 : -1;
            }
            return isDirect() ? -1 : 1;
        }
        return toString().compareToIgnoreCase( o.toString() );
    }

    public File toCanonoicalFile () {
        File f = getFld() == null ? null : FileUtil.toFile( getFld() );
        if (f != null) {
            try {
                f = f.getCanonicalFile();
            } catch ( IOException ex ) {
                Logger.getLogger( ProjectNodeKey.class.getName() ).log( Level.FINE, null, ex );
            }
        }
        return f;
    }

    @Override
    public int hashCode () {
        int hash = 7;
        hash = 19 * hash + (this.getType() != null ? this.getType().hashCode() : 0);
        hash = 19 * hash + this.path.hashCode();
        hash = 19 * hash + (this.isDirect() ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals ( Object obj ) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProjectNodeKey other = (ProjectNodeKey) obj;
        if (this.getType() != other.getType()) {
            return false;
        }
        if (this.path != other.path && (this.path == null || !this.path.equals( other.path ))) {
            return false;
        }
        if (this.isDirect() != other.isDirect()) {
            return false;
        }
        return true;
    }

    public ProjectNodeKeyTypes getType () {
        return type;
    }

    public FileObject getFld () {
        FileObject f = fld;
        if (f != null && !f.isValid() && file != null) {
            f = FileUtil.toFileObject( file );
        }
        return f;
    }

    public boolean isDirect () {
        return direct;
    }

    static class BuiltInLibrary extends ProjectNodeKey {
        private final String name;

        BuiltInLibrary ( String name ) {
            super( ProjectNodeKeyTypes.BUILT_IN_LIBRARY, null );
            this.name = name;
        }

        @Override
        public String toString () {
            return name;
        }

        @Override
        public int hashCode () {
            int hash = 7;
            hash = 23 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals ( Object obj ) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final BuiltInLibrary other = (BuiltInLibrary) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals( other.name )) {
                return false;
            }
            return true;
        }
    }

    static class MissingLibrary extends ProjectNodeKey {
        private final String name;
        List<String> references;

        MissingLibrary ( String name ) {
            super( ProjectNodeKeyTypes.MISSING_LIBRARY, null );
            this.name = name;
        }

        @Override
        public String toString () {
            return name;
        }

        @Override
        public int hashCode () {
            int hash = 7;
            hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals ( Object obj ) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MissingLibrary other = (MissingLibrary) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals( other.name )) {
                return false;
            }
            return true;
        }
    }
}
