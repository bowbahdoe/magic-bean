package dev.mccue.magicbean.processor;

import dev.mccue.magicbean.MagicBean;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * Annotation processor that handles MagicBean annotations.
 */
public final class AnnotationProcessor extends AbstractProcessor {
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("dev.mccue.magicbean.MagicBean");
    }

    private String pascal(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String staticFactoryMethod(Name className, List<VariableElement> fields) {
        var staticFactoryMethod = new StringBuilder();
        staticFactoryMethod.append("""
                    /**
                     * Creates an instance of %s.
                     */
                    public static %s of(
                """.formatted(className, className));

        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);

            staticFactoryMethod.append("        ");
            staticFactoryMethod.append(field.asType());
            staticFactoryMethod.append(" ");
            staticFactoryMethod.append(field.getSimpleName());
            if (i != fields.size() - 1) {
                staticFactoryMethod.append(",\n");
            }
        }
        staticFactoryMethod.append("\n    ) {\n");
        staticFactoryMethod.append("        var o = new %s();\n".formatted(className));
        for (var field : fields) {
            staticFactoryMethod.append("        o.set%s(%s);\n".formatted(
                    pascal(field.getSimpleName().toString()),
                    field.getSimpleName()
            ));
        }
        staticFactoryMethod.append("        return o;\n");
        staticFactoryMethod.append("    }\n\n");
        return staticFactoryMethod.toString();
    }

    private String equalsAndHashCodeMethods(String selfExpr, Name className, List<VariableElement> fields) {
        var equalsAndHashCodeMethods = new StringBuilder();
        equalsAndHashCodeMethods.append("""
                            @Override
                            public boolean equals(Object o) {
                                if (o == null || !(o instanceof %s other)) {
                                    return false;
                                }
                                else {
                                    return %s;
                                }
                            }
                                        
                        """.formatted(
                        className,
                        createEqualsExpression(selfExpr, fields)
                )
        );
        equalsAndHashCodeMethods.append("""
                            @Override
                            public int hashCode() {
                                %s
                            }
                        
                        """.formatted(
                        createHashCodeMethodBody(selfExpr, fields)
                )
        );
        return equalsAndHashCodeMethods.toString();
    }

    private String createEqualsExpression(String selfExpr, List<VariableElement> fields) {
        if (fields.isEmpty()) {
            return "true";
        }
        else {
            return fields.stream()
                    .map(field -> "java.util.Objects.equals(%s.%s, other.%s)".formatted(
                            selfExpr,
                            field.getSimpleName(),
                            field.getSimpleName()
                    ))
                    .collect(Collectors.joining(" && \n                   "));
        }
    }

    private String createHashCodeMethodBody(String selfExpr, List<VariableElement> fields) {
        if (fields.isEmpty()) {
            return "return 1;";
        }
        else {
            return """
                    return java.util.Objects.hash(
                              %s
                            );""".formatted(
                    fields.stream()
                            .map(field -> "      " + selfExpr + "." + field.getSimpleName())
                            .collect(Collectors.joining(",\n          ")));
        }
    }

    private String toStringMethod(String selfExpr, Name className, List<VariableElement> fields) {
        return """
                    @Override
                    public String toString() {
                        %s
                    }
                                
                """.formatted(
                createToStringMethodBody(selfExpr, className, fields)
        );
    }

    private String createToStringMethodBody(String selfExpr, Name className, List<VariableElement> fields) {
        if (fields.isEmpty()) {
            return "return \"%s[]\"; "
                    .formatted(className);
        }
        else {
            return "return \"%s[\" + %s + \"]\";"
                    .formatted(
                    className,
                    fields.stream()
                            .map(field ->
                                    "\"%s=\" + %s".formatted(
                                            field.getSimpleName(),
                                            selfExpr + "." + field.getSimpleName()
                                    )
                            )
                            .collect(Collectors.joining(" +\n                     \", \" + ")));
        }
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {
        var filer = this.processingEnv.getFiler();
        var messager = this.processingEnv.getMessager();
        var elementUtils = this.processingEnv.getElementUtils();

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(MagicBean.class);
        for (var element : elements) {
            if (!(element instanceof TypeElement typeElement)) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Magic beans should only be placeable on classes.",
                        element
                );
            } else {
                var members = elementUtils.getAllMembers(typeElement);
                var fields = ElementFilter.fieldsIn(members);
                for (var field : fields) {
                    var modifiers = field.getModifiers();
                    if (!modifiers.contains(Modifier.STATIC)) {
                        if (modifiers.contains(Modifier.PRIVATE)) {
                            messager.printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Magic beans are not allowed to have any private non-static fields",
                                    field
                            );

                            return true;
                        }

                        if (modifiers.contains(Modifier.FINAL)) {
                            messager.printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Magic beans are not allowed to have any final non-static fields",
                                    field
                            );

                            return true;
                        }

                    }
                    var fieldType = field.asType().getKind();
                    if (!(fieldType.isPrimitive() || fieldType == TypeKind.ARRAY || fieldType == TypeKind.DECLARED)) {
                        messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "Unsupported type for a field: " + fieldType,
                                field
                        );

                        return true;
                    }
                }

                var constructors = ElementFilter.constructorsIn(members);
                var hasValidConstructor = constructors
                        .stream()
                        .anyMatch(constructor ->
                                constructor.getParameters().size() == 0 &&
                                        !constructor.getModifiers().contains(Modifier.PRIVATE)
                        );

                var annotation = typeElement.getAnnotation(MagicBean.class);
                if (annotation.generateAllArgsStaticFactory() && !hasValidConstructor) {
                    messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Magic beans need to have a non-private zero arg constructor in order for a factory method to be generated.",
                            element
                    );
                    return true;
                }

                var className = typeElement.getSimpleName();

                var enclosingElement = typeElement.getEnclosingElement();
                if (!(enclosingElement instanceof PackageElement packageElement)) {
                    messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Magic beans must be top level classes, not nested within another class",
                            element
                    );
                    return true;
                }

                String packageName;
                if (packageElement.isUnnamed()) {
                    packageName = null;
                }
                else {
                    packageName = packageElement.toString();
                }


                String selfExpr;
                if (typeElement.getAnnotation(MagicBean.class).useTypeSafeCast()) {
                    selfExpr = "(switch (this) { case %s __ -> __; })".formatted(className);
                } else {
                    selfExpr = "((%s) this)".formatted(className);
                }

                boolean requiresFinalClass = annotation.generateEqualsAndHashCode();

                if (requiresFinalClass && !typeElement.getModifiers().contains(Modifier.FINAL)) {
                    messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "In order to use the automatic equals and hash code, a magic bean must be final.",
                            element
                    );
                    return true;
                }

                BiFunction<String, String, String> methodDefinition = (fieldType, fieldName) -> {
                    var pascalName = pascal(fieldName);
                    return """
                                /**
                                 * Get the current value for %s.
                                 */
                                public %s %s%s() {
                                    return %s.%s;
                                }
                                
                                /**
                                 * Set the current value for %s.
                                 */
                                public void set%s(%s %s) {
                                    %s.%s = %s;
                                }
                                
                            """.formatted(
                            fieldName,
                            fieldType, Set.of("boolean", "java.lang.Boolean").contains(fieldType) ? "is" : "get", pascalName,
                            selfExpr, fieldName,
                            fieldName,
                             pascalName, fieldType, fieldName,
                            selfExpr, fieldName, fieldName
                    );
                };


                var packageDecl = packageName == null ? "" : "package " + packageName + ";\n\n";

                String extendClass;
                try {
                    extendClass = annotation.extend().toString();
                } catch (MirroredTypeException e) {
                    extendClass = e.getTypeMirror().toString();
                }

                var classDeclStart = "sealed abstract class %s extends %s permits %s {\n\n".formatted(
                        className + "BeanOps",
                        extendClass,
                        className
                );

                var classDeclEnd = "}";

                var classDecl = new StringBuilder();
                classDecl.append(packageDecl);
                classDecl.append(classDeclStart);

                if (annotation.generateAllArgsStaticFactory()) {
                    classDecl.append(staticFactoryMethod(className, fields));
                }

                for (var field : fields) {
                    classDecl.append(methodDefinition.apply(
                            field.asType().toString(),
                            field.getSimpleName().toString()
                    ));
                }

                if (annotation.generateEqualsAndHashCode()) {
                    classDecl.append(equalsAndHashCodeMethods(selfExpr, className, fields));
                }

                if (annotation.generateToString()) {
                    classDecl.append(toStringMethod(selfExpr, className, fields));
                }

                classDecl.append(classDeclEnd);

                try {
                    var file = filer.createSourceFile(
                            (packageName == null ? "" : packageName + ".") + className + "BeanOps",
                            element
                    );
                    try (var writer = file.openWriter()) {
                        writer.append(classDecl.toString());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }
}
