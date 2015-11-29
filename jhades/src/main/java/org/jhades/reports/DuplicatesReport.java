package org.jhades.reports;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.jhades.model.ClasspathResource;
import org.jhades.model.ClasspathResourceVersion;
import org.jhades.model.ClasspathResources;

/**
 *
 * Report for classpath duplicates - prints first the resources with the biggest number of versions.
 *
 */
public class DuplicatesReport {

    public static final List<String> resourcesToExclude = new ArrayList<>();

    static {
        resourcesToExclude.add("/META-INF/MANIFEST.MF");
        resourcesToExclude.add("/META-INF/INDEX.LIST");
        resourcesToExclude.add("/META-INF/ORACLE_J.SF");
        resourcesToExclude.add("/META-INF/LICENSE.txt");
        resourcesToExclude.add("/META-INF/NOTICE");
        resourcesToExclude.add("/META-INF/license.txt");
        resourcesToExclude.add("/META-INF/notice.txt");
        resourcesToExclude.add("/META-INF/NOTICE.txt");
        resourcesToExclude.add("/license.txt");
        resourcesToExclude.add("/META-INF/LICENSE");
        resourcesToExclude.add("/license/NOTICE");
    }
    private final List<ClasspathResource> resourcesWithDuplicates;
    private final UrlFormatter urlFormatter;

    public DuplicatesReport(List<ClasspathResource> resourcesWithDuplicates) {
        this.resourcesWithDuplicates = resourcesWithDuplicates;
        this.urlFormatter = new DefaultUrlFormatterImpl();
    }

    public DuplicatesReport(List<ClasspathResource> resourcesWithDuplicates, UrlFormatter urlFormatter) {
        this.resourcesWithDuplicates = resourcesWithDuplicates;
        this.urlFormatter = urlFormatter;
    }

    @Override
    public String toString() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos,true,"utf-8");
            print(ps);
            return baos.toString("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void print(){
        print(System.out);
    }

    public void print(PrintStream ps) {
        ps.println("\n>> jHades multipleClassVersionsReport >> Duplicate classpath resources report: \n");
        ClasspathResources.sortByNumberOfVersionsDesc(resourcesWithDuplicates);

        for (ClasspathResource resource : resourcesWithDuplicates) {
            if (!resourcesToExclude.contains(resource.getName())) {
                ps.println(resource.getName() + " has " + resource.getResourceFileVersions().size() + " versions on these classpath locations:\n");
                for (ClasspathResourceVersion resourceFileVersion : resource.getResourceFileVersions()) {
                    String classLoaderName = resourceFileVersion.getClasspathEntry().getClassLoaderName();
                    ps.println("    " + (classLoaderName != null ? classLoaderName : "") + " - "
                            + urlFormatter.formatUrl(resourceFileVersion.getClasspathEntry().getUrl())
                            + " - class file size = " + resourceFileVersion.getFileSize());
                }
                ps.println();
            }
        }

        if (resourcesWithDuplicates.isEmpty()) {
            ps.println("No duplicates where found.\n");
        }
    }
}
