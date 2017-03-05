package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.wsdl.MessageField;
import edu.kth.wsglue.models.wsdl.SemanticField;
import edu.kth.wsglue.thirdparty.ontology.WSGlueOntologyManager;
import org.mindswap.pellet.owlapi.Reasoner;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;

/**
 * Comparator for semantically-related WSDL input/output fields
 */
public class SemanticComparator extends WsComparator<SemanticField> {
    private static final Logger log = LoggerFactory.getLogger(SemanticComparator.class.getName());

    private WSGlueOntologyManager manager;
    private OWLOntologyManager ontologyManager;
    private OWLOntology ontology;
    private Reasoner reasoner;
    private Map<String, OWLClass> ontologyMap;

    private static final Double SEMANTIC_THRESHOLD = 0.5;
    private static final String ONTOLOGY_FILE_NAME = "/SUMO.owl";


    public SemanticComparator() {
        super();
        URL ontologyUrl = this.getClass().getResource(ONTOLOGY_FILE_NAME);
        manager = new WSGlueOntologyManager();
        ontologyManager =  manager.initializeOntologyManager();
        ontology = manager.initializeOntology(ontologyManager, "file://" + ontologyUrl.getFile());
        reasoner = manager.initializeReasoner(ontology, ontologyManager);
        ontologyMap = manager.loadClasses(reasoner);
        setThreshold(SEMANTIC_THRESHOLD);
    }

    @Override
    Double compare(MessageField mf1, MessageField mf2) {
        SemanticMatchingDegree res = SemanticMatchingDegree.NotMatched;

        if (mf1 == null || mf2 == null) {
            log.warn("Input fields are NULL, returning NotMatched");
            return res.getScore();
        }
        if (!(mf1 instanceof SemanticField) || !(mf2 instanceof SemanticField)) {
            log.warn("Input fields are not semantic fields, returning NotMatched");
            return res.getScore();
        }

        String s1 = ((SemanticField) mf1).getSemanticReference().toLowerCase();
        String s2 = ((SemanticField) mf2).getSemanticReference().toLowerCase();

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

}
