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

import java.util.Comparator;
import org.openide.filesystems.FileObject;

/**
 *
 * @author tim
 */
class FileObjectComparator implements Comparator<FileObject> {
    @Override
    public int compare ( FileObject o1, FileObject o2 ) {
        boolean aJs = ("js".equals( o1.getExt() )) || ("json".equals( o1.getExt() ));
        boolean bJs = ("js".equals( o2.getExt() )) || ("json".equals( o2.getExt() ));
        boolean aFld = o1.isFolder();
        boolean bFld = o2.isFolder();
        if (aJs == bJs) {
            if (aFld == bFld) {
                return o1.getName().compareToIgnoreCase( o2.getName() );
            }
            if (aFld) {
                return 1;
            }
            return -1;
        }
        if (aJs) {
            return -1;
        }
        return 1;
    }
}
