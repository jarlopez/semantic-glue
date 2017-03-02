package edu.kth.wsglue.parsing;

import com.predic8.schema.ComplexType;
import com.predic8.schema.Element;
import com.predic8.schema.TypeDefinition;
import com.predic8.wsdl.*;
import edu.kth.wsglue.generated.MatchedWebServiceType;
import edu.kth.wsglue.generated.ObjectFactory;
import edu.kth.wsglue.generated.WSMatchingType;
import edu.kth.wsglue.thirdparty.EditDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class DocumentMatcher {
    private static final Logger log = LoggerFactory.getLogger(DocumentMatcher.class.getName());

    private static final Double ED_ACCEPTANCE_THRESHOLD = 0.8;

    private ObjectFactory factory = new ObjectFactory();
    private WSDLParser parser = new WSDLParser();

    private Map<String, HashMap> flattenCache = new HashMap<>();


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
                Double opDistance = EditDistance.getSimilarity(opA.getName(), opB.getName());
                if (opDistance >= ED_ACCEPTANCE_THRESHOLD) {
                    log.debug("Edit distance between operations " + opA.getName() + " and " + opB.getName() + " passes threshold");
                }
                Message outMessage = getMessageFromPortType(first, opA.getOutput());
                Message inMessage = getMessageFromPortType(second, opA.getInput());
                for (Part part : outMessage.getParts()) {
                    // Extract name and type of all parts
                    TypeDefinition type = part.getType();
                    if (type == null) {
                        // Need to inspect element
                        Element el = part.getElement();
                        flattenElement(first, el);
                    } else {
                        log.debug("Part " + part.getName() + " has type " + type.getName());
                    }
                }
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
        HashMap rv = new HashMap();
        if (el == null) {
            return rv;
        }
        // Check cache
        String elName = el.getName();
        if (elName != null && flattenCache.containsKey(elName)) {
            return flattenCache.get(elName);
        }

        TypeDefinition type = el.getEmbeddedType();
        if (type instanceof ComplexType) {
            ComplexType complex = (ComplexType) type;
            log.debug("Flattening complex type in " + el.getName());
        } else {

        }
        
        if (el.getName() != null) {
            flattenCache.put(elName, rv);
        }
        return rv;
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
}
