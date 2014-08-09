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
package org.netbeans.modules.avatar.platform;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonCreator;
import org.netbeans.api.project.Project;
import org.netbeans.modules.nodejs.api.ProjectMetadata;

/**
 * A Java dependency, decoded from the non-normative "java" section of a package.json
 *
 * @author Tim Boudreau
 */
final class JavaDependency {

    public final String artifactId;
    public final String groupId;
    public final String version;

    public JavaDependency(String artifactId, String groupId, String version) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
    }

    public JavaDependency(Map<String, ?> map) {
        artifactId = map.get("artifactId") + ""; //NOI18N
        groupId = map.get("groupId") + ""; //NOI18N
        version = map.get("version") + ""; //NOI18N
    }

    public JavaDependency(String toSplit) {
        String[] comps = toSplit.split(":");
        String gid = ""; //NOI18N
        String aid = ""; //NOI18N
        String v = ""; //NOI18N
        for (int i = 0; i < comps.length; i++) {
            switch (i) {
                case 0:
                    gid = comps[i];
                    break;
                case 1:
                    aid = comps[i];
                    break;
                case 2:
                    v = comps[i];
                    break;
                default:
                    break;
            }
        }
        artifactId = aid;
        groupId = gid;
        version = v;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", groupId); //NOI18N
        result.put("artifactId", artifactId); //NOI18N
        result.put("version", version); //NOI18N
        return result;
    }

    public String toString() {
        String base = System.getProperty("user.home") + File.separatorChar + ".m2" + File.separatorChar + "repository"; //NOI18N
        return base + File.separatorChar + groupId + File.separatorChar + artifactId + File.separatorChar + version;
    }

    public boolean equals(Object o) {
        if (o instanceof JavaDependency) {
            JavaDependency jd = (JavaDependency) o;
            return artifactId.equals(jd.artifactId)
                    && groupId.equals(jd.groupId);
        }
        return false;
    }

    public int hashCode() {
        return artifactId.hashCode() + (71 * groupId.hashCode());
    }

    public static void find(Project project, List<? super JavaDependency> deps) {
        ProjectMetadata meta = project.getLookup().lookup(ProjectMetadata.class);
        if (meta != null) { // It belongs to a NodeJSProject
            Map<String, Object> javaInfo = meta.getMap("java"); //NOI18N
            if (javaInfo != null) { // There is a "java" section in package.json
                Object o = javaInfo.get("dependencies"); //NOI18N
                // Double check the type
                if (o != null && o instanceof List<?>) {
                    List<?> l = (List<?>) o;
                    for (Object obj : l) {
                        // May either be in pom.xml form, or command-line form
                        // of groupId:artifactId:version
                        if (obj instanceof Map<?, ?>) {
                            Map<String, ?> m = (Map<String, ?>) obj;
                            deps.add(new JavaDependency(m));
                        } else if (obj instanceof String) {
                            deps.add(new JavaDependency((String) obj));
                        }
                    }
                }
            }
        }
    }
    
    public static void remove(Project project, JavaDependency dep) {
        ProjectMetadata meta = project.getLookup().lookup(ProjectMetadata.class);
        if (meta != null) { // It belongs to a NodeJSProject
            Map<String, Object> javaInfo = meta.getMap("java"); //NOI18N
            if (javaInfo == null) {
                javaInfo = new LinkedHashMap<>();
            } else {
                javaInfo = new LinkedHashMap<>(javaInfo);
            }
            Object o = javaInfo.get("dependencies"); //NOI18N
            if (o == null && !(o instanceof List<?>)) {
                o = new LinkedList<>();
            }
            List<?> l = (List<?>) o;
            for (Iterator<?> it=l.iterator(); it.hasNext();) {
                Object d = it.next();
                if (d instanceof String) {
                    JavaDependency dd = new JavaDependency((String) d);
                    if (dd.equals(dep)) {
                        it.remove();
                    }
                } else if (d instanceof Map) {
                    JavaDependency dd = new JavaDependency((Map<String, ?>) d);
                    if (dd.equals(dep)) {
                        it.remove();
                    }
                }
            }
            javaInfo.put("dependencies", l); //NOI18N
            meta.addMap("java", javaInfo); //NOI18N
        }
    }

    public static void add(Project project, JavaDependency dep) {
        ProjectMetadata meta = project.getLookup().lookup(ProjectMetadata.class);
        if (meta != null) { // It belongs to a NodeJSProject
            Map<String, Object> javaInfo = meta.getMap("java"); //NOI18N
            if (javaInfo == null) {
                javaInfo = new LinkedHashMap<>();
            } else {
                javaInfo = new LinkedHashMap<>(javaInfo);
            }
            Object o = javaInfo.get("dependencies"); //NOI18N
            if (o == null && !(o instanceof List<?>)) {
                o = new LinkedList<>();
            }
            List<?> l = (List<?>) o;
            List<JavaDependency> deps = new LinkedList<>();
            for (Object d : l) {
                if (d instanceof String) {
                    deps.add(new JavaDependency((String) d));
                } else if (d instanceof Map) {
                    deps.add(new JavaDependency((Map<String, ?>) d));
                }
            }
            deps.remove(dep);
            deps.add(dep);
            List<Map<String,?>> putBack = new LinkedList<>();
            for (JavaDependency d : deps) {
                putBack.add(d.toMap());
            }
            javaInfo.put("dependencies", putBack); //NOI18N
            meta.addMap("java", javaInfo); //NOI18N
        }
    }
}
