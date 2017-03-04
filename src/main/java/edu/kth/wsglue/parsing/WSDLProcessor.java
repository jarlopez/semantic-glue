package edu.kth.wsglue.parsing;

import edu.kth.wsglue.models.generated.WSMatchingType;
import edu.kth.wsglue.models.wsdl.Message;
import edu.kth.wsglue.models.wsdl.Operation;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
import edu.kth.wsglue.parsing.comparators.SyntacticComparator;
import edu.kth.wsglue.parsing.util.TagName;
import edu.kth.wsglue.parsing.util.WSDLHelper;
import edu.kth.wsglue.parsing.util.WSDLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;

public class WSDLProcessor extends DocumentProcessor {
    private static final Logger log = LoggerFactory.getLogger(WSDLProcessor.class.getName());

    private List<WSDLSummary> summaries = new ArrayList<>();

    private WSDLHelper helper = new WSDLHelper();

    public WSDLProcessor(String wd) {
        super(wd);
    }

    private Set<WSMatchingType> comparisons = new HashSet<>();

    @Override
    protected void transform() {
        for (Document document : documents) {
            // Build up search cache in helper
            helper.updateDocument(document);
            // Construct in-memory representation
            summaries.add(processService(document));
        }
        log.info("Done processing documents into in-memory summaries");
        log.debug(String.valueOf(summaries));
    }

    private WSDLSummary processService(Document document) {
        WSDLSummary summary = new WSDLSummary(document);
        Element serviceEl = (Element) document.getElementsByTagNameNS("*", "service").item(0);
        summary.setServiceName(serviceEl.getAttribute("name"));
        summary.getOperations().addAll(processOperations(document));
        return summary;
    }

    private Set<Operation> processOperations(Document document) {
        Set<Operation> rv = new HashSet<>();
        if (document == null) {
            return rv;
        }
        NodeList operations = document.getElementsByTagNameNS("*", WSDLUtil.Selectors.Operation);
        for (int i = 0; i < operations.getLength(); i++) {
            Element node = (Element) operations.item(i);
            if (!Objects.equals(node.getParentNode().getLocalName(), WSDLUtil.Selectors.PortType)) {
                log.debug("Operation node not inside PortType. Skipping");
                continue;
            }
            Operation operation = new Operation(node.getAttribute("name"));

            operation.setInput(processIO(node, WSDLUtil.Selectors.Input));
            operation.setOutput(processIO(node, WSDLUtil.Selectors.Output));

            rv.add(operation);
        }
        return rv;
    }

    private Message processIO(Element operationNode, String mode) {
        Element el = (Element) operationNode.getElementsByTagNameNS("*", String.valueOf(mode)).item(0);
        Message msg = new Message(el.getAttribute("message"));
        Element msgEl = helper.findElementByName(msg.getName());
        Set<String> fields = extractOperationFields(msgEl);
        log.debug("Fields for " + msg.getName()+ ": " + fields);
        msg.setFieldNames(fields);
        return msg;
    }

    private Set<String> extractOperationFields(Element partContainer) {
        Set<String> fields = new HashSet<>();
        if (partContainer == null) {
            return fields;
        }
        NodeList parts = partContainer.getElementsByTagNameNS("*", "part");

        for (int j = 0; j < parts.getLength(); j++) {
            Element part = (Element) parts.item(j);
            String partName = part.getAttribute("name");
            String elementCheck = part.getAttribute("element");
            if (elementCheck == null) {
                String typeCheck = part.getAttribute("type");
                if (typeCheck != null) {
                    TagName typeTag = new TagName(typeCheck);
                    if (WSDLUtil.isPrimitiveType(typeTag.getName())) {
                        log.info("Found primitive type: " + partName);
                        fields.add(partName);
                    }
                }
            } else {
                // Find element and process as needed
                TagName elementTag = new TagName(elementCheck);
                Element el = helper.findElementByName(elementTag.getName());
                if (el != null) {
                    // Flatten into basic types
                    fields.addAll(helper.flatten(el));
                    log.debug(elementTag.getName() + ": " + fields.toString());
                }
            }
        }
        return fields;
    }

    @Override
    protected void compare() {
        for (int doc1 = 0; doc1 < summaries.size() - 1; doc1++) {
            for (int doc2 = doc1 + 1; doc2 < summaries.size(); doc2++) {
                SyntacticComparator sc = new SyntacticComparator();
                comparisons.add(sc.compare(summaries.get(doc1), summaries.get(doc2)));
                comparisons.add(sc.compare(summaries.get(doc2), summaries.get(doc1)));
            }
        }
    }

    @Override
    protected void unload() {
        for (WSMatchingType res : comparisons) {
            log.info("Match for " + res.getMacthing().get(0).getInputServiceName() +
                    " to " + res.getMacthing().get(0).getOutputServiceName());
        }
    }
}
