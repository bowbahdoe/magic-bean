package dev.mccue.magicbean.processor;

import dev.mccue.magicbean.MagicBean;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("dev.mccue.magicbean.MagicBean")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class AnnotationProcessor extends AbstractProcessor {
    private String pascal(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String fqnForFieldType(Types typeUtils, VariableElement field) {
        return ((TypeElement) typeUtils.asElement(field.asType())).getQualifiedName().toString();
    }

    private String staticFactoryMethod(Types typeUtils, Name className, List<VariableElement> fields) {
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
            staticFactoryMethod.append(fqnForFieldType(typeUtils, field));
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
                        fields.stream()
                                .map(field -> "java.util.Objects.equals(%s.%s, other.%s)".formatted(
                                        selfExpr,
                                        field.getSimpleName(),
                                        field.getSimpleName()
                                ))
                                .collect(Collectors.joining(" && \n                   "))
                )
        );
        equalsAndHashCodeMethods.append("""
                            @Override
                            public int hashCode() {
                                return java.util.Objects.hash(
                                  %s
                                );
                            }
                        
                        """.formatted(
                        fields.stream()
                                .map(field -> "      " + selfExpr + "." + field.getSimpleName())
                                .collect(Collectors.joining(",\n          "))
                )
        );
        return equalsAndHashCodeMethods.toString();
    }

    private String toStringMethod(String selfExpr, Name className, List<VariableElement> fields) {
        return """
                    @Override
                    public String toString() {
                        return "%s[" + %s + "]";
                    }
                
                """.formatted(
                className,
                fields.stream()
                        .map(field ->
                                "\"%s=\" + %s".formatted(
                                        field.getSimpleName(),
                                        selfExpr + "." + field.getSimpleName()
                                )
                        )
                        .collect(Collectors.joining(" +\n                     \", \" + "))
        );
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {
        var filer = this.processingEnv.getFiler();
        var messager = this.processingEnv.getMessager();
        var elementUtils = this.processingEnv.getElementUtils();
        var typeUtils = this.processingEnv.getTypeUtils();

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
                                    element
                            );

                            return true;
                        }

                        if (modifiers.contains(Modifier.FINAL)) {
                            messager.printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Magic beans are not allowed to have any final non-static fields",
                                    element
                            );

                            return true;
                        }

                    }
                    var fieldType = field.asType().getKind();
                    if (!(fieldType.isPrimitive() || fieldType == TypeKind.ARRAY || fieldType == TypeKind.DECLARED)) {
                        messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "Unsupported type for a field: " + fieldType,
                                element
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

                boolean useAbstractClass = annotation.generateAllArgsStaticFactory()
                        || annotation.generateToString()
                        || annotation.generateEqualsAndHashCode();

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
                                %s %s %s%s() {
                                    return %s.%s;
                                }
                                
                                /**
                                 * Set the current value for %s.
                                 */
                                %s void set%s(%s %s) {
                                    %s.%s = %s;
                                }
                                
                            """.formatted(
                            fieldName,
                            useAbstractClass ? "public" : "default", fieldType, Set.of("boolean", "java.lang.Boolean").contains(fieldType) ? "is" : "get", pascalName,
                            selfExpr, fieldName,
                            fieldName,
                            useAbstractClass ? "public" : "default", pascalName, fieldType, fieldName,
                            selfExpr, fieldName, fieldName
                    );
                };


                var packageDecl = packageName == null ? "" : "package " + packageName + ";\n\n";


                String classDeclStart;
                if (useAbstractClass) {
                    classDeclStart = "sealed abstract class %s permits %s {\n\n";
                } else {
                    classDeclStart = "sealed interface %s permits %s {\n\n";
                }

                classDeclStart = classDeclStart.formatted(
                        className + "BeanOps", className
                );

                var classDeclEnd = "}";

                var classDecl = new StringBuilder();
                classDecl.append(packageDecl);
                classDecl.append(classDeclStart);

                if (annotation.generateAllArgsStaticFactory()) {
                    classDecl.append(staticFactoryMethod(typeUtils, className, fields));
                }

                for (var field : fields) {
                    classDecl.append(methodDefinition.apply(
                            fqnForFieldType(typeUtils, field),
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
