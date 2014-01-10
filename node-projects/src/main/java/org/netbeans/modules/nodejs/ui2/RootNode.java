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
package org.netbeans.modules.nodejs.ui2;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.modules.nodejs.LibrariesResolver;
import org.netbeans.modules.nodejs.NodeJSProject;
import org.netbeans.modules.nodejs.NodeJSProjectProperties;
import org.netbeans.modules.nodejs.PropertiesPanel;
import org.netbeans.modules.nodejs.node.AddLibraryAction;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.actions.FileSystemAction;
import org.openide.actions.FindAction;
import org.openide.actions.PasteAction;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
public final class RootNode extends AbstractNode implements ChangeListener {
    public static final String LIBRARY_ICON = "org/netbeans/modules/nodejs/resources/libs.png"; //NOI18N
    public static final String LOGO_ICON = "org/netbeans/modules/nodejs/resources/logo.png"; //NOI18N
    public static final String MISSING_LIBRARIES_BADGE = "org/netbeans/modules/nodejs/resources/warn.png"; //NOI18N

    public RootNode ( NodeJSProject project ) {
        this( project, new InstanceContent() );
    }

    public void updateChildren () {
        setChildren( createProjectChildren( getLookup().lookup( NodeJSProject.class ) ) );
    }

    @SuppressWarnings ("LeakingThisInConstructor") //NOI18N
    public RootNode ( NodeJSProject project, InstanceContent content ) {
        super( createProjectChildren( project ), new ProxyLookup( project.getLookup(), new AbstractLookup( content ) ) );
        content.add( this );
        final ProjectInformation info = getLookup().lookup( ProjectInformation.class );
        setDisplayName( info.getDisplayName() );
        info.addPropertyChangeListener( new PropertyChangeListener() {
            @Override
            public void propertyChange ( PropertyChangeEvent evt ) {
                setDisplayName( info.getDisplayName() );
            }
        } );
        setIconBaseWithExtension( LOGO_ICON );
    }

    private static Children createProjectChildren ( NodeJSProject project ) {
        return Children.create( new ProjectChildren( project ), true );
    }

    private boolean listening;

    @Override
    public Image getIcon ( int type ) {
        LibrariesResolver resolver = getLookup().lookup( Project.class ).getLookup().lookup( LibrariesResolver.class );
        if (!listening) {
            listening = true;
            resolver.addChangeListener( WeakListeners.change( this, resolver ) );
        }
        Image result = super.getIcon( type );
        if (resolver.hasMissingLibraries()) {
            Image badge = ImageUtilities.loadImage( MISSING_LIBRARIES_BADGE );
            result = ImageUtilities.mergeImages( result , badge, 7, 7);
        }
        return result;
    }

    @Override
    public Image getOpenedIcon ( int type ) {
        return getIcon( type );
    }

    @Override
    public Action[] getActions ( boolean ignored ) {
        NodeJSProject project = getLookup().lookup( NodeJSProject.class );
        final ResourceBundle bundle
                = NbBundle.getBundle( RootNode.class );

        List<Action> actions = new ArrayList<>();

        actions.add( CommonProjectActions.newFileAction() );
        actions.add( null );
        actions.add( ProjectSensitiveActions.projectCommandAction(
                ActionProvider.COMMAND_RUN,
                bundle.getString( "LBL_RunAction_Name" ), null ) ); // NOI18N
        actions.add( null );
        actions.add( CommonProjectActions.setAsMainProjectAction() );
        actions.add( null );
        actions.add( ProjectSensitiveActions.projectCommandAction(
                NodeJSProject.MAIN_FILE_COMMAND,
                bundle.getString( "LBL_ChooseMainFile_Name" ), null ) ); // NOI18N
        actions.add( null );
//        actions.add(ProjectSensitiveActions.projectCommandAction(
//                NodeJSProject.LIBRARIES_COMMAND,
//                bundle.getString("LBL_AddLibrary_Name"), null));
        actions.add( new AddLibraryAction( bundle, project, this ) );
        actions.add( null );
        actions.add( CommonProjectActions.setAsMainProjectAction() );
        actions.add( CommonProjectActions.closeProjectAction() );
        actions.add( null );
        actions.add( CommonProjectActions.renameProjectAction() );
        actions.add( CommonProjectActions.moveProjectAction() );
        actions.add( CommonProjectActions.copyProjectAction() );
        actions.add( CommonProjectActions.deleteProjectAction() );
        actions.add( null );
        actions.add( SystemAction.get( FindAction.class ) );
        actions.add( null );
        actions.add( SystemAction.get( PasteAction.class ) );
        actions.add( null );
        actions.add( getFilesystemAction() );
        actions.addAll( Lookups.forPath( "Project/NodeJS/Actions" ).lookupAll( Action.class ) ); //NOI18N
        actions.add( new AbstractAction( NbBundle.getMessage( RootNode.class, "PROPERTIES" ) ) { //NOI18N
            @Override
            public void actionPerformed ( ActionEvent e ) {
                Project project = getLookup().lookup( Project.class );
                new PropertiesPanel( project.getLookup().lookup( NodeJSProjectProperties.class ) ).showDialog();
            }
        } );
        final LibrariesResolver resolver = getLookup().lookup( Project.class ).getLookup().lookup( LibrariesResolver.class );
        if (resolver.hasMissingLibraries()) {
            actions.add(new AbstractAction(NbBundle.getMessage(RootNode.class, "RESOLVE_LIBRARIES")){

                @Override
                public void actionPerformed ( ActionEvent e ) {
                    resolver.install();
                }
            });
        }

        return actions.toArray( new Action[actions.size()] );
    }

    private Action getFilesystemAction () {
        Project project = getLookup().lookup( Project.class );
        FileSystemAction a = SystemAction.get( FileSystemAction.class );
        try {
            if (project.getProjectDirectory().isValid()) {
                Node n = DataObject.find( project.getProjectDirectory() ).getNodeDelegate();
                return a.createContextAwareInstance( n.getLookup() );
            } else {
                return a;
            }
        } catch ( DataObjectNotFoundException ex ) {
            Exceptions.printStackTrace( ex );
            return a;
        }
    }

    @Override
    protected void createPasteTypes ( Transferable t, List<PasteType> s ) {
        Project project = getLookup().lookup( Project.class );
        try {
            Node n = DataObject.find( project.getProjectDirectory() ).getNodeDelegate();
            s.addAll( Arrays.asList( n.getPasteTypes( t ) ) );
        } catch ( DataObjectNotFoundException ex ) {
            Logger.getLogger( RootNode.class.getName() ).log( Level.INFO,
                    "Project dir disappeared: {0}", project ); //NOI18N
        }
    }

    @Override
    public void stateChanged ( ChangeEvent e ) {
        fireIconChange();
    }
}
