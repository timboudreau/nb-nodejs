package org.netbeans.modules.nodejs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.netbeans.modules.nodejs.json.ObjectMapperProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 * Encapsulates data stored in .nbinfo next to package.json - things that are
 * either machine-specific, netbeans-specific, or have no analogue in
 * package.json
 *
 * @author Tim Boudreau
 */
final class NbInfo implements Runnable {
    private final RequestProcessor.Task task;
    private final NodeJSProject prj;
    private String platform;
    private String runArguments;
    private boolean initialized;
    private volatile boolean fileFound;

    NbInfo ( NodeJSProject prj ) {
        this.prj = prj;
        RequestProcessor rp = NodeJSProject.NODE_JS_PROJECT_THREAD_POOL;
        task = rp.create( this );
    }

    private void checkInit () {
        if (!initialized) {
            initialized = true;
            load();
        }
    }

    void setPlatformName ( String platform ) {
        checkInit();
        synchronized ( this ) {
            this.platform = platform;
        }
        task.schedule( 1000 );
    }

    void setRunArguments ( String args ) {
        checkInit();
        synchronized ( this ) {
            this.runArguments = runArguments;
        }
        task.schedule( 1000 );
    }

    public String getRunArguments () {
        checkInit();
        synchronized ( this ) {
            return runArguments;
        }
    }

    public String getPlatformName () {
        checkInit();
        synchronized ( this ) {
            return platform;
        }
    }

    boolean hasFile () {
        return fileFound;
    }

    public void run () {
        try {
            FileObject fo = prj.getProjectDirectory().getFileObject( ".nbinfo" );
            if (fo == null) {
                fo = prj.getProjectDirectory().createData( ".nbinfo" );
            }
            Map<String, String> m = new LinkedHashMap<>();
            synchronized ( this ) {
                if (platform != null) {
                    m.put( "platformName", platform );
                }
                if (runArguments != null) {
                    m.put( "arguments", runArguments );
                }
            }
            try (OutputStream out = fo.getOutputStream()) {
                ObjectMapperProvider.newObjectMapper().writeValue( out, m );
            }
        } catch ( IOException ex ) {
            Exceptions.printStackTrace( ex );
        }
    }

    private void load () {
        FileObject fo = prj.getProjectDirectory().getFileObject( ".nbinfo" );
        if (fo != null) {
            fileFound = true;
            Map<String, String> loadedData = null;
            try (InputStream in = fo.getInputStream()) {
                loadedData = ObjectMapperProvider.newObjectMapper().readValue( in, Map.class );
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            } finally {
                if (loadedData != null) {
                    synchronized ( this ) {
                        platform = loadedData.get( "platformName" );
                        runArguments = loadedData.get( "arguments" );
                    }
                }
            }
        }
    }
}
