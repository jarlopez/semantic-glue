package edu.kth.wsglue.models.wsdl;

public class Operation {
    private String name;
    private StandardMessage input;
    private StandardMessage output;

    public Operation() {}

    public Operation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StandardMessage getInput() {
        return input;
    }

    public void setInput(StandardMessage input) {
        this.input = input;
    }

    public StandardMessage getOutput() {
        return output;
    }

    public void setOutput(StandardMessage output) {
        this.output = output;
    }
}
