package edu.kth.wsglue.parsing.generators;

import edu.kth.wsglue.models.wsdl.SemanticField;
import org.w3c.dom.Element;

public class SemanticFieldGenerator implements FieldGenerator<SemanticField> {
    @Override
    public SemanticField generate(String name, Element el) throws InvalidFieldException {
        String semanticReference = el.getAttribute("sawsdl:modelReference");
        return new SemanticField(name, semanticReference);
    }
}
