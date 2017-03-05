package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.generated.*;
import edu.kth.wsglue.models.wsdl.*;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class WsComparator<T extends MessageField> {
    private static final Logger log = LoggerFactory.getLogger(WsComparator.class.getName());

    private ObjectFactory factory = new ObjectFactory();
    private Double threshold = 0.8;

    public void setThreshold(Double thresh) {
        threshold = thresh;
    }

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

                StandardMessage output = outputOperation.getOutput();
                StandardMessage input = inputOperation.getInput();

                Set<MessageField> inputNames = new HashSet<>(input.getFields());
                Set<MessageField> outputNames = new HashSet<>(output.getFields());

                if (inputNames.size() == 0 || outputNames.size() == 0 || inputNames.size() > outputNames.size()) {
                    log.debug("Skipping due incompatible I/O");
                    continue;
                }

                Map<String, Pair<String, Double>> bestMappings = new HashMap<>();

                for (MessageField inputField : inputNames) {
                    // Find best-matching outputs for given inputs
                    for (MessageField outputField : outputNames) {
                        Double distance = compare(inputField, outputField);
                        if (distance >= threshold && distance > bestMappings.getOrDefault(inputField, new Pair<>("", Double.NEGATIVE_INFINITY)).getValue()) {
                            bestMappings.put(inputField.getName(), new Pair<>(outputField.getName(), distance));
                            log.debug("Better distance between " + inputField + ":" + outputField + "=" + distance);
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

    abstract Double compare(MessageField mf1, MessageField mf2);

}
