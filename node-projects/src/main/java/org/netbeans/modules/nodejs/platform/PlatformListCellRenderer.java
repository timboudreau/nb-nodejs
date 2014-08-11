/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.nodejs.platform;

import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.UIManager;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;

/**
 *
 * @author Tim Boudreau
 */
public final class PlatformListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent ( JList<?> jlist, Object o, int i, boolean bln, boolean bln1 ) {
        if (o instanceof NodeJSExecutable) {
            NodeJSExecutable n = (NodeJSExecutable) o;
            o = n.displayName();
            if (n.isValid()) {
                setForeground( UIManager.getColor( "textText" ) );//NOI18N
            } else {
                Color err = UIManager.getColor( "nb.errorColor" );
                if (err == null) {
                    err = Color.RED;
                }
                setForeground( err );
            }
        }
        return super.getListCellRendererComponent( jlist, o, i, bln, bln1 ); //To change body of generated methods, choose Tools | Templates.
    }

}
