package dev.mccue.magicbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import dev.mccue.tools.java.Java;
import dev.mccue.tools.javac.Javac;
import dev.mccue.tools.javac.JavacArguments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.mccue.magicbean.processor.AnnotationProcessor;


class MagicBeanProcessorTest {
    Path tempDir = Path.of("testOutput");

    @AfterEach
    void deleteGeneratedFiles() throws IOException {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (final Exception e) {
        }
    }

    @BeforeEach
    void compileProcessor() throws Exception {
        Javac.run(arguments -> {
            arguments
                    ._d(tempDir.resolve("processor"))
                    ._proc(JavacArguments.Processing.NONE)
                    .argument(Path.of("src/main/java/dev/mccue/magicbean/processor/AnnotationProcessor.java"))
                    .argument(Path.of("src/main/java/dev/mccue/magicbean/MagicBean.java"))
                    .argument(Path.of("src/main/java/module-info.java"));
        });
    }

    @Test
    void testGeneration() throws Exception {
        final String source =
                Paths.get("src/test/resources/Example.java").toAbsolutePath().toString();

        Javac.run(arguments -> {
            arguments
                    .__release(Integer.getInteger("java.specification.version"))
                    ._s(tempDir.resolve("sources"))
                    ._d(tempDir.resolve("classes"))
                    .__module_path(tempDir.resolve("processor"))
                    .__add_modules("ALL-MODULE-PATH")
                    .__processor_module_path(tempDir.resolve("processor"))
                    .argument(source);
        });

        var generated =
                Files.readString(
                        tempDir.resolve("sources/dev/mccue/magicbean/models/valid/ExampleBeanOps.java").toAbsolutePath());

        assertEquals(expectedString, generated);
    }

    String expectedString =
            """
                    package dev.mccue.magicbean.models.valid;
                    
                    sealed abstract class ExampleBeanOps extends java.lang.Object permits Example {
                    
                        private Example self() {
                            return (switch (this) { case Example __ -> __; });
                        }
                    
                        /**
                         * Get the current value for x.
                         */
                        public int getX() {
                            return self().x;
                        }
                    
                        /**
                         * Set the current value for x.
                         */
                        public void setX(int x) {
                            self().x = x;
                        }
                    
                        /**
                         * Get the current value for name.
                         */
                        public java.lang.String getName() {
                            return self().name;
                        }
                    
                        /**
                         * Set the current value for name.
                         */
                        public void setName(java.lang.String name) {
                            self().name = name;
                        }
                    
                        /**
                         * Get the current value for strs.
                         */
                        public java.util.List<java.lang.String> getStrs() {
                            return self().strs;
                        }
                    
                        /**
                         * Set the current value for strs.
                         */
                        public void setStrs(java.util.List<java.lang.String> strs) {
                            self().strs = strs;
                        }
                    
                    }""";
}
