/*
 Copyright (C) 2013 Tim Boudreau

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
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.netbeans.modules.nodejs.api;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.openide.LifecycleManager;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages("UNKNOWN_VERSION=(unknown version)")
public abstract class NodeJSExecutable {

    public String name () {
        return getClass().getSimpleName();
    }
    
    public String displayName() {
        return name();
    }

    public boolean isValid () {
        return true;
    }
    
    public String version() {
        return Bundle.UNKNOWN_VERSION();
    }
    
    public abstract String path();
    
    public ExecutionDescriptor.LineConvertorFactory newLineConverter() {
        return new LineConverter(this.getSourcesLocation());
    }

    public static NodeJSExecutable getDefault () {
        NodeJSExecutable exe = Lookup.getDefault().lookup( NodeJSExecutable.class );
        if (exe == null) {
            exe = new NodeJSExecutable.DummyExectable();
        }
        return exe;
    }

    public final Future<Integer>  run ( FileObject targetFile, String args ) throws IOException {
        if (!targetFile.isValid() || !targetFile.isData()) {
            StatusDisplayer.getDefault().setStatusText( NbBundle.getMessage(
                    NodeJSExecutable.class, "MSG_CANNOT_RUN", targetFile.getPath() ) );
            Toolkit.getDefaultToolkit().beep();
        }
        assert !EventQueue.isDispatchThread();
        LifecycleManager.getDefault().saveAll();
        return doRun( targetFile, args );
    }

    protected abstract Future<Integer> doRun ( FileObject file, String args ) throws IOException;

    public abstract String getSourcesLocation ();

    public abstract void stopRunningProcesses ( Lookup.Provider owner );

    static final class DummyExectable extends NodeJSExecutable {
        @Override
        protected Future<Integer> doRun ( FileObject file, String args ) throws IOException {
            return new Future<Integer>() {
                @Override
                public boolean cancel ( boolean bln ) {
                    return false;
                }

                @Override
                public boolean isCancelled () {
                    return false;
                }

                @Override
                public boolean isDone () {
                    return true;
                }

                @Override
                public Integer get () throws InterruptedException, ExecutionException {
                    return 1;
                }

                @Override
                public Integer get ( long l, TimeUnit tu ) throws InterruptedException, ExecutionException, TimeoutException {
                    return 1;
                }
            };
        }

        @Override
        public String getSourcesLocation () {
            return null;
        }

        @Override
        public void stopRunningProcesses ( Lookup.Provider owner ) {

        }

        @Override
        public String path () {
            return "";
        }
    }
}
