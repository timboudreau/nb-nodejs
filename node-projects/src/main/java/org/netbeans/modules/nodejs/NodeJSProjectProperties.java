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
package org.netbeans.modules.nodejs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbCollections;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public class NodeJSProjectProperties {
    private final NodeJSProject project;

    NodeJSProjectProperties ( NodeJSProject project ) {
        this.project = project;
    }

    Project project () {
        return project;
    }

    public String getDisplayName () {
        String result = project.metadata().getValue( ProjectMetadata.PROP_NAME );
        if (result == null) {
            result = project.getProjectDirectory().getName();
        }
        return result;
    }

    public void setDisplayName ( String name ) {
        Parameters.notNull( "name", name ); //NOi18N
        project.metadata().setValue( ProjectMetadata.PROP_NAME, name );
    }

    public void setDescription ( String description ) { //NOi18N
        Parameters.notNull( "description", description );
        project.metadata().setValue( ProjectMetadata.PROP_DESCRIPTION, description );
    }

    public void setLicense ( String kind, URL url ) {
        project.metadata().setValue( ProjectMetadata.PROP_LICENSE_KIND, kind );
        project.metadata().setValue( ProjectMetadata.PROP_LICENSE_URL, url.toString() );
    }

    public String getVersion () {
        return project.metadata().getValue( ProjectMetadata.PROP_VERSION );
    }

    public void setVersion ( String version ) {
        project.metadata().setValue( ProjectMetadata.PROP_VERSION, version );
    }

    public void setLicenseType ( String type ) {
        project.metadata().setValue( ProjectMetadata.PROP_LICENSE_KIND, type );
    }

    public void setLicenseURL ( URL url ) {
        project.metadata().setValue( ProjectMetadata.PROP_LICENSE_URL, url.toString() );
    }

    public String getLicenseType () {
        return project.metadata().getValue( ProjectMetadata.PROP_LICENSE_KIND );
    }

    public String getLicenseURL () {
        return project.metadata().getValue( ProjectMetadata.PROP_LICENSE_URL );
    }

    public String getDescription () {
        return project.metadata().getValue( ProjectMetadata.PROP_DESCRIPTION );
    }

    public void setAuthor ( String author ) {
        project.metadata().setValue( ProjectMetadata.PROP_AUTHOR_NAME, author );
    }

    public void setAuthorEmail ( String email ) {
        if (email != null && !"".equals( email )) { //NOi18N
            Problems p = new Problems();
            StringValidators.EMAIL_ADDRESS.validate( p, "email", email ); //NOI18N
            if (p.hasFatal()) {
                throw new IllegalArgumentException( p.getLeadProblem().getMessage() );
            }
        }
        project.metadata().setValue( ProjectMetadata.PROP_AUTHOR_EMAIL, email );
    }

    public void setBugTrackerURL ( URL url ) {
        project.metadata().setValue( ProjectMetadata.PROP_BUG_URL, url + "" ); //NOI18N
    }

    public void setKeywords ( String commaDelimited ) {
        String[] keywords = commaDelimited.split( "," ); //NOI18N
        List<String> l = new ArrayList<>();
        for (String keyword : keywords) {
            keyword = keyword.trim();
            if (!keyword.isEmpty()) {
                l.add( keyword );
            }
        }
        project.metadata().setValue( ProjectMetadata.PROP_KEYWORDS, l );
    }

    public List<String> getKeywords () {
        List<?> l = project.metadata().getValues( ProjectMetadata.PROP_KEYWORDS );
        return NbCollections.checkedListByCopy( l, String.class, false );
    }

    public void setMainFile ( FileObject fileObject ) {
        Parameters.notNull( "fileObject", fileObject ); //NOI18N
        FileObject root = project.getProjectDirectory();
        String path = FileUtil.getRelativePath( root, fileObject );
        if (!path.startsWith("./")) { //NOi18N
            path = "./" + path; //NOi18N
        }
        project.metadata().setValue( ProjectMetadata.PROP_MAIN_FILE, path );
    }

    public FileObject getMainFile () {
        String relPath = project.metadata().getValue( ProjectMetadata.PROP_MAIN_FILE );
        if (relPath != null) {
            if (relPath.startsWith( "./" )) { //NOI18N
                relPath = relPath.substring( 2 );
            }
            return project.getProjectDirectory().getFileObject( relPath );
        }
        return null;
    }

    public FileObject getSourceDir () {
        FileObject result = project.getProjectDirectory();
        return result;
    }

    public String getAuthorEmail () {
        return project.metadata().getValue( ProjectMetadata.PROP_AUTHOR_EMAIL );
    }

    public String getAuthorName () {
        return project.metadata().getValue( ProjectMetadata.PROP_AUTHOR_NAME );
    }

    public String getAuthorURL() {
        return project.metadata().getValue( ProjectMetadata.PROP_AUTHOR_URL );
    }

    public void setAuthorURL(String authorURL) {
        project.metadata().setValue( ProjectMetadata.PROP_AUTHOR_URL, authorURL );
    }

    String getBugTrackerURL () {
        return project.metadata().getValue( ProjectMetadata.PROP_BUG_URL );
    }

    void save() throws IOException {
        project.metadata().save();
    }

    public void setRunArguments ( String args ) {
        //Run arguments are very likely to be machine-specific and have no
        //business in package.json
        try {
            FileObject fo = project.getProjectDirectory().getFileObject( ".nbrun" ); //NOi18N
            if (fo != null && (args == null || args.trim().length() == 0)) {
                fo.delete();
                return;
            } else if (fo == null) {
                fo = project.getProjectDirectory().createData( ".nbrun" ); //NOi18N
            }
            try (OutputStream out = fo.getOutputStream(); PrintStream ps = new PrintStream( out )) {
                ps.println( args );
            }
        } catch ( IOException ex ) {
            Exceptions.printStackTrace( ex );
        }
    }

    public String getRunArguments () {
        FileObject fo = project.getProjectDirectory().getFileObject( ".nbrun" ); //NOi18N
        if (fo != null) {
            try {
                return fo.asText().trim();
            } catch ( IOException ex ) {
                Exceptions.printStackTrace( ex );
            }
        }
        return null;
    }
}
