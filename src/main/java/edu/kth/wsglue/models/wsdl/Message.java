package edu.kth.wsglue.models.wsdl;

import edu.kth.wsglue.parsing.util.TagName;

import java.util.Set;

public class Message {
    private TagName tagName;

    private Set<Part> parts;

    private Set<String> fieldNames;

    public Message(String type) {
        tagName = new TagName(type);
    }

    public String getName() {
        return tagName.getName();
    }


    public String getFullName() {
        return tagName.getFullName();
    }


    public Set<Part> getParts() {
        return parts;
    }

    public void setParts(Set<Part> parts) {
        this.parts = parts;
    }

    public Set<String> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(Set<String> fieldNames) {
        this.fieldNames = fieldNames;
    }
}
