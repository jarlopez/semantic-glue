package edu.kth.wsglue.parsing;

import com.predic8.schema.*;
import com.predic8.schema.restriction.BaseRestriction;
import com.predic8.wsdl.*;
import edu.kth.wsglue.generated.MatchedWebServiceType;
import edu.kth.wsglue.generated.ObjectFactory;
import edu.kth.wsglue.generated.WSMatchingType;
import edu.kth.wsglue.thirdparty.EditDistance;
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
        primitiveTypes.addAll(Arrays.asList("int short long double string date dateTime".split(" ")));
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
                log.debug("Edit distance between operations " + opA.getName() + " and " + opB.getName() + " passes threshold");
                Map outputs = new HashMap();
                for (Part part : outMessage.getParts()) {
                    Element el = part.getElement();
                    outputs.putAll(flattenElement(first, el));
                }
                Map inputs = new HashMap();
                for (Part part : inMessage.getParts()) {
                    Element el = part.getElement();
                    if (el == null) {
                        inputs.put(part.getName(), part.getType().getQname().getLocalPart());
                    } else {
                        inputs.putAll(flattenElement(second, el));
                    }
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

    private Map flattenElement(Definitions doc, Element el) {
        HashMap<String, String> rv = new HashMap<>();
        if (el == null) {
            return rv;
        }
        String elName = el.getName();
        String cacheKey = doc.getTargetNamespace() + elName;
        // Prepend the namespace to ensure no collisions across vendors
        if (elName != null && flattenCache.containsKey(cacheKey)) {
            // No need to recompute
//            log.debug("CACHE  HIT: " + cacheKey);
            return flattenCache.get(cacheKey);
        }
        log.debug("CACHE MISS: " + cacheKey);

        rv.putAll(flatten(doc, el));

        if (el.getName() != null) {
            flattenCache.put(cacheKey, rv);
        }
        return rv;
    }
    private Map flatten(Definitions doc, Element el) {
        HashMap<String, String> rv = new HashMap<>();
        TypeDefinition typeDefinition = el.getEmbeddedType();
        if (typeDefinition == null) {
            String typeName = el.getType().getQualifiedName();
            typeDefinition = findElementType(el, typeName);
        }
        if (typeDefinition instanceof ComplexType) {
            ComplexType complex = (ComplexType) typeDefinition;
            if (complex.getModel() instanceof Sequence) {
                Sequence seq = (Sequence) complex.getModel();
                for (SchemaComponent it : seq.getParticles()) {
                    if (it instanceof Element) {
                        Element subEl = (Element) it;
                        String subElName = subEl.getName();
                        if (subElName == null) {
                            // Likely an object from another namespace not included in the document
                            log.warn("Cannot recurse into " + subEl);
                        } else {
                            rv.putAll(flattenElement(doc, subEl));
                        }
                    } else {
                        log.debug("TODO AA Handle " + it.getClass().getName());
                    }
                }
            } else {
                log.debug("TODO BB Handle " + complex.getModel().getClass().getName());
            }
        } else {
            String typeName = el.getType().getQualifiedName();
            String typeLookup = el.getType().getLocalPart();
            if (typeDefinition instanceof SimpleType) {
                BaseRestriction br = ((SimpleType) typeDefinition).getRestriction();
                if (br != null) {
                    typeLookup = br.getBase().getLocalPart();
                }
            }

            if (isPrimitiveType(typeLookup)) {
                rv.put(el.getName(), typeName);
            } else {
                TypeDefinition td = findElementType(el, typeName);
                rv.putAll(flatten(doc, el));
            }
        }

        return rv;
    }

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
}
