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
package org.netbeans.modules.nodejs.platform;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.netbeans.modules.nodejs.api.LaunchSupport;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakSet;

/**
 * Native NodeJS platform with specified location
 *
 * @author Tim Boudreau
 */
public final class NativeNodeJS extends NodeJSExecutable {
    private final String name;

    private final String sourceLocation;
    private final String binaryLocation;
    private final Set<Future<Integer>> futures = new WeakSet<>();
    private final String displayName;
    private final String version;

    public NativeNodeJS ( String name, String sourceLocation, String binaryLocation, String displayName, String version ) {
        this.name = name;
        this.sourceLocation = sourceLocation;
        this.binaryLocation = binaryLocation;
        this.displayName = displayName;
        this.version = version;
    }

    public String version () {
        return version;
    }

    public String displayName () {
        return displayName == null ? name() : displayName;
    }

    public String toString () {
        return name + " in " + sourceLocation;
    }

    @Override
    public String name () {
        return name;
    }

    public String path () {
        return binaryLocation == null ? "" : binaryLocation;
    }

    @Override
    public boolean isValid () {
        return new File( binaryLocation ).exists();
    }

    private final LaunchSupport ls = new LaunchSupport( this ) {
        @Override
        protected String[] getLaunchCommandLine ( boolean showDialog, Map<String, String> env ) {
            return new String[]{binaryLocation};
        }
    };

    @Override
    protected Future<Integer> doRun ( final FileObject file, String args ) throws IOException {
        return ls.doRun( file, args );
    }

    @Override
    public String getSourcesLocation () {
        return sourceLocation;
    }

    @Override
    public void stopRunningProcesses ( Lookup.Provider owner ) {
        ls.stopRunningProcesses( owner );
    }

}
