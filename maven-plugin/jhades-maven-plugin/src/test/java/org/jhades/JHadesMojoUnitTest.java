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

    @Test(expected = MojoExecutionException.class)
    public void shouldFailIfTwoClashingFilesFound() throws Exception {
        File basedir = resources.getBasedir("test-module-no-dependencies");
        Xpp3Dom classpathRootsParameter = new Xpp3Dom("classpathRoots");
        String baseDir = maven.readMavenProject(basedir).getBasedir().getAbsolutePath();
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize1"));
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize2"));
        maven.executeMojo(basedir, "check-clashes",
                newParameter("project", "${project}"),
                classpathRootsParameter);
    }

    @Test
    public void shouldSucceedIfTwoClashingFilesFoundAndFailIfFoundSetToFalse() throws Exception {
        File basedir = resources.getBasedir("test-module-no-dependencies");
        Xpp3Dom classpathRootsParameter = new Xpp3Dom("classpathRoots");
        String baseDir = maven.readMavenProject(basedir).getBasedir().getAbsolutePath();
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize1"));
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize2"));
        maven.executeMojo(basedir, "check-clashes",
                newParameter("project", "${project}"),
                classpathRootsParameter,
                newParameter("failIfFound","false"));
    }

    @Test
    public void shouldSucceedIfTwoClashingFilesFoundAndFileIsInExcludePattern() throws Exception {
        File basedir = resources.getBasedir("test-module-no-dependencies");
        Xpp3Dom classpathRootsParameter = new Xpp3Dom("classpathRoots");
        String baseDir = maven.readMavenProject(basedir).getBasedir().getAbsolutePath();
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize1"));
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingDifferentSize2"));
        Xpp3Dom excludesParameter = new Xpp3Dom("classpathRoots");
        excludesParameter.addChild(newParameter("exclude","/ClashingDifferentSize.xml"));
        maven.executeMojo(basedir, "check-clashes",
                newParameter("project", "${project}"),
                classpathRootsParameter,
                excludesParameter);
    }

    @Test
    public void shouldSucceedIfTwoClashingFilesHaveSameSize() throws Exception {
        File basedir = resources.getBasedir("test-module-no-dependencies");
        Xpp3Dom classpathRootsParameter = new Xpp3Dom("classpathRoots");
        String baseDir = maven.readMavenProject(basedir).getBasedir().getAbsolutePath();
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingSameSize1"));
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingSameSize2"));
        maven.executeMojo(basedir, "check-clashes",
                newParameter("project", "${project}"),
                classpathRootsParameter);
    }

    @Test(expected = MojoExecutionException.class)
    public void shouldFailIfTwoClashingFilesHaveSameSizeAndExcludeSameSizeIsFalse() throws Exception {
        File basedir = resources.getBasedir("test-module-no-dependencies");
        Xpp3Dom classpathRootsParameter = new Xpp3Dom("classpathRoots");
        String baseDir = maven.readMavenProject(basedir).getBasedir().getAbsolutePath();
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingSameSize1"));
        classpathRootsParameter.addChild(newParameter("classpathRoot", baseDir + "/src/main/resources/ClashingSameSize2"));
        maven.executeMojo(basedir, "check-clashes",
                newParameter("project", "${project}"),
                newParameter("excludeSameSize","false"),
                classpathRootsParameter);
    }
}
