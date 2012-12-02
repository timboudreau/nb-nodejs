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
package org.netbeans.modules.nodejs.ui2;

import java.awt.Image;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Tim Boudreau
 */
public class Key<T> implements Comparable<Key<?>> {
    private final KeyType type;
    private final T obj;
    public static final Key<KeyType> IMPORTANT_FILES = new Key<KeyType>(
            KeyType.IMPORTANT_FILES, KeyType.IMPORTANT_FILES );
    public static final Key<KeyType> LIBRARIES = new Key<KeyType>(
            KeyType.LIBRARIES, KeyType.LIBRARIES );

    Key ( FileObject obj ) {
        this( KeyType.SOURCES, (T) obj );
    }

    private Key ( KeyType type, T obj ) {
        this.type = type;
        this.obj = obj;
    }

    public T get () {
        return obj;
    }

    @Override
    public String toString () {
        return type.toString();
    }

    public Image getIcon () {
        return type.getIcon();
    }

    public KeyType type () {
        return type;
    }

    @Override
    public int hashCode () {
        int hash = 3;
        hash = 67 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 67 * hash + (this.obj != null ? this.obj.hashCode() : 0);
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
        final Key<T> other = (Key<T>) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.obj != other.obj && (this.obj == null || !this.obj.equals( other.obj ))) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo ( Key<?> o ) {
        if (o.type == type) {
            switch ( type ) {
                case SOURCES:
                    return compareFileObjects( (FileObject) get(), (FileObject) o.get() );
                case LIBRARIES:
                    return 0;
                case IMPORTANT_FILES:
                    return 0;
                default:
                    throw new AssertionError();
            }
        } else {
            return Integer.valueOf( type.ordinal() ).compareTo( o.type.ordinal() );
        }
    }

    private int compareFileObjects ( FileObject a, FileObject b ) {
        if (a.isFolder() == b.isFolder()) {
            String aext = a.getExt();
            String bext = b.getExt();
            if (aext != null && bext != null) {
                if (aext.equals( bext )) {
                    return a.getName().compareToIgnoreCase( b.getName() );
                } else {
                    return aext.compareToIgnoreCase( bext );
                }
            } else {
                if (aext == null && bext == null) {
                    return a.getName().compareToIgnoreCase( b.getName() );
                } else {
                    return aext == null ? -1 : 1;
                }
            }
//            return a.getName().compareToIgnoreCase(b.getName());
        } else {
            return a.isFolder() ? 1 : -1;
        }
    }
}
