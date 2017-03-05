package edu.kth.wsglue;

import edu.kth.wsglue.parsing.DocumentProcessor;
import edu.kth.wsglue.parsing.WSDLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class WebServiceMatcher {
    private static final Logger log = LoggerFactory.getLogger(WebServiceMatcher.class.getName());

    private static final String OUTPUT_PATH = "/wsdl-out";
    private static final String WSDL_PATH = "/WSDLs";
    private static final String SAWSDL_PATH = "/SAWSDL";

    public static void main(String[] args) {
        String workingDirectory = System.getProperty("user.dir");
        if (args.length > 0) {
            File dir = new File(args[0]);
            if (dir.exists() && dir.isDirectory()) {
                workingDirectory = args[0];
            } else {
                log.warn("Working directory argument does not exists or is not a directory. Using defaults");
            }
        }
        DocumentProcessor processor = new WSDLProcessor(workingDirectory + WSDL_PATH, workingDirectory + OUTPUT_PATH);
        processor.run();
    }

}
