package edu.kth.wsglue.parsing.comparators;


import edu.kth.wsglue.models.wsdl.MessageField;
import edu.kth.wsglue.thirdparty.EditDistance;

/**
 * Simple comparator which calculates the edit distance between two WSDL message fields
 */
public class SyntacticComparator extends WsComparator<MessageField> {

    @Override
    protected Double compare(MessageField mf1, MessageField mf2) {
        return EditDistance.getSimilarity(mf1.getName(), mf2.getName());
    }

}
