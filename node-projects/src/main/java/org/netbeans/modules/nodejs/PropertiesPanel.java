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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.validation.adapters.DialogDescriptorAdapter;
import org.netbeans.modules.nodejs.forks.EmailAddressValidator;
import org.netbeans.modules.nodejs.forks.UrlValidator;
import org.netbeans.modules.nodejs.ui.UiUtil;
import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 *
 * @author tim
 */
public class PropertiesPanel extends javax.swing.JPanel {
    private final SwingValidationGroup g;
    private final NodeJSProjectProperties props;

    public PropertiesPanel ( NodeJSProjectProperties props ) {
        this.props = props;
        g = SwingValidationGroup.create();
        initComponents();
        UiUtil.prepareComponents( this );
        set( authorEmailField, props.getAuthorEmail() );
        set( authorNameField, props.getAuthorName() );
        set( nameField, props.getDisplayName() );
        set( descriptionField, props.getDescription() );
        set( bugTrackerField, props.getBugTrackerURL() );
        set( commandLineField, props.getRunArguments() );
        set( authorURLField, props.getAuthorURL() );
        set( versionField, props.getVersion() );
        if ("null".equals( bugTrackerField.getText() )) {
            bugTrackerField.setText( "" );
        }
        String type = props.getLicenseType();
        if (type != null) {
            licenseField.setSelectedItem( type );
        }
        FileObject mainFile = props.getMainFile();
        if (mainFile != null) {
            String path = FileUtil.getRelativePath( props.project().getProjectDirectory(), mainFile );
            set( mainFileField, path );
        }
        List<String> l = props.getKeywords();
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = l.iterator(); it.hasNext();) {
            sb.append( it.next() );
            if (it.hasNext()) {
                sb.append( ", " ); //NOi18N
            }
        }
        set( keywordsField, sb.toString() );
        g.add( bugTrackerField, new AllowNullValidator( new UrlValidator() ) );
        g.add( nameField, StringValidators.REQUIRE_NON_EMPTY_STRING );
        g.add( authorEmailField, new AllowNullValidator( new EmailAddressValidator() ) );
        g.add( mainFileField, new FileRelativeValidator() );
        g.add( commandLineField, new WhitespaceValidator() );
        g.add( authorURLField, new AllowNullValidator( new UrlValidator() ) );
        g.add( versionField, new VersionValidator() );
    }

    @Override
    public void addNotify () {
        super.addNotify();
        g.performValidation();
    }

    private static final class AllowNullValidator extends AbstractValidator<String> {
        //Some validators do not allow nulls - they should and a newer version
        //of the lib fixes this
        private final Validator<String> other;

        AllowNullValidator ( Validator<String> other ) {
            super( String.class );
            this.other = other;
        }

        @Override
        public void validate ( Problems prblms, String string, String t ) {
            if (t == null || "".equals( t.trim() )) { //NOI18N
                return;
            }
            other.validate( prblms, string, t );
        }
    }

    private final class FileRelativeValidator extends AbstractValidator<String> {
        FileRelativeValidator () {
            super( String.class );
        }

        @Override
        public void validate ( Problems prblms, String string, String model ) {
            if (model == null || "".equals( model.trim() )) { //NOI18N
                return;
            }
            FileObject root = props.project().getProjectDirectory();
            FileObject fo = root.getFileObject( model );
            boolean result = fo != null;
            if (!result) {
                prblms.add( NbBundle.getMessage( PropertiesPanel.class, 
                        "MAIN_FILE_DOES_NOT_EXIST", model ) ); //NOI18N
            }
        }
    }

    private static final class WhitespaceValidator extends AbstractValidator<String> {
        private static final Pattern WHITESPACE = Pattern.compile( ".*\\s.*" ); //NOI18N

        WhitespaceValidator () {
            super( String.class );
        }

        @Override
        public void validate ( Problems prblms, String string, String model ) {
            if (model != null && WHITESPACE.matcher( model ).matches()) {
                prblms.add( NbBundle.getMessage( WhitespaceValidator.class, 
                        "INFO_MAIN_CLASS_WHITESPACE" ), Severity.INFO ); //NOI18N
            }
        }
    }
    
    private static final class VersionValidator extends AbstractValidator<String> {
        
        VersionValidator() {
            super( String.class );
        }

        @Override
        public void validate ( Problems prblms, String string, String model ) {
            if (model == null || model.trim().isEmpty()) {
                return;
            }
            String[] parts = model.trim().split("\\.");
            for (String part : parts) {
                try {
                    Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    prblms.add( NbBundle.getMessage( WhitespaceValidator.class, 
                            "INFO_VERSION_INVALID", part ), Severity.INFO ); //NOI18N
                }
            }
        }
    }

    private void set ( JTextComponent c, String s ) {
        if (s != null) {
            c.setText( s );
        }
    }

    void save () {
        props.setAuthor( authorNameField.getText().trim() );
        props.setAuthorEmail( authorEmailField.getText().trim() );
        props.setDisplayName( nameField.getText().trim() );
        props.setDescription( descriptionField.getText().trim() );
        props.setKeywords( keywordsField.getText().trim() );
        props.setRunArguments( commandLineField.getText().trim() );
        props.setAuthorURL( authorURLField.getText().trim() );
        props.setVersion( versionField.getText().trim() );
        if (bugTrackerField.getText().trim().length() > 0) {
            try {
                props.setBugTrackerURL( new URL( bugTrackerField.getText().trim() ) );
            } catch ( MalformedURLException ex ) {
                Logger.getLogger( PropertiesPanel.class.getName() ).log( Level.INFO, 
                        "Bad bug url " + bugTrackerField.getText(), ex); //NOI18N
            }
        } else {
            props.setBugTrackerURL( null );
        }
        props.setMainFile( props.project().getProjectDirectory().getFileObject( mainFileField.getText().trim() ) );
        if (!"none".equals( licenseField.getSelectedItem() )) {
            props.setLicenseType( licenseField.getSelectedItem().toString() );
        }
    }

    boolean notEmpty ( JTextComponent c ) {
        return c.getText().trim().length() > 0;
    }

    public void showDialog () {
        DialogDescriptor d = new DialogDescriptor( this, props.project().getLookup().lookup( ProjectInformation.class ).getDisplayName() );
        DialogDescriptorAdapter adap = new DialogDescriptorAdapter( d );
        g.addUI( adap );
        if (DialogDisplayer.getDefault().notify( d ).equals( DialogDescriptor.OK_OPTION )) {
            save();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings ("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nameLabel = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        descriptionLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionField = new javax.swing.JTextArea();
        authorNameLabel = new javax.swing.JLabel();
        authorNameField = new javax.swing.JTextField();
        authorEmailLabel = new javax.swing.JLabel();
        authorEmailField = new javax.swing.JTextField();
        bugTrackerLabel = new javax.swing.JLabel();
        bugTrackerField = new javax.swing.JTextField();
        licenseLabel = new javax.swing.JLabel();
        licenseField = new javax.swing.JComboBox();
        mainFileLabel = new javax.swing.JLabel();
        mainFileField = new javax.swing.JTextField();
        status = (JLabel) g.createProblemLabel();
        keywordsLabel = new javax.swing.JLabel();
        keywordsField = new javax.swing.JTextField();
        commandLineField = new javax.swing.JTextField();
        commandLineLabel = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        authorURLLabel = new javax.swing.JLabel();
        authorURLField = new javax.swing.JTextField();
        versionLabel = new javax.swing.JLabel();
        versionField = new javax.swing.JTextField();

        nameLabel.setLabelFor(nameField);
        nameLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.nameLabel.text")); // NOI18N

        nameField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.name.text")); // NOI18N
        nameField.setName("name"); // NOI18N

        descriptionLabel.setLabelFor(descriptionField);
        descriptionLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.descriptionLabel.text")); // NOI18N

        descriptionField.setColumns(20);
        descriptionField.setLineWrap(true);
        descriptionField.setRows(5);
        descriptionField.setWrapStyleWord(true);
        descriptionField.setName("description"); // NOI18N
        jScrollPane1.setViewportView(descriptionField);

        authorNameLabel.setLabelFor(authorNameField);
        authorNameLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.authorNameLabel.text")); // NOI18N

        authorNameField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.author.name.text")); // NOI18N
        authorNameField.setName("author.name"); // NOI18N

        authorEmailLabel.setLabelFor(authorEmailField);
        authorEmailLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.authorEmailLabel.text")); // NOI18N

        authorEmailField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.author.email.text")); // NOI18N
        authorEmailField.setName("author.email"); // NOI18N

        bugTrackerLabel.setLabelFor(bugTrackerField);
        bugTrackerLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.bugTrackerLabel.text")); // NOI18N

        bugTrackerField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.bugs.web.text")); // NOI18N
        bugTrackerField.setName("bugs.web"); // NOI18N

        licenseLabel.setLabelFor(licenseField);
        licenseLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.licenseLabel.text")); // NOI18N

        licenseField.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "none", "bsd", "mit", "gplv2", "gplv3", "cddl", "apache20" }));
        licenseField.setName("license.type"); // NOI18N

        mainFileLabel.setLabelFor(mainFileField);
        mainFileLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.mainFileLabel.text")); // NOI18N

        mainFileField.setEditable(false);
        mainFileField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.main.text")); // NOI18N
        mainFileField.setName("main"); // NOI18N

        status.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.status.text")); // NOI18N

        keywordsLabel.setLabelFor(keywordsField);
        keywordsLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.keywordsLabel.text")); // NOI18N

        keywordsField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.keywords.text")); // NOI18N
        keywordsField.setToolTipText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.keywordsField.toolTipText")); // NOI18N
        keywordsField.setName("keywords"); // NOI18N

        commandLineField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.commandLineField.text")); // NOI18N

        commandLineLabel.setLabelFor(commandLineField);
        commandLineLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.commandLineLabel.text")); // NOI18N
        commandLineLabel.setToolTipText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.commandLineLabel.toolTipText")); // NOI18N

        jButton1.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseMainFile(evt);
            }
        });

        authorURLLabel.setLabelFor(authorURLField);
        authorURLLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.authorURLLabel.text")); // NOI18N

        authorURLField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.author.url.text")); // NOI18N
        authorURLField.setName("author.url"); // NOI18N

        versionLabel.setLabelFor(versionField);
        versionLabel.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.versionLabel.text")); // NOI18N

        versionField.setText(org.openide.util.NbBundle.getMessage(PropertiesPanel.class, "PropertiesPanel.version.text")); // NOI18N
        versionField.setName("version"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameLabel)
                            .addComponent(descriptionLabel)
                            .addComponent(authorNameLabel)
                            .addComponent(authorEmailLabel)
                            .addComponent(bugTrackerLabel)
                            .addComponent(licenseLabel)
                            .addComponent(mainFileLabel)
                            .addComponent(keywordsLabel)
                            .addComponent(commandLineLabel)
                            .addComponent(authorURLLabel)
                            .addComponent(versionLabel))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addComponent(nameField, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addComponent(authorNameField, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addComponent(authorEmailField, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addComponent(bugTrackerField, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addComponent(keywordsField, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addComponent(commandLineField, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(mainFileField, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1)
                                .addGap(6, 6, 6))
                            .addComponent(authorURLField)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(licenseField, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(versionField))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLabel)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(versionLabel)
                    .addComponent(versionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(descriptionLabel)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(authorNameLabel)
                    .addComponent(authorNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(authorEmailLabel)
                    .addComponent(authorEmailField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(authorURLLabel)
                    .addComponent(authorURLField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bugTrackerField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bugTrackerLabel))
                .addGap(7, 7, 7)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(licenseLabel)
                    .addComponent(licenseField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mainFileLabel)
                    .addComponent(mainFileField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keywordsLabel)
                    .addComponent(keywordsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(commandLineField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandLineLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addComponent(status)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

private void browseMainFile(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseMainFile
    FileObject fo = props.project().getLookup().lookup( NodeJSProject.class ).showSelectMainFileDialog();
    if (fo != null) {
        String path = FileUtil.getRelativePath( props.project().getProjectDirectory(), fo );
        mainFileField.setText( path );
    }
}//GEN-LAST:event_browseMainFile
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField authorEmailField;
    private javax.swing.JLabel authorEmailLabel;
    private javax.swing.JTextField authorNameField;
    private javax.swing.JLabel authorNameLabel;
    private javax.swing.JTextField authorURLField;
    private javax.swing.JLabel authorURLLabel;
    private javax.swing.JTextField bugTrackerField;
    private javax.swing.JLabel bugTrackerLabel;
    private javax.swing.JTextField commandLineField;
    private javax.swing.JLabel commandLineLabel;
    private javax.swing.JTextArea descriptionField;
    private javax.swing.JLabel descriptionLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField keywordsField;
    private javax.swing.JLabel keywordsLabel;
    private javax.swing.JComboBox licenseField;
    private javax.swing.JLabel licenseLabel;
    private javax.swing.JTextField mainFileField;
    private javax.swing.JLabel mainFileLabel;
    private javax.swing.JTextField nameField;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JLabel status;
    private javax.swing.JTextField versionField;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
}
