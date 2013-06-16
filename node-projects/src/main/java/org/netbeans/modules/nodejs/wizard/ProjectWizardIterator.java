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
package org.netbeans.modules.nodejs.wizard;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.nodejs.DefaultExecutable;
import org.netbeans.modules.nodejs.options.NodePanel;
import org.netbeans.modules.projecttemplates.GeneratedProject;
import org.netbeans.modules.projecttemplates.ProjectCreator;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.Panel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbCollections;

/**
 *
 * @author Tim Boudreau
 */
public class ProjectWizardIterator implements WizardDescriptor.ProgressInstantiatingIterator<WizardDescriptor>, ChangeListener {
    private final FileObject template;
    private WizardDescriptor wiz;
    private ProjectWizardPanel panel;

    public static ProjectWizardIterator create ( FileObject template ) {
        return new ProjectWizardIterator( template );
    }

    private ProjectWizardIterator ( FileObject template ) {
        this.template = template;
    }

    @Override
    public Set<?> instantiate ( ProgressHandle h ) throws IOException {
        Set<Object> results = new HashSet<Object>();
        FileObject dest = Templates.getTargetFolder( wiz );
        Templates.setTargetFolder( wiz, dest );
        String name = (String) wiz.getProperty( ProjectWizardKeys.WIZARD_PROP_PROJECT_NAME );

        File file = (File) wiz.getProperty( ProjectWizardKeys.WIZARD_PROP_PROJECT_DIR );

        dest = FileUtil.toFileObject( FileUtil.normalizeFile( file.getParentFile() ) );

        ProjectCreator gen = new ProjectCreator( dest );

        Map<String, String> templateProperties = NbCollections.checkedMapByFilter( wiz.getProperties(), String.class, String.class, false );
        templateProperties.put( ProjectWizardKeys.WIZARD_PROP_PORT, DefaultExecutable.get().getDefaultPort() + "" );
        templateProperties.put( "project.license", panel.getLicense() );
        templateProperties.put( "license", panel.getLicense() == null ? "none" : panel.getLicense() );
        templateProperties.put( "author", NodePanel.getAuthor() );
        templateProperties.put( "email", NodePanel.getEmail() );
        templateProperties.put( "login", NodePanel.getLogin() );
        if (Boolean.TRUE.equals( wiz.getProperty( ProjectWizardKeys.WIZARD_PROP_GENERATE_PACKAGE_JSON ) )) {
            FileObject packageTemplate = FileUtil.getConfigFile( "Templates/javascript/package.json" ); //NOI18N
            if (packageTemplate != null) {
                gen.add( null, "package.json", packageTemplate, false ); //NOI18N
            }
        }

        GeneratedProject proj = gen.createProject( h, name, template, templateProperties );
        results.add( proj.projectDir );
        results.addAll( proj.filesToOpen );
        return results;
    }

    @Override
    public Set<?> instantiate () throws IOException {
        throw new AssertionError( "instantiate(ProgressHandle) " //NOI18N
                + "should have been called" ); //NOI18N
    }

    @Override
    public void initialize ( WizardDescriptor wd ) {
        wiz = wd;
        panel = new ProjectWizardPanel( wd );
        if (wiz.getProperty( ProjectWizardKeys.WIZARD_PROP_PROJECT_NAME ) == null) {
            try {
                DataObject ob = DataObject.find( template );
                String nm = ob.getNodeDelegate().getDisplayName();
                //XXX check for already existing and append _1, etc.
                wiz.putProperty( ProjectWizardKeys.WIZARD_PROP_PROJECT_NAME, nm );
            } catch ( DataObjectNotFoundException ex ) {
                Exceptions.printStackTrace( ex );
            }
        }
    }

    @Override
    public void uninitialize ( WizardDescriptor wd ) {
        panel = null;
    }

    @Override
    public Panel<WizardDescriptor> current () {
        return panel.pnl;
    }

    @Override
    public String name () {
        return NbBundle.getMessage( ProjectWizardIterator.class, "PORT" ); //NOI18N
    }

    @Override
    public boolean hasNext () {
        return false;
    }

    @Override
    public boolean hasPrevious () {
        return false;
    }

    @Override
    public void nextPanel () {
        //do nothing
    }

    @Override
    public void previousPanel () {
        //do nothing
    }

    @Override
    public void addChangeListener ( ChangeListener cl ) {
        //do nothing
    }

    @Override
    public void removeChangeListener ( ChangeListener cl ) {
        //do nothing
    }

    @Override
    public void stateChanged ( ChangeEvent ce ) {
        //do nothing
    }
}
