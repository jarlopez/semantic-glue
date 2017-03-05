package edu.kth.wsglue.models.wsdl;

import edu.kth.wsglue.parsing.generators.FieldGenerator;
import edu.kth.wsglue.parsing.util.WSDLUtil;

public class SemanticField implements MessageField {
    private String name;
    private String semanticReference;


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

    public void setSemanticReference(String semanticReference) {
        this.semanticReference = semanticReference;
    }
}
