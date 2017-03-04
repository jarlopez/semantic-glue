package edu.kth.wsglue.parsing.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WSDLUtil {
    public static final Set<String> primitiveTypes = new HashSet<>();

    private static final String PRIMITIVE_TYPE_NAMES = "boolean decimal int short long double string date dateTime Array";

    static {
        primitiveTypes.addAll(Arrays.asList(PRIMITIVE_TYPE_NAMES.split(" ")));
    }

    public static boolean isPrimitiveType(String test) {
        return primitiveTypes.contains(test);
    }

    public class Selectors {
        public static final String WSDL_NS = "wsdl:";
        public static final String Service = "service";
        public static final String PortType = "portType";
        public static final String Operation = "operation";
        public static final String Output = "output";
        public static final String Input = "input";
    }
}
