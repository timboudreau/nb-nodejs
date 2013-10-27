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
package org.netbeans.modules.nodejs.node;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import org.codehaus.jackson.type.TypeReference;
import org.netbeans.modules.nodejs.NodeJSProjectFactory;
import org.netbeans.modules.nodejs.json.JsonPanel;
import org.netbeans.modules.nodejs.json.ObjectMapperProvider;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 *
 * @author tim
 */
final class LibraryFilterNode extends FilterNode {
    private final ProjectNodeKey key;
    private static RequestProcessor jsonReader = new RequestProcessor(
            "Node lib json loader", 1 ); //NOI18N

    public LibraryFilterNode ( ProjectNodeKey key ) {
        this( key, null );
    }

    public LibraryFilterNode ( ProjectNodeKey key, CountDownLatch latch ) {
        this( nodeFromKey( key ), key, latch );
    }

    private static Node nodeFromKey ( ProjectNodeKey key ) {
        FileObject fo = key.getFld();
        if (fo != null && fo.isValid()) {
            File f = FileUtil.toFile( fo );
            if (f != null) {
                try {
                    f = f.getCanonicalFile();
                    FileObject nue = FileUtil.toFileObject( FileUtil.normalizeFile( f ) );
                    if (nue != null && nue.isValid()) {
                        fo = nue;
                    }
                } catch ( IOException ex ) {
                    Logger.getLogger( LibraryFilterNode.class.getName() ).log( Level.INFO,
                            "Could not canonicalize " + fo.getPath(), ex ); //NOI18N
                }
            }
        }
        if (fo != null) {
            try {
                DataObject dob = DataObject.find( fo );
                return dob.getNodeDelegate();
            } catch ( DataObjectNotFoundException donfe ) {
                Logger.getLogger( LibraryFilterNode.class.getName() ).log( Level.INFO,
                        "File disappeared: " + fo.getPath(), donfe ); //NOI18N
            }
        }
        return Node.EMPTY;
    }

    static boolean isFileNode ( Node n ) {
        FileObject fo = n.getLookup().lookup( FileObject.class );
        if (fo == null) {
            DataObject dob = n.getLookup().lookup( DataObject.class );
            if (dob != null) {
                fo = dob.getPrimaryFile();
            }
        }
        return fo == null || !fo.isValid() ? false : fo.isData();
    }

    public String getVersion () {
        synchronized ( key ) {
            return getString( getPackageInfo(), "version", null );
        }
    }

    public ProjectNodeKey getKey () {
        return key;
    }

