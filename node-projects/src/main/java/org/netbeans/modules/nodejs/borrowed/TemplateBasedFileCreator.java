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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.MapFormat;

class TemplateBasedFileCreator extends FileCreator {
    private final FileObject template;

    public TemplateBasedFileCreator(String destPath, String name, FileObject template, boolean openOnLoad) {
        super(destPath, name, openOnLoad);
        this.template = template;
    }

    public DataObject create(FileObject project, Map<String, String> params) throws IOException {
        String folder = this.dest;
        String fileName = this.name;
        if (fileName == null) {
            int ix = folder.lastIndexOf('/');
            if (ix > 0) {
                fileName = folder.substring(ix + 1);
                folder = folder.substring(0, ix);
            } else {
                fileName = folder;
                folder = null;
            }
        }
        DataObject ob = DataObject.find(template);
        FileObject destFolder = project;
        if (folder != null) {
            destFolder = FileUtil.createFolder(project, folder);
        }
        DataFolder destFld = DataFolder.findFolder(destFolder);
        //Manifest module seems not to support fremarker templates :-(
        boolean performManualSubstitutions = fileName.endsWith(".MF");
        //strip the extension from the filename or we get applet.xml.xml
        int ix = fileName.lastIndexOf('.');
        if (ix > 0) {
            fileName = fileName.substring(0, ix);
        }
        DataObject result = ob.createFromTemplate(destFld, fileName, params);
        if (performManualSubstitutions) {
            substitute(result, params);
        }
        if (result.getPrimaryFile().isLocked()) {
            throw new IllegalStateException ("Output file " + result.getPrimaryFile().getPath() + " should not be locked after create-from-template");
        }
        return result;
    }

    private void substitute(DataObject result, Map<String, String> params) throws IOException {
        FileObject file = result.getPrimaryFile();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        InputStream in = file.getInputStream();
        try {
            FileUtil.copy(in, data);
        } finally {
            in.close();
            data.close();
        }
        String content = data.toString("UTF-8");
        MapFormat fmt = new MapFormat(params);
        fmt.setLeftBrace("${");
        fmt.setRightBrace("}");
        String output = fmt.format(content);
        ByteArrayInputStream outData = new ByteArrayInputStream(output.getBytes("UTF-8"));
        FileLock lock = file.lock();
        OutputStream fileOut = new BufferedOutputStream(file.getOutputStream(lock));
        try {
            FileUtil.copy(outData, fileOut);
        } finally {
            outData.close();
            fileOut.close();
            lock.releaseLock();
        }
    }

    @Override
    public String toString() {
        return "Create " + dest + (name == null ? "" : name) + " from " + template.getPath();
    }
}
