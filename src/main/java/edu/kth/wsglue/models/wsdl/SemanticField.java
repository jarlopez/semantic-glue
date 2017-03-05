package edu.kth.wsglue.models.wsdl;

public class SemanticField implements MessageField {
    private String name;
    private String semanticReference;


    public SemanticField(String name, String semanticReference) {
        this.name = name;
        this.semanticReference = semanticReference;
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
