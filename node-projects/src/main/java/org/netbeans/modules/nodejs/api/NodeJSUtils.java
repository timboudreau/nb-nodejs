package org.netbeans.modules.nodejs.api;

import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Minimal API for lookup up other javascript files referenced from a 
 * javascript file
 *
 * @author Tim Boudreau
 */
public abstract class NodeJSUtils {
    /**
     * Get the default instance
     * @return A NodeJSUtils
     */
    public static NodeJSUtils getDefault () {
        return Lookup.getDefault().lookup( NodeJSUtils.class );
    }
    
    /**
     * Resolve a FileObject based on a name referenced by a call to, e.g.,
     * <code>require('http')</code> (matches a built-in library or a stub
     * version of it) or <code>require('./foo.js')</code> (resolves relative
     * to the file) or if the file is part of a NodeJS project, may resolve
     * the main file of a library under its <code>node_modules</code> directory.
     * 
     * @param name The literal name of the module (i.e. should not end with .js)
     * @param relativeTo The file that contains the reference
     * @return A FileObject or null
     */
    public final FileObject resolve(String name, FileObject relativeTo) {
        FileObject result = resolveImpl(name, relativeTo);
        if (result != null && relativeTo.equals(result)) {
            // Don't allow endless parse loops because a module imports
            // itself - illegal but possible
            result = null;
        }
        return result;
    }
    /**
     * Resolve the reference
     * @param name The name, no .js included
     * @param relativeTo The file to resolve relative to, which may be handled
     * specially if it is part of a NodeJS project
     * @return The file object or null
     */
    protected abstract FileObject resolveImpl(String name, FileObject relativeTo);
}
