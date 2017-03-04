package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.generated.MatchedWebServiceType;
import edu.kth.wsglue.models.generated.ObjectFactory;
import edu.kth.wsglue.models.generated.WSMatchingType;
import edu.kth.wsglue.models.wsdl.Message;
import edu.kth.wsglue.models.wsdl.Operation;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
import edu.kth.wsglue.thirdparty.EditDistance;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SyntacticComparator implements WsComparator<WSDLSummary> {
    private static final Logger log = LoggerFactory.getLogger(SyntacticComparator.class.getName());

    private ObjectFactory factory = new ObjectFactory();

    @Override
    public WSMatchingType compare(WSDLSummary o1, WSDLSummary o2) {
        WSMatchingType results = factory.createWSMatchingType();
        MatchedWebServiceType serviceMatch = factory.createMatchedWebServiceType();
        serviceMatch.setOutputServiceName(o1.getServiceName());
        serviceMatch.setInputServiceName(o2.getServiceName());

        Double serviceScore = 0.0;
        for (Operation op1 : o1.getOperations()) {
            for (Operation op2: o2.getOperations()) {
                Double operationScore = 0.0;
                log.debug("Comparing " + op1.getName() + " to " + op2.getName());

                Message output = op1.getOutput();
                Message input = op2.getInput();

                Set<String> inputNames = new HashSet<>(input.getFieldNames());
                Set<String> outputNames = new HashSet<>(output.getFieldNames());

                Map<String, Pair<String, Integer>> bestMappings = new HashMap<>();

                for (String inputName : inputNames) {
                    // Find best-matching
                    for (String outputName : outputNames) {
                        Double distance = EditDistance.getSimilarity(inputName, outputName);
                        log.debug("Distance between " + inputName + ":" + outputName + "=" + distance);
                    }
                }
            }
        }

        results.getMacthing().add(serviceMatch);
        return results;
    }
}
