import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.fenum.qual.FenumPattern;

@SuppressWarnings("fenum:assignment.type.incompatible")
public class TestFenumDefination {

    // 1. Check ducplicated constant value in the same fake enum set
    public static final @Fenum("A") int ACONST1 = 1;
    public static final @Fenum("A") int ACONST2 = 2;
    // :: error: (duplicated.constant.value)
    public static final @Fenum("A") int ACONST3 = 2;

    public final @Fenum("B") String BCONST1 = "4";
    public static final @Fenum("B") String BCONST2 = "5";
    // :: error: (duplicated.constant.value)
    public static final @Fenum("B") String BCONST3 = "5";

    // 2. Ensure the underlying constant type is either int or String.
    // :: error: (unsupported.constant.type)
    public static final @Fenum("C") Integer CCONST1 = 1;

    // 3. Ensure constants of consecutive pattern are consecutive.
    // :: error: (nonconsecutive.constant.values)
    public static final @Fenum(value = "D", pattern = FenumPattern.CONSECUTIVE) int DCONST1 = 1;
    public static final @Fenum(value = "D", pattern = FenumPattern.CONSECUTIVE) int DCONST2 = 3;

    // 4. Ensure constants of flag pattern are powers of 2.
    public static final @Fenum(value = "E", pattern = FenumPattern.FLAG) int ECONST1 = 1;
    public static final @Fenum(value = "E", pattern = FenumPattern.FLAG) int ECONST2 = 2;
    public static final @Fenum(value = "E", pattern = FenumPattern.FLAG) int ECONST3 = 4;
    // :: error: (not.powerof.two)
    public static final @Fenum(value = "E", pattern = FenumPattern.FLAG) int ECONST4 = 5;
    public static final @Fenum(value = "E", pattern = FenumPattern.FLAG) int ECONST5 =
            1 << 4; // flag bit does not have to cover every bit

    // 5. Validate Fenum annotations
    public static final @Fenum(value = "F", pattern = FenumPattern.CONSECUTIVE) int FCONST1 = 1;
    // :: error: (fenum.pattern.conflict)
    public static final @Fenum(value = "F", pattern = FenumPattern.FLAG) int FCONST2 = 2;
    // :: error: (fenum.pattern.conflict)
    public static final @Fenum(value = "F", pattern = FenumPattern.FLAG) int FCONST3 = 4;

    // :: error: (fenum.pattern.not.applicable)
    public static final @Fenum(value = "G", pattern = FenumPattern.FLAG) String GCONST1 = "1";

    // 6. Ensure constants in the same fake enum set are of the same underlying type
    public static final @Fenum("H") String HCONST1 = "1";
    // :: error: (constant.type.conflict)
    public static final @Fenum("H") int HCONST2 = 1;
}

class FenumUserTestDuplicatedConst {}
