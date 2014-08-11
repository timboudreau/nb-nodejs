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
package org.netbeans.modules.avatar.platform.wizard;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.util.NbBundle.Messages;

@Messages ({"DISPLAY_NAME=Name for this copy of NodeJS", "DISPLAY_NAME_STEP=Set Name"})
public final class DisplayNameVisualPanel extends JPanel implements DocumentListener {

    public DisplayNameVisualPanel () {
        initComponents();
        jTextField1.getDocument().addDocumentListener( this );
    }

    @Override
    public String getName () {
        return Bundle.DISPLAY_NAME_STEP();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();

        jLabel1.setLabelFor(jTextField1);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(DisplayNameVisualPanel.class, "ChooseNodeJSBinaryVisualPanel.jLabel1.text")); // NOI18N

        jTextField1.setText(org.openide.util.NbBundle.getMessage(DisplayNameVisualPanel.class, "ChooseNodeJSBinaryVisualPanel.jTextField1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextField1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 489, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(366, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
    private boolean inPropertyChange;

    private boolean inSetDisplayName;

    public String getDisplayName () {
        return jTextField1.getText();
    }
    

    public void setDisplayName ( String name ) {
        if (inSetDisplayName) {
            return;
        }
        try {
            inSetDisplayName = true;
            if (!inUpdate) {
                jTextField1.setText( name );
            }
            firePropertyChange("displayName", null, jTextField1.getText());
        } finally {
            inSetDisplayName = false;
        }
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
            setDisplayName( s );
        } finally {
            inUpdate = false;
        }
    }
}
