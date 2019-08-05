/* Copyright (C) 2012-2019 Tim Boudreau

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
package org.netbeans.modules.nodejs.ui2;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.modules.nodejs.NodeJSProject;
import static org.netbeans.modules.nodejs.NodeJSProjectFactory.DOT_NPMIGNORE;
import static org.netbeans.modules.nodejs.NodeJSProjectFactory.PACKAGE_JSON;
import static org.netbeans.modules.nodejs.NodeJSProjectFactory.PACKAGE_LOCK_JSON;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

/**
 *
 * @author tim
 */
public class ImportantFilesChildFactory extends ChildFactory.Detachable<FileObject> implements Comparator<FileObject> {
    private final NodeJSProject project;

    private static final Pattern PROBABLE_LICENSE_FILE = Pattern.compile( ".*?licens.*", Pattern.CASE_INSENSITIVE );
    private static final Pattern PROBABLE_CHANGE_LOG_FILE = Pattern.compile( ".*?changelog.*", Pattern.CASE_INSENSITIVE );
    private static final Pattern PROBABLE_TO_DO_FILE = Pattern.compile( "to.?do.*", Pattern.CASE_INSENSITIVE );
    private static final String DOT_GITIGNORE = ".gitignore";
    private static final String DOT_TRAVIS_YML = ".travis.yml";
    private static final String README_MD = "README.md";
    private static Set<String> COMMON_IMPORTANT_FILES = new HashSet<>( Arrays.asList(
            PACKAGE_LOCK_JSON,
            PACKAGE_JSON, DOT_NPMIGNORE, DOT_GITIGNORE,
            DOT_TRAVIS_YML, README_MD ) );

    public ImportantFilesChildFactory ( NodeJSProject project ) {
        this.project = project;
    }

    static Collection<FileObject> importantFiles ( NodeJSProject prj ) {
        Set<FileObject> all = new HashSet<>();
        importantFiles( prj.getProjectDirectory(), all );
        return all;
    }

    static Predicate<FileObject> importantFileTester ( NodeJSProject prj ) {
        Collection<FileObject> all = importantFiles( prj );
        return all::contains;
    }

    static void importantFiles ( FileObject root, Collection<? super FileObject> toPopulate ) {
        for (String name : COMMON_IMPORTANT_FILES) {
            FileObject fo = root.getFileObject( name );
            if (fo != null) {
                toPopulate.add( fo );
            }
        }
        for (FileObject ch : root.getChildren()) {
            String name = ch.getName();
            if (!COMMON_IMPORTANT_FILES.contains( name ) && !isCodeFile( name )) {
                if (name.toLowerCase().contains( "readme" ) && !toPopulate.contains( ch )) {
                    toPopulate.add( ch );
                    continue;
                }
                Matcher m = PROBABLE_LICENSE_FILE.matcher( name );
                if (m.matches()) {
                    toPopulate.add( ch );
                    continue;
                }
                m = PROBABLE_CHANGE_LOG_FILE.matcher( name );
                if (m.matches()) {
                    toPopulate.add( ch );
                    continue;
                }
                m = PROBABLE_TO_DO_FILE.matcher( name );
                if (m.matches()) {
                    toPopulate.add( ch );
                    continue;
                }
            }
        }
    }

    private static boolean isCodeFile ( String name ) {
        return name.endsWith( ".js" ) && !name.endsWith( ".sh" ) && !name.endsWith( ".json" );
    }

    @Override
    protected boolean createKeys ( List<FileObject> toPopulate ) {
        FileObject root = project.getProjectDirectory();
        importantFiles( root, toPopulate );
        Collections.sort( toPopulate, this );
        return true;
    }

    @Override
    protected Node createNodeForKey ( FileObject key ) {
        try {
            DataObject dob = DataObject.find( key );
            return new FilterNode( dob.getNodeDelegate() );
        } catch ( DataObjectNotFoundException ex ) {
            Logger.getLogger( ImportantFilesChildFactory.class.getName() )
                    .log( Level.INFO, "File disappeared before node could be created: {0}", key );
        }
        return null;
    }

    @Override
    public int compare ( FileObject o1, FileObject o2 ) {
        return o1.getName().compareToIgnoreCase( o2.getName() );
    }
}
