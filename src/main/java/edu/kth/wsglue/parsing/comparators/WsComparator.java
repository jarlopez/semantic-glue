package edu.kth.wsglue.parsing.comparators;

import edu.kth.wsglue.models.wsdl.WSDLRepresentation;

import javax.xml.bind.JAXBElement;

public interface WsComparator<T extends WSDLRepresentation> {
    JAXBElement compare(T o1, T o2);
}
