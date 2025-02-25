package org.dimer.compiler.util;

import org.dimer.compiler.data.ComparisonOps;

import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class CompilerConstants {
    public static final String TYPE_INT = "int";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_VOID = "void";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_BOOL = "bool";

    public static final String OPERATOR_GT = ">";
    public static final String OPERATOR_LT = "<";
    public static final String OPERATOR_GTE = ">=";
    public static final String OPERATOR_LTE = "<=";
    public static final String OPERATOR_EQUAL = "==";
    public static final String OPERATOR_NOTEQUAL = "!=";
}
