package edu.kth.wsglue.parsing.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WSDLUtil {
    public static final Set<String> primitiveTypes = new HashSet<>();

    private static final String PT_DELIM = "\r\n";
    private static final String PRIMITIVE_TYPE_NAMES = "" +
            "anyType" + PT_DELIM +
            "anyURI" + PT_DELIM +
            "Array" + PT_DELIM +
            "base64Binary" + PT_DELIM +
            "boolean" + PT_DELIM +
            "byte" + PT_DELIM +
            "date" + PT_DELIM +
            "dateTime" + PT_DELIM +
            "decimal" + PT_DELIM +
            "double" + PT_DELIM +
            "float" + PT_DELIM +
            "ID" + PT_DELIM +
            "IDREF" + PT_DELIM +
            "int" + PT_DELIM +
            "integer" + PT_DELIM +
            "long" + PT_DELIM +
            "QName" + PT_DELIM +
            "short" + PT_DELIM +
            "string" + PT_DELIM +
            "unsignedByte" + PT_DELIM +
            "unsignedInt" + PT_DELIM +
            "unsignedLong" + PT_DELIM +
            "unsignedShort";
    static {
        primitiveTypes.addAll(Arrays.asList(PRIMITIVE_TYPE_NAMES.split(PT_DELIM)));
    }

    public static boolean isPrimitiveType(String test) {
        return primitiveTypes.contains(test);
    }

    public static boolean isEmptyString(String s) {
        return s == null || "".equals(s.trim());
    }
    public class Selectors {
        public static final String WSDL_NS = "wsdl:";
        public static final String Service = "service";
        public static final String PortType = "portType";
        public static final String Operation = "operation";
        public static final String Output = "output";
        public static final String Input = "input";

        public static final String WSDL_Service = WSDL_NS + "service";
        public static final String WSDL_PortType = WSDL_NS + "portType";
        public static final String WSDL_Operation = WSDL_NS + "operation";
        public static final String WSDL_Output = WSDL_NS + "output";
        public static final String WSDL_Input = WSDL_NS + "input";
    }
}
