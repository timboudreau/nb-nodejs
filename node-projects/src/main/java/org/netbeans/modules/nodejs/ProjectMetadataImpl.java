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

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.nodejs.json.ObjectMapperProvider;
import org.netbeans.modules.nodejs.json.SimpleJSONParser;
import org.netbeans.modules.nodejs.json.SimpleJSONParser.JsonException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem.AtomicAction;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.openide.util.NbBundle;
import org.openide.util.NbCollections;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
@SuppressWarnings ("unchecked")
public final class ProjectMetadataImpl extends FileChangeAdapter implements ProjectMetadata {
    private final PropertyChangeSupport supp = new PropertyChangeSupport( this );
    private final Project project;
    private static final RequestProcessor rp = new RequestProcessor( "node.js project metadata saver", 1, true );

    public ProjectMetadataImpl ( Project project ) {
        this.project = project;
    }

    @Override
    public String getValue ( String key ) {
        if (key.indexOf( '.' ) > 0) {
            List<String> keys = new ArrayList<>( Arrays.asList( key.split( "\\." ) ) );
            Object result = getValue( getMap(), keys );
            synchronized ( this ) {
                if (map.isEmpty()) {
                    map = null;
                }
            }
            return toString( result );
        } else {
            return toString( getMap().get( key ) );
        }
    }

    public List<?> getValues ( String key ) {
        Object result;
        if (key.indexOf( '.' ) > 0) {
            List<String> keys = new ArrayList<>( Arrays.asList( key.split( "\\." ) ) );
            result = getValue( getMap(), keys );
        } else {
            result = getMap().get( key );
        }
        synchronized ( this ) {
            if (map.isEmpty()) {
                map = null;
            }
        }
        if (result instanceof List) {
            return (List<?>) result;
        } else if (result instanceof Map) {
            return Arrays.asList( toString( result ) );
        } else if (result instanceof String) {
            return Arrays.asList( (String) result );
        } else {
            return Collections.emptyList();
        }
    }

