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

import java.io.File;
import java.net.URI;
import org.netbeans.api.queries.SharabilityQuery.Sharability;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider (service = SharabilityQueryImplementation2.class)
public class NodeSharabilityQuery implements SharabilityQueryImplementation2 {
    @Override
    public Sharability getSharability ( URI uri ) {
        File file = Utilities.toFile( uri );
        if (NodeJSProjectFactory.NB_METADATA.equals( file.getName() )
                && new File( file.getParent(), NodeJSProjectFactory.PACKAGE_JSON ).exists()) {
            return Sharability.NOT_SHARABLE;
        }
        return Sharability.UNKNOWN;
    }
}
