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
package org.netbeans.modules.nodejs.registry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.netbeans.api.project.Project;
import org.netbeans.modules.nodejs.registry.FileChangeRegistry.EventType;
import org.netbeans.modules.nodejs.registry.FileChangeRegistry.FileObserver;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public class FileChangeRegistryTest {
    FileSystem mfs;
    FileObject projectRoot;
    Project fake;
    FileObject sub;
    FileObject a;
    FileObject b;
    FileObject c;
    FileObject d;
    FileObject su;
    FileObject sublet;
    FileChangeRegistry reg;
    O oRoot;
    O oSub;
    O oa;
    O ob;
    O oc;
    O od;

    @Before
    public void setUp () throws IOException {
        mfs = FileUtil.createMemoryFileSystem();
        projectRoot = addFolder( "prj" );
        fake = new Fake( projectRoot );
        sub = addFolder( "prj/sub" );
        sublet = addFolder( "prj/sublet" );
        a = addFile( "prj/sub/a.txt" );
        b = addFile( "prj/sub/b.txt" );
        c = addFile( "prj/project.json" );
        d = addFile( "offRoot.txt" );
        su = addFolder( "prj/su" );
        reg = new FileChangeRegistry( fake );
        oRoot = new O( "root" );
        oSub = new O( "sub" );
        oa = new O( "a" );
        ob = new O( "b" );
        oc = new O( "c" );
        od = new O( "d" );
        reg.registerInterest( a, oa );
        reg.registerInterest( b, ob );
        reg.registerInterest( c, oc );
        reg.registerInterest( c, od );
        reg.registerInterest( projectRoot, oRoot );
        reg.registerInterest( sub, oSub );
    }

    @Test
    public void testEditFile () throws IOException, InterruptedException {
        write( "prj/sub/a.txt", "Hello world" );
        List<EventType> l = oSub.await();
        assertFalse( l.isEmpty() );
        assertEquals( EventType.CHANGE, l.iterator().next() );
        assertEquals( "a.txt", oSub.lastPath );

        l = oa.await();
        assertFalse( l.isEmpty() );
        assertEquals( EventType.CHANGE, l.iterator().next() );
        assertNull( oa.lastPath );
    }

    @Test
    public void testRename () throws IOException, InterruptedException {
        List<EventType> l;
        write( "prj/sub/a.txt", "Hello world" );
        l = oa.await();
        assertFalse( l.isEmpty() );
        assertEquals( EventType.CHANGE, l.iterator().next() );
        assertNull( oa.lastPath );

        rename( "prj/sub/a.txt", "something" );

        write( "prj/sub/something.txt", "Goodbye" );
        l = oa.await();
        assertFalse( l + "", l.isEmpty() );
        assertEquals( EventType.CHANGE, l.iterator().next() );
        assertNull( oa.lastPath );
    }

    @Test
    public void testPartialMatch () throws Exception {
        su.delete();
        Thread.sleep( FileChangeRegistry.DEFAULT_DELAY * 2 );
        List<EventType> l = oSub.await();
        assertTrue( l.isEmpty() );
        sublet.delete();
        l = oSub.await();
        assertTrue( l.isEmpty() );
    }

    @Test
    public void testBasic () throws IOException, InterruptedException {
        FileObject e = addFile( "prj/sub/e.txt" );
        List<EventType> l = oSub.await();
        assertFalse( l.isEmpty() );
        assertEquals( 1, l.size() );
        assertEquals( EventType.NEW_CHILD, l.iterator().next() );
        Thread.sleep( FileChangeRegistry.DEFAULT_DELAY * 2 );
        l = oRoot.await();
        assertFalse( l.isEmpty() );
        assertEquals( EventType.NEW_CHILD, l.iterator().next() );
        assertTrue( oa.evts.isEmpty() );
        assertTrue( ob.evts.isEmpty() );
        assertTrue( oc.evts.isEmpty() );
        assertTrue( od.evts.isEmpty() );

        c.delete();
        l = oc.await();
        assertFalse( l.isEmpty() );
        assertEquals( EventType.DELETED, l.iterator().next() );

        projectRoot.delete();
        l = oRoot.await();
        Thread.sleep( FileChangeRegistry.DEFAULT_DELAY * 2 );

        assertFalse( l.isEmpty() );
        assertFalse( oSub.evts.isEmpty() );
        assertFalse( oa.evts.isEmpty() );
        assertFalse( ob.evts.isEmpty() );
        assertFalse( oc.evts.isEmpty() );
        assertFalse( od.evts.isEmpty() );
    }

    private static class O implements FileObserver {
        private List<EventType> evts = new ArrayList<EventType>();
        private final String name;
        private String lastPath;

        public O ( String name ) {
            this.name = name;
        }

        public String toString () {
            return name;
        }

        synchronized List<EventType> drain () {
            List<EventType> nue = new ArrayList<EventType>( evts );
            evts.clear();
            return nue;
        }

        @Override
        public synchronized void onEvent ( EventType type, String path ) {
            evts.add( type );
            lastPath = path;
            System.out.println( name + " got " + type + " " + path );
        }

        List<EventType> await () throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                Thread.sleep( FileChangeRegistry.DEFAULT_DELAY * 2 );
                if (!evts.isEmpty()) {
                    break;
                }
            }
            return drain();
        }
    }

    private FileObject addFolder ( String path ) throws IOException {
        return FileUtil.createFolder( mfs.getRoot(), path );
    }

    private FileObject addFile ( String path ) throws IOException {
        return FileUtil.createData( mfs.getRoot(), path );
    }

    private void delete ( String path ) throws IOException {
        FileObject fo = mfs.getRoot().getFileObject( path );
        fo.delete();
    }

    private void rename ( String path, String newName ) throws IOException {
        FileObject fo = mfs.getRoot().getFileObject( path );
        FileLock lock = fo.lock();
        try {
            fo.rename( lock, newName, "txt" );
        } finally {
            lock.releaseLock();
        }
    }

    private void write ( String path, String data ) throws IOException {
        FileObject fo = mfs.getRoot().getFileObject( path );
        if (fo == null) {
            fo = addFile( path );
        }
        ByteArrayInputStream o = new ByteArrayInputStream( data.getBytes() );
        OutputStream out = fo.getOutputStream();
        FileUtil.copy( o, out );
        out.close();
    }

    private static class Fake implements Project {
        private final FileObject fo;

        public Fake ( FileObject fo ) {
            this.fo = fo;
        }

        @Override
        public FileObject getProjectDirectory () {
            return fo;
        }

        @Override
        public Lookup getLookup () {
            return Lookups.fixed( this );
        }
    }
}
