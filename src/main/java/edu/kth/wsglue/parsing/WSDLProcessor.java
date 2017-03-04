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

public class WSDLProcessor extends DocumentProcessor {
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class.getName());

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
                Element inputEl = (Element) node.getElementsByTagName(WSDLUtil.Selectors.Input).item(0);
                Element outputEl = (Element) node.getElementsByTagName(WSDLUtil.Selectors.Output).item(0);
                Operation operation = new Operation(node.getAttribute("name"));
                Message input = new Message(inputEl.getAttribute("message"));
                Message output = new Message(outputEl.getAttribute("message"));

                // Extracts message parts
                Element inputMsg = helper.findElementByName(input.getName());
                Element outputMsg = helper.findElementByName(output.getName());
                NodeList inputParts = inputMsg.getElementsByTagNameNS("*", "part");
                NodeList outputParts = outputMsg.getElementsByTagNameNS("*", "part");

                for (int j = 0; j < inputParts.getLength(); j++) {
                    Element part = (Element) inputParts.item(j);
                    String partName = part.getAttribute("name");
                    String elementCheck = part.getAttribute("element");
                    if (elementCheck == null) {
                        log.debug("Element is null for " + input.getFullName());
                        String typeCheck = part.getAttribute("type");
                        if (typeCheck != null) {
                            TagName typeTag = new TagName(typeCheck);
                            if (WSDLUtil.isPrimitiveType(typeTag.getName())) {
                                log.info("Found primitive type: " + partName);
                            }
                        }
                    } else {
                        // Find element and process as needed
                        TagName elementTag = new TagName(elementCheck);
                        log.debug("Processing element " + elementTag.getName() + " in part " + partName);
                    }
                }

            }

            // Walk through inputs and outputs, mapping them to messages and further on to their base simple types
        }
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
