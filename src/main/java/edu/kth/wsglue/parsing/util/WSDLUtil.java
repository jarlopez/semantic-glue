package edu.kth.wsglue.parsing.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WSDLUtil {
    public static final Set<String> primitiveTypes = new HashSet<>();

    static {
        primitiveTypes.addAll(Arrays.asList("int short long double string date dateTime Array".split(" ")));
    }

    public static boolean isPrimitiveType(String test) {
        return primitiveTypes.contains(test);
    }

    public class Selectors {
        public static final String WSDL_NS = "wsdl:";
        public static final String Operation = WSDL_NS + "operation";
        public static final String Output = WSDL_NS + "output";
        public static final String Input = WSDL_NS + "input";
    }
}
