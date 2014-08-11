/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.nodejs.platform;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;

/**
 *
 * @author tim
 */
public final class PlatformListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent ( JList<?> jlist, Object o, int i, boolean bln, boolean bln1 ) {
        if (o instanceof NodeJSExecutable) {
            NodeJSExecutable n = (NodeJSExecutable) o;
            o = n.displayName();
        }
        return super.getListCellRendererComponent( jlist, o, i, bln, bln1 ); //To change body of generated methods, choose Tools | Templates.
    }
    
}
