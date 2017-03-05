package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.wsdl.NamedField;
import org.w3c.dom.Element;

public class NamedFieldGenerator implements FieldGenerator<NamedField> {
    @Override
    public NamedField generate(String fieldName, Element el) {
        return new NamedField(fieldName);
    }
}
