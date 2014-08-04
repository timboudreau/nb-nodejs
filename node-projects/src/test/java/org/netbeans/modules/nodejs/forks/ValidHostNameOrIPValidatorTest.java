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

import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

/**
 *
 * @author Tim Boudreau
 */
public class ValidHostNameOrIPValidatorTest {

    @Test
    public void testValidate() {
        Validator<String> v = StringValidators.IP_ADDRESS;
        assertValid (v, "192.168.2.1");
        assertValid (v, "127.0.0.1");
        assertValid (v, "192.168.2.1:8100");
        assertNotValid (v, "192.168.2.1.5.2.3.4:8100");
        assertNotValid (v, ":8100");
        assertNotValid (v, "192.168.2.1:");
        assertNotValid (v, "192.168.2.1:81000");
        assertNotValid (v, "192.168.2.1:boo");
        assertNotValid (v, "192.168.2.1:");
        assertNotValid (v, "192.168.boo.1");
        assertNotValid (v, "com.foo.bar.baz:1");
        assertNotValid (v, "com.foo.bar.baz");
        assertNotValid (v, "");

        v = new HostNameValidator(true);
        assertValid (v, "java.sun.com");
        assertValid (v, "central");
        assertValid (v, "sun.com");
        assertValid (v, "netbeans.org");
        assertValid (v, "netbeans.org:2203");
        assertNotValid (v, "netbeans.192.1");
        assertNotValid (v, "netbeans.org.192:239");
        assertNotValid (v, "");

        v = new ValidHostNameOrIPValidator();
        assertValid (v, "192.168.2.1");
        assertValid (v, "192.168.2.1");
        assertValid (v, "192.168.2.1:8100");
        assertValid (v, "java.sun.com");
        assertValid (v, "java.sun.com:8100");
        assertNotValid (v, "");
        assertNotValid (v, "java.100.com:boo");
        assertValid (v, "java.100.com:8100");
        assertNotValid (v, ":8100");
        assertNotValid (v, ".com");
        assertNotValid (v, ".com:8100");
        assertNotValid (v, " .com:8100");
        assertNotValid (v, " foo.com:8100");
        assertNotValid (v, " foo.com");
        assertNotValid (v, "foo.com ");
        assertNotValid (v, ".128");
        assertNotValid (v, ".128.");
        assertNotValid (v, "128.");
        assertNotValid (v, "128 ");
        assertNotValid (v, "128 .");
        assertNotValid (v, "myhost:");
        assertNotValid (v, "foo:bar.myhost:2020");
        assertNotValid (v, "foo:2020.myhost:2020");
        assertNotValid(v, "www.foo.com:2040:2802");
        assertNotValid (v, "1.2");
        assertNotValid (v, "127.0.0.1:");
        assertNotValid (v, "com.foo.bar:203:");
   }

    private void assertValid(Validator<String> v, String string) {
        Problems p = new Problems();
        v.validate(p, "X", string);
        assertNull(p.getLeadProblem());
    }

    private void assertNotValid(Validator<String> v, String string) {
        Problems p = new Problems();
        v.validate(p, "X", string);
        assertTrue( p.hasFatal());
    }


}