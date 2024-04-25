package dev.mccue.magicbean;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import dev.mccue.magicbean.processor.AnnotationProcessor;

class MagicBeanProcessorTest {

  @AfterEach
  void deleteGeneratedFiles() throws IOException {
    try {
      Files.walk(Paths.get("dev").toAbsolutePath())
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    } catch (final Exception e) {
    }
  }

  @Test
  void testGeneration() throws Exception {
    final String source =
        Paths.get("src/test/java/dev/mccue/magicbean/models/valid").toAbsolutePath().toString();

    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);

    manager.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(new File(source)));

    final Set<Kind> fileKinds = Collections.singleton(Kind.SOURCE);

    final Iterable<JavaFileObject> files =
        manager.list(StandardLocation.SOURCE_PATH, "", fileKinds, true);

    final CompilationTask task =
        compiler.getTask(
            new PrintWriter(System.out),
            null,
            null,
            List.of("--release=" + Integer.getInteger("java.specification.version")),
            null,
            files);
    task.setProcessors(Arrays.asList(new AnnotationProcessor()));

    task.call();

    // assert the files are correct by loading them or something idk
    var generated =
        Files.readString(
            Paths.get("dev/mccue/magicbean/models/valid/ExampleBeanOps.java").toAbsolutePath());

    assertEquals(expectedString, generated);
  }

  String expectedString =
      """
	package dev.mccue.magicbean.models.valid;

	sealed abstract class ExampleBeanOps extends java.lang.Object permits Example {

	    /**
	     * Get the current value for x.
	     */
	    public int getX() {
	        return (switch (this) { case Example __ -> __; }).x;
	    }

	    /**
	     * Set the current value for x.
	     */
	    public void setX(int x) {
	        (switch (this) { case Example __ -> __; }).x = x;
	    }

	    /**
	     * Get the current value for name.
	     */
	    public java.lang.String getName() {
	        return (switch (this) { case Example __ -> __; }).name;
	    }

	    /**
	     * Set the current value for name.
	     */
	    public void setName(java.lang.String name) {
	        (switch (this) { case Example __ -> __; }).name = name;
	    }

	    /**
	     * Get the current value for strs.
	     */
	    public java.util.List<java.lang.String> getStrs() {
	        return (switch (this) { case Example __ -> __; }).strs;
	    }

	    /**
	     * Set the current value for strs.
	     */
	    public void setStrs(java.util.List<java.lang.String> strs) {
	        (switch (this) { case Example __ -> __; }).strs = strs;
	    }

	}""";
}
