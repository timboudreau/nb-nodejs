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

import static org.netbeans.modules.nodejs.json.SimpleJSONParser.S.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

/**
 * A trivial JSON parser. Generally should not be used, and exists only because
 * early versions of the node.js module could not use Jackson. However, it does
 * have a "permissive" mode in which it will keep as much of the tree as it was
 * able to parse.
 *
 * @author Tim Boudreau
 */
public final class SimpleJSONParser {
    private boolean permissive;
    boolean thrown = false;

    public SimpleJSONParser () {
        this( false );
    }

    public SimpleJSONParser ( boolean permissive ) {
        setPermissive( permissive );
    }

    void setPermissive ( boolean val ) {
        this.permissive = val;
    }

    public boolean hasErrors () {
        return thrown;
    }

    public CharSequence toJSON ( Properties properties ) {
        StringBuilder sb = new StringBuilder( "{\n" );
        for (String key : properties.stringPropertyNames()) {
            sb.append( "    " ).append( '"' ).append( key ).append( '"' ).append( '\n' );
        }
        sb.append( "}" );
        return sb.toString();
    }

    public CharSequence toJSON ( Map<String, Object> properties ) {
        return out( properties );
    }

    CharSequence reflectToJSON ( Object o ) {
        StringBuilder sb = new StringBuilder();
        reflectOut( o, sb, 0 );
        return sb;
    }

    public Map<String, Object> parse ( FileObject in ) throws JsonException, IOException {
        return parse( in.asText( "UTF-8" ) );
    }

    public Map<String, Object> parse ( InputStream in ) throws JsonException, IOException {
        return parse( in, "UTF-8" );
    }

