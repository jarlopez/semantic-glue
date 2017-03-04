package edu.kth.wsglue.parsing.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for managing nodes in a document and caching results.
 */
public class WSDLHelper {
    private static final Logger log = LoggerFactory.getLogger(WSDLHelper.class.getName());

    private Document document;

    private Map<String, Element> elementsCache = new HashMap<>();
    private boolean staleCache = true;

    public WSDLHelper() {}

    /**
     * Clears any built-up caches and invalidates the document.
     */
    public void clear() {
        document = null;
        elementsCache.clear();
        staleCache = false;
    }

    public void updateDocument(Document doc) {
        setDocument(doc);
        buildElementsCache();
    }
    private void setDocument(Document doc) {
        document = doc;
        staleCache = true;
    }

    /**
     * Finds all elements (simple, complex, element, message, part) and stores them in in-memory map.
     */
    private void buildElementsCache() {
        NodeList complexTypes = document.getElementsByTagNameNS("*","complexType");
        NodeList simpleTypes = document.getElementsByTagNameNS("*","simpleType");
        NodeList elements = document.getElementsByTagNameNS("*","element");
        NodeList messages = document.getElementsByTagNameNS("*","message");
        NodeList parts = document.getElementsByTagNameNS("*","part");
        elementsCache.putAll(nodeListToNameMap(complexTypes));
        elementsCache.putAll(nodeListToNameMap(simpleTypes));
        elementsCache.putAll(nodeListToNameMap(elements));
        elementsCache.putAll(nodeListToNameMap(messages));
        elementsCache.putAll(nodeListToNameMap(parts));
        staleCache = false;
    }

    private Map<String, Element> nodeListToNameMap(NodeList elements) {
        Map<String, Element> rv = new HashMap<>();
        for (int i = 0; i < elements.getLength(); i++) {
            Element el = (Element) elements.item(i);
            String name = el.getAttribute("name");
            if (name != null) {
                rv.put(name, el);
            }
        }
        return rv;
    }


    public Element findElementByName(String name) {
        if (staleCache) {
            log.warn("Forcing a rebuild of the elements cache");
            buildElementsCache();
        }
        return elementsCache.get(name);

    }
}
