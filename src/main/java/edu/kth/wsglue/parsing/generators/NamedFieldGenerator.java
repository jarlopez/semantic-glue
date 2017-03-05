package edu.kth.wsglue.parsing.generators;

import edu.kth.wsglue.models.wsdl.NamedField;
import org.w3c.dom.Element;

public class NamedFieldGenerator implements FieldGenerator<NamedField> {
    @Override
    public NamedField generate(String fieldName, Element el) throws InvalidFieldException {
        return new NamedField(fieldName);
    }
}
