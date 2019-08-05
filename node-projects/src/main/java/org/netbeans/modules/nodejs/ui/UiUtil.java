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
import java.util.function.Consumer;
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

    private static void splitOnCaps ( String key, Consumer<String> c ) {
        int max = key.length();
        if (max == 0) {
            c.accept( key );
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean lastWasUpper = true;
        for (int i = 0; i < max; i++) {
            char ch = key.charAt( i );
            boolean isUpper = Character.isUpperCase( ch );
            if (i != 0 && sb.length() > 0 && !lastWasUpper && isUpper) {
                c.accept( sb.toString() );
                sb.setLength( 0 );
            }
            sb.append( ch );
            lastWasUpper = isUpper;
        }
        if (sb.length() > 0) {
            c.accept( sb.toString() );
        }
    }

    private static void splitOnSeparators ( String s, Consumer<String> c ) {
        String[] parts = s.split( "[\\s_\\-]" ); //NOI18N
        for (int i = 0; i < parts.length; i++) {
            String word = parts[i].trim();
            if (word.isEmpty()) {
                continue;
            }
            char[] chars = word.toLowerCase().toCharArray();
            if (chars.length > 0) {
                chars[0] = Character.toUpperCase( chars[0] );
            }
            c.accept( new String( chars ) );
        }
    }

    public static String toMenuTitle ( String key ) {
        StringBuilder sb = new StringBuilder();
        splitOnCaps( key, capSplitPart -> {
            splitOnSeparators( capSplitPart, word -> {
                if (sb.length() > 0) {
                    sb.append( ' ' );
                }
                sb.append( word );
            } );
        } );
        return sb.toString();
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
                if (text.contains( "&" )) {
                    Mnemonics.setLocalizedText( (JLabel) c, text );
                }
            } else if (c instanceof AbstractButton) {
                String text = ((AbstractButton) c).getText();
                if (text.contains( "&" )) {
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
