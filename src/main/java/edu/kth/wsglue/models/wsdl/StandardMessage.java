package edu.kth.wsglue.models.wsdl;

import edu.kth.wsglue.parsing.util.TagName;

import java.util.Set;

public class StandardMessage implements Message {
    private TagName tagName;

    private Set<Part> parts;

    private Set<MessageField> fields;

    public StandardMessage(String type) {
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

    public Set<MessageField> getFields() {
        return fields;
    }

    public void setFields(Set<MessageField> fields) {
        this.fields = fields;
    }
}
