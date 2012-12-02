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
package org.netbeans.modules.nodejs.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.MissingResourceException;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public class UiUtil {
    private UiUtil () {
    }

    public static void prepareComponents ( Container container ) {
        prepareComponents( container, container );
    }

    private static void prepareComponents ( Container container, Container outer ) {
        for (Component c : container.getComponents()) {
            String name = c.getName();
            if (name != null) {
                try {
                    String newName = NbBundle.getMessage( outer.getClass(), name );
                    c.setName( newName );
                } catch ( MissingResourceException e ) {
                    System.out.println( name + '=' );
                    continue;
                }
            }
            if (c instanceof JLabel) {
                String text = ((JLabel) c).getText();
                if (text.indexOf( "&" ) >= 0) {
                    Mnemonics.setLocalizedText( (JLabel) c, text );
                }
            } else if (c instanceof AbstractButton) {
                String text = ((AbstractButton) c).getText();
                if (text.indexOf( "&" ) >= 0) {
                    Mnemonics.setLocalizedText( (AbstractButton) c, text );
                }
            }
            if (c instanceof Container) {
                if (!c.getClass().getName().startsWith( "java" )) { //NOI18N
                    prepareComponents( (Container) c, outer );
                }
            }
            if (c instanceof JTextField) {
                final JTextField jtf = (JTextField) c;
                jtf.addFocusListener( new FocusListener() {
                    @Override
                    public void focusGained ( FocusEvent e ) {
                        jtf.selectAll();
                    }

                    @Override
                    public void focusLost ( FocusEvent e ) {
                        //do nothing
                    }
                } );
            }
        }
    }
}
