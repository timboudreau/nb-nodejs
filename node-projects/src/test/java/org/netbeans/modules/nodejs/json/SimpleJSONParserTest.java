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

import java.io.IOException;
import java.util.Map;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.modules.nodejs.json.SimpleJSONParser.JsonException;

/**
 *
 * @author Tim Boudreau
 */
public class SimpleJSONParserTest {
    @Test
    public void testParse () throws IOException, JsonException {
        for (int i = 0; i < 11; i++) {
            parseJSON( "package_" + i + ".json" );
        }
        for (int i = 0; i < 4; i++) {
            try {
                parseJSON( "bad_" + (i + 1) + ".json" );
                fail( "bad_" + i + " should not have been parsed" );
            } catch ( JsonException e ) {
                System.out.println( e.getMessage() );
            }
        }
    }

    @Test
    public void testIntAndBool () throws Exception {
        String t = "{ \"foo\": 23, \"bar\": true, \"baz\" : [5,10,15,20], \"quux\": [true,false,false,true]  }";
        Map<String, Object> m = new SimpleJSONParser().parse( t );
        assertNotNull( m.get( "foo" ) );
        assertNotNull( m.get( "bar" ) );
        assertNotNull( m.get( "baz" ) );
        assertNotNull( m.get( "quux" ) );
        assertTrue( m.get( "foo" ) instanceof Integer );
        assertTrue( m.get( "bar" ) instanceof Boolean );
        assertTrue( m.get( "baz" ) instanceof List );
        assertTrue( m.get( "baz" ) instanceof List );

        CharSequence nue = new SimpleJSONParser().toJSON( m );
        Map<String, Object> m1 = new SimpleJSONParser().parse( nue );
        assertEquals( m, m1 );
    }

    private void parseJSON ( String what ) throws IOException, JsonException {
        System.out.println( "-------------------------------" );
        InputStream in = SimpleJSONParserTest.class.getResourceAsStream( what );
        assertNotNull( "Test data missing: " + what, in );
        Map<String, Object> m = new SimpleJSONParser().parse( in, "UTF-8" );
        CharSequence seq = SimpleJSONParser.out( m );
        System.out.println( seq );
        System.out.println( "-------------------------------" );
        Map<String, Object> reconstituted = new SimpleJSONParser().parse( seq );
        assertMapsEqual( m, reconstituted );
    }

    private static void assertMapsEqual ( Map<String, Object> a, Map<String, Object> b ) {
        boolean match = a.equals( b );
        if (!match) {
            fail( "No match:\n" + a + "\n" + b );
        }
    }
}
