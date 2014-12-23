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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.MapFormat;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.openide.util.NbBundle;
import org.openide.util.NbCollections;
import org.openide.util.Parameters;

/**
 * A simple way to define an entire project template in a properties file.
 * A project template is simply a .properties file fileobject with an internal
 * microformat that consists of a few simple rules
 * about the keys and values.
 * <p/>
 * The first step is to create a New Project Wizard which will gather a
 * set of string key/value pairs into a Map which will be used for
 * substitutions in your generated project.  Then you pass that Map and
 * the project directory to <code>createProject()</code>.  You also pass
 * in a <code>ProgressHandle</code> to show the user progress as the
 * project is created in the background.
 * <p/>
 * To add files to your project, you can simply provide pointers to
 * templates in the system filesystem &mdash; if you want a servlet,
 * you can use the existing Servlet template.  Your template
 * defines a mapping from file paths to be generated
 * in the projects
 * to templates in the system file system;  or you can implement custom
 * file generation by adding your own
 * <code>FileCreators</code> to the <code>ProjectCreator</code>.
 * <p/>
 * <h4>Project Template File Format</h4>
 * The project template is really a standard Java properties file - we just
 * have some conventions about how the properties are interpreted.
 * With the exception of two special prefixes, all properties are assumed to be
 * in the format
 * <pre>
 * relative/path/to/file/in/project=path/in/system/filesystem/to/template
 * </pre>
 * Additionally, if the created file should be opened when the project is
 * opened, simply append * to the <i>key</i> (the filename will not include the
 * *), e.g.
 * <pre>
 * com/foo/bar/MyFile.java*=path/to/some/Template.java
 * </pre>
 * <h4>Substituting values</h4>
 * <code>createProject</code> takes a Map of Strings.  These can be used for
 * substitutions <i>both in the template file itself and in the files created
 * from NetBeans file templates</i>.  In your project template properties file,
 * simply surround a key name with {{ }} to have it automatically replaced
 * by a value from the map, e.g.
 * <pre>
 * {{package-name-slashes}}/{{project-name}}.java*=Templates/Classes/Class.java
 * </pre>
 * NetBeans file templates use FreeMarker for templating.  The same map of keys
 * and values is also used for substitutions in the file templates that are
 * used when creating your project.
 *
 * <h4>Project Properties and Private Properties</h4>
 * Two special prefixes are used for auto-generating the standard
 * <code>nbproject/project.properties</code> and <code>nbproject/private/private.properties</code>.
 * <ul>
 * <li><code>pp.</code> - any property in your template with this prefix will
 * be assumed to be a property that should be put into
 * <code>nbproject/project.properties</code>.</li>
 * <li><code>pvp.</code> - any property in your template with this prefix
 * will be assumed to be a property that should go into
 * <code>nbproject/private/private.properties</code>.
 * </li>
 * </ul>
 * The same substitution rules apply - you can use {{ }} in keys or values
 * to modify the keys or values, for example
 * <code>
 * pvp.intercalInterpreter={{absolute-path-to-interpreter}}
 * pp.srcDir=src
 * </code>
 *
 * If there are no properties in the template prefixed with <code>pp.</code>,
 * no <code>nbproject/project.properties</code> file will be generated.  The
 * same is true for private properties.
 *
 * <h4>Handling project.xml and other custom files</h4>
 * Typically, you will implement FileCreator for your project.xml (or generate
 * it some other way before or after calling <code>createProject()</code>.
 * You can add any sort of custom file generation you want by extending
 * FileCreator.  It is just often simpler to work with templates.
 *
 * <h4>Sample Usage</h4>
 * Here is a sample from a template for a Java Card&trade; project:
 * <pre>
 * META-INF/javacard.xml=org-netbeans-modules-javacard/templates/javacard.xml
 * META-INF/MANIFEST.MF=org-netbeans-modules-javacard/templates/EAP_MANIFEST.MF
 * APPLET-INF/applet.xml=org-netbeans-modules-javacard/templates/applet.xml
 * scripts/{{classnamelowercase}}.scr=org-netbeans-modules-javacard/templates/test.scr
 * src/{{packagepath}}/{{classname}}.java*=Templates/javacard/ExtendedApplet.java
 * nbproject/deployment.xml=org-netbeans-modules-javacard/templates/deployment.xml
 * pp.display.name={{projectname}}
 * pp.platform.active={{activeplatform}}
 * pp.active.device={{activedevice}}
 * pp.runtime.descriptor=META-INF/MANIFEST.MF
 * pp.jcap.descriptor=META-INF/javacard.xml
 * pp.applet.descriptor=APPLET-INF/applet.xml
 * </pre>
 * 
 * <p/>
 * <b>Note:</b>You probably do not want to use . characters in the keys in
 * the map you pass into createProject().  This is because most NetBeans
 * file templates use FreeMarker for templating, and FreeMarker interprets .
 * characters as indicating an expression to evaluate.
 * <p/>
 * If you are depending on file templates from specific modules that you do not
 * control, it is a good idea to declare a dependency on those modules (it
 * can be a runtime-only dependency).  This will make sure all the templates
 * you need are there.
 *
 * @author Tim Boudreau
 */
