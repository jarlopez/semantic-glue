package edu.kth.wsglue.parsing.generators;

import edu.kth.wsglue.models.wsdl.MessageField;
import org.w3c.dom.Element;

public interface FieldGenerator<T extends MessageField> {
    T generate(String fieldName, Element el);
}
