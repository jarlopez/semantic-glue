package edu.kth.wsglue;

import edu.kth.wsglue.parsing.processors.UnloadMode;
import edu.kth.wsglue.parsing.processors.WSDLProcessor;
import edu.kth.wsglue.parsing.comparators.SemanticComparator;
import edu.kth.wsglue.parsing.filters.ServiceScoreFilter;
import edu.kth.wsglue.parsing.generators.SemanticFieldGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Main launcher of the WSDL and SAWSDL document processor
 */
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
        String wsdlPath = workingDirectory + SAWSDL_PATH;
//        String wsdlPath = workingDirectory + WSDL_PATH;
        String outPath = workingDirectory + OUTPUT_PATH;
        WSDLProcessor processor = new WSDLProcessor(wsdlPath, outPath);
        processor
                .withUnloadMode(UnloadMode.SystemOut)
                .withFilterFunction(new ServiceScoreFilter(0.0))
                .withFieldGenerator(new SemanticFieldGenerator())
                .withDocumentComparator(new SemanticComparator())
                .run();
    }

}