public final class ProjectCreator {

    private final FileObject dir;
    private final List<FileCreator> entries = new LinkedList<FileCreator>();

    /**
     * Create a new ProjectCreator
     * @param projectDir The directory in which the project should be created
     * (<i>not</i> the project root directory itself)
     */
    public ProjectCreator(FileObject projectDir) {
        this.dir = projectDir;
    }
    
    public void add(String destPath, String name, FileObject fileTemplate, boolean openOnLoad) {
        add(new TemplateBasedFileCreator(destPath, name, fileTemplate, openOnLoad));
    }

    /**
     * Add a FileCreator that can create a file in the new project.  You will
     * typically use this to generate the <code>project.xml</code>, and possibly
     * build files.
     * @param creator An object which can generate one file and knows the
     * relative path on which to do it.
     */
    public void add(FileCreator creator) {
        Parameters.notNull("creator", creator); //NOI18N
        entries.add(creator);
    }

    /**
     * Create the project
     * @param handle A ProgressHandle to show the user progress
     * @param name The name for the project
     * @param template The template for the project - the properties file as
     * described above
     * @param substitutions The keys and values that should be used for substitutions
     * both in the project template itself, and in the NetBeans file templates
     * used in your project
     * @return A GeneratedProject (an object which just provides the created
     * project directory and a Set of files which should be opened in the
     * editor)
     * @throws IOException is something goes wrong
     */
    public final GeneratedProject createProject(final ProgressHandle handle, final String name, final FileObject template, final Map<String, String> substitutions) throws IOException {
        final Map <String, String> params = Collections.unmodifiableMap(substitutions);
        Parameters.notNull("name", name);
        Parameters.notNull("template", template);

        handle.start(6);

        final EditableProperties[] projectProps = parseTemplate(template, params);
        handle.progress(1);
        handle.setDisplayName(NbBundle.getMessage(ProjectCreator.class,
                "LBL_CreatingProject")); //NOI18N

        handle.progress(1);

        final FileObject projectDir = FileUtil.createFolder(dir, name);
        final GeneratedProject[] result = new GeneratedProject[1];
        projectDir.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
            @Override
            public void run() throws IOException {
                result[0] = doCreateProject(handle, projectProps, projectDir, name, template, params);
            }
        });
        handle.finish();
        return result[0];
    }

    private GeneratedProject doCreateProject(ProgressHandle handle, Map<String,String> projectProps[], FileObject projectDir, String name, FileObject template, Map<String, String> params) throws IOException {
        assert projectDir != null;
        handle.progress(2);
        if (!projectProps[0].isEmpty()) {
            entries.add (new ProjectPropertiesEntry(projectProps[0]));
        }
        if (!projectProps[1].isEmpty()) {
            entries.add (new ProjectPropertiesEntry(AntProjectHelper.PRIVATE_PROPERTIES_PATH,
                    projectProps[1]));
        }

        handle.progress(3);
        Set<DataObject> toOpen = new HashSet<DataObject>();
        handle.progress(4);
        for (FileCreator entry : entries) {
            handle.setDisplayName(entry.getName());
            DataObject ob = entry.create(projectDir, params);
            if (entry.isOpenOnLoad()) {
                toOpen.add(ob);
            }
        }
        synchronized (this) {
            notifyAll(); //unit tests
        }
        return new GeneratedProject(projectDir, toOpen);
    }
    
    /**
     * Get a list of files which will be created, using the passed target
     * directory as a parent.
     * 
     * @param target The folder where the project may be created
     * @return A list files which will be created if createProject() is 
     * invoked with this target folder
     */
    public final List<File> listCreatedFiles (File target) {
        List<File> files = new LinkedList<File>();
        for (FileCreator c : entries) {
            String targetFile = c.dest;
            if (targetFile != null && !targetFile.endsWith("/")) {
                targetFile += '/';
            }
            targetFile = targetFile + c.getName();
            File f = new File (target, targetFile);
            files.add(f);
        }
        Collections.sort(files, new PathLengthComparator());
        return files;
    }
    
    private static final class PathLengthComparator implements Comparator<File> {

        @Override
        public int compare(File o1, File o2) {
            int l1 = o1.getPath().length();
            int l2 = o2.getPath().length();
            if (l1 == l2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            } else {
                return l1 > l2 ? 1 : -1;
            }
        }
    }

    private EditableProperties[] parseTemplate(FileObject template, Map<String, String> params) throws IOException {
        EditableProperties templateProps = loadProperties(template);
        EditableProperties projectProperties = new EditableProperties (true);
        EditableProperties privateProperties = new EditableProperties (true);
        MapFormat fmt = new MapFormat(params);
        fmt.setLeftBrace("{{");
        fmt.setRightBrace("}}");
        for (String key : NbCollections.checkedSetByFilter(templateProps.keySet(), String.class, false)) {
            String value = templateProps.getProperty(key);
            //Perform substitutions
            try {
                value = fmt.format(value);
            } catch (IllegalArgumentException e) {
                //More useful logging
                throw new IllegalArgumentException ("Exception parsing template " + template.getPath() +
                        " on '" + key + '=' + value, e); //NOI18N
            }
            key = fmt.format(key);
            if (!key.startsWith("pp.")) {
                //regular file entry
                FileObject tplFile = FileUtil.getConfigFile(value);
                if (tplFile == null) {
                    throw new IOException("No template named " + value + //NOI18N
                            " in the system FS"); //NOI18N
                }
                boolean open = key.endsWith("*");
                if (open) {
                    key = key.substring(0, key.length() - 1);
                }
                add(key, null, tplFile, open); //NOI18N
            } else if (key.startsWith("pp")) {
                //special keys here
                key = key.substring(3);
                projectProperties.put(key, value);
            } else if (key.startsWith("pvp")) {
                key = key.substring(3);
                privateProperties.put(key, value);
            }
        }
        return new EditableProperties[] { projectProperties, privateProperties };
    }

    private EditableProperties loadProperties(FileObject template) throws IOException {
        EditableProperties result = new EditableProperties(true);
        InputStream in = new BufferedInputStream(template.getInputStream());
        try {
            result.load(in);
        } finally {
            in.close();
        }
        return result;
    }

    private static class ProjectPropertiesEntry extends TemplateBasedFileCreator {
        private final Map<String,String> props;
        ProjectPropertiesEntry (Map<String,String> props) {
            this (AntProjectHelper.PROJECT_PROPERTIES_PATH, props);
        }

        ProjectPropertiesEntry (String path, Map<String,String> props) {
            super (path, null, ProjectCreator.findPropertiesTemplate(), false);
            this.props = props;
        }

        @Override
        public DataObject create(FileObject project, Map<String, String> params) throws IOException {
            final DataObject result = super.create(project, params);
            try {
                ProjectManager.mutex().writeAccess(new Mutex.ExceptionAction<Void>() {
                    @Override
                    public Void run() throws IOException {
                        final EditableProperties ed = new EditableProperties(props);
                        final FileObject file = result.getPrimaryFile();
                        final FileLock lock = file.lock();
                        try {
                            final OutputStream out = new BufferedOutputStream(file.getOutputStream(lock));
                            try {
                                ed.store(out);
                            } finally {
                                out.close();
                            }
                        } finally {
                            lock.releaseLock();
                        }
                        return null;
                    }
                });
            } catch (MutexException me) {
                if (me.getException() instanceof  IOException) {
                    throw (IOException) me.getException();
                } else {
                    throw new IOException(me);
                }
            }
            return result;
        }
    }

    private static FileObject findPropertiesTemplate() {
        String path = "Templates/Other/properties.properties"; //NOI18N
        FileObject result = FileUtil.getConfigFile(path);
        if (result == null) {
            throw new IllegalStateException("Could not find properties template" + //NOI18N
                    " in SFS at " + path); //NOI18N
        }
        return result;
    }
}
