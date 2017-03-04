package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.generated.WSMatchingType;
import edu.kth.wsglue.models.wsdl.WSDLRepresentation;

public interface WsComparator<T extends WSDLRepresentation> {
    WSMatchingType compare(T o1, T o2);
}
