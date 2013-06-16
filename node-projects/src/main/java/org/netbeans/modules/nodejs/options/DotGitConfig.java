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
package org.netbeans.modules.nodejs.options;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.filesystems.FileUtil;

/**
 * Reads values from ~/.gitconfig to try to provide reasonable defaults for
 * things which aren't available as system properties
 *
 * @author Tim Boudreau
 */
public class DotGitConfig {

    private static DotGitConfig INSTANCE;
    private final Map<String, Map<String, String>> props = new HashMap<>();

    DotGitConfig ( File dotGit ) {
        if (dotGit != null && dotGit.exists() && dotGit.isFile()) {
            String curr = "core";
            Pattern keyValue = Pattern.compile( "(.*?)\\s*=\\s*(\\S.*)$" );
            Pattern sect = Pattern.compile( "^\\[(.*)\\]" );
            try {
                String content = readFile( dotGit );
                for (String s : content.split( "\n" )) {
                    s = s.trim();
                    if (s.isEmpty() || (!s.isEmpty() && s.charAt( 0 ) == '#')) {
                        continue;
                    }
                    Matcher sm = sect.matcher( s );
                    if (!sm.find()) {
                        Matcher m = keyValue.matcher( s );
                        if (m.find()) {
                            String key = m.group( 1 );
                            String val = m.group( 2 );
                            Map<String, String> cp = props.get( curr );
                            if (cp == null) {
                                cp = new HashMap<>();
                                props.put( curr, cp );
                            }
                            cp.put( key, val );
                        }
                    } else {
                        curr = sm.group( 1 ).trim().toLowerCase();
                    }
                }
            } catch ( IOException ex ) {
                Logger.getLogger( DotGitConfig.class.getName() ).log( Level.INFO, null, ex );
            }
        }
    }

    private DotGitConfig () {
        this( dotGitFile() );
    }

    public String get ( String category, String key ) {
        Map<String, String> m = props.get( category.toLowerCase() );
        if (m == null) {
            return null;
        }
        return m.get( key );
    }

    public String get ( String category, String key, String defaultValue ) {
        String result = get( category, key );
        return result == null ? defaultValue : result;
    }

    private static File dotGitFile () {
        File home = new File( System.getProperty( "user.home" ) );
        if (home.exists()) {
            File dotGit = new File( home, ".gitconfig" );
            if (dotGit.exists() && dotGit.isFile()) {
                return dotGit;
            }
        }
        return null;
    }

    public static synchronized DotGitConfig getDefault () {
        if (INSTANCE == null) {
            INSTANCE = new DotGitConfig();
        }
        return INSTANCE;
    }

    private static String readFile ( File f ) throws IOException {
        FileInputStream in = new FileInputStream( f );
        ByteArrayOutputStream out = new ByteArrayOutputStream( (int) f.length() );
        FileUtil.copy( in, out );
        return new String( out.toByteArray() );
    }
}
