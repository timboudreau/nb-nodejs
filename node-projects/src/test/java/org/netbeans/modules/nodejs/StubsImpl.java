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
package org.netbeans.modules.nodejs;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import org.netbeans.modules.nodejs.api.Stubs;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.XMLFileSystem;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;
import org.xml.sax.SAXException;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=Stubs.class)
public class StubsImpl extends Stubs {

    private Reference<FileSystem> stubs;

    @Override
    public synchronized FileSystem getStubs(SpecificationVersion ver) {
        FileSystem result = stubs == null ? null : stubs.get();
        if (result == null) {
            result = load();
            stubs = new SoftReference<>(result);
        }
        return result;
    }

    private FileSystem load() {
        URL url = StubsImpl.class.getResource("stubs.xml");
        if (url == null) {
            System.err.println("Missing node.js stubs");
            return FileUtil.createMemoryFileSystem();
        }
        try {
            return new XMLFileSystem(url);
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
            return FileUtil.createMemoryFileSystem();
        }
    }
}
