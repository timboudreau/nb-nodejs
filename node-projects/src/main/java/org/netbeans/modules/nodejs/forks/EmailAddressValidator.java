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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.validation.api.AbstractValidator;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.openide.util.NbBundle;

/**
 * DELETEME when NetBeans on simplevalidation 1.8
 *
 * @author Tim Boudreau
 */
public class EmailAddressValidator extends AbstractValidator<String> {
    private final Validator<String> hv = new ValidHostNameOrIPValidator( false );
    private final Validator<String> spv = StringValidators.NO_WHITESPACE;
    private final Validator<String> encv = StringValidators.encodableInCharset( "US-ASCII" );

    // Forked from simplevalidation because NetBeans is still using an
    // old version.  Can be deleted once NetBeans is on simplevalidation 1.8
    static final Pattern ADDRESS_PATTERN = Pattern.compile( "(.*?)<(.*)>$" ); //NOI18N

    public EmailAddressValidator () {
        super( String.class );
    }

    @Override
    public void validate ( Problems problems, String compName, String model ) {
        Matcher m = ADDRESS_PATTERN.matcher( model );
        String address;
        if (m.lookingAt()) {
            if (m.groupCount() == 2) {
                address = m.group( 2 );
            } else {
                address = m.group( 1 );
            }
        } else {
            address = model;
        }
        String[] nameAndHost = address.split( "@" );
        if (nameAndHost.length == 0) {
            problems.add( NbBundle.getMessage( StringValidators.class,
                    "NO_AT_SYMBOL", compName, address ) );
            return;
        }
        if (nameAndHost.length == 1 && nameAndHost[0].contains( "@" )) {
            problems.add( NbBundle.getMessage( StringValidators.class,
                    "EMAIL_MISSING_HOST", compName, nameAndHost[0] ) );
            return;
        }
        if (nameAndHost.length > 2) {
            problems.add( NbBundle.getMessage( StringValidators.class,
                    "EMAIL_HAS_>1_@", compName, address ) );
            return;
        }
        String name = nameAndHost[0];
        if (name.length() == 0) {
            problems.add( NbBundle.getMessage( StringValidators.class,
                    "EMAIL_MISSING_NAME", compName, name ) );
            return;
        }
        if (name.length() > 64) {
            problems.add( NbBundle.getMessage( StringValidators.class,
                    "ADDRESS_MAY_BE_TOO_LONG", compName, name ), Severity.WARNING );
        }
        String host = nameAndHost.length >= 2 ? nameAndHost[1] : null;
        if (host == null) {
            problems.add( NbBundle.getMessage( StringValidators.class,
                    "EMAIL_MISSING_HOST", compName, nameAndHost[0] ) );
            return;
        }
        hv.validate( problems, compName, host );
        spv.validate( problems, compName, name );
        encv.validate( problems, compName, address );
    }
}
