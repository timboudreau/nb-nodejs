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
package org.netbeans.modules.nodejs.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.netbeans.modules.nodejs.ProjectMetadataImpl;

import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class MetadataTest {
    Fake fake = new Fake();
    ProjectMetadataImpl impl = new ProjectMetadataImpl( fake );

    @Test
    public void testLoading () {
        Map<String,Object> m = impl.getMap();
        System.out.println( "GOT " + m );
        assertNotNull( m );
        assertFalse( m.isEmpty() );
        assertEquals( "recon", impl.getValue( "name" ) );
        assertEquals( "0.0.8", impl.getValue( "version" ) );
        assertEquals( "git", impl.getValue( "repository.type" ) );
    }

    @Test
    public void test () {
        impl.setValue( "name", "thing" );
        assertEquals( "thing", impl.getValue( "name" ) );
        impl.setValue( "name", "another" );
        assertEquals( "another", impl.getValue( "name" ) );
        test( "foo.bar", "foobar" );
        test( "foo.baz", "foobaz" );
        test( "foo.fung.hey", "hey" );
        System.out.println( impl );
    }

    private void test ( String key, String val ) {
        impl.setValue( key, val );
        assertEquals( val, impl.getValue( key ) );
    }

    static class Fake implements Project {
        FileObject root = FileUtil.createMemoryFileSystem().getRoot();

        Fake () {
            try {
                InputStream in = MetadataTest.class.getResourceAsStream( "package_0.json" );
                try {
                    FileObject fo = root.createData( "package.json" );
                    OutputStream out = fo.getOutputStream();
                    try {
                        FileUtil.copy( in, out );
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } catch ( IOException ex ) {
                throw new Error( ex );
            }
        }

        public FileObject getProjectDirectory () {
            return root;
        }

        public Lookup getLookup () {
            return Lookup.EMPTY;
        }
    }
}
