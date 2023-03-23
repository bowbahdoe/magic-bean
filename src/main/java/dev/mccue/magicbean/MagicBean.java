package dev.mccue.magicbean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Classes marked with this annotation must
 *
 * <ul>
 *   <li> Not have any private, non-static fields </li>
 *   <li> Not have any generic fields </li>
 * </ul>
 *
 * <p>
 * In exchange, an abstract class will be generated which
 *
 * <ul>
 *   <li> Is named <pre>[...]BeanOps</pre> where <pre>[...]</pre> is the name of the class. </li>
 *   <li> Is intended to be extended only by the annotated class. </li>
 *   <li> Will define get and set methods for the package-private and public fields. </li>
 * </ul>
 *
 * <p>
 * If the marked class
 *
 * <ul>
 *     <li> Has a non-private zero argument constructor </li>
 * </ul>
 *
 * <p>
 * Then a static method named <pre>of</pre> may be generated which provides an
 * equivalent to an "all args constructor"
 *
 * <p>
 * If the marked class
 *
 * <ul>
 *     <li> Is final </li>
 * </ul>
 *
 * <p>
 * Then an implementation of equals/hashCode may be generated which is based on every non-static field.
 * Note that if you are using this for making a JPA Entity or similar class, you likely do not
 * want to do this.
 *
 * <p>
 * You may also request a default implementation of toString which includes every field.
 *
 * <p>
 *     The generated class can also extend another class provided that other class is extensible
 *     and has a zero arg constructor.
 * </p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface MagicBean {
    /**
     * Will require --enable-preview on java 17.
     *
     * @return Whether to Use a CUPS expression instead of a normal cast.
     */
    boolean useTypeSafeCast() default false;

    /**
     * @return Whether to generate an all args static factory method.
     */
    boolean generateAllArgsStaticFactory() default false;

    /**
     * @return Whether to generate an equals and hash code implementation.
     */
    boolean generateEqualsAndHashCode() default false;

    /**
     * @return Whether to generate a basic toString.
     */
    boolean generateToString() default false;

    /**
     * @return A class for the generated abstract class to extend. Does not support
     * providing type parameters to generic classes or extending classes which do not have a
     * zero arg constructor.
     */
    Class<?> extend() default Object.class;
}
