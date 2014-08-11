/* Copyright (C) 2014 Tim Boudreau

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
package org.netbeans.modules.nodejs.platform.wizard;

import java.io.File;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.NbBundle.Messages;

@Messages ({"LOCATE_BINARY=Locate NodeJS Binary", "STEP_TWO=Locate NodeJS"})
public final class ChooseNodeJSBinaryVisualPanel extends JPanel implements DocumentListener {

    public ChooseNodeJSBinaryVisualPanel () {
        initComponents();
        jTextField1.getDocument().addDocumentListener( this );
    }

    @Override
    public String getName () {
        return Bundle.STEP_TWO();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ChooseNodeJSBinaryVisualPanel.class, "ChooseNodeJSBinaryVisualPanel.jLabel1.text")); // NOI18N

        jTextField1.setText(org.openide.util.NbBundle.getMessage(ChooseNodeJSBinaryVisualPanel.class, "ChooseNodeJSBinaryVisualPanel.jTextField1.text")); // NOI18N
        jTextField1.setToolTipText(org.openide.util.NbBundle.getMessage(ChooseNodeJSBinaryVisualPanel.class, "ChooseNodeJSBinaryVisualPanel.jTextField1.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(ChooseNodeJSBinaryVisualPanel.class, "ChooseNodeJSBinaryVisualPanel.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 403, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addContainerGap(360, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        File f = new FileChooserBuilder( ChooseNodeJSBinaryVisualPanel.class )
                .setFilesOnly( true ).showOpenDialog();
        if (f != null) {
            setFile( f );
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
    private boolean inPropertyChange;

    private File file;
    private boolean inSetFile;

    public void setFile ( File file ) {
        if (inSetFile) {
            return;
        }
        try {
            inSetFile = true;
            if (!Objects.equals( this.file, file )) {
                File old = this.file;
                this.file = file;
                if (file != null && !inUpdate) {
                    jTextField1.setText( file.getAbsolutePath() );
                }
                firePropertyChange( "file", old, file );
            }
        } finally {
            inSetFile = false;
        }
    }

    public File getFile () {
        return file;
    }

    @Override
    public void insertUpdate ( DocumentEvent de ) {
        changedUpdate( de );
    }

    @Override
    public void removeUpdate ( DocumentEvent de ) {
        changedUpdate( de );
    }

    private boolean inUpdate;

    @Override
    public void changedUpdate ( DocumentEvent de ) {
        inUpdate = true;
        try {
            String s = jTextField1.getText();
            File f = new File( s );
            setFile( f );
        } finally {
            inUpdate = false;
        }
    }
}
