/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.nodejs.forks;

import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.openide.util.NbBundle;

/**
 * DELETEME when NetBeans on simplevalidation 1.8
 * 
 * @author Tim Boudreau
 */
final class ValidHostNameOrIPValidator extends AbstractValidator<String> {
    
    // Forked from simplevalidation because NetBeans is still using an
    // old version.  Can be deleted once NetBeans is on simplevalidation 1.8
    
    private final HostNameValidator hostVal;
    private final Validator<String> ipVal = StringValidators.IP_ADDRESS;
    ValidHostNameOrIPValidator(boolean allowPort) {
        super(String.class);
        hostVal = new HostNameValidator(allowPort);
    }

    ValidHostNameOrIPValidator() {
        this(true);
    }

    @Override
    public void validate(Problems problems, String compName, String model) {
        String[] parts = model.split ("\\.");
        boolean hasIntParts = false;
        boolean hasNonIntParts = false;
        if (model.indexOf(" ") > 0 || model.indexOf ("\t") > 0) {
            problems.add (NbBundle.getMessage(StringValidators.class,
                    "HOST_MAY_NOT_CONTAIN_WHITESPACE", compName, model)); //NOI18N
            return;
        }
        if (parts.length == 0) { //the string "."
            problems.add (NbBundle.getMessage(StringValidators.class,
                    "INVALID_HOST_OR_IP", compName, model)); //NOI18N
            return;
        }
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i];
            if (i == parts.length - 1 && s.contains(":")) { //NOI18N
                String[] partAndPort = s.split(":"); //NOI18N
                if (partAndPort.length > 2) {
                    problems.add (NbBundle.getMessage(StringValidators.class,
                            "TOO_MANY_COLONS", compName, model)); //NOI18N
                    return;
                }
                if (partAndPort.length == 0) { //the string ":"
                    problems.add (NbBundle.getMessage(StringValidators.class,
                            "INVALID_HOST_OR_IP", compName, model)); //NOI18N
                    return;
                }
                s = partAndPort[0];
                if (partAndPort.length == 2) {
                    try {
                        Integer.parseInt (partAndPort[1]);
                    } catch (NumberFormatException nfe) {
                        problems.add (NbBundle.getMessage(StringValidators.class,
                            "INVALID_PORT", compName, partAndPort[1])); //NOI18N
                        return;
                    }
                }
            }
            try {
                Integer.parseInt (s);
                hasIntParts = true;
            } catch (NumberFormatException nfe) {
                hasNonIntParts = true;
            }
        }
        if(hasNonIntParts){
            hostVal.validate(problems, compName, model);
        } else {
            assert hasIntParts;
            ipVal.validate(problems, compName, model);
        }
    }

}
