package dev.mccue.magicbean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Classes marked with this annotation must
 * </p>
 *
 * <ul>
 *   <li> Not have any private, non-static fields </li>
 *   <li> Not have any generic fields </li>
 * </ul>
 *
 * <p>
 * In exchange, an abstract class will be generated which
 * </p>
 *
 * <ul>
 *   <li> Is named <pre>[...]BeanOps</pre> where <pre>[...]</pre> is the name of the class. </li>
 *   <li> Is intended to be extended only by the annotated class. </li>
 *   <li> Will define get and set methods for the package-private and public fields. </li>
 * </ul>
 *
 * <p>
 * If the marked class
 * </p>
 *
 * <ul>
 *     <li> Has a non-private zero argument constructor </li>
 * </ul>
 *
 * <p>
 * Then a static method named <pre>of</pre> may be generated which provides an
 * equivalent to an "all args constructor"
 * </p>
 *
 * <p>
 * If the marked class
 * </p>
 *
 * <ul>
 *     <li> Is final </li>
 * </ul>
 *
 * <p>
 * Then an implementation of equals/hashCode may be generated which is based on every non-static field.
 * Note that if you are using this for making a JPA Entity or similar class, you likely do not
 * want to do this.
 * </p>
 *
 * <p>
 * You may also request a default implementation of toString which includes every field.
 * </p>
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
