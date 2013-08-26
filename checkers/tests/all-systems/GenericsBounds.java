import java.util.*;

interface A<ID extends Object> {}

class B1<ID extends Object> implements A<ID> {}

interface B2 extends A<Long> {}

class C extends B1<Long> implements B2 {}

class Upper<ID extends Object, X extends A<ID>, Y extends X> {}

class Lower extends Upper<Long, B2, C> {}

class Test {
    Upper<Long, B2, C> f = new Upper<Long, B2, C>();
}

class Upper1<ID extends Object, X extends List<ID>> {}
class Lower1 extends Upper1<Long, List<Long>> {}

class Upper2<ID extends Object, X extends List<ID>, Y extends X> {}
class Lower2 extends Upper2<Long, List<Long>, LinkedList<Long>> {}


class GenericGetClass {

    <U extends Object> Class<? extends U> getClass(Class<?> orig, Class<U> cast) {
      return orig.asSubclass(cast);
    }

}
