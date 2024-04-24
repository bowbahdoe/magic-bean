# Tips for usage with Rife2

Rife2 has a few parts that want classes with getters and setters but where those classes need to extend from
a common base class. The `extends` option is useful for this sort of sceneario.

```java
@MagicBean(extend = MetaData.class)
public non-sealed class Person extends PersonBeanOps {
    Integer id;
    String name;

    @Override
    public void activateMetaData() {
        addConstraint(new ConstrainedProperty("name")
                .maxLength(20)
                .notNull(true));
    }
}
```