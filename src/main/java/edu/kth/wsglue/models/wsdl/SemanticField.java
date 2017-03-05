package edu.kth.wsglue.models.wsdl;

public class SemanticField implements MessageField {
    private String name;
    private String semanticReference;


    public SemanticField(String name, String semanticReference) {
        this.name = name;
        if (semanticReference != null && (semanticReference.startsWith("http") || semanticReference.contains("#"))) {
            String[] referenceParts = semanticReference.split("#");
            this.semanticReference = referenceParts[referenceParts.length - 1];
        } else {
            this.semanticReference = semanticReference;
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