    private LibraryFilterNode ( Node original, final ProjectNodeKey key, final CountDownLatch latch ) {
        super( nodeFromKey( key ), isFileNode( original ) ? Children.LEAF
                : Children.create( new LibraryNodeChildren( original.getLookup().lookup( DataObject.class ) ), true ) );
        disableDelegation( DELEGATE_SET_NAME | DELEGATE_SET_SHORT_DESCRIPTION | DELEGATE_SET_DISPLAY_NAME | DELEGATE_SET_VALUE );
        this.key = key;
        jsonReader.post( new Runnable() {
            @Override
            public void run () {
                try {
                    Map<String, Object> json = getPackageInfo();
                    synchronized ( key ) {
                        LibraryFilterNode.this.name = getString( json, "name", getDisplayName() ); //NOI18N
                        LibraryFilterNode.this.setName( LibraryFilterNode.this.name );
                        LibraryFilterNode.this.description = getString( json, "description", "[no description]" );
                        LibraryFilterNode.this.author = getString( json, "author", null ); //NOI18N
                        LibraryFilterNode.this.version = getString( json, "version", null ); //NOI18N
                    }
                    Object license = json.get( "license" ); //NOI18N
                    List<String> l = new ArrayList<String>();
                    if (license == null) {
                        license = json.get( "licenses" ); //NOI18N
                    }
                    if (license instanceof String) {
                        l.add( license.toString() );
                    }
                    if (license instanceof List) {
                        for (Object o : (List<?>) license) {
                            if (o instanceof String) {
                                l.add( o.toString() );
                            } else if (o instanceof Map) {
                                Map<?, ?> m = (Map<?, ?>) o;
                                Object val = m.get( "type" ); //NOI18N
                                if (val != null) {
                                    l.add( val.toString() );
                                }
                            }
                        }
                    }
                    if (license instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) license;
                        Object val = m.get( "type" ); //NOI18N
                        if (val != null) {
                            l.add( val.toString() );
                        }
                    }
                    Object repo = json.get( "repository" ); //NOI18N
                    if (repo instanceof String) {
                        synchronized ( key ) {
                            LibraryFilterNode.this.repo = repo.toString();
                            LibraryFilterNode.this.repoType = "[unknown]";
                        }
                    }
                    if (repo instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) repo;
                        Object rType = m.get( "type" ); //NOI18N
                        if (rType instanceof String) {
                            synchronized ( key ) {
                                LibraryFilterNode.this.repoType = rType.toString();
                            }
                        }
                        Object r = m.get( "url" ); //NOI18N
                        if (r instanceof String) {
                            synchronized ( key ) {
                                LibraryFilterNode.this.repo = r.toString();
                            }
                        }
                    }
                    if (author == null) {
                        Object a = json.get( "author" ); //NOI18N
                        if (a instanceof Map) {
                            StringBuilder sb = new StringBuilder();
                            Object nm = ((Map) a).get( "name" ); //NOI18N
                            if (nm != null) {
                                sb.append( nm );
                            }
                            nm = ((Map) a).get( "email" ); //NOI18N
                            if (nm != null) {
                                sb.append( " <" ).append( nm ).append( ">" ); //NOI18N
                            }
                            synchronized ( key ) {
                                LibraryFilterNode.this.author = sb.toString();
                            }
                        } else if (a instanceof List) {
                            StringBuilder sb = new StringBuilder();
                            List<?> list = (List<?>) a;
                            for (Iterator<?> it = list.iterator(); it.hasNext();) {
                                Object o = it.next();
                                if (o instanceof String) {
                                    sb.append( o );
                                    if (it.hasNext()) {
                                        sb.append( ", " ); //NOI18N
                                    }
                                } else if (o instanceof Map) {
                                    Object nm = ((Map) o).get( "name" ); //NOI18N
                                    if (nm != null) {
                                        sb.append( nm );
                                    }
                                    nm = ((Map) o).get( "email" ); //NOI18N
                                    if (nm != null) {
                                        sb.append( " <" ).append( nm ).append( ">" ); //NOI18N
                                    }
                                }
                            }
                            synchronized ( key ) {
                                LibraryFilterNode.this.author = sb.toString();
                            }
                        }
                    }
                    Object o = json.get( "bugs" ); //NOI18N
                    if (o instanceof String) {
                        synchronized ( key ) {
                            LibraryFilterNode.this.bugUrl = o.toString();
                        }
                    } else if (o instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) o;
                        Object web = m.get( "web" ); //NOI18N
                        if (web instanceof String) {
                            synchronized ( key ) {
                                LibraryFilterNode.this.bugUrl = web.toString();
                            }
                        }
                    }
                    synchronized ( key ) {
                        LibraryFilterNode.this.licenses = l.toArray( new String[l.size()] );
                    }
                } finally {
                    if (latch != null) {
                        latch.countDown();
                    }
                }
            }
        } );
    }
    private String version;
    private String author;
    private String name;
    private String description;
    private String[] licenses;
    private String repoType;
    private String repo;
    private String bugUrl;

    @Override
    public Image getIcon ( int type ) {
        Image result = ImageUtilities.loadImage( key.getType()
                == ProjectNodeKeyTypes.BUILT_IN_LIBRARY
                ? "org/netbeans/modules/nodejs/resources/libs.png" //NOI18N
                : "org/netbeans/modules/nodejs/resources/libs.png" ); //NOI18N
        if (!key.isDirect() && key.getType() != ProjectNodeKeyTypes.BUILT_IN_LIBRARY) {
            result = ImageUtilities.createDisabledImage( result );
        }
        if (key.getType() == ProjectNodeKeyTypes.BUILT_IN_LIBRARY) {
            Image badge = ImageUtilities.loadImage(
                    "org/netbeans/modules/nodejs/resources/logoBadge.png" ); //NOI18N
            result = ImageUtilities.mergeImages( result, badge, 8, 8 );
        }
        return result;
    }

    @Override
    public Image getOpenedIcon ( int type ) {
        return getIcon( type );
    }

    @Override
    public String getHtmlDisplayName () {
        StringBuilder sb = new StringBuilder();
        if (key.getType() == ProjectNodeKeyTypes.BUILT_IN_LIBRARY) {
            sb.append( "<font color='#22AA22'><i>" ); //NOI18N
        } else if (!key.isDirect()) {
            sb.append( "<font color='!controlDkShadow'>" ); //NOI18N
        }
        if (isFileNode( getOriginal() )) {
            DataObject dob = getOriginal().getLookup().lookup( DataObject.class );
            sb.append( dob.getName() );
        } else {
            sb.append( getDisplayName() );
        }
        if (key.getType() == ProjectNodeKeyTypes.BUILT_IN_LIBRARY) {
            sb.append( "</i>" ); //NOI18N
        }
        if (version != null) {
            sb.append( " <i><font color='#9999AA'> " ).append( version ).append( "</i>" ); //NOI18N
            if (!key.isDirect()) {
                sb.append( "<font color='!controlDkShadow'>" ); //NOI18N
            }
        }
        if (!key.isDirect() && key.getType() != ProjectNodeKeyTypes.BUILT_IN_LIBRARY && key.getFld() != null) {
            sb.append( " (&lt;-" ).append( //NOI18N
                    key.getFld().getParent().getParent().getName() ).append( ")" ); //NOI18N
        }
        return sb.toString();
    }

    @Override
    public String getShortDescription () {
        if (this.description != null || this.name != null) {
            StringBuilder sb = new StringBuilder( "<html><body>" ); //NOI18N
            synchronized ( key ) {
                sb.append( "<b><u>" ).append( name == null ? getDisplayName() : name ).append( "</u></b><br>\n" ); //NOI18N
                sb.append( "<table border=0>" ); //NOI18N
                if (description != null) {
                    sb.append( "<tr><th align=\"left\">" ).append( "Description" ).append( "</th><td>" ).append( description ).append( "</td></tr>\n" );
                }
                if (version != null) {
                    sb.append( "<tr><th align=\"left\">" ).append( "Version" ).append( "</th><td>" ).append( version ).append( "</td></tr>\n" );
                }
                if (author != null) {
                    sb.append( "<tr><th align=\"left\">" ).append( author.indexOf( ',' ) > 0 ? "Authors" : "Author" ).append( "</th><td>" ).append( author ).append( "</td></tr>\n" );
                }
                if (licenses != null && licenses.length > 0) {
                    sb.append( "<tr><th align=\"left\">" ); //NOI18N
                    sb.append( licenses.length > 0 ? "Licenses" : "License" );
                    sb.append( "</th><td>" ); //NOI18N
                    for (int i = 0; i < licenses.length; i++) {
                        sb.append( licenses[i] );
                        if (i != licenses.length - 1) {
                            sb.append( ", " );
                        }
                    }
                    sb.append( "</td></tr>" ); //NOI18N
                }
                if (repo != null) {
                    sb.append( "<tr><th align=\"left\">" ); //NOI18N
                    sb.append( "Repository" );
                    if (repoType != null) {
                        sb.append( '(' ).append( repoType ).append( ')' );
                    }
                    sb.append( "</th><td>" ); //NOI18N
                    sb.append( repo );
                    sb.append( "</td></tr>" ); //NOI18N
                }
                if (bugUrl != null) {
                    sb.append( "<tr><th align=\"left\">" ); //NOI18N
                    sb.append( "Bugs:" );
                    sb.append( "</th><td>" ); //NOI18N
                    sb.append( bugUrl );
                    sb.append( "</td></tr>" ); //NOI18N
                }
                sb.append( "</table>" ); //NOI18N
            }
            return sb.toString();
        }
        return super.getShortDescription();
    }

    private String getString ( Map<String, Object> m, String key, String def ) {
        Object o = m.get( key );
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<?> it = ((List<?>) o).iterator(); it.hasNext();) {
                sb.append( it.next() );
                if (it.hasNext()) {
                    sb.append( ',' ); //NOI18N
                }
            }
            return sb.toString();
        }
        return def;
    }

    public Action[] getActions ( boolean ignored ) {
        Action[] result = super.getActions( ignored );
        List<Action> l = new ArrayList<Action>( Arrays.asList( result ) );
        if (bugUrl != null) {
            try {
                URL url = new URL( bugUrl );
                l.add( new BugAction( url ) );
            } catch ( MalformedURLException ex ) {
                Logger.getLogger( LibraryFilterNode.class.getName() ).log( Level.INFO,
                        "Bad bug URL in " + //NOI18N
                        getLookup().lookup( DataObject.class ).getPrimaryFile().getPath()
                        + ":" + bugUrl, ex ); //NOI18N
            }
        }
        FileObject prim = getLookup().lookup( DataObject.class ).getPrimaryFile();
        if (prim != null && prim.isValid()) {
            FileObject packageInfo = prim.getFileObject( NodeJSProjectFactory.PACKAGE_JSON ); //NOI18N
            if (packageInfo != null && packageInfo.isValid()) {
                l.add( new OpenInfoAction( packageInfo ) );
            }
        }
        if (key.isDirect() && key.getType() == ProjectNodeKeyTypes.LIBRARY) {
            l.add( 1, new AbstractAction() {
                {
                    putValue( NAME, NbBundle.getMessage( LibraryFilterNode.class, "REMOVE_DEPENDENCY", getDisplayName() ) );
                }

                @Override
                public void actionPerformed ( ActionEvent e ) {
                    DataObject dob = getLookup().lookup( DataObject.class );
                    if (dob != null && dob.isValid()) {
                        try {
                            dob.delete();
                        } catch ( IOException ex ) {
                            Logger.getLogger( LibraryFilterNode.class.getName() ).log( Level.INFO, "Could not delete " + dob.getName(), ex );
                        }
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            } );
        }
        if (l.size() != result.length) {
            result = l.toArray( new Action[l.size()] );
        }
        return result;
    }

    private static final class OpenInfoAction extends AbstractAction {
        private final FileObject fo;

        OpenInfoAction ( FileObject fo ) {
            this.fo = fo;
            putValue( NAME, NbBundle.getMessage( OpenInfoAction.class,
                    "OPEN_INFO_ACTION" ) ); //NOI18N
        }

        @Override
        public void actionPerformed ( ActionEvent e ) {
            try {
                JsonPanel jp = new JsonPanel( fo );
                DataObject dob = DataObject.find( fo );
                TopComponent tc = new TopComponent( dob.getLookup() ) {
                    @Override
                    public int getPersistenceType () {
                        return TopComponent.PERSISTENCE_NEVER;
                    }
                };
                tc.setDisplayName( fo.getParent().getName() );
                tc.setLayout( new BorderLayout() );
                JScrollPane ssc = new JScrollPane( jp );
                ssc.setBorder( BorderFactory.createEmptyBorder() );
                ssc.setViewportBorder( BorderFactory.createEmptyBorder() );
                tc.add( ssc, BorderLayout.CENTER );
                tc.open();
                tc.requestActive();
            } catch ( Exception ex ) {
                //already logged
            }
        }
    }

    private static class BugAction extends AbstractAction {
        private final URL url;

        BugAction ( URL url ) {
            putValue( NAME, NbBundle.getMessage( BugAction.class, "FILE_BUG" ) );
            this.url = url;
        }

        @Override
        public void actionPerformed ( ActionEvent e ) {
            URLDisplayer.getDefault().showURL( url );
        }
    }

    private Map<String, Object> getPackageInfo () {
        assert !EventQueue.isDispatchThread();
        FileObject json = getLookup().lookup( DataObject.class )
                .getPrimaryFile().getFileObject( NodeJSProjectFactory.PACKAGE_JSON );
        if (json != null && json.isValid()) {
            try {
                InputStream in = json.getInputStream();
                try {
                    TypeReference<Map<String, Object>> tr = new TypeReference<Map<String, Object>>() {
                    };
                    Map<String, Object> m = ObjectMapperProvider.newObjectMapper().readValue( in, tr );
                    return m;
                } finally {
                    in.close();
                }
            } catch ( IOException ex ) {
                Logger.getLogger( LibraryFilterNode.class.getName() ).log(
                        Level.INFO, "Failed to read JSON in " + json.getPath(), ex ); //NOI18N
            }
        }
        return Collections.<String, Object>emptyMap();
    }
}
