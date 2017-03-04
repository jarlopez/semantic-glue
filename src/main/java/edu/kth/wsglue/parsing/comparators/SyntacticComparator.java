package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.generated.MatchedWebServiceType;
import edu.kth.wsglue.models.generated.ObjectFactory;
import edu.kth.wsglue.models.generated.WSMatchingType;
import edu.kth.wsglue.models.wsdl.Operation;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyntacticComparator implements WsComparator<WSDLSummary> {
    private static final Logger log = LoggerFactory.getLogger(SyntacticComparator.class.getName());

    private ObjectFactory factory = new ObjectFactory();

    @Override
    public WSMatchingType compare(WSDLSummary o1, WSDLSummary o2) {
        WSMatchingType results = factory.createWSMatchingType();
        MatchedWebServiceType serviceMatch = factory.createMatchedWebServiceType();
        serviceMatch.setOutputServiceName(o1.getServiceName());
        serviceMatch.setInputServiceName(o2.getServiceName());

        for (Operation op1 : o1.getOperations()) {
            for (Operation op2: o2.getOperations()) {
                log.debug("Comparing " + op1.getName() + " to " + op2.getName());
            }
        }

        results.getMacthing().add(serviceMatch);
        return results;
    }
}
