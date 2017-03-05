package edu.kth.wsglue.parsing.generators;

import edu.kth.wsglue.models.wsdl.MessageField;
import org.w3c.dom.Element;

/**
 * Base interface for field generators used during the translation of
 * a WSDL document into its in-memory representation
 * @param <T> the type of message field to be created by this generator
 */
public interface FieldGenerator<T extends MessageField> {
    T generate(String fieldName, Element el) throws InvalidFieldException;

    class InvalidFieldException extends Exception {
        public InvalidFieldException(String msg) {
            super(msg);
        }
        public InvalidFieldException() {
            super();
        }
    }
}
