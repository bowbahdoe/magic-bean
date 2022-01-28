# Tips for usage with JPA

Based on the spec [here](https://docs.oracle.com/javaee/6/tutorial/doc/bnbqa.html)

## 1. Declare your JPA classes as non-sealed.

Because this processor generates a sealed abstract class, Java requires that
you explicitly decide whether any extending class is `final`, `sealed`, or `non-sealed`.

[The correct choice when making a JPA entity is `non-sealed`](https://stackoverflow.com/questions/55574416/why-jpa-requires-entity-classes-to-be-non-final-fields-non-final#:~:text=The%20class%20must%20not%20be,protected%2C%20no%2Dargument%20constructor.)


## 2. Do not generate equals or hashCode
You won't be able to do that without making your class final, which goes afoul of the rule
above, but also a "proper" equals and hashCode implementation for a JPA entity should not
be based on the fields of said entity.

[For more information, consult those more experienced in this](https://thorben-janssen.com/ultimate-guide-to-implementing-equals-and-hashcode-with-hibernate/)

## 3. Make fields be package-private and don't access them directly
One of the limitations of this processor is that none of the fields can be made
private. You should still make fields package private and follow the convention
of accessing them only through the getters.


## 4. Be careful when generating toString
There are some caveats with making a toString for an entity. Make sure to read up 
[here](https://struberg.wordpress.com/2016/10/15/tostring-equals-and-hashcode-in-jpa-entities/).

Likely you will want to write this yourself or not writing one at all.

## 5. Override methods as needed.

In some contexts, you might want to have a different implementation for
a getter or a setter. In these cases, you should simply `@Override`
the method in the generated superclass.

## Full example

```java 
import javax.persistence.*;
import java.util.Objects;

import dev.mccue.magicbean.MagicBean;

@Entity
@Table(name = "employees")
@MagicBean
public non-sealed class Employee extends EmployeeBeanOps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "department")
    String department;

    @Column(name = "email")
    String email;

    @Column(name = "salary")
    String salary;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "client")
    Collection<ClientAttributeEntity> attributes;
    
    @Override
    public Collection<ClientAttributeEntity> getAttributes() {
        if (attributes == null) {
            attributes = new LinkedList<>();
        }
        return attributes;
    }

    public Employee() {

    }

    public Employee(String firstName, String lastName, String department, String email, String salary) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.email = email;
        this.salary = salary;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClientEntity that)) {
            return false;
        }
        else {
            return Objects.equals(this.id, that.getId());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
```
