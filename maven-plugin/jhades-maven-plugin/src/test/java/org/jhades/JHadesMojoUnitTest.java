package org.jhades;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.suppliers.TestedOn;

import java.io.File;
import java.io.IOException;

import static io.takari.maven.testing.TestMavenRuntime.*;
import static io.takari.maven.testing.TestResources.*;

public class JHadesMojoUnitTest {
    @Rule
    public final TestResources resources = new TestResources();

    @Rule
    public final TestMavenRuntime maven = new TestMavenRuntime();

    @Test
    public void shouldPassWhenNoDependencies() throws Exception {
        File basedir = resources.getBasedir("test-module-no-dependencies");
        maven.executeMojo(basedir, "check-clashes", newParameter("project", "${project}"));
    }

//    @Ignore
    @Test(expected = MojoExecutionException.class)
    public void shouldFailIfTwoClashingFilesFound() throws Exception {
        File basedir = resources.getBasedir("test-module-no-dependencies");
        Xpp3Dom classpathRootsParameter = new Xpp3Dom("classpathRoots");
        String baseDir = maven.readMavenProject(basedir).getBasedir().getAbsolutePath();
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize1"));
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize2"));
//        Xpp3Dom excludePatternsParameter = new Xpp3Dom("excludePatterns");
//        excludePatternsParameter.addChild(newParameter("excludePattern", ""));
        maven.executeMojo(basedir, "check-clashes",
                newParameter("project", "${project}"),
                classpathRootsParameter);
    }
}
