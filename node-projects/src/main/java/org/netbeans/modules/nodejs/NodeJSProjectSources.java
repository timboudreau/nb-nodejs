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

import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public class NodeJSProjectSources implements Sources, SourceGroup {
    private final NodeJSProject project;
    private final ChangeSupport supp = new ChangeSupport( this );

    public NodeJSProjectSources ( NodeJSProject project ) {
        this.project = project;
    }

    @Override
    public SourceGroup[] getSourceGroups ( String type ) {
        return new SourceGroup[]{this};
    }

    @Override
    public void addChangeListener ( ChangeListener listener ) {
        supp.addChangeListener( listener );
    }

    @Override
    public void removeChangeListener ( ChangeListener listener ) {
        supp.removeChangeListener( listener );
    }

    @Override
    public FileObject getRootFolder () {
        return project.getProjectDirectory();
    }

    @Override
    public String getName () {
        return Sources.TYPE_GENERIC;
    }

    @Override
    public String getDisplayName () {
        return NbBundle.getMessage( NodeJSProjectSources.class, "SOURCES" );
    }

    @Override
    public Icon getIcon ( boolean opened ) {
        return null;
    }

    @Override
    public boolean contains ( FileObject file ) throws IllegalArgumentException {
        FileObject srcDir = project.getLookup().lookup( NodeJSProjectProperties.class ).getSourceDir();
        return !srcDir.isValid() ? false : FileUtil.isParentOf( srcDir, file );
    }

    @Override
    public void addPropertyChangeListener ( PropertyChangeListener listener ) {
        //do nothing
    }

    @Override
    public void removePropertyChangeListener ( PropertyChangeListener listener ) {
        //do nothing
    }
}