    public Map<String, Object> parse ( InputStream in, String encoding ) throws JsonException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copy( in, out );
        String data = encoding == null ? new String( out.toByteArray() ) : new String( out.toByteArray(), encoding );
        return parse( data );
    }

    public Map<String, Object> parse ( CharSequence seq ) throws JsonException {
        CharVisitor v = new CharVisitor();
        State state = new State();
        int len = seq.length();
        int line = 0;
        for (int i = 0; i < len; i++) {
            char c = seq.charAt( i );
            if (c == '\n') {
                line++;
            }
            v.visitChar( c, i, line, state );
        }
        return state.map;
    }

    private final class CharVisitor {
        StringBuilder sb = new StringBuilder();
        S s = S.BEGIN;
        Stack<S> awaits = new Stack<S>();
        char lastChar;
        S stateBeforeComment;

        void setState ( S s, char c, int pos ) {
            stateChange( this.s, s, c, pos );
            this.s = s;
        }

        void visitChar ( char c, int pos, int line, State state ) throws JsonException {
            if (c == '/' && s != IN_ARRAY_ELEMENT && s != IN_KEY && s != IN_VALUE && s != AWAIT_BEGIN_COMMENT && s != IN_COMMENT && s != IN_LINE_COMMENT) {
                stateBeforeComment = s;
                setState( AWAIT_BEGIN_COMMENT, c, pos );
                return;
            }
            if (c == '"' && lastChar == '\\' && (s == IN_KEY || s == IN_VALUE || s == IN_ARRAY_ELEMENT)) {
                if (sb.charAt( sb.length() - 1 ) == '\\') {
                    sb.setLength( sb.length() - 1 ); //manage escaped quotes
                }
                lastChar = c;
                sb.append( c );
                return;
            }
            try {
                switch ( s ) {
                    case BEGIN:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case '{':
                                setState( AWAITING_KEY, c, pos );
                                break;
                            case '/':
                                setState( AWAIT_BEGIN_COMMENT, c, pos );
                                break;
                            default:
                                error( "Expected '{'", c, line, pos );
                        }
                        break;
                    case AWAIT_BEGIN_COMMENT:
                        if (c == '*') {
                            setState( IN_COMMENT, c, pos );
                            break;
                        } else if (c == '/') {
                            setState( IN_LINE_COMMENT, c, pos );
                            break;
                        } else {
                            if (Character.isWhitespace( c )) {
                                break;
                            }
                            error( "Expected / or * awaiting comment marker", c, line, pos );
                        }
                        break;
                    case IN_COMMENT:
                        if (c == '/' && lastChar == '*') {
                            setState( stateBeforeComment, c, pos );
                        }
                        break;
                    case IN_LINE_COMMENT:
                        if (c == '\n') {
                            setState( stateBeforeComment, c, pos );
                        }
                        break;
                    case AWAITING_COMPOUND_VALUE:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case '"':
                                setState( IN_KEY, c, pos );
                                break;
                            case '}':
                                setState( AFTER_VALUE, c, pos );
                                break;
                            default:
                                error( "Expected \" or key awaiting compound value", c, line, pos );
                        }
                        break;
                    case AWAITING_KEY:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case '"':
                                setState( IN_KEY, c, pos );
                                break;
                            case '}':
                                setState( AFTER_VALUE, c, pos );
                                state.exitCompoundValue();
                                break;
                            default:
                                error( "Expected '\"' or whitespace before key", c, line, pos );
                        }
                        break;
                    case IN_KEY:
                        switch ( c ) {
                            case '"':
                                setState( BETWEEN_KEY_AND_VALUE, c, pos );
                                state.enterKey( sb.toString() );
                                sb.setLength( 0 );
                                return;
                            default:
                                sb.append( c );
                        }
                        break;
                    case BETWEEN_KEY_AND_VALUE:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case (':'):
                                setState( S.AWAITING_VALUE, c, pos );
                                break;
                            default:
                                error( "Expected : or whitespace between key and value", c, line, pos );
                        }
                        break;
                    case AWAITING_VALUE:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case ('['):
                                setState( S.AWAITING_ARRAY_ELEMENT, c, pos );
                                state.enterArrayValue();
                                break;
                            case ('"'):
                                setState( IN_VALUE, c, pos );
                                break;
                            case ('{'):
                                setState( AWAITING_COMPOUND_VALUE, c, pos );
                                state.enterCompoundValue();
                                break;
                            case 'f':
                            case 't':
                                sb.append( c );
                                setState( IN_BOOLEAN_VALUE, c, pos );
                                break;
                            case '-':
                            case '.':
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                sb.append( c );
                                setState( IN_NUMERIC_VALUE, c, pos );
                                break;
                            default:
                                error( "Expected '\"' or ':' to start value", c, line, pos );
                        }
                        break;
                    case AWAITING_ARRAY_ELEMENT:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case '{':
                                setState( AWAITING_KEY, c, pos );
                                state.enterCompoundValue(); //XXX
                                break;
                            case '[':
                                setState( AWAITING_ARRAY_ELEMENT, c, pos );
                                state.enterArrayValue();
                                break;
                            case '"':
                                setState( S.IN_ARRAY_ELEMENT, c, pos );
                                break;
                            case 'f':
                            case 't':
                                setState( IN_BOOLEAN_ARRAY_ELEMENT, c, pos );
                                sb.append( c );
                                break;
                            case '-':
                            case '.':
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                setState( IN_NUMERIC_ARRAY_ELEMENT, c, pos );
                                sb.append( c );
                                break;
                            case ']':
                                setState( AFTER_VALUE, c, pos );
                                state.exitArrayValue();
                                break;
                            default:
                                error( "Expected '{' or '\"' awaiting array value", c, line, pos );
                        }
                        break;
                    case IN_ARRAY_ELEMENT:
                        if (c == '"') {
                            setState( AFTER_ARRAY_ELEMENT, c, pos );
                            state.arrayValue( sb.toString() );
                            sb.setLength( 0 );
                            return;
                        }
                        sb.append( c );
                        break;
                    case IN_NUMERIC_ARRAY_ELEMENT:
                        if ((Character.isWhitespace( c )) || (c == ',') || (c == ']')) {
                            if (sb.length() > 0) {
                                state.numericArrayElement( sb.toString() );
                                sb.setLength( 0 );
                            }
                            if (Character.isWhitespace( c )) {
                                setState( AFTER_ARRAY_ELEMENT, c, pos );
                            } else {
                                setState( c == ']' ? AFTER_VALUE : AWAITING_ARRAY_ELEMENT, c, pos );
                                if (c == ']') {
                                    state.exitArrayValue();
                                }
                            }
                            return;
                        }
                        if ((c != '.') && (c != '-') && (!Character.isDigit( c ))) {
                            error( "Invalid character in numeric array element: ", c, line, pos );
                        } else {
                            sb.append( c );
                        }

                        break;
                    case IN_BOOLEAN_ARRAY_ELEMENT:
                        if ((Character.isWhitespace( c )) || (c == ',') || (c == ']')) {
                            if (sb.length() > 0) {
                                state.booleanArrayElement( sb.toString() );
                                sb.setLength( 0 );
                            }
                            if (Character.isWhitespace( c )) {
                                setState( AFTER_ARRAY_ELEMENT, c, pos );
                            } else {
                                setState( c == ']' ? AFTER_VALUE : AWAITING_ARRAY_ELEMENT, c, pos );
                                if (c == ']') {
                                    state.exitArrayValue();
                                }
                            }
                            return;
                        }
                        if ((!"true".startsWith( sb.toString() )) && (!"false".startsWith( sb.toString() ))) {
                            error( "Invalid character in boolean array element for '" + this.sb + "': ", c, line, pos );
                        } else {
                            sb.append( c );
                        }

                        break;
                    case IN_NUMERIC_VALUE:
                        if ((Character.isWhitespace( c )) || (c == ',')) {
                            setState( AWAITING_KEY, c, pos );
                            state.numberValue( sb.toString() );
                            sb.setLength( 0 );
                            return;
                        }
                        if ((Character.isDigit( c )) || (c == '.') || (c == '-')) {
                            if ((sb.indexOf( "." ) >= 0) && (c == '.')) {
                                error( "Extra decimal in number: ", c, line, pos );
                            } else {
                                sb.append( c );
                            }
                        } else {
                            error( "Invalid character in number: ", c, line, pos );
                        }
                        break;
                    case IN_BOOLEAN_VALUE:
                        if ((Character.isWhitespace( c )) || (c == ',')) {
                            setState( AWAITING_KEY, c, pos );
                            state.booleanValue( sb.toString() );
                            sb.setLength( 0 );
                            return;
                        }
                        char lc = sb.length() == 0 ? '\000' : sb.charAt( sb.length() - 1 );
                        switch ( c ) {
                            case 'r':
                                if (lc != 't') {
                                    error( "Invalid character in boolean - lc=" + lc + ": " + this.sb, c, line, pos );
                                } else {
                                    sb.append( c );
                                }
                                break;
                            case 'u':
                                if (lc != 'r') {
                                    error( "Invalid character in boolean: " + this.sb + " lc is " + lc + " - ", c, line, pos );
                                } else {
                                    sb.append( c );
                                }
                                break;
                            case 'e':
                                if ((lc != 'u') && (lc != 's')) {
                                    error( "Invalid character in boolean: ", c, line, pos );
                                } else {
                                    sb.append( c );
                                }
                                break;
                            case 'a':
                                if (lc != 'f') {
                                    error( "Invalid character in boolean: ", c, line, pos );
                                } else {
                                    sb.append( c );
                                }
                                break;
                            case 'l':
                                if (lc != 'a') {
                                    error( "Invalid character in boolean: ", c, line, pos );
                                } else {
                                    sb.append( c );
                                }
                                break;
                            case 's':
                                if (lc != 'l') {
                                    error( "Invalid character in boolean: ", c, line, pos );
                                } else {
                                    sb.append( c );
                                }
                                break;
                            default:
                                error( "Invalid character in boolean: ", c, line, pos );
                        }
                        break;
                    case IN_VALUE:
                        if (c == '"') {
                            setState( S.AFTER_VALUE, c, pos );
                            state.value( sb.toString() );
                            sb.setLength( 0 );
                            return;
                        }
                        sb.append( c );
                        break;
                    case AFTER_VALUE:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case (','):
                                setState( AWAITING_KEY, c, pos );
                                break;
                            case ('}'):
                                state.exitCompoundValue();
                                if (state.hasOuterList()) {
                                    setState( AFTER_ARRAY_ELEMENT, c, pos );
                                } else {
                                    setState( S.AFTER_VALUE, c, pos );
                                }

                                break;
                            case (']'):
                                setState( AFTER_VALUE, c, pos );
                                state.exitArrayValue();
                                break;
                            default:
                                error( "Expected , or EOF after value", c, line, pos );
                        }
                        break;
                    case AFTER_ARRAY_ELEMENT:
                        if (Character.isWhitespace( c )) {
                            return;
                        }
                        switch ( c ) {
                            case ',':
                                setState( AWAITING_ARRAY_ELEMENT, c, pos );
                                break;
                            case ']':
                                state.exitArrayValue();
                                if (state.hasOuterList()) {
                                    setState( AWAITING_ARRAY_ELEMENT, c, pos );
                                } else {
                                    setState( AFTER_VALUE, c, pos );
                                }
                                break;
                            default:
                                error( "Expected , \" or ] after array value", c, line, pos );
                        }
                        break;
                    default:
                        throw new AssertionError( s );
                }
            } catch ( Internal i ) {
                if (!thrown) {
                    JsonException e = new JsonException( s + ": " + i.getMessage(), c, line, pos, i );
                    if (permissive) {
                        Logger.getLogger( SimpleJSONParser.class.getName() ).log( Level.WARNING, null, e );
                    } else {
                        throw e;
                    }
                    thrown = true;
                }
            } catch ( RuntimeException ex ) {
                if (!thrown) {
                    if (permissive) {
                        Logger.getLogger( SimpleJSONParser.class.getName() ).log( Level.WARNING, null, ex );
                    } else {
                        throw ex;
                    }
                    thrown = true;
                }
            }
            lastChar = c;
        }

        void error ( String msg, char what, int line, int pos ) throws JsonException {
            if (thrown) {
                return; //anything can go wrong at this point
            }
            JsonException e = new JsonException( s + " - " + msg, what, line, pos );
            if (permissive) {
                thrown = true;
                Logger.getLogger( SimpleJSONParser.class.getName() ).log( Level.INFO, null, e );
            } else {
                throw e;
            }
        }

        private void stateChange ( S s, S to, char c, int pos ) {
        }
    }

    public enum S {
        AWAIT_BEGIN_COMMENT,
        IN_COMMENT,
        IN_LINE_COMMENT,
        IN_KEY,
        IN_VALUE,
        IN_NUMERIC_VALUE,
        IN_BOOLEAN_VALUE,
        IN_ARRAY_ELEMENT,
        IN_BOOLEAN_ARRAY_ELEMENT,
        IN_NUMERIC_ARRAY_ELEMENT,
        BEGIN,
        AWAITING_KEY,
        AWAITING_COMPOUND_VALUE,
        AWAITING_ARRAY_ELEMENT,
        AFTER_ARRAY_ELEMENT,
        BETWEEN_KEY_AND_VALUE,
        AWAITING_VALUE,
        AFTER_VALUE;

        boolean isAwaitState () {
            switch ( this ) {
                case AWAITING_ARRAY_ELEMENT:
                case AWAITING_VALUE:
                    return true;
            }
            return false;
        }
    }

    private static final class State {
        private final Stack<List<Object>> currList = new Stack<List<Object>>();
        private final Stack<String> currKey = new Stack<String>();
        private final Stack<Map<String, Object>> currMap = new Stack<Map<String, Object>>();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        Map<String, Object> curr = map;
        String lastKey;

        public void enterKey ( String s ) {
            currKey.push( s );
            lastKey = s;
        }

        S state () {
            if (currKey.isEmpty()) {
                if (map.isEmpty()) {
                    return BEGIN;
                } else {
                    return AWAITING_KEY;
                }
            }
            String k = currKey.peek();
            Object o = map.get( k );
            if (o == null) {
                return AWAITING_VALUE;
            } else if (o instanceof Map) {
                return AWAITING_COMPOUND_VALUE;
            } else if (o instanceof List) {
                return AWAITING_ARRAY_ELEMENT;
            }
            return AWAITING_KEY;
        }

        private void numericArrayElement ( String value ) {
            try {
                List<Object> l = null;
                if ((l == null) && (!this.currList.isEmpty())) {
                    l = this.currList.peek();
                } else if (l == null) {
                    throw new SimpleJSONParser.Internal( "No array present for array value " + value );
                }
                l.add( toNumber( value ) );
            } catch ( NumberFormatException nfe ) {
                throw new SimpleJSONParser.Internal( "Bad number '" + value + "'" );
            }
        }

        private void booleanArrayElement ( String value ) {
            List<Object> l = null;
            if ((l == null) && (!this.currList.isEmpty())) {
                l = (List) this.currList.peek();
            } else if (l == null) {
                throw new SimpleJSONParser.Internal( "No array present for array value " + value );
            }
            if ("true".equals( value )) {
                l.add( Boolean.valueOf( true ) );
            } else if ("false".equals( value )) {
                l.add( Boolean.valueOf( false ) );
            } else {
                throw new SimpleJSONParser.Internal( "Illegal boolean value '" + value + "'" );
            }
        }

        private void booleanValue ( String s ) {
            if ("true".equals( s )) {
                String key = (String) this.currKey.pop();
                this.curr.put( key, Boolean.TRUE );
            } else if ("false".equals( s )) {
                String key = this.currKey.pop();
                this.curr.put( key, Boolean.FALSE );
            } else {
                throw new SimpleJSONParser.Internal( "Invalid boolean '" + s + "'" );
            }
        }

        private Number toNumber ( String toString ) {
            Number n;
            if (toString.indexOf( "." ) >= 0) {
                n = Double.valueOf( Double.parseDouble( toString ) );
                if (n.floatValue() == n.doubleValue()) {
                    n = Float.valueOf( n.floatValue() );
                }
            } else {
                n = Long.valueOf( Long.parseLong( toString ) );
                if (n.longValue() == n.intValue()) {
                    n = Integer.valueOf( n.intValue() );
                }
            }
            return n;
        }

        private void numberValue ( String toString ) {
            try {
                String key = this.currKey.pop();
                this.curr.put( key, toNumber( toString ) );
            } catch ( NumberFormatException nfe ) {
                throw new SimpleJSONParser.Internal( "Invalid number '" + toString + "'" );
            }
        }

        public void enterCompoundValue () {
            String key = currKey.isEmpty() ? null : currKey.peek();
            if (key == null) {
                key = lastKey;  //XXX - need better handling of compounds inside arrays
            }
            Map<String, Object> nue = new LinkedHashMap<String, Object>();
            curr.put( key, nue );
            currMap.push( curr );
            curr = nue;
        }

        public void enterArrayValue () {
            String key = currKey.peek();
            List<Object> l = currList.isEmpty() ? null : currList.peek();
            if (l != null) {
                List<Object> nue = new ArrayList<Object>();
                currList.push( nue );
                l.add( nue );
            } else {
                List<Object> nue = new ArrayList<Object>();
                currList.push( nue );
                curr.put( key, nue );
            }
        }

        public void exitArrayValue () {
            String key = currKey.isEmpty() ? lastKey : currKey.pop();
            if (!currList.isEmpty()) {
                currList.pop();
            }
        }

        public void arrayValue ( String value ) {
            String key = currKey.peek();
            List<Object> l = null;
            if (l == null && !currList.isEmpty()) {
                l = currList.peek();
            } else if (l == null) {
                throw new Internal( "No array present for array value " + value );
            }
            l.add( value );
        }

        public void value ( String value ) {
            String key = currKey.pop();
            curr.put( key, value );
        }

        public void exitCompoundValue () {
            if (!currMap.isEmpty()) {
                curr = currMap.pop();
            }
            String s = currKey.isEmpty() ? lastKey : currKey.pop();
        }

        private boolean hasOuterList () {
            return !currList.isEmpty();
        }
    }

    public static CharSequence out ( Map<String, Object> m ) {
        StringBuilder sb = new StringBuilder( "{\n" );
        out( m, sb, 1 );
        sb.append( "}\n" );
        return sb;
    }
    private static final int INDENT_COUNT = 4;

    @SuppressWarnings ("unchecked")
    private static final void out ( List<Object> l, StringBuilder sb, int indent ) {
        char[] indentChars = new char[indent * INDENT_COUNT];
        Arrays.fill( indentChars, ' ' );
        String ind = new String( indentChars );
        String indl = ind + "    ";
        boolean inline = l.isEmpty() || l.get( 0 ) instanceof Boolean || l.get( 0 ) instanceof Number;
        if (!inline) {
            sb.append( '\n' ).append( ind );
        }
        sb.append( '[' );
        if (!inline) {
            sb.append( '\n' );
        }
        int ix = 0;
        for (Iterator<Object> it = l.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof Map) {
                Map<String, Object> mm = (Map<String, Object>) o;
                out( mm, sb, indent + 1 );
            } else if (o instanceof List) {
                out( (List<Object>) o, sb, indent + 1 );
            } else if (((o instanceof Number)) || ((o instanceof Boolean))) {
                sb.append( o );
                if (it.hasNext()) {
                    sb.append( ',' );
                }
            } else if ((o instanceof CharSequence)) {
                String s = new StringBuilder().append( "" ).append( o ).toString().replace( "\"", "\\\"" );
                sb.append( indl ).append( '"' ).append( s ).append( '"' );
                if (it.hasNext()) {
                    sb.append( ',' );
                }
                sb.append( '\n' );
            } else if (o != null) {
                if (o.getClass().isArray()) {
                    if (o.getClass().getComponentType().isPrimitive()) {
                        o = Utilities.toObjectArray( o );
                    }
                    Object[] oo = (Object[]) o;
                    out( Arrays.asList( oo ), sb, indent + 1 );
                } else {
                    reflectOut( o, sb, indent + 1 );
                }
            }
            ix++;
        }
        if (!inline) {
            sb.append( ind );
        }
        sb.append( ']' );
    }

    private static final void reflectOut ( Object o, StringBuilder sb, int indent ) {
        if (indent > 30) {
            return;
        }
        char[] indentChars = new char[indent * INDENT_COUNT];
        Arrays.fill( indentChars, ' ' );
        String ind = new String( indentChars );
        sb.append( ind ).append( '{' ).append( '\n' );
        char[] indentChars2 = new char[indent * INDENT_COUNT];
        Arrays.fill( indentChars2, ' ' );
        String ind2 = new String( indentChars2 );
        Class<?> c = o.getClass();
        Field[] ff = c.getDeclaredFields();
        for (int i = 0; i < ff.length; i++) {
            Field f = ff[i];
            if ((f.getModifiers() & Modifier.STATIC) != 0) {
                continue;
            }
            if (f.getDeclaringClass() == Character.class || f.getDeclaringClass() == String.class
                    || f.getDeclaringClass() == Integer.class || f.getDeclaringClass() == Short.class
                    || f.getDeclaringClass() == Double.class || f.getDeclaringClass() == Long.class
                    || f.getDeclaringClass() == Float.class) {
                continue;
            }
            f.setAccessible( true );
            try {
                Object value = f.get( o );
                if (value instanceof List) {
                    List<Object> l = (List) value;
                    out( l, sb, indent + 1 );
                } else if (value instanceof Map) {
                    Map<String, Object> m = (Map) o;
                    out( m, sb, indent + 1 );
                } else if (value != null && value.getClass().isArray()) {
                    if (value.getClass().getComponentType().isPrimitive()) {
                        o = Utilities.toObjectArray( value );
                    }
                    List<Object> l = Arrays.asList( value );
                    out( l, sb, indent + 1 );
                } else if (value instanceof CharSequence) {
                    sb.append( ind2 ).append( '"' );
                    sb.append( f.getName() );
                    sb.append( '"' ).append( " " ).append( ':' ).append( ' ' );
                    sb.append( '"' );
                    String val = "" + f.get( o );
                    sb.append( val.replace( "\"", "\\\"" ) );
                    sb.append( '"' );
                } else if (value != null && value != o) {
                    reflectOut( value, sb, indent + 1 );
                }
            } catch ( IllegalArgumentException ex ) {
                sb.append( ex.getClass().getSimpleName() );
            } catch ( IllegalAccessException ex ) {
                sb.append( ex.getClass().getSimpleName() );
            }
            if (i < ff.length - 1) {
                sb.append( ',' );
            }
            sb.append( '\n' );
        }
        sb.append( ind ).append( '}' ).append( '\n' );
    }

    private static final void out ( Map<String, Object> m, StringBuilder sb, int indent ) {
        char[] indentChars = new char[indent * INDENT_COUNT];
        Arrays.fill( indentChars, ' ' );
        String ind = new String( indentChars );
        for (Iterator<Map.Entry<String, Object>> it = m.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> e = it.next();
            sb.append( ind );
            sb.append( '"' ).append( e.getKey().replace( "\"", "\\\"" ) ).append( '"' ).append( ' ' ).append( ':' ).append( ' ' );
            if (e.getValue() instanceof CharSequence) {
                String s = e.getValue().toString();
                s = s.replace( "\"", "\\\"" );
                sb.append( '"' ).append( s ).append( '"' );
            } else if (e.getValue() instanceof List) {
                List<Object> l = (List) e.getValue();
                out( l, sb, indent + 1 );
            } else if (e.getValue() instanceof Map) {
                sb.append( '{' );
                sb.append( '\n' );
                out( (Map<String, Object>) e.getValue(), sb, indent + 1 );
                sb.append( ind );
                sb.append( '}' );
            } else if (((e.getValue() instanceof Number)) || ((e.getValue() instanceof Boolean))) {
                sb.append( e.getValue() );
            } else if (e.getValue().getClass().isArray()) {
                if (e.getValue().getClass().getComponentType().isPrimitive()) {
                    Object[] o = Utilities.toObjectArray( e.getValue() );
                    out( Arrays.asList( o ), sb, indent + 1 );
                }
            } else {
                reflectOut( e.getValue(), sb, indent + 1 );
            }
            if (it.hasNext()) {
                sb.append( ',' );
            }
            sb.append( '\n' );
        }
    }

    public static final class JsonException extends Exception {
        private final char what;
        private final int position;

        JsonException ( String msg, char what, int line, int position, Throwable cause ) {
            super( "Illegal JSON at line " + line + " offset " + position + " with character '" + what + "': " + msg, cause );
            this.what = what;
            this.position = position;
        }

        JsonException ( String msg, char what, int line, int position ) {
            super( "Illegal JSON at line " + line + " offset " + position + " with character '" + what + "': " + msg );
            this.what = what;
            this.position = position;
        }
    }

    private static final class Internal extends RuntimeException {
        Internal ( String s ) {
            super( s );
        }
    }
}
