package edu.kth.wsglue.parsing.filters;

import edu.kth.wsglue.models.generated.WSMatchingType;

import javax.xml.bind.JAXBElement;

public interface FilterFunction {
    /**
     * Determines whether the given comparison is prohibited from further processing.
     * @param comparison document representation in question
     * @return true if the document representation is prohibited
     */
    boolean isProhibited(JAXBElement<WSMatchingType> comparison);
}
