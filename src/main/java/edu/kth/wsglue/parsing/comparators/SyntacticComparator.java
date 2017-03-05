package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.generated.*;
import edu.kth.wsglue.models.wsdl.Message;
import edu.kth.wsglue.models.wsdl.Operation;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
import edu.kth.wsglue.thirdparty.EditDistance;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SyntacticComparator implements WsComparator<WSDLSummary> {
    private static final Logger log = LoggerFactory.getLogger(SyntacticComparator.class.getName());

    private ObjectFactory factory = new ObjectFactory();

    private static final Double ED_THRESHOLD = 0.8;

    @Override
    public JAXBElement compare(WSDLSummary outputService, WSDLSummary inputService) {
        WSMatchingType results = factory.createWSMatchingType();
        MatchedWebServiceType serviceMatch = factory.createMatchedWebServiceType();
        serviceMatch.setOutputServiceName(outputService.getServiceName());
        serviceMatch.setInputServiceName(inputService.getServiceName());

        Double serviceScore = 0.0;
        for (Operation outputOperation : outputService.getOperations()) {
            for (Operation inputOperation: inputService.getOperations()) {
                Double operationScore = 0.0;
                log.debug("Comparing " + outputOperation.getName() + " to " + inputOperation.getName());

                Message output = outputOperation.getOutput();
                Message input = inputOperation.getInput();

                Set<String> inputNames = new HashSet<>(input.getFieldNames());
                Set<String> outputNames = new HashSet<>(output.getFieldNames());

                if (inputNames.size() == 0 || outputNames.size() == 0 || inputNames.size() > outputNames.size()) {
                    log.debug("Skipping due incompatible I/O");
                    continue;
                }

                Map<String, Pair<String, Double>> bestMappings = new HashMap<>();

                for (String inputName : inputNames) {
                    // Find best-matching outputs for given inputs
                    for (String outputName : outputNames) {
                        Double distance = EditDistance.getSimilarity(inputName, outputName);
                        if (distance >= ED_THRESHOLD && distance > bestMappings.getOrDefault(inputName, new Pair<>("", Double.NEGATIVE_INFINITY)).getValue()) {
                            bestMappings.put(inputName, new Pair<>(outputName, distance));
                            log.debug("Better distance between " + inputName + ":" + outputName + "=" + distance);
                        }
                    }
                }
                if (bestMappings.size() == inputNames.size()) {
                    MatchedOperationType operationMatch = factory.createMatchedOperationType();
                    for (Map.Entry<String, Pair<String, Double>> match : bestMappings.entrySet()) {
                        operationScore += match.getValue().getValue();

                        MatchedElementType matchedEl = factory.createMatchedElementType();

                        matchedEl.setInputElement(match.getKey());
                        matchedEl.setOutputElement(match.getValue().getKey());

                        matchedEl.setScore(match.getValue().getValue());

                        operationMatch.getMacthedElement().add(matchedEl);
                    }

                    operationScore = operationScore / bestMappings.size();
                    serviceScore += operationScore;

                    operationMatch.setOutputOperationName(outputOperation.getName());
                    operationMatch.setInputOperationName(inputOperation.getName());
                    operationMatch.setOpScore(operationScore);
                    serviceMatch.getMacthedOperation().add(operationMatch);
                }
            }
        }
        if (serviceMatch.getMacthedOperation().size() > 0) {
            serviceScore = serviceScore / serviceMatch.getMacthedOperation().size();
        }
        serviceMatch.setWsScore(serviceScore);
        results.getMacthing().add(serviceMatch);
        return factory.createWSMatching(results);
    }
}
