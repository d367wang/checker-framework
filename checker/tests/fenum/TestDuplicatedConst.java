import org.checkerframework.checker.fenum.qual.Fenum;

@SuppressWarnings("fenum:assignment.type.incompatible")
public class TestDuplicatedConst {

    public static final @Fenum("A") int ACONST1 = 1;
    public static final @Fenum("A") int ACONST2 = 2;
    // :: error: (duplicated.constant.value)
    public static final @Fenum("A") int ACONST3 = 2;

    public final @Fenum("B") String BCONST1 = "4";
    public static final @Fenum("B") String BCONST2 = "5";
    // :: error: (duplicated.constant.value)
    public static final @Fenum("B") String BCONST3 = "5";

    // :: error: (multiple.underlying.types)
    public static final @Fenum("A") String ACONST4 = "1";
}

class FenumUserTestDuplicatedConst {
    void foo() {}
}
