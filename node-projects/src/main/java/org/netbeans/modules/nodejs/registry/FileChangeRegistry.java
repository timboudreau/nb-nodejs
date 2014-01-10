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
package org.netbeans.modules.nodejs.registry;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tim Boudreau
 */
public final class FileChangeRegistry {
    private final Project project;
    private final FileChangeAdapter adapter = new A();
    private final Set<Entry> entries = new HashSet<>();
    private final Set<Event> pendingEvents = new HashSet<>();
    private final RequestProcessor processor;
    private final RequestProcessor.Task task;
    private final int delay;
    public static final int DEFAULT_DELAY = 100;
    private FileObject hold;

    public FileChangeRegistry ( Project project ) {
        this( project, DEFAULT_DELAY );
    }

    public FileChangeRegistry ( Project project, int delay ) {
        this.project = project;
        this.delay = delay;
        processor = new RequestProcessor( "FileChangeRegistry " + project, 1, true ); //NOI18N
        task = processor.create( deliverer );
    }

    private boolean drainPendingEvents ( Set<Event> into ) {
        assert into != pendingEvents;
        boolean res = false;
        synchronized ( this ) {
            res = !pendingEvents.isEmpty();
            into.addAll( pendingEvents );
            pendingEvents.clear();
        }
        return res;
    }

