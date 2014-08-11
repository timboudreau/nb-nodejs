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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

@ActionID (
        category = "Tools",
        id = "org.netbeans.modules.nodejs.platform.NodeJSPlatformsAction"
)
@ActionRegistration (
        displayName = "#CTL_NodeJSPlatformsAction"
)
@ActionReference (path = "Menu/Tools", position = 437)
@Messages ({"CTL_NodeJSPlatformsAction=Node JS Platforms", "TTL_Platforms=NodeJS Platforms"})
public final class NodeJSPlatformsAction implements ActionListener {
    @Override
    public void actionPerformed ( ActionEvent e ) {
        DialogDescriptor desc = new DialogDescriptor( new NodeJSPlatformsPanel(),
                NbBundle.getMessage( NodeJSPlatformsAction.class, "TTL_Platforms" ), false,
                new Object[]{DialogDescriptor.CLOSED_OPTION}, DialogDescriptor.CLOSED_OPTION,
                DialogDescriptor.DEFAULT_ALIGN, HelpCtx.DEFAULT_HELP, null );
        DialogDisplayer.getDefault().notify( desc );
    }
}
