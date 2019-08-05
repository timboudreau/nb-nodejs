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

import java.awt.EventQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.nodejs.DefaultExecutable;
import org.netbeans.modules.nodejs.Npm;
import org.netbeans.modules.nodejs.forks.EmailAddressValidator;
import org.netbeans.modules.nodejs.ui.UiUtil;
import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

public final class NodePanel extends JPanel implements ValidationUI, DocumentListener {
    private final NodeOptionsPanelController controller;
    private final SwingValidationGroup g;
    private final DefaultExecutable exe = DefaultExecutable.get();

    @SuppressWarnings ("LeakingThisInConstructor")
    NodePanel ( NodeOptionsPanelController controller ) {
        this.controller = controller;
        g = SwingValidationGroup.create( this );
        initComponents();
        g.add( portField, ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING, ValidatorUtils.merge( StringValidators.REQUIRE_VALID_NUMBER, ValidatorUtils.merge( StringValidators.REQUIRE_VALID_INTEGER, StringValidators.REQUIRE_NON_NEGATIVE_NUMBER ) ) ) );
        g.add( binaryField, ValidatorUtils.merge( StringValidators.REQUIRE_NON_EMPTY_STRING, ValidatorUtils.merge( StringValidators.FILE_MUST_EXIST, StringValidators.FILE_MUST_BE_FILE ) ) );
        g.add( sourcesField, new FileOrArchiveValidator() );
        g.add( authorField, StringValidators.REQUIRE_NON_EMPTY_STRING );
        g.add( emailField, new AllowNullValidator( new EmailAddressValidator() ) );
        UiUtil.prepareComponents( this );
        portField.getDocument().addDocumentListener( this );
        binaryField.getDocument().addDocumentListener( this );
    }

    private static boolean containsJsFiles ( File folder ) {
        if (folder.isDirectory()) {
            File f = new File( folder, "http.js" );
            if (f.exists()) {
                return true;
            }
        }
        return false;
    }

    private static final class AllowNullValidator extends AbstractValidator<String> {
        private final Validator<String> other;

        AllowNullValidator ( Validator<String> other ) {
            super( String.class );
            this.other = other;
        }

        @Override
        public void validate ( Problems prblms, String string, String model ) {
            if (model == null) {
                return;
            }
            other.validate( prblms, string, model );
        }
    }

    private static final class FileOrArchiveValidator extends AbstractValidator<String> {
        FileOrArchiveValidator () {
            super( String.class );
        }

        @Override
        public void validate ( Problems prblms, String string, String model ) {
            if (model == null || "".equals( model )) {
                prblms.add( NbBundle.getMessage( NodePanel.class, "NO_SOURCES" ), Severity.INFO );
                return;
            }
            File f = new File( model );
            if (!f.exists()) {
                prblms.add( NbBundle.getMessage( NodePanel.class, "SOURCE_DIR_DOES_NOT_EXIST", model ), Severity.WARNING );
                return;
            }
            if (f.isDirectory() && !downloading) {
                boolean jsFound = containsJsFiles( f );
                File child = new File( f, "lib" );
                jsFound = child.exists() && containsJsFiles( child );
                if (!jsFound) {
                    prblms.add( NbBundle.getMessage( NodePanel.class, "NO_JS_FILES", f.getName() ), Severity.WARNING );
                }
                return;
            }
            if (!f.isDirectory()) {
                if (!f.getName().endsWith( ".zip" ) && !f.getName().endsWith( ".jar" )) {
                    prblms.add( NbBundle.getMessage( NodePanel.class, "NOT_FILE_OR_ARCHIVE", model ), Severity.WARNING );
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        problemLabel = (JLabel) g.createProblemLabel();
        binaryLabel = new javax.swing.JLabel();
        binaryField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        portLabel = new javax.swing.JLabel();
        portField = new javax.swing.JTextField();
        sourceLabel = new javax.swing.JLabel();
        sourcesField = new javax.swing.JTextField();
        browseForSources = new javax.swing.JButton();
        packageHeading = new javax.swing.JLabel();
        authorLabel = new javax.swing.JLabel();
        authorField = new javax.swing.JTextField();
        emailLabel = new javax.swing.JLabel();
        emailField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        loginField = new javax.swing.JTextField();
        downloadButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        npmLabel = new javax.swing.JLabel();
        npmField = new javax.swing.JTextField();
        npmBrowse = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(problemLabel, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.problemLabel.text")); // NOI18N

        binaryLabel.setLabelFor(binaryField);
        org.openide.awt.Mnemonics.setLocalizedText(binaryLabel, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.binaryLabel.text")); // NOI18N

        binaryField.setText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.binary.text")); // NOI18N
        binaryField.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.binaryField.toolTipText")); // NOI18N
        binaryField.setName("binary"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        portLabel.setLabelFor(portField);
        org.openide.awt.Mnemonics.setLocalizedText(portLabel, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.portLabel.text")); // NOI18N

        portField.setText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.port.text")); // NOI18N
        portField.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.portField.toolTipText")); // NOI18N
        portField.setName("port"); // NOI18N

        sourceLabel.setLabelFor(sourcesField);
        org.openide.awt.Mnemonics.setLocalizedText(sourceLabel, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.sourceLabel.text")); // NOI18N

        sourcesField.setText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.sources.text")); // NOI18N
        sourcesField.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.sourcesField.toolTipText")); // NOI18N
        sourcesField.setName("sources"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseForSources, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.browseForSources.text")); // NOI18N
        browseForSources.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.browseForSources.toolTipText")); // NOI18N
        browseForSources.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseForSourcesActionPerformed(evt);
            }
        });

        packageHeading.setFont(packageHeading.getFont().deriveFont(packageHeading.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(packageHeading, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.packageHeading.text")); // NOI18N
        packageHeading.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.packageHeading.toolTipText")); // NOI18N
        packageHeading.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, javax.swing.UIManager.getDefaults().getColor("controlShadow")));

        authorLabel.setLabelFor(authorField);
        org.openide.awt.Mnemonics.setLocalizedText(authorLabel, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.authorLabel.text")); // NOI18N

        authorField.setText(NodePanel.getAuthor());
        authorField.setName("author"); // NOI18N

        emailLabel.setLabelFor(emailField);
        org.openide.awt.Mnemonics.setLocalizedText(emailLabel, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.emailLabel.text")); // NOI18N

        emailField.setText(NodePanel.getEmail());
        emailField.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.emailField.toolTipText")); // NOI18N
        emailField.setName("email"); // NOI18N

        jLabel1.setLabelFor(loginField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.jLabel1.text")); // NOI18N

        loginField.setText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.login.text")); // NOI18N
        loginField.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.login.toolTipText")); // NOI18N
        loginField.setName("login"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(downloadButton, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.downloadButton.text")); // NOI18N
        downloadButton.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.downloadButton.toolTipText")); // NOI18N
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadSources(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.jLabel2.text")); // NOI18N

        npmLabel.setLabelFor(npmField);
        org.openide.awt.Mnemonics.setLocalizedText(npmLabel, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.npmLabel.text")); // NOI18N

        npmField.setText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.npmField.text")); // NOI18N
        npmField.setToolTipText(org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.npmField.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(npmBrowse, org.openide.util.NbBundle.getMessage(NodePanel.class, "NodePanel.npmBrowse.text")); // NOI18N
        npmBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                npmBrowseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(packageHeading, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(184, 184, 184))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(portLabel)
                            .addComponent(sourceLabel)
                            .addComponent(authorLabel)
                            .addComponent(emailLabel)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(sourcesField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(downloadButton))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(loginField, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel2))
                                    .addComponent(emailField, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(authorField, javax.swing.GroupLayout.Alignment.TRAILING))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseForSources)
                        .addContainerGap())))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(problemLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(binaryLabel)
                            .addComponent(npmLabel))
                        .addGap(23, 23, 23)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(portField, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(binaryField)
                                    .addComponent(npmField))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(browseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(npmBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(binaryLabel)
                    .addComponent(binaryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(npmLabel)
                    .addComponent(npmField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(npmBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(portLabel)
                    .addComponent(portField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sourcesField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseForSources)
                    .addComponent(sourceLabel)
                    .addComponent(downloadButton))
                .addGap(18, 18, 18)
                .addComponent(packageHeading)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(authorLabel)
                    .addComponent(authorField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(emailLabel)
                    .addComponent(emailField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(loginField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addComponent(problemLabel))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        String s = exe.askUserForExecutableLocation();
        if (s != null) {
            binaryField.setText( s );
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void browseForSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseForSourcesActionPerformed
        File where = new FileChooserBuilder( NodePanel.class ).setApproveText( NbBundle.getMessage( NodePanel.class, "BROWSE_FOR_SOURCES_APPROVE" ) ).setTitle( NbBundle.getMessage( NodePanel.class, "BROWSE_FOR_SOURCES" ) ).showOpenDialog();
        if (where != null) {
            sourcesField.setText( where.getAbsolutePath() );
        }
    }//GEN-LAST:event_browseForSourcesActionPerformed
    private static volatile boolean downloading;

    private void downloadSources(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadSources
        downloadButton.setEnabled( false );
        final File targetDir = new FileChooserBuilder( NodePanel.class.getName() + "_sources" ).setDirectoriesOnly( true ).setTitle( NbBundle.getMessage( NodePanel.class, "DOWNLOAD_FOLDER" ) ).showOpenDialog();
        String destName;
        if (!new File( targetDir, "node" ).exists()) {
            destName = "node";
        } else {
            int ix = 0;
            while ( new File( targetDir, "node_" + ix++ ).exists() ) {
            }
            destName = "node_" + ix;
        }
        if (targetDir == null) {
            downloadButton.setEnabled( true );
            return;
        }
        final String name = destName == null ? "node" : destName;
        RequestProcessor.getDefault().post( new Runnable() {
            int eqCount;
            volatile File dest;

            @Override
            public void run () {
                if (!EventQueue.isDispatchThread()) {
                    downloading = true;
                    ProgressHandle h = ProgressHandleFactory.createHandle( NbBundle.getMessage( NodePanel.class, "DOWNLOADING_SOURCES" ) );
                    h.start( 4 );
                    h.progress( "which git" );
                    System.out.println( "run which git" );
                    ProcessBuilder b = new ProcessBuilder( "which", "git" );
                    try {
                        Process p = b.start();
                        InputStream in = p.getInputStream();
                        int ix = p.waitFor();
                        if (ix != 0) {
                            return;
                        }
                        h.progress( 1 );
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        FileUtil.copy( in, out );
                        String s = new String( out.toByteArray() ).trim();
                        System.out.println( "Git is " + s );
                        dest = new File( targetDir, name );
                        if (!dest.exists()) {
                            dest.mkdirs();
                        }
                        System.out.println( "Dest is " + dest );
                        ProcessBuilder b2 = new ProcessBuilder( s, "clone", "https://github.com/nodejs/node", name );
                        b2.directory( dest.getParentFile() );
                        System.out.println( "starting git clone in " + dest.getAbsolutePath() );
                        Process p2 = b2.start();
                        h.progress( "git clone ", 2 );
                        Thread.sleep( 1000 );
                        h.progress( 3 );
                        EventQueue.invokeLater( this );
                        int code = p2.waitFor();
                        System.out.println( "git exit code was " + code );
                    } catch ( InterruptedException ex ) {
                        Exceptions.printStackTrace( ex );
                    } catch ( IOException ex ) {
                        Exceptions.printStackTrace( ex );
                    } finally {
                        h.finish();
                        downloading = false;
                        EventQueue.invokeLater( this );
                    }
                } else {
                    switch ( eqCount++ ) {
                        case 0:
                            if (dest != null) {
                            sourcesField.setText( dest.getAbsolutePath() );
                            g.performValidation(); //get rid of the warning
                            break;
                        }
                        case 1:
                            downloadButton.setEnabled( true );
                            if (dest != null && dest.exists()) {
                                sourcesField.setText( dest.getAbsolutePath() );
                                DefaultExecutable.get().setSourcesLocation( dest.getAbsolutePath() );
                            }
                    }
                }

            }
        } );

    }//GEN-LAST:event_downloadSources

    private void npmBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_npmBrowseActionPerformed
        Npm npm = Npm.getDefault();
        String pth = npm.askUser( false );
        if (pth != null) {
            npmField.setText( pth );
        }
    }//GEN-LAST:event_npmBrowseActionPerformed

    void load () {
        g.runWithValidationSuspended( new Runnable() {
            @Override
            public void run () {
                String s = exe.getNodeExecutable( false );
                if (s != null) {
                    binaryField.setText( s );
                } else {
                    binaryField.setText( "" );
                }
                portField.setText( "" + exe.getDefaultPort() );
                s = exe.getSourcesLocation();
                if (s != null) {
                    sourcesField.setText( s );
                }
                Preferences p = preferences();
                authorField.setText( getAuthor() );
                emailField.setText( getEmail() );
                loginField.setText( p.get( "login", "YOUR_LOGIN" ) );
                Npm npm = Npm.getDefault();
                String npmPath = npm.exePath( false);
                if (npmPath != null) {
                    npmField.setText(npmPath);
                }
            }
        } );
    }

    public static Preferences preferences () {
        return NbPreferences.forModule( NodePanel.class ).node( "options" );
    }

    void store () {
        exe.setNodeExecutable( binaryField.getText() );
        exe.setDefaultPort( Integer.parseInt( portField.getText() ) );
        exe.setSourcesLocation( sourcesField.getText() );
        Preferences p = preferences();
        p.put( "author", authorField.getText() );
        p.put( "email", emailField.getText() );
        p.put( "login", loginField.getText() );
        Npm npm = Npm.getDefault();
        String s = npmField.getText().trim();
        if (!s.isEmpty()) {
            // will not actually change it unless modified and exists
            npm.setExePath( s );
        }
        try {
            p.flush();
        } catch ( BackingStoreException ex ) {
            Exceptions.printStackTrace( ex );
        }
    }

    public static String getAuthor () {
        String nm = DotGitConfig.getDefault().get( "user", "name", System.getProperty("user.name"));
        return preferences().get( "author", nm );
    }

    public static String getLogin () {
        return preferences().get( "login", "YOUR_LOGIN" );
    }

    public static String getEmail () {
        String em = DotGitConfig.getDefault().get( "user", "email", "you@email.example");
        return preferences().get( "email", em );
    }

    boolean valid () {
        return controller.isValid();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField authorField;
    private javax.swing.JLabel authorLabel;
    private javax.swing.JTextField binaryField;
    private javax.swing.JLabel binaryLabel;
    private javax.swing.JButton browseButton;
    private javax.swing.JButton browseForSources;
    private javax.swing.JButton downloadButton;
    private javax.swing.JTextField emailField;
    private javax.swing.JLabel emailLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField loginField;
    private javax.swing.JButton npmBrowse;
    private javax.swing.JTextField npmField;
    private javax.swing.JLabel npmLabel;
    private javax.swing.JLabel packageHeading;
    private javax.swing.JTextField portField;
    private javax.swing.JLabel portLabel;
    private javax.swing.JLabel problemLabel;
    private javax.swing.JLabel sourceLabel;
    private javax.swing.JTextField sourcesField;
    // End of variables declaration//GEN-END:variables

    @Override
    public void clearProblem () {
        controller.setValid( true );
    }

    @Override
    public void showProblem ( Problem prblm ) {
        controller.setValid( !prblm.isFatal() );
    }

    @Override
    public void insertUpdate ( DocumentEvent e ) {
        controller.changed();
    }

    @Override
    public void removeUpdate ( DocumentEvent e ) {
        controller.changed();
    }

    @Override
    public void changedUpdate ( DocumentEvent e ) {
        controller.changed();
    }
}
