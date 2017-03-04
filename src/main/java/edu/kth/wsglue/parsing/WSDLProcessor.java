package edu.kth.wsglue.parsing;

public class WSDLProcessor extends DocumentProcessor {

    public WSDLProcessor(String wd) {
        super(wd);
    }

    @Override
    protected void transform() {
        System.out.println("transform");
    }

    @Override
    protected void compare() {
        System.out.println("compare");
    }

    @Override
    protected void unload() {
        System.out.println("unload");
    }
}
