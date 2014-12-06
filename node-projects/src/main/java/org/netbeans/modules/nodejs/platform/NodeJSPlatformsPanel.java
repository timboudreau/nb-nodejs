/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.nodejs.platform;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.modules.nodejs.api.NodeJSExecutable;
import org.netbeans.modules.nodejs.api.NodeJSPlatformType;
import org.netbeans.modules.nodejs.platform.wizard.PlatformWizardIterator;
import org.netbeans.modules.nodejs.ui.UiUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages ("ADD_PLATFORM=Add NodeJS Platform")
final class NodeJSPlatformsPanel extends javax.swing.JPanel {

    public NodeJSPlatformsPanel () {
        initComponents();
        platformList.setCellRenderer( new PlatformListCellRenderer() );
        platformList.addListSelectionListener( new ListSelectionListener() {

            @Override
            public void valueChanged ( ListSelectionEvent lse ) {
                Object o = platformList.getSelectedValue();
                if (o instanceof NodeJSExecutable) {
                    NodeJSExecutable n = (NodeJSExecutable) o;
                    NodeJSPlatformsPanel.this.nameField.setText( n.name() );
                    pathField.setText(n.path());
                }
            }
        } );
        refresh( null );
        UiUtil.prepareComponents( this );
    }

    private void refresh ( String nm ) {
        Object old = platformList.getSelectedValue();
        String toSelect = "default";
        if (nm != null) {
            if (old != null && old instanceof NodeJSExecutable) {
                toSelect = ((NodeJSExecutable) old).name();
            }
        } else {
            toSelect = nm;
        }
        DefaultListModel<NodeJSExecutable> mdl = new DefaultListModel<NodeJSExecutable>();
        Set<NodeJSExecutable> seen = new HashSet<>();
        NodeJSExecutable selectMe = null;
        for (NodeJSExecutable exe : NodeJSPlatforms.all()) {
            if (!seen.contains( exe )) {
                mdl.addElement( exe );
                seen.add( exe );
            }
            if (exe.name().equals( toSelect )) {
                selectMe = exe;
            }
        }
        platformList.setModel( mdl );
        if (selectMe != null) {
            platformList.setSelectedValue( selectMe, true );
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

        platforms = new javax.swing.JScrollPane();
        platformList = new javax.swing.JList();
        pathLabel = new javax.swing.JLabel();
        pathField = new javax.swing.JTextField();
        nameLabel = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        versionLabel = new javax.swing.JLabel();
        versionField = new javax.swing.JTextField();

        platformList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        platforms.setViewportView(platformList);

        pathLabel.setLabelFor(pathField);
        org.openide.awt.Mnemonics.setLocalizedText(pathLabel, org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.pathLabel.text")); // NOI18N

        pathField.setEditable(false);
        pathField.setText(org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.pathField.text")); // NOI18N

        nameLabel.setLabelFor(nameField);
        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.nameLabel.text")); // NOI18N

        nameField.setEditable(false);
        nameField.setText(org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.nameField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(addButton, org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.addButton.text")); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(removeButton, org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.removeButton.text")); // NOI18N
        removeButton.setEnabled(false);

        versionLabel.setLabelFor(versionField);
        org.openide.awt.Mnemonics.setLocalizedText(versionLabel, org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.versionLabel.text")); // NOI18N

        versionField.setEditable(false);
        versionField.setText(org.openide.util.NbBundle.getMessage(NodeJSPlatformsPanel.class, "NodeJSPlatformsPanel.versionField.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(platforms, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameLabel)
                            .addComponent(pathLabel))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameField, javax.swing.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE)
                            .addComponent(pathField)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(versionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(versionField)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(pathLabel)
                            .addComponent(pathField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(nameLabel)
                            .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(versionLabel)
                            .addComponent(versionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(platforms, javax.swing.GroupLayout.PREFERRED_SIZE, 370, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(addButton)
                            .addComponent(removeButton))))
                .addContainerGap(16, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        WizardDescriptor wiz = new WizardDescriptor( new PlatformWizardIterator() );
        if (NodeJSPlatformType.allTypes().size() == 1) {
            wiz.putProperty( "type", NodeJSPlatformType.allTypes().iterator().next() );
        }
        //             // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
        //             // {1} will be replaced by WizardDescriptor.Iterator.name()
        wiz.setTitleFormat( new MessageFormat( "{0} ({1})" ) );
        wiz.setTitle( Bundle.ADD_PLATFORM() );
        if (DialogDisplayer.getDefault().notify( wiz ) == WizardDescriptor.FINISH_OPTION) {
            System.out.println( "OK " + wiz.getProperties() );
            NodeJSPlatformType type = (NodeJSPlatformType) wiz.getProperty( "type" );
            File f = (File) wiz.getProperty( "file" );
            Map<String, Object> data = (Map<String, Object>) wiz.getProperty( "info" );
            String displayName = (String) wiz.getProperty( "displayName" );
            type.add( f, data, displayName );
            refresh( null );
        }
    }//GEN-LAST:event_addButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JTextField nameField;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField pathField;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JList platformList;
    private javax.swing.JScrollPane platforms;
    private javax.swing.JButton removeButton;
    private javax.swing.JTextField versionField;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
}