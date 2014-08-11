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
