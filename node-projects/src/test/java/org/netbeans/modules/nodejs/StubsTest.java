package org.netbeans.modules.nodejs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;
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
    FileObject booDir;

    NodeJSProject booProject;

    @Before
    public void setup () throws Exception {
        File tmp = new File( System.getProperty( "java.io.tmpdir" ) );
        prjdir = new File( tmp, "test-" + System.currentTimeMillis() );
        assertTrue( prjdir.getAbsolutePath(), prjdir.mkdirs() );
        for (String name : new String[]{"index.js", "other.js", "package.json"}) {
            copy( name );
        }
        prjFo = FileUtil.toFileObject( prjdir );
        prj = new NodeJSProject( prjFo, new PS() );
        NodeJSProjectFactory fac = Lookup.getDefault().lookup( NodeJSProjectFactory.class );
        fac.register( prj );
        File modulesdir = new File( prjdir, "node_modules" );
        assertTrue( modulesdir.getAbsolutePath(), modulesdir.mkdir() );

        File boo = new File( modulesdir, "boo" );
        assertTrue( boo.getAbsolutePath(), boo.mkdir() );

        copy( "package_1.json", "package.json", boo );
        copy( "boo.js", "boo.js", boo );
        copy( "boo.js", "moo.js", boo );

        booDir = FileUtil.toFileObject( boo );

        booProject = new NodeJSProject( booDir, new PS() );
        fac.register( booProject );
    }

    @After
    public void teardown () throws IOException {
        System.out.println( "PROJECT FO " + prjFo.getPath() );
//        prjFo.delete();
    }

    private void copy ( String name ) throws IOException {
        copy( name, name, prjdir );
    }

    private void copy ( String name, String destName, File prjdir ) throws IOException {
        try (InputStream in = StubsTest.class.getResourceAsStream( name )) {
            assertNotNull(in);
            File f = new File( prjdir, destName );
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

        LibrariesChildFactory f = new LibrariesChildFactory( prj );
        Set<String> libs = new HashSet<>();
        for (ProjectNodeKey key : f.libraries()) {
            libs.add( key.toString() );
            if (!"boo".equals( key.toString() )) {
                assertTrue( key.isBuiltIn() );
            } else {
                assertFalse( key.isBuiltIn() );
            }
        }
        assertTrue( libs.contains( "util" ) );
        assertTrue( libs.contains( "fs" ) );
        assertTrue( libs.contains( "http" ) );
        assertFalse( libs.contains( "url" ) );
        assertTrue( libs.contains( "boo" ) );

        FileObject boo = utils.resolve( "boo", index );
        assertNotNull( boo );
        assertEquals( booDir.getFileObject( "boo.js" ), boo );

        NodeJSProjectProperties props = prj.getLookup().lookup( NodeJSProjectProperties.class );
        assertNotNull( props );
        assertEquals( "testproject", props.getDisplayName() );

        assertEquals( "joe@mail.example", props.getAuthorEmail() );

        // #8 - Ensure the main file path is relative
        props = booProject.getLookup().lookup( NodeJSProjectProperties.class );

        FileObject fo = props.getMainFile();
        assertEquals("boo", fo.getName());

        props.setMainFile( booProject.getProjectDirectory().getFileObject( "moo.js" ));

        fo = props.getMainFile();
        assertNotNull("Main file should be moo.js but was null", fo);
        assertEquals("moo", fo.getName());

        FileObject booMetadata = booProject.getProjectDirectory().getFileObject("package.json");
        try (InputStream in = booMetadata.getInputStream()) {
            ObjectMapper m = new ObjectMapper();
            Map<String,Object> map = m.readValue( in, Map.class);
            String path = (String) map.get("main");
            assertNotNull(path);
            assertEquals("./boo.js", path);
        }

        props.setAuthor( "" );
        props.setAuthorEmail( "" );
        props.save();
        try (InputStream in = booMetadata.getInputStream()) {
            ObjectMapper m = new ObjectMapper();
            Map<String,Object> map = m.readValue( in, Map.class);
            assertFalse( "If author fields are clear, empty entries "
                    + "should not remain in package.json", map.containsKey( "author" ));
        }
    }

    private static class PS implements ProjectState {

        @Override
        public void markModified () {
        }

        @Override
        public void notifyDeleted () throws IllegalStateException {
        }
    }

}