    private void addEvent ( Event evt ) {
        synchronized ( this ) {
            pendingEvents.add( evt );
            trigger();
        }
    }
    private final Object lock = new Object();
    private volatile boolean delivering = false;
    private final Runnable deliverer = new Runnable() {
        private final Set<Event> deliver = new HashSet<Event>();

        @Override
        public void run () {
            Set<Entry> toRemove = new HashSet<>();
            Set<Entry> entries;
            synchronized ( lock ) {
                entries = new HashSet<>( FileChangeRegistry.this.entries );
            }
            while ( drainPendingEvents( deliver ) ) {
                delivering = true;
                for (Event evt : deliver) {
                    for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
                        Entry en = it.next();
                        if (en.get() == null) {
                            toRemove.add( en );
                        } else if (en.matches( evt )) {
                            try {
                                String eventPath = evt.path;
                                String entryPath = en.relativePath;
                                String deliverPath;
                                if (eventPath.equals( entryPath )) {
                                    deliverPath = null;
                                } else {
                                    if (eventPath.startsWith( entryPath )) {
                                        deliverPath = eventPath.substring( entryPath.length(), eventPath.length() );
                                    } else {
                                        deliverPath = eventPath;
                                    }
                                }
                                if (deliverPath != null && !deliverPath.isEmpty() && deliverPath.charAt( 0 ) == '/') {
                                    deliverPath = deliverPath.substring( 1 );
                                }
                                boolean delivered = en.deliver( evt, deliverPath );
                                if (!delivered) {
                                    toRemove.add( en );
                                }
                            } catch ( Exception e ) {
                                Exceptions.printStackTrace( e );
                            }
                        }
                    }
                }
            }
            synchronized ( lock ) {
                FileChangeRegistry.this.entries.removeAll( toRemove );
            }
            delivering = false;
            synchronized ( this ) {
                notifyAll();
            }
            maybeStopListening();
        }
    };
    private AtomicBoolean listening = new AtomicBoolean();

    private void maybeStartListening () {
        boolean empty;
        synchronized ( lock ) {
            empty = this.entries.isEmpty();
            if (!empty) {
                startListening();
            }
        }
    }

    private void maybeStopListening () {
        boolean empty;
        synchronized ( lock ) {
            empty = this.entries.isEmpty();
            if (empty) {
                stopListening();
            }
        }
    }

    private void startListening () {
        if (listening.compareAndSet( false, true )) {
            hold = project.getProjectDirectory();
            if (hold.isValid()) {
                hold.addRecursiveListener( adapter );
                hold.addFileChangeListener( adapter );
            }
        }
    }

    private void stopListening () {
        if (listening.compareAndSet( true, false )) {
            if (hold != null && hold.isValid()) {
                hold.removeRecursiveListener( adapter );
                hold.removeFileChangeListener( adapter );
            }
        }
    }

    public void awaitNextDelivery () throws InterruptedException {
        synchronized ( deliverer ) {
            deliverer.wait();
        }
    }

    public void awaitNextDelivery ( long timeout ) throws InterruptedException {
        synchronized ( deliverer ) {
            deliverer.wait( timeout );
        }
    }

    private void trigger () {
        if (!delivering) {
            task.schedule( delay );
        }
    }

    public void registerInterest ( String relativePath, FileObserver obs ) {
        synchronized ( lock ) {
            entries.add( new Entry( obs, relativePath ) );
        }
        maybeStartListening();
    }

    public void registerInterest ( FileObject o, FileObserver obs ) {
        String path = FileUtil.getRelativePath( project.getProjectDirectory(), o );
        synchronized ( lock ) {
            entries.add( new Entry( obs, path ) );
        }
        maybeStartListening();
    }

    private final class A extends FileChangeAdapter {
        @Override
        public void fileFolderCreated ( FileEvent fe ) {
            FileObject fo = project.getProjectDirectory();
            String evtPath = FileUtil.getRelativePath( fo, fe.getFile() );
            fe.runWhenDeliveryOver( new R( EventType.NEW_CHILD, evtPath ) );
        }

        @Override
        public void fileDataCreated ( FileEvent fe ) {
            FileObject fo = project.getProjectDirectory();
            String evtPath = FileUtil.getRelativePath( fo, fe.getFile() );
            fe.runWhenDeliveryOver( new R( EventType.NEW_CHILD, evtPath ) );
        }

        @Override
        public void fileChanged ( FileEvent fe ) {
            FileObject fo = project.getProjectDirectory();
            String evtPath = FileUtil.getRelativePath( fo, fe.getFile() );
            fe.runWhenDeliveryOver( new R( EventType.CHANGE, evtPath ) );
        }

        @Override
        public void fileDeleted ( FileEvent fe ) {
            FileObject fo = project.getProjectDirectory();
            String evtPath = FileUtil.getRelativePath( fo, fe.getFile() );
            fe.runWhenDeliveryOver( new R( EventType.DELETED, evtPath ) );
        }

        @Override
        public void fileRenamed ( FileRenameEvent fe ) {
            //Check if the root has been renamed, and if so, reattach
            String originalName = fe.getName() + "." + fe.getExt();
            FileObject fo = fe.getFile();
            String pth = FileUtil.getRelativePath( project.getProjectDirectory(), fo.getParent() );

            String origPath = pth + '/' + originalName; //NOI18N
            String newPath = pth + '/' + fo.getNameExt(); //NOI18N
            Set<Entry> newEntries = new HashSet<>();
            Set<Entry> entries;
            synchronized ( lock ) {
                entries = new HashSet<>( FileChangeRegistry.this.entries );
            }
            for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
                Entry e = it.next();
                FileObserver obs = e.get();
                if (obs == null) {
                    it.remove();
                } else {
                    String curr = e.relativePath;
                    if (curr.equals( origPath )) {
                        Entry nue = new Entry( obs, newPath );
                        newEntries.add( nue );
                        it.remove();
                    } else if (curr.startsWith( origPath )) {
                        String ss = curr.substring( origPath.length(), curr.length() );
                        Entry nue = new Entry( obs, newPath + "/" + ss );
                        newEntries.add( nue );
                        it.remove();
                    }
                }
            }
            maybeStopListening();
            synchronized ( lock ) {
                FileChangeRegistry.this.entries.addAll( newEntries );
            }
        }

        private class R implements Runnable {
            private final EventType type;
            private final String path;

            public R ( EventType type, String path ) {
                this.type = type;
                this.path = path;
            }

            @Override
            public void run () {
                addEvent( new Event( type, path ) );
            }
        }
    }

    private static final class Entry implements Comparable<Entry> {
        private final Reference<FileObserver> obs;
        private final String relativePath;

        public Entry ( FileObserver obs, String relativePath ) {
            this.obs = new WeakReference<>( obs );
            this.relativePath = relativePath;
        }

        public String toString () {
            return relativePath + " for " + get();
        }

        FileObserver get () {
            return obs.get();
        }

        public boolean matches ( Event evt ) {
            if (this.relativePath == null || "".equals( this.relativePath ) || "/".equals( this.relativePath )) {
                return true;
            }
            boolean result = evt.path.startsWith( relativePath );
            if (result) {
                int ep = evt.path.length();
                int rp = relativePath.length();
                if (ep > rp) {
                    char c = evt.path.charAt( relativePath.length() );
                    result = c == '/';
                }
            }
            return result;
        }

        public boolean deliver ( Event event, String path ) {
            FileObserver o = get();
            boolean result = o != null;
            if (result) {
                o.onEvent( event.type, path );
            }
            return result;
        }

        private Integer countSlashes () {
            int result = 0;
            for (char c : relativePath.toCharArray()) {
                if (c == '/') { //NOI18N
                    result++;
                }
            }
            return result;
        }

        @Override
        public int compareTo ( Entry o ) {
            return -countSlashes().compareTo( o.countSlashes() );
        }
    }

    public static class Event {
        private final EventType type;
        private final String path;

        public Event ( EventType type, String path ) {
            this.type = type;
            this.path = path;
        }

        @Override
        public int hashCode () {
            int hash = 7;
            hash = 83 * hash + (this.type != null ? this.type.hashCode() : 0);
            hash = 83 * hash + (this.path != null ? this.path.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals ( Object obj ) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Event other = (Event) obj;
            if (this.type != other.type) {
                return false;
            }
            if ((this.path == null) ? (other.path != null) : !this.path.equals( other.path )) {
                return false;
            }
            return true;
        }

        @Override
        public String toString () {
            return type + ": " + path;
        }
    }

    public interface FileObserver {
        public void onEvent ( EventType type, String path );
    }

    public enum EventType {
        CHANGE,
        DELETED,
        NEW_CHILD
    }
}
