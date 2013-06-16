package org.netbeans.modules.nodejs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.modules.nodejs.api.NodeJSUtils;
import org.netbeans.modules.nodejs.api.Stubs;
import org.netbeans.modules.nodejs.node.LibrariesChildFactory;
import org.netbeans.modules.nodejs.node.ProjectNodeKey;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class StubsTest {
    File prjdir;
    FileObject prjFo;
    NodeJSProject prj;

    @Before
    public void setup () throws Exception {
        File tmp = new File( System.getProperty( "java.io.tmpdir" ) );
        prjdir = new File( tmp, "test-" + System.currentTimeMillis() );
        assertTrue( prjdir.getAbsolutePath(), prjdir.mkdirs() );
        for (String name : new String[]{"index.js", "other.js", "package.json"}) {
            copy( name );
        }
        prjFo = FileUtil.toFileObject( prjdir );
        prj = new NodeJSProject( prjFo, new ProjectState() {
            @Override
            public void markModified () {
            }

            @Override
            public void notifyDeleted () throws IllegalStateException {
            }
        } );
        NodeJSProjectFactory fac = Lookup.getDefault().lookup( NodeJSProjectFactory.class );
        fac.register( prj );
    }

    @After
    public void teardown () throws IOException {
//        prjFo.delete();
    }

    private void copy ( String name ) throws IOException {
        try (InputStream in = StubsTest.class.getResourceAsStream( name )) {
            File f = new File( prjdir, name );
            assertTrue( f.getAbsolutePath(), f.createNewFile() );
            try (FileOutputStream out = new FileOutputStream( f )) {
                FileUtil.copy( in, out );
            }
        }
    }

    @Test
    public void test () throws Exception {
        Stubs stubs = Stubs.getDefault();
        assertNotNull( stubs );
        FileObject actualHttp = stubs.getStubs( null ).getRoot().getFileObject( "http.js" );
        assertNotNull( actualHttp );

        NodeJSUtils utils = NodeJSUtils.getDefault();
        assertNotNull( utils );
        FileObject index = prj.getProjectDirectory().getFileObject( "index.js" );
        assertNotNull( index );
        FileObject other = utils.resolve( "./other", index );
        assertNotNull( other );
        assertEquals( "other.js", other.getNameExt() );
        assertEquals( index.getParent(), other.getParent() );

        FileObject http = utils.resolve( "http", index );
        assertNotNull( http );

        assertEquals( actualHttp, http );
        System.out.println( "HTTP CONTENT: " + actualHttp.asText() );

        LibrariesChildFactory f = new LibrariesChildFactory( prj );
        Set<String> libs = new HashSet<>();
        for (ProjectNodeKey key : f.libraries()) {
            libs.add(key.toString());
        }
        System.out.println( "LIBS: " + libs );
        assertTrue(libs.contains("util"));
        assertTrue(libs.contains("fs"));
        assertTrue(libs.contains("http"));
        assertFalse(libs.contains("url"));
    }
}
