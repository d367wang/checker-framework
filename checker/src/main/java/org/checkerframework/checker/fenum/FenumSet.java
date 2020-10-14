package org.checkerframework.checker.fenum;

import com.sun.source.tree.VariableTree;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.checker.fenum.qual.FenumPattern;

public class FenumSet {
    /** Constant that indicates the type of this FenumSet is "integer". */
    public static final boolean TYPE_INTEGER = true;

    /** Constant that indicates the type of this FenumSet is "string". */
    public static final boolean TYPE_STRING = false;

    /**
     * Set that stores the constant int values in the fake enum set, will be null if this is a fake
     * enum of constant strings.
     */
    private Set<String> stringVals;

    /**
     * Set that stores the constant string values in the fake enum set, will be null if this is a
     * fake enum of constant integers.
     */
    private Set<Integer> intVals;

    /**
     * Flag that indicates the type of this FenumSet, true if the type is "integer" and flase if the
     * type is "string".
     */
    private boolean type;

    /**
     * String that identifies the FenumSet, which is specified as the parameter {@code Fenum}
     * annotations.
     */
    private String name;

    /**
     * Enum that indicates the pattern of FenumSet, which is specified as an optional parameter
     * {@code Fenum} annotations.
     */
    private FenumPattern pattern;

    /** Location of the starting constant of the fenum enum set. */
    private VariableTree start;

    /** Is this fake enum set already validated? */
    private boolean isValidated;

    /**
     * Constructor of FenumSet
     *
     * @param name
     * @param type
     * @param pattern
     * @param start
     */
    public FenumSet(String name, boolean type, FenumPattern pattern, VariableTree start) {
        this.type = type;
        this.name = name;
        this.pattern = pattern;
        this.start = start;

        stringVals = new HashSet<>();
        intVals = new TreeSet<>();
    }

    public String getName() {
        return name;
    }

    /**
     * Check if this FenumSet is of type "integer".
     *
     * @return field {@link type}, which indicates the type of the FenumSet
     */
    public boolean isInteger() {
        return type;
    }

    /**
     * Checks if this FenumSet is of type "string".
     *
     * @return the negation of {@link type}, which indicates the type of the FenumSet
     */
    public boolean isString() {
        return !type;
    }

    /**
     * Checks if the fake enum set contains a certain value.
     *
     * @param o the value to be test
     * @return true if the set contains the specified value
     */
    public boolean containsValue(Object o) {
        if (isInteger()) {
            return intVals.contains((int) o);
        } else {
            return stringVals.contains((String) o);
        }
    }

    /**
     * Adds certain constant value to the fake enum set.
     *
     * @param o the value to be added
     */
    public void addValue(Object o) {
        if (isInteger()) {
            intVals.add((int) o);
        } else {
            stringVals.add((String) o);
        }
    }

    /**
     * Returns the fake enum pattern {@link FenumPattern}
     *
     * @return fake enum pattern
     */
    public FenumPattern getPattern() {
        return pattern;
    }

    /**
     * Returns the starting constant of the fake enum set.
     *
     * @return the starting constant of the fake enum set
     */
    public VariableTree getStart() {
        return start;
    }

    /**
     * Checks if the fake enum set is already validated.
     *
     * @return true if the fake enum set is already validated
     */
    public boolean isValidated() {
        return isValidated;
    }

    /**
     * Set the flag that indicates if the fake enum set is already validated.
     *
     * @param validated the flag {@link isValidated} that indicates if the fake enum set is already
     *     validated
     */
    public void setValidated(boolean validated) {
        isValidated = validated;
    }

    /**
     * Checks if the items in the fake enum set are consecutive integers.
     *
     * @return true if the items in the fake enum set are consecutive integers
     */
    public boolean isConsecutive() {
        return isInteger()
                && (intVals.size() - 1
                        == ((TreeSet<Integer>) intVals).last()
                                - ((TreeSet<Integer>) intVals).last());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("name: " + name);
        sb.append("\n");
        sb.append("pattern: " + pattern);
        sb.append("\n");
        sb.append("{");
        if (type) {
            for (int i : intVals) {
                sb.append(i);
                sb.append(", ");
            }
        } else {
            for (String s : stringVals) {
                sb.append("\"");
                sb.append(s);
                sb.append("\"");
                sb.append(", ");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
}
