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
package org.netbeans.modules.nodejs.options;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Tim Boudreau
 */
public class DotGitConfigTest {

    @Test
    public void test () throws IOException {
        File tmp = new File( System.getProperty( "java.io.tmpdir" ) );
        File dgt = new File( tmp, getClass().getSimpleName() + "-" + System.currentTimeMillis() );
        if (!dgt.exists()) {
            assertTrue( dgt.createNewFile() );
        }
        try (InputStream in = DotGitConfigTest.class.getResourceAsStream( "dotgitconfig" )) {
            assertNotNull( "Test resource missing", in );
            try (FileOutputStream out = new FileOutputStream( dgt )) {
                FileUtil.copy( in, out );
            }
        }
        DotGitConfig dotGitFile = new DotGitConfig(dgt);
        String user = dotGitFile.get("user", "name");
        String email = dotGitFile.get( "user", "email");
        assertNotNull(user);
        assertNotNull(email);
        assertEquals("Joe Blow", user);
        assertEquals("joe@bobmail.info", email);
    }
}
