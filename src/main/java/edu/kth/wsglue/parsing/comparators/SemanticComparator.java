package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.generated.ObjectFactory;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
import edu.kth.wsglue.thirdparty.ontology.WSGlueOntologyManager;
import org.mindswap.pellet.owlapi.Reasoner;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.Map;

public class SemanticComparator implements WsComparator<WSDLSummary> {
    private static final Logger log = LoggerFactory.getLogger(SemanticComparator.class.getName());
    private static final Double THRESHOLD = 0.5;

    private WSGlueOntologyManager manager;
    private OWLOntologyManager ontologyManager;
    private OWLOntology ontology;
    private Reasoner reasoner;

    private ObjectFactory factory = new ObjectFactory();


    private Map<String, OWLClass> ontologyMap;

    private static final String ONTOLOGY_LOC = "/resources/SUMO.owl";

    public SemanticComparator() {
        super();
        manager = new WSGlueOntologyManager();
        ontologyManager =  manager.initializeOntologyManager();
        ontology = manager.initializeOntology(ontologyManager, ONTOLOGY_LOC);
        reasoner = manager.initializeReasoner(ontology, ontologyManager);
        ontologyMap = manager.loadClasses(reasoner);
    }
//
//    @Override
//    public JAXBElement compare(WSDLSummary outputService, WSDLSummary inputService) {
//        WSMatchingType results = factory.createWSMatchingType();
//        MatchedWebServiceType serviceMatch = factory.createMatchedWebServiceType();
//        serviceMatch.setOutputServiceName(outputService.getServiceName());
//        serviceMatch.setInputServiceName(inputService.getServiceName());
//
//        Double serviceScore = 0.0;
//        for (Operation outputOperation : outputService.getOperations()) {
//            for (Operation inputOperation: inputService.getOperations()) {
//                Double operationScore = 0.0;
//                log.debug("Comparing " + outputOperation.getName() + " to " + inputOperation.getName());
//
//                StandardMessage output = outputOperation.getOutput();
//                StandardMessage input = inputOperation.getInput();
//
//                Set<String> inputNames = new HashSet<>(input.getFields());
//                Set<String> outputNames = new HashSet<>(output.getFields());
//
//                if (inputNames.size() == 0 || outputNames.size() == 0 || inputNames.size() > outputNames.size()) {
//                    log.debug("Skipping due incompatible I/O");
//                    continue;
//                }
//
//                Map<String, Pair<String, Double>> bestMappings = new HashMap<>();
//
//                for (String inputName : inputNames) {
//                    // Find best-matching outputs for given inputs
//                    for (String outputName : outputNames) {
//
//                        Double distance = compare(inputName, outputName);
////                        Double distance = EditDistance.getSimilarity(inputName, outputName);
//                        if (distance >= THRESHOLD && distance > bestMappings.getOrDefault(inputName, new Pair<>("", Double.NEGATIVE_INFINITY)).getValue()) {
//                            bestMappings.put(inputName, new Pair<>(outputName, distance));
//                            log.debug("Better distance between " + inputName + ":" + outputName + "=" + distance);
//                        }
//                    }
//                }
//                if (bestMappings.size() == inputNames.size()) {
//                    MatchedOperationType operationMatch = factory.createMatchedOperationType();
//                    for (Map.Entry<String, Pair<String, Double>> match : bestMappings.entrySet()) {
//                        operationScore += match.getValue().getValue();
//
//                        MatchedElementType matchedEl = factory.createMatchedElementType();
//
//                        matchedEl.setInputElement(match.getKey());
//                        matchedEl.setOutputElement(match.getValue().getKey());
//
//                        matchedEl.setScore(match.getValue().getValue());
//
//                        operationMatch.getMacthedElement().add(matchedEl);
//                    }
//
//                    operationScore = operationScore / bestMappings.size();
//                    serviceScore += operationScore;
//
//                    operationMatch.setOutputOperationName(outputOperation.getName());
//                    operationMatch.setInputOperationName(inputOperation.getName());
//                    operationMatch.setOpScore(operationScore);
//                    serviceMatch.getMacthedOperation().add(operationMatch);
//                }
//            }
//        }
//        if (serviceMatch.getMacthedOperation().size() > 0) {
//            serviceScore = serviceScore / serviceMatch.getMacthedOperation().size();
//        }
//        serviceMatch.setWsScore(serviceScore);
//        results.getMacthing().add(serviceMatch);
//        return factory.createWSMatching(results);
//    }

    private Double compare(String s1, String s2) {
        SemanticMatchingDegree res = SemanticMatchingDegree.NotMatched;

        if (s1 == null || s2 == null) {
            return res.getScore();
        }

        OWLClass c1 = ontologyMap.get(s1);
        OWLClass c2 = ontologyMap.get(s2);

        if (c1 == null || c2 == null) {
            return res.getScore();
        }

        Integer comparison = c1.compareTo(c2);
        if (comparison == 0) {
            res = SemanticMatchingDegree.Exact;
        } else if (reasoner.isSubClassOf(c2, c1)) {
            res = SemanticMatchingDegree.Subsumption;
        } else if (reasoner.isSubClassOf(c1, c2)) {
            res = SemanticMatchingDegree.PlugIn;
        } else if (manager.findRelationship(c1, c2, reasoner).size() != 0) {
            res = SemanticMatchingDegree.Structural;
        }
        return res.getScore();
    }

    @Override
    public JAXBElement compare(WSDLSummary o1, WSDLSummary o2) {
        return null;
    }
}
