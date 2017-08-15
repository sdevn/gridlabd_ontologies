package ontologies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import ontologies.GridlabdCSVParser.CSVParseListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Application's entry point. <br>
 * Parses the .csv and the .owl files and produces a new .owl file with individuals
 */
public class Main
{
	private static final String CLASS_ID_PREFIX = "auction";
	private static final String DOUBLE_IRI = "http://www.w3.org/2001/XMLSchema#double";
	private static final String PLAIN_LITERAL_IRI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral";
	
	private static void printHelp()
	{
		System.out.println("---");
		System.out.println("Run with the following arguments:");
		System.out.println("\tjava -jar ontologies-x.y.z-SNAPSHOT.jar <gridlabd csv file> <ontology input file> <ontology output file>");
		System.out.println("---");
	}
	
	public static void main(String[] args) 
			throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		if (args.length < 3
		|| !Files.exists(Paths.get(args[0]))
		|| !Files.exists(Paths.get(args[1])))
		{
			printHelp();
			
			return;
		}
		
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document doc = dBuilder.parse(new File(args[1]));
		
		final Node ontologyNode = doc.getDocumentElement();
		
		final GridlabdCSVParser csvParser = new GridlabdCSVParser(new File(args[0]));
		csvParser.parse(new CSVParseListener(){
			int curID = 0;
			@SuppressWarnings("unused")
			List<String> headers;

			public void onHeadersParsed(List<String> headers)
			{				
				this.headers = headers;
				System.out.print("Reading file content");
			}

			public void onLineParsed(Map<String, String> values)
			{
				final String individualName = "#" + CLASS_ID_PREFIX + ++curID;
				String value;
				String datatypeIRI;
				
				final Element individualDeclaration = doc.createElement("Declaration");
				final Element individualNamedIndividual = doc.createElement("NamedIndividual");
				individualNamedIndividual.setAttribute("IRI", individualName);
				individualDeclaration.appendChild(individualNamedIndividual);
				ontologyNode.appendChild(individualDeclaration);

				final Element individualClassAssertion = doc.createElement("ClassAssertion");
				final Element individualClass = doc.createElement("Class");
				individualClass.setAttribute("IRI", "#Auction");
				individualClassAssertion.appendChild(individualClass);
				individualClassAssertion.appendChild(individualNamedIndividual.cloneNode(true));
				ontologyNode.appendChild(individualClassAssertion);

				for (String k : values.keySet())
				{
					System.out.print(".");
					
					value = values.get(k);
					
					if (value != null)
					{
						if (k.equals("timestamp")
						|| k.equals("clearing_type"))
						{
							datatypeIRI = PLAIN_LITERAL_IRI;
						}
						else 
						{
							datatypeIRI = DOUBLE_IRI;
						}
						
						final Element individualDataPropertyAssertion = doc.createElement("DataPropertyAssertion");
						final Element individualDataProperty = doc.createElement("DataProperty");
						individualDataProperty.setAttribute("IRI", "#" + k);
						individualDataPropertyAssertion.appendChild(individualDataProperty);
						individualDataPropertyAssertion.appendChild(individualNamedIndividual.cloneNode(true));
						final Element individualLiteral = doc.createElement("Literal");
						individualLiteral.setAttribute("datatypeIRI", datatypeIRI);
						individualLiteral.setTextContent(value);
						individualDataPropertyAssertion.appendChild(individualLiteral);
						ontologyNode.appendChild(individualDataPropertyAssertion);
					}
				}
			}
		});

		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//	FIXME:	Adding a "stanard" doctype node, breaks compatibility with protege
//		final DOMImplementation domImpl = doc.getImplementation();
//		final DocumentType doctype = domImpl.createDocumentType("doctype",
//			    "",
//			    "[\n\t<!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\" >\n"
//			    + "\t<!ENTITY xml \"http://www.w3.org/XML/1998/namespace\" >\n"
//			    + "\t<!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\" >\n"
//			    + "\t<!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" >\n"
//			    + "]");
//		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
//		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
		
		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(new File(args[2]));
		transformer.transform(source, result);

		System.out.println(" Done!");
	}
}
