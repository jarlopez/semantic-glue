package edu.kth.wsglue.models.wsdl;

import edu.kth.wsglue.parsing.generators.FieldGenerator;
import edu.kth.wsglue.parsing.util.WSDLUtil;

public class NamedField implements MessageField {
    private String name;

    public NamedField(String n) throws FieldGenerator.InvalidFieldException {
        if (WSDLUtil.isEmptyString(n)) {
            throw new FieldGenerator.InvalidFieldException("NamedField cannot be created with null or empty name");
        }
        name = n;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "NamedField{" +
                "name='" + name + '\'' +
                '}';
    }
}
