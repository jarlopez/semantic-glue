package edu.kth.wsglue.parsing.processors;

import edu.kth.wsglue.models.generated.WSMatchingType;
import edu.kth.wsglue.models.wsdl.MessageField;
import edu.kth.wsglue.models.wsdl.Operation;
import edu.kth.wsglue.models.wsdl.StandardMessage;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
import edu.kth.wsglue.parsing.comparators.SyntacticComparator;
import edu.kth.wsglue.parsing.comparators.WsComparator;
import edu.kth.wsglue.parsing.filters.FilterFunction;
import edu.kth.wsglue.parsing.generators.FieldGenerator;
import edu.kth.wsglue.parsing.generators.NamedFieldGenerator;
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

/**
 * Processor for WSDL and SAWSDL-enabled WSDL documents.
 * Responsible for:
 *      - Specifying how to transform the documents into a custom in-memory model
 *      - Providing a mechanism for comparing the resulting models
 *      - Unloading the model comparison results (persisting to disk, to an IO-stream, etc.)
 */
public class WSDLProcessor extends DocumentProcessor {
    private static final Logger log = LoggerFactory.getLogger(WSDLProcessor.class.getName());

    private UnloadMode unloadMode = UnloadMode.SystemOut;

    private FilterFunction filter = null;
    private FieldGenerator fieldGenerator = null;
    private WsComparator documentComparator = null;

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
        fieldGenerator = new NamedFieldGenerator();
        documentComparator = new SyntacticComparator();
    }

    public WSDLProcessor withUnloadMode(UnloadMode mode) {
        unloadMode = mode;
        return this;
    }
    public WSDLProcessor withFilterFunction(FilterFunction filterFunction) {
        filter = filterFunction;
        return this;
    }
    public WSDLProcessor withFieldGenerator(FieldGenerator generator) {
        fieldGenerator = generator;
        return this;
    }
    public WSDLProcessor withDocumentComparator(WsComparator comparator) {
        documentComparator = comparator;
        return this;
    }

    public UnloadMode getUnloadMode() {
        return unloadMode;
    }

    public void setUnloadMode(UnloadMode mode) {
        unloadMode = mode;
    }

    public FilterFunction getFilter() {
        return filter;
    }

    public void setFilter(FilterFunction filter) {
        this.filter = filter;
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
        Set<MessageField> fields = helper.extractOperationFields(fieldGenerator, msgEl);
        log.debug("Fields for " + msg.getName()+ ": " + fields);
        msg.setFields(fields);
        return msg;
    }

    @Override
    protected void compare() {
        for (int doc1 = 0; doc1 < summaries.size() - 1; doc1++) {
            for (int doc2 = doc1 + 1; doc2 < summaries.size(); doc2++) {
                JAXBElement<WSMatchingType> match1 = documentComparator.compare(summaries.get(doc1), summaries.get(doc2));
                JAXBElement<WSMatchingType> match2 = documentComparator.compare(summaries.get(doc1), summaries.get(doc2));

                // TODO Clean up this terrible if-then logic
                if (filter != null) {
                    if (match1 != null && !filter.isProhibited(match1)) {
                        comparisons.add(match1);
                    }
                    if (match2 != null && !filter.isProhibited(match2)) {
                        comparisons.add(match2);
                    }
                } else {
                    if (match1 != null) {
                        comparisons.add(match1);
                    }
                    if (match2 != null) {
                        comparisons.add(match2);
                    }
                }
            }
        }
        log.info("Found " + comparisons.size() + " valid service matches");
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
                    case None:
                        // Do nothing
                        break;
                }
            } catch (JAXBException jaxbe) {
                log.warn("Exception when marshalling " + fileName + ". This comparison will be skipped");
                jaxbe.printStackTrace();
            }
        }
    }

}
