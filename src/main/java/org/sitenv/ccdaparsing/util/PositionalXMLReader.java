package org.sitenv.ccdaparsing.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PositionalXMLReader {

	
	final static String LINE_NUMBER_KEY_NAME = "lineNumber";
    final static String END_LINE_NUMBER_KEY_NAME = "endLineNumber";

    public static Document readXML(final InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory=DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        InputSource inputsource = new InputSource(reader);
        inputsource.setEncoding("UTF-8");

        final Stack<Element> elementStack = new Stack<Element>();
        final StringBuilder textBuffer = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            private Locator locator;

            @Override
            public void setDocumentLocator(final Locator locator) {
                this.locator = locator; // Save the locator,
		//so that it can be used later for line tracking when traversing nodes.
            }

            @Override
            public void startElement(final String uri, final String localName, 
			final String qName, final Attributes attributes)
                    throws SAXException {
                addTextIfNeeded();
                final Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }
                el.setUserData(LINE_NUMBER_KEY_NAME, 
			String.valueOf(this.locator.getLineNumber()), null);
                elementStack.push(el);
            }

            @Override
            public void endElement(String uri, String localName, String qName){
                addTextIfNeeded();
                Element closedEl = elementStack.pop();
                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                } else {
                    Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);                   
                }
                closedEl.setUserData(END_LINE_NUMBER_KEY_NAME, 
            			String.valueOf(this.locator.getLineNumber()), null);
            }
            
            @Override
            public void characters (char ch[], int start, int length) throws SAXException {
                textBuffer.append(ch, start, length);
            }
            
            // Outputs text accumulated under the current node
            private void addTextIfNeeded() {
                if (textBuffer.length() > 0) {
                    Element el = elementStack.peek();
                    Node textNode = doc.createTextNode(textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }           
        };
        parser.parse(inputsource, handler);

        return doc;
    }
}
