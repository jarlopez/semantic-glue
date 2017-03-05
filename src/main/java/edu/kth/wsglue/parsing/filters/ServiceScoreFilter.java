package edu.kth.wsglue.parsing.filters;

import edu.kth.wsglue.models.generated.WSMatchingType;

import javax.xml.bind.JAXBElement;

public class ServiceScoreFilter implements FilterFunction {
    /**
     * All service comparison documents with a service score
     * equal to or lower than this will be prohibited
     */
    private Double threshold = 0.0;

    public ServiceScoreFilter(Double thresh) {
        super();
        threshold = thresh;
    }

    @Override
    public boolean isProhibited(JAXBElement<WSMatchingType> comparison) {
        // Jeezy creezy, I'm sorry for below
        return comparison != null &&
                comparison.getValue() != null &&
                comparison.getValue().getMacthing() != null &&
                comparison.getValue().getMacthing().size() > 0 &&
                comparison.getValue().getMacthing().get(0) != null &&
                comparison.getValue().getMacthing().get(0).getWsScore() <= threshold;
    }
}
