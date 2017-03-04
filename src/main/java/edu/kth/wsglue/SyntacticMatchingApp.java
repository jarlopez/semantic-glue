package edu.kth.wsglue;

import com.predic8.schema.*;
import com.predic8.schema.Documentation;
import com.predic8.schema.Import;
import com.predic8.wsdl.*;
import edu.kth.wsglue.parsing.DocumentProcessor;
import edu.kth.wsglue.parsing.WSDLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SyntacticMatchingApp {
    private static final Logger log = LoggerFactory.getLogger(SyntacticMatchingApp.class.getName());

    private static final String TEMP_WSDL_0 = "/home/johan/school/2017-p3/web-services/project/WSDLs/FlightwiseAPIProfile.wsdl";
    private static final String TEMP_WSDL_1 = "/home/johan/school/2017-p3/web-services/project/WSDLs/FlightAwareAPIProfile.wsdl";
    private static final String TEMP_SWSDL_1 = "/home/johan/school/current/web-services/project/SAWSDL/_skilledoccupation_BMWservice.wsdl";

    public static void main(String[] args) {
        DocumentProcessor processor = new WSDLProcessor("/home/johan/school/current/web-services/project/WSDLs");
        processor.run();
//        String first = TEMP_SWSDL_1;
//        String second = TEMP_WSDL_1;
//        if  (args.length == 2) {
//            first = args[0];
//            second = args[1];
//        }
//        SyntacticMatchingApp app = new SyntacticMatchingApp();
//        DocumentMatcher matcher = new DocumentMatcher();
////        app.printXmlSchema(first);
//        try {
//            WSMatchingType outputWsdl = matcher.generateMatchProfile(first, second);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
        // TODO Marshal to output file (could be provided in args[2])
    }

    private static void out(String str) {
        System.out.println(str);
    }

    private void printWsdl(String input) {
        WSDLParser parser = new WSDLParser();
        Definitions defs = parser.parse(input);

        out("-------------- WSDL Details --------------");
        out("TargenNamespace: \t" + defs.getTargetNamespace());
        if (defs.getDocumentation() != null) {
            out("Documentation: \t\t" + defs.getDocumentation());
        }
        out("\n");

        /* For detailed schema information see the FullSchemaParser.java sample.*/
        out("Schemas: ");
        for (Schema schema : defs.getSchemas()) {
            out("  TargetNamespace: \t" + schema.getTargetNamespace());
        }
        out("\n");

        out("Messages: ");
        for (Message msg : defs.getMessages()) {
            out("  Message Name: " + msg.getName());
            out("  Message Parts: ");
            for (Part part : msg.getParts()) {
                out("    Part Name: " + part.getName());
                out("    Part Element: " + ((part.getElement() != null) ? part.getElement() : "not available!"));
                out("    Part Type: " + ((part.getType() != null) ? part.getType() : "not available!" ));
                out("");
            }
        }
        out("");

        out("PortTypes: ");
        for (PortType pt : defs.getPortTypes()) {
            out("  PortType Name: " + pt.getName());
            out("  PortType Operations: ");
            for (Operation op : pt.getOperations()) {
                out("    Operation Name: " + op.getName());
                out("    Operation Input Name: "
                        + ((op.getInput().getName() != null) ? op.getInput().getName() : "not available!"));
                out("    Operation Input Message: "
                        + op.getInput().getMessage().getQname());
                out("    Operation Output Name: "
                        + ((op.getOutput().getName() != null) ? op.getOutput().getName() : "not available!"));
                out("    Operation Output Message: "
                        + op.getOutput().getMessage().getQname());
                out("    Operation Faults: ");
                if (op.getFaults().size() > 0) {
                    for (Fault fault : op.getFaults()) {
                        out("      Fault Name: " + fault.getName());
                        out("      Fault Message: " + fault.getMessage().getQname());
                    }
                } else out("      There are no faults available!");

            }
            out("");
        }
        out("");

        out("Bindings: ");
        for (Binding bnd : defs.getBindings()) {
            out("  Binding Name: " + bnd.getName());
            out("  Binding Type: " + bnd.getPortType().getName());
            out("  Binding Protocol: " + bnd.getBinding().getProtocol());
            if(bnd.getBinding() instanceof AbstractSOAPBinding) out("  Style: " + (((AbstractSOAPBinding)bnd.getBinding()).getStyle()));
            out("  Binding Operations: ");
            for (BindingOperation bop : bnd.getOperations()) {
                out("    Operation Name: " + bop.getName());
                if(bnd.getBinding() instanceof AbstractSOAPBinding) {
                    out("    Operation SoapAction: " + bop.getOperation().getSoapAction());
                    out("    SOAP Body Use: " + bop.getInput().getBindingElements().get(0).getUse());
                }
            }
            out("");
        }
        out("");

        out("Services: ");
        for (Service service : defs.getServices()) {
            out("  Service Name: " + service.getName());
            out("  Service Potrs: ");
            for (Port port : service.getPorts()) {
                out("    Port Name: " + port.getName());
                out("    Port Binding: " + port.getBinding().getName());
                out("    Port Address Location: " + port.getAddress().getLocation()
                        + "\n");
            }
        }
        out("");
    }

    private void printXmlSchema(String input) {
        SchemaParser parser = new SchemaParser();
        Schema schema = parser.parse(input);

        out("-------------- Schema Information --------------");
        out("  Schema TargetNamespace: " + schema.getTargetNamespace());
        out("  AttributeFormDefault: " + schema.getAttributeFormDefault());
        out("  ElementFormDefault: " + schema.getElementFormDefault());
        out("");

        if (schema.getImports().size() > 0) {
            out("  Schema Imports: ");
            for (Import imp : schema.getImports()) {
                out("    Import Namespace: " + imp.getNamespace());
                out("    Import Location: " + imp.getSchemaLocation());
            }
            out("");
        }

        if (schema.getIncludes().size() > 0) {
            out("  Schema Includes: ");
            for (Include inc : schema.getIncludes()) {
                out("    Include Location: " + inc.getSchemaLocation());
            }
            out("");
        }

        out("  Schema Elements: ");
        for (Element e : schema.getAllElements()) {
            out("    Element Name: " + e.getName());
            if (e.getType() != null) {
                /*
                 * schema.getType() delivers a TypeDefinition (SimpleType orComplexType)
                 * object.
                 */
                out("    Element Type Name: " + schema.getType(e.getType()).getName());
                out("    Element minoccurs: " + e.getMinOccurs());
                out("    Element maxoccurs: " + e.getMaxOccurs());
                if (e.getAnnotation() != null)
                    annotationOut(e);
            }
        }
        out("");

        out("  Schema ComplexTypes: ");
        for (ComplexType ct : schema.getComplexTypes()) {
            out("    ComplexType Name: " + ct.getName());
            if (ct.getAnnotation() != null)
                annotationOut(ct);
            if (ct.getAttributes().size() > 0) {
                out("    ComplexType Attributes: ");
                /*
                 * If available, attributeGroup could be read as same as attribute in
                 * the following.
                 */
                for (Attribute attr : ct.getAttributes()) {
                    out("      Attribute Name: " + attr.getName());
                    out("      Attribute Form: " + attr.getForm());
                    out("      Attribute ID: " + attr.getId());
                    out("      Attribute Use: " + attr.getUse());
                    out("      Attribute FixedValue: " + attr.getFixedValue());
                    out("      Attribute DefaultValue: " + attr.getDefaultValue());
                }
            }
            /*
             * ct.getModel() delivers the child element used in complexType. In case
             * of 'sequence' you can also use the getSequence() method.
             */
            out("    ComplexType Model: " + ct.getModel().getClass().getSimpleName());
            if (ct.getModel() instanceof ModelGroup) {
                out("    Model Particles: ");
                for (SchemaComponent sc : ((ModelGroup) ct.getModel()).getParticles()) {
                    out("      Particle Kind: " + sc.getClass().getSimpleName());
                    out("      Particle Name: " + sc.getName() + "\n");
                }
            }

            if (ct.getModel() instanceof ComplexContent) {
                Derivation der = ((ComplexContent) ct.getModel()).getDerivation();
                out("      ComplexConten Derivation: " + der.getClass().getSimpleName());
                out("      Derivation Base: " + der.getBase());
            }

            if (ct.getAbstractAttr() != null)
                out("    ComplexType AbstractAttribute: " + ct.getAbstractAttr());
            if (ct.getAnyAttribute() != null)
                out("    ComplexType AnyAttribute: " + ct.getAnyAttribute());

            out("");
        }

        if (schema.getSimpleTypes().size() > 0) {
            out("  Schema SimpleTypes: ");
            for (SimpleType st : schema.getSimpleTypes()) {
                out("    SimpleType Name: " + st.getName());
                out("    SimpleType Restriction: " + st.getRestriction());
                out("    SimpleType Union: " + st.getUnion());
                out("    SimpleType List: " + st.getList());
            }
        }
    }

    private static void annotationOut(SchemaComponent sc) {
        if (sc.getAnnotation().getAppinfos().size() > 0) {
            System.out
                    .print("    Annotation (appinfos) available with the content: ");
            for (Appinfo appinfo : sc.getAnnotation().getAppinfos()) {
                out(appinfo.getContent());
            }
        } else {
            System.out
                    .print("    Annotation (documentation) available with the content: ");
            for (Documentation doc : sc.getAnnotation().getDocumentations()) {
                out(doc.getContent());
            }
        }
    }
}
