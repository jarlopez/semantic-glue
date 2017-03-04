package edu.kth.wsglue.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public abstract class DocumentProcessor {
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class.getName());

    protected Set<Document> documents = new HashSet<>();

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private static DocumentBuilder documentBuilder;

    private String workingDirectory;

    private static final String FILETYPE_EXT = ".wsdl";
    private static final long PARSE_TIMEOUT_MS = 20000;
    private static final long DEBUG_MAX_FILES = 10000;

    private static Set<String> skipList = new HashSet<>();
    private static final String SKIP_FILE_DELIM = "\r\n";
    private static final String SKIP_FILES = "" +
            "CheckCompanyStatusResponse" + SKIP_FILE_DELIM +                // Parse error
            "InnovativeMerchantSolutionsAPIProfile" + SKIP_FILE_DELIM +     // Parse error
            "PaymentVisionPayAPIProfile" + SKIP_FILE_DELIM +                // Parse error
            "Looking4ParkingAPIProfile" + SKIP_FILE_DELIM +                 // Parse error
            "DeveloperGardenClickandBuyAPIProfile" + SKIP_FILE_DELIM +      // Parse error
            "compositeFlightStatsAPIProfile" + SKIP_FILE_DELIM +            // Too long timeout
            "GoToBillingAPIProfile" + SKIP_FILE_DELIM +                     // Too long timeout
            "BangoDirectBillingAPIProfile" + SKIP_FILE_DELIM                // Parse error
            ;
    static {
        skipList.addAll(Arrays.asList(SKIP_FILES.split(SKIP_FILE_DELIM)));
    }

    public DocumentProcessor(String wd) {
        workingDirectory = wd;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            log.error("Could not instantiate document builder");
            pce.printStackTrace();
        }
    }

    public void run() {
        load();
        transform();
        compare();
        unload();
        cleanup();
    }

    public void cleanup() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(PARSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("Executor interrupted when awaiting termination");
        }
    }

    /**
     * Loads working directory contents into memory.
     */
    protected void load() {
        log.debug("Loading " + FILETYPE_EXT + " documents in " + workingDirectory);
        File dir = new File(workingDirectory);
        File [] files = dir.listFiles((dir1, name) -> name.endsWith(FILETYPE_EXT));

        assert files != null : "No " + FILETYPE_EXT + " files exist in " + workingDirectory;
        int debugIndex = 0;
        for (File wsdlFile : files) {
            if (skipList.contains(wsdlFile.getName().split(FILETYPE_EXT)[0])) {
                log.debug("Skipping " + wsdlFile.getName() + " due to skiplist entry");
                continue;
            }
            debugIndex += 1;
            if (debugIndex > DEBUG_MAX_FILES) {
                log.debug("Breaking out of loop due to DEBUG_MAX_FILES");
                break;
            }
            try {
                log.debug("Parsing " + wsdlFile.getName());
                Document document = buildDocument(wsdlFile);
                documents.add(document);
            } catch(IOException ioex) {
                log.error("I/O exception on " + wsdlFile.getAbsolutePath());
            } catch (SAXException saxex) {
                log.error("Could not parse " + wsdlFile.getAbsolutePath());
            } catch (InterruptedException iex) {
                log.warn("Interrupted when parsing " + wsdlFile.getName());
            } catch (ExecutionException eex) {
                log.error("Error submitting parse task to executor");
                eex.printStackTrace();
            } catch (TimeoutException toex) {
                log.warn("Parsing " + wsdlFile.getName() + " took longer than " + PARSE_TIMEOUT_MS + "ms and was skipped.");
            }
        }
        log.debug("Completed loading " + FILETYPE_EXT + " files in " + workingDirectory);
    }

    private Document buildDocument(File wsdlFile) throws IOException, SAXException, InterruptedException, ExecutionException, TimeoutException {
        return buildDocument(wsdlFile, PARSE_TIMEOUT_MS);
    }

    private Document buildDocument(File wsdlFile, long timeoutMs) throws IOException, SAXException, InterruptedException, ExecutionException, TimeoutException {
        Document document;
        Future<Document> task = executor.submit(() -> documentBuilder.parse(wsdlFile));
        document = task.get(timeoutMs, TimeUnit.MILLISECONDS);
        return document;
    }

    /**
     * Transforms in-memory documents into workable models.
     */
    protected abstract void transform();

    /**
     * Begins document comparison.
     */
    protected abstract void compare();

    /**
     * Persists the results of the document comparison.
     */
    protected abstract void unload();
}
