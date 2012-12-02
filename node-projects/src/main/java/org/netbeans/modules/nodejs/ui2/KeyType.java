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
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author tim
 */
public enum KeyType {
    SOURCES,
    LIBRARIES,
    IMPORTANT_FILES;

    @Override
    public String toString () {
        return NbBundle.getMessage( Key.class, name() );
    }

    public Image getIcon () {
        switch ( this ) {
            case LIBRARIES:
                return ImageUtilities.loadImage( "org/netbeans/modules/nodejs/resources/libs.png" ); //NOI18N
            case SOURCES:
                return ImageUtilities.loadImage( "org/netbeans/modules/nodejs/resources/js.png" ); //NOI18N
            case IMPORTANT_FILES:
                return ImageUtilities.loadImage( "org/netbeans/modules/nodejs/resources/hollow.png" ); //NOI18N
            default:
                throw new AssertionError();
        }
    }
}
