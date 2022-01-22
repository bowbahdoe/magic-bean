package dev.mccue.magicbean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes marked with this annotation must
 * <ul>
 *   <li> Not have any private, non-static fields </li>
 *   <li> Not have any generic fields </li>
 * </ul>
 * 
 * In exchange, an interface will be generated which
 * <ul>
 *   <li> Is named <pre>[...]BeanOps</pre> where <pre>[...]</pre> is the name of the class. </li>
 *   <li> Is intended to be implement only by the annotated class. </li>
 *   <li> Will define get and set methods for the package-private and public fields. </li>
 * </ul>
 *
 * In addition, if the marked class
 *
 * <ul>
 *     <li> Has a non-private zero argument constructor </li>
 * </ul>
 *
 * Then a static method named <pre>of</pre> may be generated which provides an
 * equivalent to an "all args constructor"
 *
 * If it makes sense to for your class you can also request equals/hashCode or toString
 * implementations.
 *
 * Any of these extensions will make the generated interface instead be an abstract class.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface MagicBean {
    /**
     * Use a CUPS expression instead of a normal cast.
     *
     * Will require --enable-preview on java 17.
     */
    boolean useTypeSafeCast() default false;

    /**
     * Whether to generate an all args static factory method.
     */
    boolean generateAllArgsStaticFactory() default false;

    /**
     * Whether to generate an equals and hash code implementation.
     *
     * Will make the generated BeanOps class be an abstract class, not an interface.
     */
    boolean generateEqualsAndHashCode() default false;

    /**
     * Whether to generate a basic toString.
     *
     * Will make the generated BeanOps class be an abstract class, not an interface.
     */
    boolean generateToString() default false;
}
