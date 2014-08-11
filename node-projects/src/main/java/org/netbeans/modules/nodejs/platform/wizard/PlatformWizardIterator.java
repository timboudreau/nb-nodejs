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

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.nodejs.api.NodeJSPlatformType;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;

public final class PlatformWizardIterator implements WizardDescriptor.Iterator<WizardDescriptor> {
    private final ChangeSupport supp = new ChangeSupport( this );
    private int index;

    private List<WizardDescriptor.Panel<WizardDescriptor>> panels;

    private final Map<NodeJSPlatformType, List<WizardDescriptor.Panel<WizardDescriptor>>> panelsForType = new HashMap<>();

    private List<WizardDescriptor.Panel<WizardDescriptor>> panelsFor ( NodeJSPlatformType type ) {
        List<WizardDescriptor.Panel<WizardDescriptor>> result = panelsForType.get( type );
        if (result == null) {
            result = type.getAddPlatformWizardPages();
            panelsForType.put( type, result );
        }
        return result;
    }

    ChoosePlatformPanel first;

    private List<WizardDescriptor.Panel<WizardDescriptor>> getPanels () {
        if (panels == null) {
            panels = new ArrayList<>();
            if (NodeJSPlatformType.allTypes().size() > 1) {
                if (first == null) {
                    first = new ChoosePlatformPanel();
                    first.addChangeListener( new ChangeListener() {

                        @Override
                        public void stateChanged ( ChangeEvent ce ) {
                            NodeJSPlatformType type = first.getType();
                            panels.clear();
                            panels.add( first );
                            panels.addAll( panelsFor( type ) );
                            updatePanels();
                        }
                    } );
                }
                panels.add( first );
            } else {
                panels.clear();
                panels.addAll( panelsFor( NodeJSPlatformType.allTypes().iterator().next() ) );
            }
            updatePanels();
        }
        return panels;
    }

    private void updatePanels () {
        //            panels.add( new PlatformWizardPanel2() );
//            panels.add( new PlatformWizardPanel3() );
        String[] steps = new String[panels.size()];
        for (int i = 0; i < panels.size(); i++) {
            Component c = panels.get( i ).getComponent();
            // Default step name to component name of panel.
            steps[i] = c.getName();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                jc.putClientProperty( WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i );
                jc.putClientProperty( WizardDescriptor.PROP_CONTENT_DATA, steps );
                jc.putClientProperty( WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true );
                jc.putClientProperty( WizardDescriptor.PROP_CONTENT_DISPLAYED, true );
                jc.putClientProperty( WizardDescriptor.PROP_CONTENT_NUMBERED, true );
            }
        }
        supp.fireChange();
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current () {
        return getPanels().get( index );
    }

    @Override
    public String name () {
        return index + 1 + ". of " + getPanels().size();
    }

    @Override
    public boolean hasNext () {
        return index < getPanels().size() - 1;
    }

    @Override
    public boolean hasPrevious () {
        return index > 0;
    }

    @Override
    public void nextPanel () {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
    }

    @Override
    public void previousPanel () {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
    }

    // If nothing unusual changes in the middle of the wizard, simply:
    @Override
    public void addChangeListener ( ChangeListener l ) {
        supp.addChangeListener( l );
    }

    @Override
    public void removeChangeListener ( ChangeListener l ) {
        supp.removeChangeListener( l );
    }

}
