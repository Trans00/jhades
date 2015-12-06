package org.jhades;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jhades.model.ClasspathEntries;
import org.jhades.model.ClasspathEntry;
import org.jhades.model.ClasspathResource;
import org.jhades.model.ClasspathResources;
import org.jhades.reports.DuplicatesReport;
import org.jhades.service.ClasspathScanner;
import org.jhades.service.ClasspathScannerListener;
import org.jhades.utils.StdOutLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Mojo(name = "check-clashes",
        defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JHadesMojo extends AbstractMojo {
    private final StdOutLogger logger = new StdOutLogger();
    private final ClasspathScanner scanner = new ClasspathScanner();
    private List<Pattern> compiledPatterns;

    @Parameter
    List<String> excludePatterns = new ArrayList<>();

    @Parameter
    List<String> classpathRoots = new ArrayList<>();

    @Parameter
    List<String> libFolders = new ArrayList<>();

    @Parameter(defaultValue = "true")
    boolean excludeSameSize;

    @Parameter(defaultValue = "true")
    boolean failIfFound;

    @Parameter(defaultValue = "true")
    boolean useProjectArtifact;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        excludePatterns.addAll(DuplicatesReport.resourcesToExclude);
        compiledPatterns = initPatterns(excludePatterns);
        List<ClasspathEntry> entries = new ArrayList<>();
        classpathRoots.parallelStream().forEach(path ->
            {
                File classpathRoot = new File(path);
                if (classpathRoot.exists()) {
                    entries.add(new ClasspathEntry(null, classpathRoot.toURI().toString()));
                }
            }
        );
        libFolders.parallelStream().forEach(path ->
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

        project.getArtifacts().parallelStream()
                .forEach(artifact -> entries.add(new ClasspathEntry(null,artifact.getFile().toURI().toString())));

        Artifact projectArtifact = project.getArtifact();
        if(useProjectArtifact && "jar".equals(projectArtifact.getType())){
            if(projectArtifact.getFile() == null){
                Build build = project.getBuild();
                projectArtifact.setFile(new File(build.getDirectory() + "/" + build.getFinalName() + ".jar"));
            }
            entries.add(new ClasspathEntry(null,projectArtifact.getFile().toURI().toString()));
        } else {
            logger.info("Skiping project artifact: " + projectArtifact);
        }

        List<ClasspathResource> resources = ClasspathEntries.findClasspathResourcesInEntries(entries, logger, null);
        List<ClasspathResource> overlapReportLines = ClasspathResources
                .findResourcesWithDuplicates(resources,excludeSameSize).stream()
                .filter(this::exclude)
                .collect(Collectors.toList());
        DuplicatesReport report = new DuplicatesReport(overlapReportLines);
        report.print();

        if (failIfFound && overlapReportLines.size() > 0) {
            throw new MojoExecutionException(report.toString());
        }

    }

    private List<Pattern> initPatterns(List<String> patternStrings) {
        return patternStrings.stream().map(Pattern::compile).collect(Collectors.toList());
    }

    private boolean exclude(ClasspathResource resource) {
        return compiledPatterns.parallelStream().noneMatch(pattern -> pattern.asPredicate().test(resource.getName()));
    }

}
