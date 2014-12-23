/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.nodejs.borrowed;

import java.io.IOException;
import java.util.Map;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 * Factory which will write one file on project creation.
 *
 * @author Tim Boudreau
 */
public abstract class FileCreator {
    /**
     * The relative path (unix-style / directory separators) to the
     * directory where the file should be created.
     */
    protected final String dest;
    /**
     * The name of the file to be created.
     */
    protected final String name;
    /**
     * If true, the created file will be added to the list of files to
     * open in the editor once the project is created.
     */
    protected final boolean openOnLoad;

    /**
     *
     * @param destPath Relative path (unix style) from the project root
     * to the directory the file should be created in
     * @param name The name of the file to create
     * @param openOnLoad Whether or not this file should be opened in the
     * editor
     */
    public FileCreator(String destPath, String name, boolean openOnLoad) {
        this.dest = destPath;
        this.name = name;
        this.openOnLoad = openOnLoad;
    }

    final String getName() {
        return name;
    }

    /**
     * Actually create the file.  Note this method is responsible for
     * ensuring that the directory to be created really exists (note:
     * as of NB 6.7 you may get a SyncFailedException if you try to
     * create a directory that already exists - it is safe to ignore it).
     *
     * @param project The root directory of the project
     * @param params The substitution parameters that were passed into
     * <code>ProjectCreator.createProject()</code>.
     *
     * @return The created DataObject
     * @throws IOException if something goes wrong
     */
    public abstract DataObject create(FileObject project,
            Map<String, String> params) throws IOException;

    boolean isOpenOnLoad() {
        return openOnLoad;
    }

    @Override
    public String toString() {
        return "Create " + dest + (name == null ? "" : name);
    }

}
