package edu.kth.wsglue.models.wsdl;

public class NamedField implements MessageField {
    private String name;

    public NamedField(String n) {
        name = n;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
