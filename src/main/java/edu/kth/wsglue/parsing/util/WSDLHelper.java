package edu.kth.wsglue.parsing.util;

import edu.kth.wsglue.models.wsdl.MessageField;
import edu.kth.wsglue.parsing.generators.FieldGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.*;

/**
 * Helper class for managing nodes in a document and caching important nodes
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
     * Finds all elements (simple, complex, element, message, part) and stores them in in-memory map
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

    /**
     * Generates a map of key-value pairs of element name (and type) to the actual element.
     * Note that each entry is created twice: once with the name, and again with its type prepended to it
     * @param elements the elements to be used in the map
     * @return the map of name-to-node pairs
     */
    private Map<String, Element> nodeListToNameMap(NodeList elements) {
        Map<String, Element> rv = new HashMap<>();
        for (int i = 0; i < elements.getLength(); i++) {
            Element el = (Element) elements.item(i);
            String tag = new TagName(el.getTagName()).getName();
            String name = el.getAttribute("name");
            if (name != null) {
                rv.put(WSDLUtil.generateCacheKey(tag, name), el);
                rv.put(name, el);
            }
        }
        return rv;
    }


    /**
     * Retrieves the element from the cache.
     * If the cache is stale for this document, it rebuilds it before retrieving.
     * @param name the name to look up
     * @return the element (or null if it doesn't exist)
     */
    public Element findElementByName(String name) {
        if (staleCache) {
            log.warn("Forcing a rebuild of the elements cache");
            buildElementsCache();
        }
        return elementsCache.get(name);
    }

    /**
     * Retrieves the element from the cache, prioritizing on tag type.
     * If the cache is stale for this document, it rebuilds it before retrieving.
     * @param expectedTag the tag type one expects for this element
     * @param name the name to look up
     * @return the element (or null if it doesn't exist)
     */
    public Element findElementByTagAndName(String expectedTag, String name) {
        if (staleCache) {
            log.warn("Forcing a rebuild of the elements cache");
            buildElementsCache();
        }
        return elementsCache.get(WSDLUtil.generateCacheKey(expectedTag, name));
    }

    private Set<MessageField> extractSemanticOperationFields(FieldGenerator fg, Element partContainer) throws XPathExpressionException {
        Set<MessageField> fields = new HashSet<>();
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = expr = xpath.compile("//*[@*[local-name()='modelReference']]");
        NodeList semanticTypes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

        Map<String, Element> semanticTypesCache = new HashMap<>();
        semanticTypesCache.putAll(nodeListToNameMap(semanticTypes));


        return fields;
    }

    private Set<MessageField> extractBaseOperationFields(FieldGenerator fg, Element partContainer) {
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
                        try {
                            MessageField field = fg.generate(partName, part);
                            fields.add(field);
                        } catch (FieldGenerator.InvalidFieldException ifex) {
                            log.warn("InvalidFieldException occurred when creating field " + partName + ", so it will be ignored");
                        }
                    } else {
                        // Look it up and process
                        // TODO Prioritize on complex type?
                        Element el = findElementByName(typeTag.getName());
                        fields.addAll(flatten(new HashSet<>(), fg, el));
                    }
                }
            } else {
                // Find element and process as needed
                TagName elementTag = new TagName(elementCheck);
                Element el = findElementByName(elementTag.getName());
                if (el != null) {
                    // Flatten into basic types
                    fields.addAll(flatten(new HashSet<>(), fg, el));
                    log.debug(elementTag.getName() + ": " + fields.toString());
                }
            }
        }
        return fields;
    }


    public Set<MessageField> extractOperationFields(FieldGenerator fg, Element partContainer) {
        return extractBaseOperationFields(fg, partContainer);
    }

    /**
     * Recursively looks up and flattens an element into its most basic fields and primitive types.
     * @param seenElements set of already-seen elements to allow breaking out of infinite recursions
     * @param fg the field generator responsible for creating new fields when base types are found
     * @param el the element to flatten
     * @return
     */
    public Set<MessageField> flatten(Set<Element> seenElements, FieldGenerator fg, Element el) {
        Set<MessageField> rv = new HashSet<>();
        if (el == null) {
            return rv;
        }
        if (seenElements.contains(el)) {
            log.warn("Flattening already-seen element (" + el.getAttribute("name") + "). Breaking the recursion");
            return rv;
        }
        seenElements.add(el);

        log.debug("Flattening " + el.getAttribute("name"));
        if (el.hasAttribute("sawsdl:modelReference")) {
            try {
                rv.add(fg.generate(el.getAttribute("name"), el));
                log.debug("Successfully added semantic type " + el.getAttribute("name") + ": " + el.getTagName());
            } catch (FieldGenerator.InvalidFieldException e) {
                log.warn("InvalidFieldException occurred when creating field " + el.getAttribute("name") + ", so it will be ignored");
            }
        } else {
            String typeCheck = el.getAttribute("type");
            if (typeCheck != null && !Objects.equals(typeCheck, "")) {
                TagName typeTag = new TagName(typeCheck);
                if (WSDLUtil.isPrimitiveType(typeTag.getName())) {
                    try {
                        MessageField field = fg.generate(el.getAttribute("name"), el);
                        rv.add(field);
                    } catch (FieldGenerator.InvalidFieldException e) {
                        log.warn("InvalidFieldException occurred when creating field " + el.getAttribute("name") + ", so it will be ignored");
                    }
                } else {
                    log.debug("Looking up type: " + typeTag.getName());
                    Element check = findElementByTagAndName("complexType", typeTag.getName());
                    if (check == null) {
                        check = findElementByTagAndName("simpleType", typeTag.getName());
                    }
                    if (check == null) {
                        check = findElementByTagAndName("element", typeTag.getName());
                    }
                    if (check == null) {
                        // Now we're in trouble!
                        check = findElementByName(typeTag.getName());
                    }
                    rv.addAll(flatten(seenElements, fg, check));
                }
            } else {
                // Handle complex case
                NodeList children = el.getElementsByTagNameNS("*", "element");
                if (children.getLength() == 0) {
                    // Might be restricted simple-type (used in SAWSDL)
                    NodeList restrictions = el.getElementsByTagNameNS("*", "restriction");
                    if (restrictions.getLength() == 1) {
                        // Extract current element as primitive type
                        log.debug("Found element with restrictions: " + el.getAttribute("name") + ". Treating as primitive type");
                        try {
                            rv.add(fg.generate(el.getAttribute("name"), el));
                        } catch (FieldGenerator.InvalidFieldException e) {
                            log.warn("InvalidFieldException occurred when creating field " + el.getAttribute("name") + ", so it will be ignored");
                        }
                    }

                } else {
                    for (int i = 0; i < children.getLength(); i++) {
                        rv.addAll(flatten(seenElements, fg, (Element) children.item(i)));
                    }
                }
            }
        }
        return rv;
    }

}
