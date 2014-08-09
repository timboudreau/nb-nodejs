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
package org.netbeans.modules.nodejs.api;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tim
 */
/*  PENDING
 @NbBundle.Messages({
 "JS_SYSTEM_SCRIPT=Javascript System Script"
 })
 @MIMEResolver.Registration(displayName = "JS_SYSTEM_SCRIPT", resource = "usrEnvNodeResolver.xml")
 */
public interface ProjectMetadata {
    public static final String PROP_MAIN_FILE = "main"; //NOi18N
    public static final String PROP_TEST_FILE = "test"; //NOi18N
    public static final String PROP_NAME = "name"; //NOi18N
    public static final String PROP_DESCRIPTION = "description"; //NOi18N
    public static final String PROP_LICENSE_KIND = "license.type"; //NOi18N
    public static final String PROP_LICENSE_URL = "license.url"; //NOi18N
    public static final String PROP_AUTHOR_NAME = "author.name"; //NOi18N
    public static final String PROP_AUTHOR_EMAIL = "author.email"; //NOi18N
    public static final String PROP_AUTHOR_URL = "author.url"; //NOi18N
    public static final String PROP_BUG_URL = "bugs.url"; //NOi18N
    public static final String PROP_KEYWORDS = "keywords"; //NOi18N
    public static final String PROP_VERSION = "version"; //NOi18N

    public String getValue ( String key );

    public void setValue ( String key, String value );

    public void save () throws IOException;

    public void addPropertyChangeListener ( PropertyChangeListener pcl );

    public void addMap ( String key, Map<String, Object> m );

    public Map<String, Object> getMap ( String key );

    public void clearValue ( String key );

    public void addList ( String key, List<?> l );

    public <T> T setValueAs ( String key, T obj );
}
