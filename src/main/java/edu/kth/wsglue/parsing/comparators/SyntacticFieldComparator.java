package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.wsdl.MessageField;
import edu.kth.wsglue.thirdparty.EditDistance;

public class SyntacticFieldComparator implements FieldComparator {
    @Override
    public Double compare(MessageField mf1, MessageField mf2) {
        return EditDistance.getSimilarity(mf1.getName(), mf2.getName());
    }
}
