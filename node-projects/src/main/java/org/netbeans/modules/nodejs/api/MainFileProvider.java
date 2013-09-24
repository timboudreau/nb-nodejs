package org.netbeans.modules.nodejs.api;

import org.openide.filesystems.FileObject;

/**
 *
 * @author Tim Boudreau
 */
public interface MainFileProvider {
    public FileObject getMainFile();
}
