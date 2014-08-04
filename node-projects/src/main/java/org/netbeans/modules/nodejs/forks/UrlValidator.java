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

import java.net.MalformedURLException;
import java.net.URL;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.openide.util.NbBundle;

/**
 * DELETEME when NetBeans on simplevalidation 1.8
 * 
 * @author Tim Boudreau
 */
public class UrlValidator implements Validator<String> {
    
    // Forked from simplevalidation because NetBeans is still using an
    // old version.  Can be deleted once NetBeans is on simplevalidation 1.8

    @Override
    public void validate(Problems problems, String compName, String model) {
        try {
            URL url = new URL (model);
            //java.net.url does not require US-ASCII host names,
            //but the spec does
            String host = url.getHost();
            if (!"".equals(host)) { //NOI18N
                new ValidHostNameOrIPValidator(true).validate(problems,
                        compName, host);
                return;
            }
            String protocol = url.getProtocol();
            if ("mailto".equals(protocol)) { //NOI18N
                String emailAddress = url.toString().substring("mailto:".length()); //NOI18N
                emailAddress = emailAddress == null ? "" : emailAddress;
                StringValidators.EMAIL_ADDRESS.validate(problems, compName,
                        emailAddress);
                return;
            }
        } catch (MalformedURLException e) {
            String problem = NbBundle.getMessage(StringValidators.class,
                    "URL_NOT_VALID", model); //NOI18N
            problems.add(problem);
        }
    }

    @Override
    public Class<String> modelType () {
        return String.class;
    }
}
