package edu.kth.wsglue.parsing;

import com.predic8.schema.*;
import com.predic8.schema.restriction.BaseRestriction;
import com.predic8.wsdl.Definitions;
import com.predic8.wsdl.Part;
import groovy.xml.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WSDLFlattener {
    private static final Logger log = LoggerFactory.getLogger(WSDLFlattener.class.getName());

    private static Map<String, HashMap> flattenCache = new HashMap<>();
    private static final Set<String> primitiveTypes = new HashSet<>();

    static {
        primitiveTypes.addAll(Arrays.asList("int short long double string date dateTime Array".split(" ")));
    }
    public static Map flatten(Definitions defs, Part part) {
        HashMap<String, String> rv = new HashMap<>();
        if (defs == null || part == null) {
            return rv;
        }
        // Part can either point to element or a type:
        //      <wsdl:part name="..." element="tns:SomeElement"/>
        //      <wsdl:part name='...' type='xsd:string'/>

        Element el = part.getElement();
        TypeDefinition td = part.getType();
        String name = null;
        String cacheKey = null;
        if (part.getType() != null &&
                part.getType().getQname() != null &&
                isPrimitiveType(part.getType().getQname().getLocalPart())) {
            rv.put(part.getName(), part.getType().getQname().getLocalPart());
        } else if (el != null) {
            rv.putAll(flatten(defs, el));
        } else if (td != null) {
            rv.putAll(flatten(defs, td));
        }
        return rv;
    }

    public static Map flatten(Definitions defs, SchemaComponent it) {
        HashMap<String, String> rv = new HashMap<>();
        if (it == null) {
            return rv;
        }
        String cacheKey = genCacheKey(defs, it);

        // Separate Element vs TypeDefinition
        TypeDefinition type = null;
        if (it instanceof Element) {
            Element el = (Element) it;
            String typeName = null;
            String typeLookup = null;
            if (el.getType() != null) {
                typeName = el.getType().getQualifiedName();
                typeLookup = el.getType().getLocalPart();
                type = findElementType(el, typeName);
            } else if (el.getEmbeddedType() != null) {
                type = el.getEmbeddedType();
                typeName = type.getName();
                typeLookup = "";
            }

            if (isPrimitiveType(typeLookup)) {
                rv.put(el.getName(), typeName);
            } else if (type != null) {
                rv.putAll(flatten(defs, type));
            } else {
                log.warn("Unsure how I got here :( " + cacheKey);
            }

        } else if (it instanceof TypeDefinition) {
            TypeDefinition td = (TypeDefinition) it;
            if (td instanceof SimpleType) {
                // Easy case
                BaseRestriction br = ((SimpleType) td).getRestriction();
                if (br != null) {
                    String baseType = br.getBase().getLocalPart();
                    if (isPrimitiveType(baseType)) {
                        rv.put(it.getName(), baseType);
                    }
                }
            } else if (td instanceof ComplexType) {
                // Pull out sub-components
                ComplexType ct = (ComplexType) td;
                if (ct.getModel() instanceof ModelGroup) {
                    ModelGroup seq = (ModelGroup) ct.getModel();
                    for (SchemaComponent sc : seq.getParticles()) {
                        rv.putAll(flatten(defs, sc));
                    }
                } else if (ct.getModel() instanceof ComplexContent) {
                    // Could be a complex SOAP structure such as SOAP-ENC:Array
                    ComplexContent cc = (ComplexContent) ct.getModel();
                    Derivation der = cc.getDerivation();
                    if (der != null) {
                        QName complexBaseType = der.getBase();
                        if (complexBaseType != null && isPrimitiveType(complexBaseType.getLocalPart())) {
                            rv.put(it.getName(), complexBaseType.getLocalPart());
                        }
                    }

                } else {
                    log.warn("Unknown model for complex type " + ct);
                }
            } else if (td instanceof BuiltInSchemaType) {

            } else {
                log.warn("Unknown type definition " + td);
            }
        }

        flattenCache.put(cacheKey, rv);

        return rv;
    }

    private static String genCacheKey(Definitions defs, SchemaComponent it) {
        return defs.getTargetNamespace() + ":" + it.getName();
    }

    private static boolean isPrimitiveType(String type) {
        return primitiveTypes.contains(type);
    }

    private static TypeDefinition findElementType(Element el, String typeName) {
        String localTypeName = el.getType().getLocalPart();
        return el.getSchema().getType(localTypeName);
    }
}
