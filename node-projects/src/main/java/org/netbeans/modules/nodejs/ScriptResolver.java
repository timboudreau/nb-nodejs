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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 * Failover until I decipher the problem with the declarative version
 *
 * @author tim
 */
@ServiceProvider (service = MIMEResolver.class, position = 3214327)
public class ScriptResolver extends MIMEResolver {
    private static final byte[] lookFor;

    static {
        try {
            lookFor = "#!/usr/bin/env node".getBytes( "US-ASCII" );
        } catch ( UnsupportedEncodingException ex ) {
            throw new AssertionError( ex );
        }
    }

    @Override
    public String findMIMEType ( FileObject fo ) {
        String ext = fo.getExt();
        if (ext.isEmpty() || "sh".equals( ext )) {
            try {
                InputStream in = fo.getInputStream();
                try {
                    byte[] nue = new byte[lookFor.length];
                    int ct = in.read( nue );
                    if (ct == nue.length) {
                        if (Arrays.equals( lookFor, nue )) {
                            return "text/javascript";
                        }
                    }
                } finally {
                    in.close();
                }
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            }
        }
        return null;
    }
}
