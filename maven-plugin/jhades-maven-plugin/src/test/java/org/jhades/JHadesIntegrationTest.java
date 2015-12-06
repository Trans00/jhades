package org.jhades;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;

/*
 * All dependencies that this project has, including transitive ones. Contents are lazily populated, so depending on
 * what phases have run dependencies in some scopes won't be included. eg. if only compile phase has run,
 * dependencies with scope test won't be included.
 * <p/>
 * So full maven run is required to test clashing dependencies
 */

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.2.5", "3.3.3", "3.3.9"})
public class JHadesIntegrationTest {
    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime maven;

    public JHadesIntegrationTest(MavenRuntimeBuilder mavenBuilder) throws Exception {
        maven = mavenBuilder.withCliOptions("-B", "-U", "-Dmaven.install.skip=true").build();
    }

    @Test
    public void testClashingDependencies() throws Exception {
        run("test-module-with-clashing-dependencies");
    }

    @Test
    public void testModuleClassClashes() throws Exception {
        run("test-module-class-clashes-with-dependency");
    }

    private void run(String module) throws Exception {
        File basedir = resources.getBasedir(module);
        MavenExecutionResult result = maven.forProject(basedir).execute("clean", "install");
        Field f = result.getClass().getDeclaredField("log");
        f.setAccessible(true);
        ((Collection<String>) f.get(result)).stream().forEach(System.out::println);
        result.assertLogText("/ClashingClass.class has 2 versions on these classpath locations");
        result.assertLogText("[ERROR] >> jHades multipleClassVersionsReport >> Duplicate classpath resources report:");
    }
}
