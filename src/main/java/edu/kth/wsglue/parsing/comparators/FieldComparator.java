package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.wsdl.MessageField;

public interface FieldComparator {
    Double compare(MessageField mf1, MessageField mf2);
}
