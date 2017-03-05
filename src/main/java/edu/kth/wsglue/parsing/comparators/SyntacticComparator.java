package edu.kth.wsglue.parsing.comparators;


import edu.kth.wsglue.models.wsdl.MessageField;
import edu.kth.wsglue.thirdparty.EditDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyntacticComparator extends WsComparator<MessageField> {
    private static final Logger log = LoggerFactory.getLogger(SyntacticComparator.class.getName());

    @Override
    protected Double compare(MessageField mf1, MessageField mf2) {
        return EditDistance.getSimilarity(mf1.getName(), mf2.getName());
    }

}
