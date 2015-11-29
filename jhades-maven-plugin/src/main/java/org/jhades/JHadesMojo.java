package org.jhades;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jhades.model.ClasspathEntries;
import org.jhades.model.ClasspathEntry;
import org.jhades.model.ClasspathResource;
import org.jhades.service.ClasspathScanner;
import org.jhades.service.ClasspathScannerListener;
import org.jhades.utils.StdOutLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "check-clashes", defaultPhase = LifecyclePhase.VERIFY)
public class JHadesMojo extends AbstractMojo{
    private final StdOutLogger logger = new StdOutLogger();
    private static final Pattern JAR_NAME = Pattern.compile("^.*/(.*jar)$");
    private final ClasspathScanner scanner = new ClasspathScanner();
    private List<Pattern> compiledPatterns;

    @Parameter
    List<String> excludePatterns;

    @Parameter
    List<String> classpathRoots;

    @Parameter
    List<String> libFolders;

    @Parameter(defaultValue = "true")
    boolean excludeSameSizeDups;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        compiledPatterns = initPatterns(excludePatterns);
        List<ClasspathEntry> entries = new ArrayList<>();
        classpathRoots.stream().parallel().forEach(path ->
            {
                File classpathRoot = new File(path);
                if (classpathRoot.exists()){
                    entries.add(new ClasspathEntry(null, classpathRoot.toURI().toString()));
                }
            }
        );
        libFolders.stream().parallel().forEach(path ->
                {
                    File libFolder = new File(path);
                    File[] children = libFolder.listFiles();
                    if (children != null) {
                        Arrays.stream(children)
                                .filter(file -> file.getName().endsWith(".jar"))
                                .forEach(jar -> entries.add(new ClasspathEntry(null, jar.toURI().toString())));
                    }
                }
        );

        ClasspathScannerListener listener = (new ClasspathScannerListener() {
            @Override
            public void onEntryScanStart(ClasspathEntry entry) {
                String filePath = entry.getUrl().toString();
                Matcher matcher = JAR_NAME.matcher(filePath);
                if (matcher.matches()) {
                    logger.info("Processing jar " + matcher.group(1));
                }
            }

            @Override
            public void onEntryScanEnd(ClasspathEntry entry) {
                String filePath = entry.getUrl().toString();
                Matcher matcher = JAR_NAME.matcher(filePath);
                if (matcher.matches()) {
                    logger.info("Finished processing jar " + matcher.group(1));
                }
            }
        });

        List<ClasspathResource> resources = ClasspathEntries.findClasspathResourcesInEntries(entries, logger, listener);
        List<ClasspathResource> overlapReportLines = scanner.findClassFileDuplicates(resources, excludeSameSizeDups);
        overlapReportLines.stream().filter(line -> exclude(line.getName()))

    }

    private List<Pattern> initPatterns(List<String> patternStrings) {
        List<Pattern> patterns = new ArrayList<>();
        patternStrings.stream().forEach(pattern ->patterns.add(Pattern.compile(pattern)));
        return patterns;
    }

    private boolean exclude(String filename){
        return compiledPatterns.stream().parallel().noneMatch(pattern -> pattern.asPredicate().test(filename));
    }

}
