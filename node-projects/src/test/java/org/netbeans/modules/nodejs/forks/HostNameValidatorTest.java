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
import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class HostNameValidatorTest {

    private Problem check(Validator<String> v, String str){
        Problems p = new Problems();
        v.validate(p, "", str);
        return p.getLeadProblem();
    }

    @Test
    public void testValidate() {
        assertTrue(true);
        Validator<String> v = new HostNameValidator(true);
        // Problems p = new Problems();

        assertNull(check(v, "www.foo.com"));
        assertNull(check(v, "www.foo.com:8080"));
        //test AIOOBE
        assertTrue(check(v,  "bar.com ").isFatal());
        assertTrue(check(v,  " bar.com").isFatal());
        assertTrue(check(v,  ":").isFatal());

        v = new HostNameValidator(false);
        assertTrue(check(v,  "myhost.com:204").isFatal());
        assertTrue(check(v,  "myhost.com:204-").isFatal());

        v = new HostNameValidator(true);
        assertTrue(check(v,  "128.foo.129").isFatal());
        assertTrue(check(v,  "128.foo.129:1024").isFatal());
        assertTrue(check(v,  "www.foo.com:abcd").isFatal());
        assertTrue(check(v,  "foo.").isFatal());
        assertTrue(check(v,  "128.").isFatal());
        assertNull(check(v,  "www.foo-bar.com"));
        assertTrue(check(v,  "www.foo-bar.com-").isFatal());
        assertTrue(check(v,  "-www.foo-bar.com").isFatal());
        assertTrue(check(v,  "www.foo-bar.com ").isFatal());
        assertTrue(check(v,  " www.foo-bar.com").isFatal());
        assertTrue(check(v,  "204.128").isFatal());

        assertTrue(check(v,  "foo@bar.com").isFatal());
        assertTrue(check(v,  "foo.бar.com").isFatal());
        assertTrue(check(v,  "фу.бар.ком").isFatal());
        assertTrue(check(v,  "myhost:").isFatal());
        assertTrue(check(v,  "myhost::").isFatal());
        assertTrue(check(v,  "www.foo.com::2040").isFatal());
        assertTrue(check(v,  "www.foo.com:2040802").isFatal());
        assertTrue(check(v,  "www.foo.com:2040:2802").isFatal());
        assertTrue(check(v,  "www.foo.com:2040:").isFatal());
        assertTrue(check(v,  "www..foo.com").isFatal());
    }
    
    @Test
    public void testEmailAddress() {
        assertNoProblems("foo@gmail.com");
        assertNoProblems("bar@502.com");
        assertHasProblems("foo@gmail.1");
//        assertHasProblems("foo@7.3.2.256");
        assertHasProblems("foo@7.3.2.5.3");
    }
    
    private void assertNoProblems(String addr) {
        Problems p = testOneAddress(addr);
        assertFalse(addr + ": " + p.getLeadProblem(), testOneAddress(addr).allProblems().iterator().hasNext());
    }

    private Problem assertHasProblems(String addr) {
        Problems p = testOneAddress(addr);
        assertTrue(addr, p.allProblems().iterator().hasNext());
        return p.getLeadProblem();
    }
    
    private Problems testOneAddress(String addr) {
        Problems p = new Problems();
        EmailAddressValidator v = new EmailAddressValidator();
        v.validate( p, addr, addr );
        return p;
    }

    @Test
    public void testValidateHostOrIP() {
        assertTrue(true);
        Validator<String> v = new ValidHostNameOrIPValidator(true);

        assertNull(check(v, "www.foo.com"));
        assertNull(check(v, "www.foo.com:8080"));
        assertTrue(check(v,  "bar.com ").isFatal());
        assertTrue(check(v,  " bar.com").isFatal());
        assertTrue(check(v,  ":").isFatal());
        assertTrue(check(v,  "myhost:").isFatal());

        v = new ValidHostNameOrIPValidator(false);
        assertTrue(check(v,  "myhost.com:204").isFatal());
        assertTrue(check(v,  "myhost.com:204-").isFatal());
        assertNull(check(v,  "205.foo.com"));

        v = new ValidHostNameOrIPValidator(true);
        assertTrue(check(v,  "128.foo.129").isFatal());
        assertTrue(check(v,  "128.foo.129:1024").isFatal());
        assertTrue(check(v,  "www.foo.com:abcd").isFatal());
        assertTrue(check(v,  "foo.").isFatal());
        assertTrue(check(v,  "128.").isFatal());
        assertNull(check(v, "www.foo-bar.com"));
        assertTrue(check(v,  "www.foo-bar.com-").isFatal());
        assertTrue(check(v,  "-www.foo-bar.com").isFatal());
        assertTrue(check(v,  "www.foo-bar.com ").isFatal());
        assertTrue(check(v,  " www.foo-bar.com").isFatal());
        assertTrue(check(v,  "204.128").isFatal());

        assertTrue(check(v,  "foo@bar.com").isFatal());
        assertTrue(check(v,  "foo.бar.com").isFatal());
        assertTrue(check(v,  "фу.бар.ком").isFatal());
        assertTrue(check(v,  "фу.бар.ком:1023").isFatal());
        assertTrue(check(v,  "фу.бар.ком:102a").isFatal());
        assertTrue(check(v,  "2050").isFatal());
        assertTrue(check(v,  ":2050").isFatal());
        assertTrue(check(v,  "фу.бар.ком:10232034").isFatal());
        assertTrue(check(v,  "www..foo.com").isFatal());
        assertTrue(check(v,  "192.168.2.1::1024").isFatal());
        assertTrue(check(v,  "192.168.2.1::1024:").isFatal());
        assertTrue(check(v,  ".").isFatal());
        assertTrue(check(v,  "").isFatal());
    }
}