package edu.kth.wsglue;

import edu.kth.wsglue.parsing.comparators.SemanticComparator;
import edu.kth.wsglue.parsing.comparators.SyntacticComparator;
import edu.kth.wsglue.parsing.filters.ServiceScoreFilter;
import edu.kth.wsglue.parsing.generators.NamedFieldGenerator;
import edu.kth.wsglue.parsing.generators.SemanticFieldGenerator;
import edu.kth.wsglue.parsing.processors.UnloadMode;
import edu.kth.wsglue.parsing.processors.WSDLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main launcher of the WSDL and SAWSDL document processor
 */
public class WebServiceMatcher {
    private static final Logger log = LoggerFactory.getLogger(WebServiceMatcher.class.getName());

    private static final String OUTPUT_PATH = "/comparison-out";
    private static final String WSDL_PATH = "/WSDLs";
    private static final String SAWSDL_PATH = "/SAWSDL";

    private enum ExecutionMode {
        WSDL,
        SAWSDL,
        Hybrid
    }

    public static void main(String[] args) {
        String cwd = System.getProperty("user.dir");
        String out = cwd + OUTPUT_PATH;

        ExecutionMode executionMode = ExecutionMode.Hybrid;
        UnloadMode unloadMode = UnloadMode.File;
        if (args.length > 0) {
            String mode = args[0].toLowerCase();
            switch (mode) {
                case "wsdl":
                    executionMode = ExecutionMode.WSDL;
                    break;
                case "sawsdl":
                    executionMode = ExecutionMode.SAWSDL;
                    break;
                case "hybrid":
                default:
                    executionMode = ExecutionMode.Hybrid;
            }
        }

        switch (executionMode) {
            case WSDL:
                launchWSDL(unloadMode, cwd, out);
                break;
            case SAWSDL:
                launchSAWSDL(unloadMode, cwd, out);
                break;
            case Hybrid:
                launchWSDL(unloadMode, cwd, out);
                launchSAWSDL(unloadMode, cwd, out);
                break;
        }
    }

    private static void launchSAWSDL(UnloadMode mode, String cwd, String out) {
        log.info("Running SAWSDL processor");

        String wsdlPath = cwd + SAWSDL_PATH;
        out += "/sawsdl";
        WSDLProcessor processor = new WSDLProcessor(wsdlPath, out);
        processor
                .withUnloadMode(mode)
                .withFilterFunction(new ServiceScoreFilter(0.0))
                .withFieldGenerator(new SemanticFieldGenerator())
                .withDocumentComparator(new SemanticComparator())
                .run();
    }

    private static void launchWSDL(UnloadMode mode, String cwd, String out) {
        log.info("Running WSDL processor");

        String wsdlPath = cwd + WSDL_PATH;
        out += "/wsdl";
        WSDLProcessor processor = new WSDLProcessor(wsdlPath, out);
        processor
                .withUnloadMode(mode)
                .withFilterFunction(new ServiceScoreFilter(0.0))
                .withFieldGenerator(new NamedFieldGenerator())
                .withDocumentComparator(new SyntacticComparator())
                .run();

    }

}
