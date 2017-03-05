package edu.kth.wsglue.models.wsdl;

import edu.kth.wsglue.parsing.generators.FieldGenerator;
import edu.kth.wsglue.parsing.util.WSDLUtil;

/**
 * An input or output field of a SAWSDL-enabled WSDL message.
 * The constructor is very strict in what it considers a valid SemanticField.
 */
public class SemanticField implements MessageField {
    private String name;
    private String semanticReference;


    /**
     * Constructs a new SemanticField if the parameters are deemed valid.
     * The semanticReference is parsed according to the expected format: [some URL]#Semantic-Class
     * @param name the field's read-only name
     * @param semanticReference the field's semantic class, as defined in an OWL ontology
     * @throws FieldGenerator.InvalidFieldException if the inputs are empty or the semantic class is invalid or empty
     */
    public SemanticField(String name, String semanticReference) throws FieldGenerator.InvalidFieldException {
        super();
        if (WSDLUtil.isEmptyString(name) || WSDLUtil.isEmptyString(semanticReference)) {
            throw new FieldGenerator.InvalidFieldException("SemanticField cannot be created with null or empty parameters");
        }
        this.name = name;
        if (semanticReference.startsWith("http") || semanticReference.contains("#")) {
            String[] referenceParts = semanticReference.split("#");
            this.semanticReference = referenceParts[referenceParts.length - 1];
        } else {
            this.semanticReference = semanticReference;
        }
        if (WSDLUtil.isEmptyString(this.semanticReference)) {
            throw new FieldGenerator.InvalidFieldException("SemanticField cannot be created with null or empty semantic reference");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSemanticReference() {
        return semanticReference;
    }

}
