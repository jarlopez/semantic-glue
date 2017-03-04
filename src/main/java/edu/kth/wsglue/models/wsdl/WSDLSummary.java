package edu.kth.wsglue.models.wsdl;

import org.w3c.dom.Document;

import java.util.HashSet;
import java.util.Set;

public class WSDLSummary implements WSDLRepresentation {
    private Document documentRef;
    private Set<Operation> operations = new HashSet<>();
    private String serviceName;

    public WSDLSummary(Document ref) {
        documentRef = ref;
    }

    public Document getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(Document documentRef) {
        this.documentRef = documentRef;
    }

    public Set<Operation> getOperations() {
        return operations;
    }

    public void setOperations(Set<Operation> operations) {
        this.operations = operations;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
