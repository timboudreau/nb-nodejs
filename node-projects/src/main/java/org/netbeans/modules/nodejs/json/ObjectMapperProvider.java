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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;


/**
 * Jackson's ObjectMapper is stateful, so we need to create new ones; and we
 * don't want to put configuration all over the place.
 *
 * @author Tim Boudreau
 */
public class ObjectMapperProvider {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.configure( JsonParser.Feature.ALLOW_COMMENTS, true );
        MAPPER.configure( DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true );
        MAPPER.configure( SerializationFeature.INDENT_OUTPUT, true );
        MAPPER.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
        MAPPER.configure( SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true );
        MAPPER.configure( JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true );
        MAPPER.configure( JsonParser.Feature.AUTO_CLOSE_SOURCE, true );
        
    }
    
    public static ObjectMapper newObjectMapper () {
        return MAPPER.copy();
    }

    public static final TypeReference<Map<String,Object>> STRING_OBJECT_MAP
            = new TR();

    private static final class TR extends TypeReference<Map<String,Object>> {
    }
}