    private String toString ( Object o ) {
        if (o instanceof Map) {
            try {
                return ObjectMapperProvider.newObjectMapper().writeValueAsString( o );
            } catch ( JsonGenerationException ex ) {
                Exceptions.printStackTrace( ex );
            } catch ( JsonMappingException ex ) {
                Exceptions.printStackTrace( ex );
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            }
            return "" + o;
        } else if (o instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<?> it = ((List<?>) o).iterator(); it.hasNext();) {
                sb.append( toString( it.next() ) );
                if (it.hasNext()) {
                    sb.append( ", " );
                }
            }
        } else if (o instanceof CharSequence) {
            return o.toString();
        } else if (o == null) {
            return "";
        }
        return o.toString();
    }

    @Override
    public void clearValue ( String key ) {
        if (key.indexOf( "." ) >= 0) {
            List<String> keys = new ArrayList<>( Arrays.asList( key.split( "\\." ) ) );
            String last = keys.get( keys.size() - 1 );
            keys.remove( keys.size() - 1 );
            Map<?, ?> result = (Map<?, ?>) getValue( getMap(), keys );
            if (result != null) {
                if (result.containsKey( last )) {
                    Object o = result.remove( last );
                    if (o != null) {
                        queueSave();
                    }
                }
            }
        }
    }

    @SuppressWarnings ("unchecked")
    private Object getValue ( Map<String, Object> m, List<String> keys ) {
        String next = keys.remove( 0 );
        if (keys.isEmpty()) {
            Object result = m.get( next );
            return result;
        } else {
            Object o = m.get( next );
            if (o instanceof Map) {
                return getValue( (Map<String, Object>) o, keys );
            } else {
                return toString( o );
            }
        }
    }
    private volatile Map<String, Object> map;
    private volatile boolean hasErrors;
    private volatile boolean listening;
    private final ReentrantLock lock = new ReentrantLock();

    private Map<String, Object> load ( FileObject fo ) throws IOException {
        if (!fo.isValid()) {
            Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING, "Project root dir became invalid" );
            return new LinkedHashMap<>();
        }
        lock.lock();
        boolean err = false;
        try {
            synchronized ( this ) {
                if (map != null) {
                    return map;
                }
            }
            InputStream in = fo.getInputStream();
            try {
                try {
                    return ObjectMapperProvider.newObjectMapper().readValue( in, Map.class );
                } finally {
                    in.close();
                }
            } catch ( FileStateInvalidException inv ) {
                Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.INFO,
                        "Invalid package.json" );
                return new LinkedHashMap<>();
            } catch ( IOException ex ) {
                Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.INFO,
                        "Bad package.json in " + fo.getPath() + " - will try with permissive parser", ex );
            }
            try {
                SimpleJSONParser p = new SimpleJSONParser( true ); //permissive mode - will parse as much as it can
                if (!fo.isValid()) {
                    Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING, "Project root dir became invalid" );
                    return new LinkedHashMap<>();
                }
                in = fo.getInputStream();
                try {
                    Map<String, Object> m = p.parse( fo );
                    ProjectMetadataImpl.this.hasErrors = err = p.hasErrors();
                    synchronized ( this ) {
                        map = Collections.synchronizedMap( m );
                        return map;
                    }
                } finally {
                    in.close();
                }
            } catch ( FileStateInvalidException e ) {
                Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING, "Project root dir became invalid" );
                return new LinkedHashMap<>();
            } catch ( JsonException ex ) {
                Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.INFO,
                        "Bad package.json in " + fo.getPath(), ex );
                return new LinkedHashMap<>();
            }
        } finally {
            lock.unlock();
            if (err) {
                StatusDisplayer.getDefault().setStatusText( NbBundle.getMessage( ProjectMetadataImpl.class, "ERROR_PARSING_PACKAGE_JSON", ProjectUtils.getInformation( project).getDisplayName() ), 3 );
            }
        }
    }

    public final Map<String, Object> getMap () {
        Map<String, Object> result = map;
        if (result == null) {
            synchronized ( this ) {
                result = map;
            }
        }
        if (result == null) {
            final FileObject fo = project.getProjectDirectory().getFileObject( NodeJSProjectFactory.PACKAGE_JSON );
            if (fo == null) {
                return new LinkedHashMap<>();
            }
            if (!listening) {
                listening = true;
                fo.addFileChangeListener( FileUtil.weakFileChangeListener( this, fo ) );
            }
            try {
                result = load( fo );
                synchronized ( this ) {
                    map = result;
                }
            } catch ( IOException ioe ) {
                Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING,
                        "Problems loading " + fo.getPath(), ioe );
                result = new LinkedHashMap<>();
            }
        }
        return result;
    }
    volatile int saveCount;

    @Override
    public void fileChanged ( FileEvent fe ) {
        if (saveCount > 0) {
            saveCount--;
            return;
        }
        map = null;
    }

    public void setValue ( String key, List<String> values ) {
        Object oldValue;
        if (key.indexOf( '.' ) > 0) {
            List<String> keys = new ArrayList<>( Arrays.asList( key.split( "\\." ) ) );
            oldValue = setValues( getMap(), keys, values );
        } else {
            oldValue = getMap().put( key, values );
        }
        if (unequal( oldValue, values )) {
            queueSave();
            supp.firePropertyChange( key, toString( oldValue ), values );
        }
    }

    @Override
    public void setValue ( String key, String value ) {
        Object oldValue;
        if (key.indexOf( '.' ) > 0) {
            List<String> keys = new ArrayList<>( Arrays.asList( key.split( "\\." ) ) );
            oldValue = setValue( getMap(), keys, value );
        } else {
            oldValue = getMap().put( key, value );
        }
        if (unequal( oldValue, value )) {
            queueSave();
            supp.firePropertyChange( key, toString( oldValue ), value );
        }
    }

    private boolean unequal ( Object a, Object b ) {
        if (a == null && b == null) {
            return false;
        }
        if (a == null || b == null) {
            return false;
        }
        return !a.equals( b );
    }

    private Object setValue ( Map<String, Object> m, List<String> keys, String value ) {
        String nextKey = keys.remove( 0 );
        if (keys.isEmpty()) {
            Object result = m.put( nextKey, value );
            if (unequal( value, result )) {
                queueSave();
            }
            return result;
        } else {
            Object o = m.get( nextKey );
            Map<String, Object> nue = null;
            if (o instanceof Map) {
                nue = (Map<String, Object>) o;
            }
            if (nue == null) {
                //if the value was a string, we clobber it here - careful
                nue = new LinkedHashMap<>();
                queueSave();
            }
            if (value == null) {
                m.remove( nextKey );
            } else {
                m.put( nextKey, nue );
            }
            return setValue( nue, keys, value );
        }
    }

    private Object setValues ( Map<String, Object> m, List<String> keys, List<String> values ) {
        String nextKey = keys.remove( 0 );
        if (keys.isEmpty()) {
            Object result = m.put( nextKey, values );
            if (unequal( values, result )) {
                queueSave();
            }
            return result;
        } else {
            Map<String, Object> nue = (Map<String, Object>) m.get( nextKey );
            if (nue == null) {
                nue = new LinkedHashMap<>();
                queueSave();
            }
            if (values == null) {
                m.remove( nextKey );
            } else {
                m.put( nextKey, nue );
            }
            return setValues( nue, keys, values );
        }
    }

    @Override
    public String toString () {
        try {
            return ObjectMapperProvider.newObjectMapper().writeValueAsString( map );
        } catch ( IOException ex ) {
            Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING, "Bad metadata in project " + project.getProjectDirectory().getPath(), ex );
            return SimpleJSONParser.out( getMap() ).toString();
        }
    }
    
    private static Map<String,Object> copyPruningEmptyValues(Map<String,Object> map) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (Map.Entry<String,Object> e : map.entrySet()) {
            Object o = e.getValue();
            // Prune empty values
            if (o == null || o instanceof String && ((String) o).trim().isEmpty()) {
                continue;
            }
            if (o instanceof Map<?,?>) {
                Map<String,Object> m = (Map<String,Object>) o;
                Map<String,Object> copy = copyPruningEmptyValues(m);
                if (!copy.isEmpty()) {
                    result.put( e.getKey(), copy);
                }
            } else {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    @Override
    public void save () throws IOException {
        assert !EventQueue.isDispatchThread();
        if (this.map != null) {
            if (hasErrors) {
                NotifyDescriptor nd = new NotifyDescriptor.Confirmation( NbBundle.getMessage( ProjectMetadataImpl.class, "OVERWRITE_BAD_JSON", ProjectUtils.getInformation( project).getDisplayName() ) );
                if (!DialogDisplayer.getDefault().notify( nd ).equals( NotifyDescriptor.OK_OPTION )) {
                    synchronized ( this ) {
                        map = null;
                    }
                    return;
                }
            }
            if (!project.getProjectDirectory().isValid()) {
                Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING, "Project root dir became invalid" );
                return;
            }
            final FileObject fo = project.getProjectDirectory().getFileObject( NodeJSProjectFactory.PACKAGE_JSON );
            if (!fo.isValid()) {
                Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING, "Project root dir became invalid" );
                return;
            }
            project.getProjectDirectory().getFileSystem().runAtomicAction( new AtomicAction() {
                @Override
                public void run () throws IOException {
                    FileObject save = fo;
                    if (save == null) {
                        save = project.getProjectDirectory().createData( NodeJSProjectFactory.PACKAGE_JSON );
                    }
                    final FileObject writeTo = save;
                    try {
                        ProjectManager.mutex().writeAccess( new Mutex.ExceptionAction<Void>() {
                            @Override
                            public Void run () throws Exception {
                                Map<String,Object> writeOut = copyPruningEmptyValues(map);
                                CharSequence seq = ObjectMapperProvider.newObjectMapper()
                                        .writeValueAsString( writeOut );
                                try (OutputStream out = writeTo.getOutputStream()) {
                                    out.write(seq.toString().getBytes( "UTF-8" ));
                                    out.write( "\n".getBytes( "UTF-8" ) );
                                    task.cancel();
                                } catch ( FileAlreadyLockedException e ) {
                                    Logger.getLogger( ProjectMetadataImpl.class.getName() ).log(
                                            Level.INFO, "Could not save properties for {0} - queue for later",
                                            project.getProjectDirectory().getPath() );
                                    queueSave();
                                } finally {
                                    synchronized ( ProjectMetadataImpl.this ) { //tests
                                        ProjectMetadataImpl.this.notifyAll();
                                    }
                                    saveCount++;
                                }
                                hasErrors = false;
                                return null;
                            }
                        } );
                    } catch ( MutexException e ) {
                        if (e.getCause() instanceof FileStateInvalidException) {
                            Logger.getLogger( ProjectMetadataImpl.class.getName() ).log( Level.WARNING, "Project root dir became invalid", e );
                        } else if (e.getCause() instanceof IOException) {
                            throw (IOException) e.getCause();
                        } else if (e.getCause() instanceof RuntimeException) {
                            throw (RuntimeException) e.getCause();
                        } else if (e.getCause() instanceof Error) {
                            throw (Error) e.getCause();
                        } else {
                            throw new AssertionError( e );
                        }
                    }
                }
            } );
        } else {
            System.out.println( "Map is null, cannot save" );
        }
    }

    @Override
    public void addMap ( String key, Map<String, Object> m ) {
        Map into = getMap();
        if (key == null && into != m) {
            into.putAll( m );
            supp.firePropertyChange( null, null, null );
        } else {
            assert m != into : "Cannot add map to itself";
            into.put( key, m );
            supp.firePropertyChange( key, null, null );
        }
        System.out.println( "METADATA NOW: " + into );
        queueSave();
    }

    @Override
    public Map<String, Object> getMap ( String key ) {
        if (key == null) {
            return getMap();
        }
        Object o = getMap().get( key );
        if (o instanceof Map) {
            return NbCollections.checkedMapByFilter( (Map) o, String.class, Object.class, false );
        }
        return null;
    }

    class R implements Runnable {
        @Override
        public void run () {
            try {
                save();
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            }
        }
    }
    private final Task task = rp.create( new R() );

    private void queueSave () {
        task.schedule( 1000 );
    }

    @Override
    public void addPropertyChangeListener ( PropertyChangeListener pcl ) {
        supp.addPropertyChangeListener( WeakListeners.propertyChange( pcl, supp ) );
    }
}
