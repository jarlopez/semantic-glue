package edu.kth.wsglue.parsing;

import edu.kth.wsglue.models.generated.WSMatchingType;
import edu.kth.wsglue.models.wsdl.*;
import edu.kth.wsglue.parsing.comparators.SyntacticComparator;
import edu.kth.wsglue.parsing.filters.FilterFunction;
import edu.kth.wsglue.parsing.util.TagName;
import edu.kth.wsglue.parsing.util.WSDLHelper;
import edu.kth.wsglue.parsing.util.WSDLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.*;

import static edu.kth.wsglue.parsing.UnloadMode.SystemOut;

public class WSDLProcessor extends DocumentProcessor {
    private static final Logger log = LoggerFactory.getLogger(WSDLProcessor.class.getName());

    private UnloadMode unloadMode = SystemOut;

    private FilterFunction filter = null;

    private List<WSDLSummary> summaries = new ArrayList<>();

    private WSDLHelper helper = new WSDLHelper();

    public WSDLProcessor(String wd, String od) {
        super(wd, od);
    }
    public WSDLProcessor(String wd, String od, UnloadMode mode) {
        super(wd, od);
        unloadMode = mode;
    }
    public WSDLProcessor(String wd, String od, UnloadMode mode, FilterFunction filterFunction) {
        super(wd, od);
        unloadMode = mode;
        filter = filterFunction;
    }

    public UnloadMode getUnloadMode() {
        return unloadMode;
    }

    public void setUnloadMode(UnloadMode mode) {
        unloadMode = mode;
    }

    private Set<JAXBElement<WSMatchingType>> comparisons = new HashSet<>();

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

    private StandardMessage processIO(Element operationNode, String mode) {
        Element el = (Element) operationNode.getElementsByTagNameNS("*", String.valueOf(mode)).item(0);
        StandardMessage msg = new StandardMessage(el.getAttribute("message"));
        Element msgEl = helper.findElementByName(msg.getName());
        // TODO Store messages separately in helper
        Set<MessageField> fields = extractOperationFields(msgEl);
        log.debug("Fields for " + msg.getName()+ ": " + fields);
        msg.setFields(fields);
        return msg;
    }

    private Set<MessageField> extractOperationFields(Element partContainer) {
        Set<MessageField> fields = new HashSet<>();
        if (partContainer == null) {
            return fields;
        }
        NodeList parts = partContainer.getElementsByTagNameNS("*", "part");

        for (int j = 0; j < parts.getLength(); j++) {
            Element part = (Element) parts.item(j);
            String partName = part.getAttribute("name");
            String elementCheck = part.getAttribute("element");
            if (elementCheck == null || elementCheck.equals("")) {
                String typeCheck = part.getAttribute("type");
                if (typeCheck != null) {
                    TagName typeTag = new TagName(typeCheck);
                    if (WSDLUtil.isPrimitiveType(typeTag.getName())) {
                        log.info("Found primitive type: " + partName);
                        // XXX
                        fields.add(new NamedField(partName));
                    } else {
                        // Look it up and process
                        // TODO Prioritize on complex type?
                        Element el = helper.findElementByName(typeTag.getName());
                        // XXX
                        fields.addAll(helper.flatten(el));

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
                JAXBElement<WSMatchingType> match1 = sc.compare(summaries.get(doc1), summaries.get(doc2));
                JAXBElement<WSMatchingType> match2 = sc.compare(summaries.get(doc1), summaries.get(doc2));

                if (filter != null) {
                    if (!filter.isProhibited(match1)) {
                        comparisons.add(match1);
                    }
                    if (!filter.isProhibited(match2)) {
                        comparisons.add(match2);
                    }
                } else {
                    comparisons.add(match1);
                    comparisons.add(match2);
                }
            }
        }
    }

    @Override
    protected void unload() {
        JAXBContext context;
        Marshaller m;
        try {
            context = JAXBContext.newInstance(WSMatchingType.class);
            m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (JAXBException jaxbe) {
            log.error("JAXB Exception when creating marshaller. Cannot continue persisting comparisons to WSDL format");
            jaxbe.printStackTrace();
            return;
        }

        for (JAXBElement<WSMatchingType> res : comparisons) {
            log.debug("Match for " + res.getValue().getMacthing().get(0).getInputServiceName() +
                    " to " + res.getValue().getMacthing().get(0).getOutputServiceName());
            String fileName = outputDirectory + "/" +
                    res.getValue().getMacthing().get(0).getInputServiceName() +
                    ":" +
                    res.getValue().getMacthing().get(0).getOutputServiceName() +
                    ".wsdl";
            try {
                switch (unloadMode) {
                    case File:
                        File outFile = new File(fileName);
                        outFile.getParentFile().mkdirs();
                        m.marshal(res, outFile);
                        break;
                    case SystemOut:
                        m.marshal(res, System.out);
                        break;
                }
            } catch (JAXBException jaxbe) {
                log.warn("Exception when marshalling " + fileName + ". This comparison will be skipped");
                jaxbe.printStackTrace();
            }
        }
    }

    public FilterFunction getFilter() {
        return filter;
    }

    public void setFilter(FilterFunction filter) {
        this.filter = filter;
    }
}
