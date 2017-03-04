package edu.kth.wsglue.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Set;

public abstract class DocumentProcessor {
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class.getName());

    private static DocumentBuilder documentBuilder;
    private Set<Document> documents;

    private String workingDirectory;

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

    /**
     * Loads working directory contents into memory.
     */
    protected abstract void load();

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
