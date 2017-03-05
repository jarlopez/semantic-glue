package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.generated.ObjectFactory;
import edu.kth.wsglue.models.wsdl.MessageField;
import edu.kth.wsglue.models.wsdl.SemanticField;
import edu.kth.wsglue.models.wsdl.WSDLSummary;
import edu.kth.wsglue.thirdparty.ontology.WSGlueOntologyManager;
import org.mindswap.pellet.owlapi.Reasoner;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.net.URL;
import java.util.Map;

public class SemanticComparator extends WsComparator<SemanticField> {
    private static final Logger log = LoggerFactory.getLogger(SemanticComparator.class.getName());
    private static final Double THRESHOLD = 0.5;

    private WSGlueOntologyManager manager;
    private OWLOntologyManager ontologyManager;
    private OWLOntology ontology;
    private Reasoner reasoner;

    private ObjectFactory factory = new ObjectFactory();


    private Map<String, OWLClass> ontologyMap;

    private static final String ONTOLOGY_FILE_NAME = "/SUMO.owl";

    public SemanticComparator() {
        super();
        URL ontologyUrl = this.getClass().getResource(ONTOLOGY_FILE_NAME);
        manager = new WSGlueOntologyManager();
        ontologyManager =  manager.initializeOntologyManager();
        ontology = manager.initializeOntology(ontologyManager, "file://" + ontologyUrl.getFile());
        reasoner = manager.initializeReasoner(ontology, ontologyManager);
        ontologyMap = manager.loadClasses(reasoner);
    }

    @Override
    Double compare(MessageField mf1, MessageField mf2) {
        SemanticMatchingDegree res = SemanticMatchingDegree.NotMatched;

        if (mf1 == null || mf2 == null) {
            return res.getScore();
        }
        if (!(mf1 instanceof SemanticField) || !(mf2 instanceof SemanticField)) {
            return res.getScore();
        }

        String s1 = ((SemanticField) mf1).getSemanticReference();
        String s2 = ((SemanticField) mf2).getSemanticReference();

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
