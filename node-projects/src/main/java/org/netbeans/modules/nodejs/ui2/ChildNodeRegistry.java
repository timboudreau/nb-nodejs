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
package org.netbeans.modules.nodejs.ui2;

import java.util.List;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.api.NodeJSProjectChildNodeFactory;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = ChildNodeRegistry.class)
public class ChildNodeRegistry {

    public void populateKeys ( NodeJSProject project, List<? super Key<?>> keys ) {
        for (NodeJSProjectChildNodeFactory<?> k : Lookup.getDefault().lookupAll(NodeJSProjectChildNodeFactory.class )) {
            if (k.isPresent( project )) {
                Key<NodeJSProjectChildNodeFactory<?>> key = new Key<>( k, k );
                keys.add( key );
            }
        }
    }
}
