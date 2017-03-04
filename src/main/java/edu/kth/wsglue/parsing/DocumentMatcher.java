package edu.kth.wsglue.parsing;

import com.predic8.schema.*;
import com.predic8.schema.restriction.BaseRestriction;
import com.predic8.wsdl.*;
import edu.kth.wsglue.generated.MatchedWebServiceType;
import edu.kth.wsglue.generated.ObjectFactory;
import edu.kth.wsglue.generated.WSMatchingType;
import edu.kth.wsglue.thirdparty.EditDistance;
import groovy.xml.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class DocumentMatcher {
    private static final Logger log = LoggerFactory.getLogger(DocumentMatcher.class.getName());

    private static final Double ED_ACCEPTANCE_THRESHOLD = 0.8;

    private ObjectFactory factory = new ObjectFactory();
    private WSDLParser parser = new WSDLParser();

    private Map<String, HashMap> flattenCache = new HashMap<>();
    private static final Set<String> primitiveTypes = new HashSet<>();

    static {
        primitiveTypes.addAll(Arrays.asList("int short long double string date dateTime Array".split(" ")));
    }

    public WSMatchingType generateMatchProfile(Definitions first, Definitions second) throws Exception {
        WSMatchingType match = factory.createWSMatchingType();
        MatchedWebServiceType serviceMatch = factory.createMatchedWebServiceType();

        if (first.getServices().size() != 1) {
            log.warn("Document providing " + first.getServices().get(0).getName() + " has more than one defined service! Using the first one..");
        }
        if (second.getServices().size() != 1) {
            log.warn("Document providing " + second.getServices().get(0).getName()+ " has more than one defined service! Using the first one..");
        }

        String firstName = first.getServices().get(0).getName();
        String secondName = second.getServices().get(0).getName();
        serviceMatch.setInputServiceName(firstName);
        serviceMatch.setOutputServiceName(secondName);

        for (Operation opA : first.getOperations()) {
            for (Operation opB : second.getOperations()) {
                Message outMessage = getMessageFromPortType(first, opA.getOutput());
                Message inMessage = getMessageFromPortType(second, opB.getInput());
                if (outMessage.getParts().size() != inMessage.getParts().size()) {
                    log.debug("Ignoring matching operations " + opA.getName() + " to " + opB.getName() + " because of mismatched inputs/outputs");
                    continue;
                }
                Double opDistance = EditDistance.getSimilarity(opA.getName(), opB.getName());
                if (!(opDistance >= ED_ACCEPTANCE_THRESHOLD)) {
//                    log.debug("Skipping " + opA.getName() + " --> " + opB.getName());
//                    continue;
                }
                log.debug("Edit distance between operations " + opA.getName() + " and " + opB.getName() + " is " + opDistance);
                Map outputs = new HashMap();
                for (Part part : outMessage.getParts()) {
                    Element el = part.getElement();
//                    if (el == null) {
//                        TypeDefinition type = findPartType(first, part);
//                        outputs.putAll(flattenType(first, type));
//                    }
//                    outputs.putAll(flattenElement(first, el));
                    outputs.putAll(flatten(first, part));
                }
                Map inputs = new HashMap();
                for (Part part : inMessage.getParts()) {
                    Element el = part.getElement();
//                    if (el == null) {
//                        inputs.put(part.getName(), part.getType().getQname().getLocalPart());
//                    } else {
//                        inputs.putAll(flattenElement(second, el));
//                    }
                }
                log.debug("Generated input/output maps for " + opA.getName() + " --> " + opB.getName());
                log.debug(String.valueOf(outputs));
                log.debug(String.valueOf(inputs));
            }
        }

        match.getMacthing().add(serviceMatch);
        return match;
    }

    public WSMatchingType generateMatchProfile(String inputA, String inputB) throws Exception {
        Definitions first = parser.parse(inputA);
        Definitions second = parser.parse(inputB);
        return generateMatchProfile(first, second);
    }


    public Map flatten(Definitions defs, Part part) {
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

    public Map flatten(Definitions defs, SchemaComponent it) {
        HashMap<String, String> rv = new HashMap<>();
        if (it == null) {
            return rv;
        }
        String cacheKey = genCacheKey(defs, it);
//        if (flattenCache.containsKey(cacheKey)) {
//            return flattenCache.get(cacheKey);
//        }

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

    private String genCacheKey(Definitions defs, SchemaComponent it) {
        return defs.getTargetNamespace() + ":" + it.getName();
    }
//
//    private Map flattenElement(Definitions doc, Element el) {
//        HashMap<String, String> rv = new HashMap<>();
//        if (el == null) {
//            return rv;
//        }
//        String elName = el.getName();
//        String cacheKey = doc.getTargetNamespace() + elName;
//        // Prepend the namespace to ensure no collisions across vendors
//        if (elName != null && flattenCache.containsKey(cacheKey)) {
//            // No need to recompute
////            log.debug("CACHE  HIT: " + cacheKey);
//            return flattenCache.get(cacheKey);
//        }
//        log.debug("CACHE MISS: " + cacheKey);
//
//        rv.putAll(flatten(doc, el, Optional.empty()));
//
//        if (el.getName() != null) {
//            flattenCache.put(cacheKey, rv);
//        }
//        return rv;
//    }
//
//    private Map flatten(Definitions doc, Element el, Optional<TypeDefinition> typeDefinition) {
//        HashMap<String, String> rv = new HashMap<>();
//        if (!typeDefinition.isPresent()) {
//            typeDefinition = Optional.ofNullable(el.getEmbeddedType());
//        }
//        if (!typeDefinition.isPresent()) {
//            String typeName = el.getType().getQualifiedName();
//            typeDefinition = Optional.ofNullable(findElementType(el, typeName));
//        }
//        if (typeDefinition.isPresent() && typeDefinition.get() instanceof ComplexType) {
//            ComplexType complex = (ComplexType) typeDefinition.get();
//            if (complex.getModel() instanceof ModelGroup) {
//                ModelGroup seq = (ModelGroup) complex.getModel();
//                for (SchemaComponent it : seq.getParticles()) {
//                    if (it instanceof Element) {
//                        Element subEl = (Element) it;
//                        String subElName = subEl.getName();
//                        if (subElName == null) {
//                            // Likely an object from another namespace not included in the document
//                            log.warn("Cannot recurse into " + subEl);
//                        } else {
//                            rv.putAll(flattenElement(doc, subEl));
//                        }
//                    } else {
//                        log.debug("TODO AA Handle " + it.getClass().getName());
//                    }
//                }
//            } else {
//                log.debug("TODO BB Handle " + complex.getModel().getClass().getName());
//            }
//        } else {
//            String typeName = el.getType().getQualifiedName();
//            String typeLookup = el.getType().getLocalPart();
//            if (typeDefinition.isPresent() && typeDefinition.get() instanceof SimpleType) {
//                BaseRestriction br = ((SimpleType) typeDefinition.get()).getRestriction();
//                if (br != null) {
//                    typeLookup = br.getBase().getLocalPart();
//                }
//            }
//
//            if (isPrimitiveType(typeLookup)) {
//                rv.put(el.getName(), typeName);
//            } else {
//                TypeDefinition td = findElementType(el, typeName);
//                rv.putAll(flatten(doc, el, Optional.ofNullable(td)));
//            }
//        }
//
//        return rv;
//    }
//
//    private Map flattenType(Definitions doc, TypeDefinition type) {
//        HashMap<String, String> rv = new HashMap<>();
//
//        return rv;
//    }

    private boolean isPrimitiveType(String type) {
        return primitiveTypes.contains(type);
    }

    private String getFirstServiceName(Definitions defs) throws Exception {
        if (defs.getServices().size() > 0) {
            return defs.getServices().get(0).getName();
        } else {
            throw new Exception("Definition document has no services");
        }
    }

    private Message getMessageFromPortType(Definitions document, AbstractPortTypeMessage it) throws Exception {
        String messageName = null;
        if (it.getMessage() != null) {
            messageName = it.getMessage().getName();
        } else if (it.getMessagePrefixedName() != null) {
            messageName = it.getMessagePrefixedName().getLocalName();
        }
        if (messageName == null) {
            throw new Exception("Message name is null for port type " + it.toString());
        }
        return document.getMessage(messageName);
    }

    private TypeDefinition findElementType(Element el, String typeName) {
        String localTypeName = el.getType().getLocalPart();
        return el.getSchema().getType(localTypeName);
    }

    private TypeDefinition findPartType(Definitions doc, Part type) {
        return doc.getSchema(type.getType().getQname().getNamespaceURI()).getType(type.getType().getQname());
    }
}
