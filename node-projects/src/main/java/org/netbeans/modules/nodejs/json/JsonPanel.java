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
package org.netbeans.modules.nodejs.json;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import org.netbeans.modules.nodejs.json.SimpleJSONParser.JsonException;
import org.openide.filesystems.FileObject;
import org.openide.util.NbCollections;

public final class JsonPanel extends JPanel {
    public JsonPanel ( FileObject fo ) throws JsonException, IOException {
        this( parse( fo ), null );
    }

    private static Map<String, Object> parse ( FileObject fo ) throws FileNotFoundException, IOException {
        InputStream in = fo.getInputStream();
        try {
            return NbCollections.checkedMapByFilter( ObjectMapperProvider.newObjectMapper().readValue( in, Map.class ), String.class, Object.class, false );
        } catch ( Exception e ) {
            Logger.getLogger( JsonPanel.class.getName() ).log( Level.WARNING, "Bad json in " + fo.getPath(), e );
            in = fo.getInputStream();
            try {
                try {
                    return new SimpleJSONParser( true ).parse( in );
                } catch ( JsonException ex ) {
                    Logger.getLogger( JsonPanel.class.getName() ).log( Level.WARNING, "Bad json in " + fo.getPath(), e );
                    return new HashMap<String, Object>();
                }
            } finally {
                in.close();
            }
        } finally {
            in.close();
        }
    }

    private String capitalize ( String s ) {
        StringBuilder sb = new StringBuilder( s );
        sb.setCharAt( 0, Character.toUpperCase( sb.charAt( 0 ) ) );
        return sb.toString();
    }

    public JsonPanel ( Map<String, Object> map, String nm ) {
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        Object name = map.get( "name" );
        Object desc = map.get( "description" );
        if (nm != null) {
            name = capitalize( nm );
        }
        boolean nameUsed = name instanceof String;
        boolean descUsed = desc instanceof String;
        if (nameUsed) {
            JLabel l = new JLabel( capitalize( name.toString() ) );
            if (nm == null) {
                l.setFont( l.getFont().deriveFont( 24F ) );
            } else {
                l.setFont( l.getFont().deriveFont( 14F ) );
            }
            if (!descUsed) {
                l.setBorder( new MatteBorder( 0, 0, 1, 0, UIManager.getColor( "controlShadow" ) ) );
            }
            add( l );
        }
        if (descUsed) {
            JLabel l = border( new JLabel( desc.toString() ) );
            if (nameUsed) {
                l.setBorder( new MatteBorder( 0, 0, 1, 0, UIManager.getColor( "controlShadow" ) ) );
            }
            add( l );
        }
        if (nameUsed || descUsed) {
            JLabel lbl = new JLabel( "   " );
            add( lbl );
        }
        for (String k : sortKeys( map )) {
            Object o = map.get( k );
            k = capitalize( k );
            if ("description".equals( k ) && descUsed) {
                continue;
            }
            if ("name".equals( k ) && nameUsed) {
                continue;
            }
            if (o instanceof String) {
                JLabel l = new JLabel( k + ":  " + o );
                add( l );
            } else if (o instanceof List) {
                StringBuilder sb = new StringBuilder( k ).append( ":  " );
                for (Iterator<?> it = ((List<?>) o).iterator(); it.hasNext();) {
                    sb.append( it.next() );
                    if (it.hasNext()) {
                        sb.append( ", " );
                    }
                }
                JLabel lbl = border( new JLabel( sb.toString() ) );
                add( lbl );
            } else if (o instanceof Map) {
                Map<String, Object> asMap = NbCollections.checkedMapByFilter(
                        (Map<?, ?>) o, String.class, Object.class, false );
                add( new JsonPanel( asMap, capitalize( k ) ) );
            }
        }
        setBorder( new EmptyBorder( 12, 24, 12, 12 ) );
    }
    private static final Border b = new EmptyBorder( 5, 0, 5, 0 );

    private JLabel border ( JLabel lbl ) {
        lbl.setBorder( b );
        return lbl;
    }

    List<String> sortKeys ( Map<String, ?> m ) {
        List<String> l = new ArrayList<String>( m.keySet() );
        Collections.sort( l, new C( m ) );
        return l;
    }

    private static class C implements Comparator<String> {
        private final Map<String, ?> m;

        public C ( Map<String, ?> m ) {
            this.m = m;
        }

        @Override
        public int compare ( String o1, String o2 ) {
            if (o1.equals( o2 )) {
                return 0;
            }
            Object a = m.get( o1 );
            Object b = m.get( o2 );
            if (a.getClass() == b.getClass()) {
                return 0;
            }
            if ((a.getClass() != String.class && a.getClass() != List.class) && b.getClass() == String.class) {
                return 1;
            }
            if ((a.getClass() == String.class || a.getClass() == List.class) && b.getClass() != String.class) {
                return -1;
            }
            return 0;

        }
    }
}
