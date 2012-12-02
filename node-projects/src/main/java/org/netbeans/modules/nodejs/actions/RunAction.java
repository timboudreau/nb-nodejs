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
package org.netbeans.modules.nodejs.actions;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import org.netbeans.api.actions.Savable;
import org.netbeans.modules.nodejs.NodeJSExecutable;
import org.openide.loaders.DataObject;

import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

@ActionID (category = "Tools",
id = "org.netbeans.modules.nodejs.actions.RunAction")
@ActionRegistration (displayName = "#CTL_RunAction")
@ActionReferences ({
    @ActionReference (path = "Loaders/text/javascript/Actions", position = 250, separatorBefore = 225, separatorAfter = 275),
    @ActionReference (path = "Editors/text/javascript/Popup", position = 4950, separatorBefore = 4925, separatorAfter = 4975)
})
@Messages ("CTL_RunAction=Run With Node")
public final class RunAction implements ActionListener, Runnable {
    private final DataObject context;

    public RunAction ( DataObject context ) {
        this.context = context;
    }

    public void actionPerformed ( ActionEvent ev ) {
        RequestProcessor.getDefault().post( this );
    }

    @Override
    public void run () {
        try {
            Savable save = context.getLookup().lookup( Savable.class );
            if (save != null) {
                save.save();
            }
            NodeJSExecutable.getDefault().run( context.getPrimaryFile(), null );
        } catch ( IOException ex ) {
            Exceptions.printStackTrace( ex );
        }
    }
}
