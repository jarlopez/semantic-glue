package edu.kth.wsglue.parsing;

import edu.kth.wsglue.models.wsdl.Message;
import edu.kth.wsglue.models.wsdl.Operation;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
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
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class.getName());

    private List<WSDLSummary> summaries = new ArrayList<>();

    private WSDLHelper helper = new WSDLHelper();

    public WSDLProcessor(String wd) {
        super(wd);
    }

    @Override
    protected void transform() {
        for (Document document : documents) {
            // Build up search cache in helper
            helper.updateDocument(document);

            WSDLSummary summary = new WSDLSummary(document);
            // Process operations
            NodeList operations = document.getElementsByTagName(WSDLUtil.Selectors.Operation);
            for (int i = 0; i < operations.getLength(); i++) {
                Element node = (Element) operations.item(i);
                if (!Objects.equals(node.getParentNode().getNodeName(), WSDLUtil.Selectors.PortType)) {
                    log.debug("Operation node not inside PortType. Skipping");
                    continue;
                }
                Element inputEl = (Element) node.getElementsByTagName(WSDLUtil.Selectors.Input).item(0);
                Element outputEl = (Element) node.getElementsByTagName(WSDLUtil.Selectors.Output).item(0);
                Operation operation = new Operation(node.getAttribute("name"));
                Message input = new Message(inputEl.getAttribute("message"));
                Message output = new Message(outputEl.getAttribute("message"));

                // Extracts message parts
                Element inputMsg = helper.findElementByName(input.getName());
                Element outputMsg = helper.findElementByName(output.getName());
                Set<String> inputFields = extractOperationFields(inputMsg);
                Set<String> outputFields = extractOperationFields(outputMsg);

                input.setFieldNames(inputFields);
                output.setFieldNames(outputFields);

                operation.setInput(input);
                operation.setOutput(output);

                summary.getOperations().add(operation);
                log.debug("Fields generated for " + node.getAttribute("name") + ": " + inputFields + outputFields);
            }
            summaries.add(summary);
        }
        log.info("Done processing documents into in-memory summaries");
        log.debug(String.valueOf(summaries));
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
        System.out.println("compare");
    }

    @Override
    protected void unload() {
        System.out.println("unload");
    }
}
