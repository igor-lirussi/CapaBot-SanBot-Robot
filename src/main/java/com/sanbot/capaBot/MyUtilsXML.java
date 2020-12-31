package com.sanbot.capaBot;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * A class of utils for XML
 * for formatting:
 * http://www.webtoolkitonline.com/xml-formatter.html
 */
public final class MyUtilsXML {

    private final static String TAG = "IGOR-XML";

    /**
     *  Takes the string-value from an sub-element (tag) that's inside the element passed
     *  (it takes the string from the first sub-element)
     * @param tag Tag to find in the element
     * @param element element of an XML where searching the tag
     * @return the string-value of the tag inside the element passed
     */
    public static String getStringOfTagInside(String tag, Element element) {
        //every tag-element inside the element passed => takes the first and the children
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        //first child
        Node node = nodeList.item(0);
        //value of the first child
        return node.getNodeValue();
    }

    /**
     *  Takes the string inside an element
     * @param element element of an XML
     * @return string inside the element
     */
    public static String getStringInside( Element element) {
        //go inside and take the first element from the children (takes the node string)
        Node node = element.getChildNodes().item(0);
        //value of the string-node
        return node.getNodeValue();
    }



    /**
     * creates a file in the directory of CAPABOTXML
     * the file is not initialized, so if you want to add stuff to the xml
     * before you have to initialize the XML with initializeXMLfromFile() function.
     * @param fileName the name of the file
     * @return the file created
     */
    public static File createFileInXMLDirectory(String fileName) {
        Log.i(TAG, "checking or creating XML file");
        //open a directory
        File fileDir = new File(Environment.getExternalStorageDirectory() + "/" + "CAPABOTXML" + "/");
        if (!fileDir.exists()) {
            //if no directory it is created
            final boolean mkdirs = fileDir.mkdirs();
            if (!mkdirs) Log.e("IGOR", "Error in mkdirs creating directory file");
        }
        //create file in directory
        File fileXML = new File(fileDir, fileName);
        return fileXML;
    }


    /**
     * creates an XML File from a file passed and initializes the xml with a forecastContainerLL tag "userData".
     * @param fileXML the file to create as XML
     */
    public static void initializeXMLfromFile(File fileXML) {
        //initialize and create the structure of the file as XML
        try {
            Log.i(TAG, "initializing XML file with random things");
            fileXML.createNewFile();
            FileOutputStream fileos = new FileOutputStream(fileXML);
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.startTag(null, "userData");

            boolean exmple_content = false;
            if (exmple_content) {
                xmlSerializer.startTag(null, "element");

                xmlSerializer.startTag(null, "sub-element");
                xmlSerializer.text("sub-element content");
                xmlSerializer.endTag(null, "sub-element");

                xmlSerializer.startTag(null, "sub-element2");
                xmlSerializer.text("sub-element2 content");
                xmlSerializer.endTag(null, "sub-element2");

                xmlSerializer.endTag(null, "element");
            }

            xmlSerializer.endTag(null, "userData");
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            String dataWrite = writer.toString();
            fileos.write(dataWrite.getBytes());
            fileos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**    SUGGESTION FUNCTIONS    */

    public static void xmlSuggestionsCreateFile(File fileXML) {
        //CREATE
        try {
            Log.i(TAG, "creating structure for suggestions XML file");
            fileXML.createNewFile();
            FileOutputStream fileos = new FileOutputStream(fileXML);
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.startTag(null, "userData");


            //if (when creates the xml) puts inside an example block
            boolean exampleCreatingXML = false;
            if (exampleCreatingXML) {
                xmlSerializer.startTag(null, "suggestion");

                xmlSerializer.startTag(null, "name");
                xmlSerializer.text("name example");
                xmlSerializer.endTag(null, "name");

                xmlSerializer.startTag(null, "text");
                xmlSerializer.text("suggestion text example");
                xmlSerializer.endTag(null, "text");

                xmlSerializer.endTag(null, "suggestion");
            }

            xmlSerializer.endTag(null, "userData");
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            String dataWrite = writer.toString();
            fileos.write(dataWrite.getBytes());
            fileos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void xmlAddSuggestion(File fileXML, String name_passed, String suggestion_passed) {
        try {
            Log.i(TAG, "adding suggestion XML file");
            InputStream inpstr;
            try {
                inpstr = new FileInputStream(fileXML);
            } catch (FileNotFoundException e) {
                xmlSuggestionsCreateFile(fileXML);
                inpstr = new FileInputStream(fileXML);
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inpstr);
            //gets the root element
            Element root = doc.getDocumentElement();

            // new element
            Element newElement = doc.createElement("suggestion");

            //element inside the new element with string inside
            Element name = doc.createElement("name");
            name.appendChild(doc.createTextNode(name_passed));
            newElement.appendChild(name);

            //idem
            Element suggestion = doc.createElement("text");
            suggestion.appendChild(doc.createTextNode(suggestion_passed));
            newElement.appendChild(suggestion);

            //append the new element
            root.appendChild(newElement);

            //save the xml via dom
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(fileXML);
            transformer.transform(source, result);

        } catch ( Exception e) {
            e.printStackTrace();
        }
    }

    public static String xmlReadSuggestionTextOf(File fileXML, String name_passed) {
        //READ
        try {
            Log.i(TAG, "reading suggestion XML file");
            //open doc
            InputStream inpstr;
            try {
                inpstr = new FileInputStream(fileXML);
            } catch (FileNotFoundException e) {
                xmlSuggestionsCreateFile(fileXML);
                inpstr = new FileInputStream(fileXML);
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inpstr);

            //gets the root element
            Element elementRoot=doc.getDocumentElement();
            elementRoot.normalize();

            //list of suggestions
            NodeList nList = doc.getElementsByTagName("suggestion");
            //cycling all the sub elements
            for (int i=0; i<nList.getLength(); i++) {
                //node is current node in list
                Node node = nList.item(i);
                //if node is an element
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    //convert to element
                    Element element = (Element) node;
                    //if the name inside the suggestion is equal to the name passed
                    if (getStringOfTagInside("name", element).equals(name_passed)) {
                        return getStringOfTagInside("text", element);
                    }
                }
            }
        } catch (Exception e) {e.printStackTrace();}
        return null;
    }



    /**  STATS FUNCTIONS   */


    public static void xmlStatsCreateFile(File fileXML) {
        //CREATE
        try {
            Log.i(TAG, "creating structure stats XML file");
            fileXML.createNewFile();
            FileOutputStream fileos = new FileOutputStream(fileXML);
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);

            xmlSerializer.startTag(null, "userData");

            //filling the structure
            //handshakes
            xmlSerializer.startTag(null, "handshakes");
            boolean exampleCreatingXML = false;
            if (exampleCreatingXML) {
                xmlSerializer.startTag(null, "hs");
                xmlSerializer.text("handshake example");
                xmlSerializer.endTag(null, "hs");
            }
            xmlSerializer.endTag(null, "handshakes");

            //request_shake
            xmlSerializer.startTag(null, "request_shake");
            xmlSerializer.endTag(null, "request_shake");
            //request_location
            xmlSerializer.startTag(null, "request_location");
            xmlSerializer.endTag(null, "request_location");
            //request_video
            xmlSerializer.startTag(null, "request_video");
            xmlSerializer.endTag(null, "request_video");
            //interaction_button
            xmlSerializer.startTag(null, "interaction_button");
            xmlSerializer.endTag(null, "interaction_button");
            //interaction_face
            xmlSerializer.startTag(null, "interaction_face");
            xmlSerializer.endTag(null, "interaction_face");

            xmlSerializer.endTag(null, "userData");

            xmlSerializer.endDocument();
            xmlSerializer.flush();
            String dataWrite = writer.toString();
            fileos.write(dataWrite.getBytes());
            fileos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void xmlAddToNodeNow(File fileXML, String sub_node_name) {
        try {
            InputStream inpstr;
            try {
                inpstr = new FileInputStream(fileXML);
            } catch (FileNotFoundException e) {
                xmlStatsCreateFile(fileXML);
                inpstr = new FileInputStream(fileXML);
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inpstr);
            //gets the root element
            Element elementRoot = doc.getDocumentElement();
            elementRoot.normalize();

            //sub-node with the name passed
            Node sub_node = doc.getElementsByTagName(sub_node_name).item(0);

            // new element called event
            Element newEvent = doc.createElement("event");

            String date_now = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ITALY).format(Calendar.getInstance().getTime());
            newEvent.appendChild(doc.createTextNode(date_now));

            //append the new element
            sub_node.appendChild(newEvent);

            //save the xml via dom
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(fileXML);
            transformer.transform(source, result);

        } catch ( Exception e) {
            e.printStackTrace();
        }
    }

    public static int xmlReadStatsHandshakesNumber(File fileXML) {
        //READ
        try {
            //open doc
            InputStream inpstr;
            try {
                inpstr = new FileInputStream(fileXML);
            } catch (FileNotFoundException e) {
                xmlStatsCreateFile(fileXML);
                inpstr = new FileInputStream(fileXML);
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inpstr);

            //gets the root element
            Element elementRoot = doc.getDocumentElement();
            elementRoot.normalize();

            //node handshakes
            Node handshakes = doc.getElementsByTagName("handshakes").item(0);

            //list of all the hs
            NodeList hsList = handshakes.getChildNodes();
            //return the number of hs
            return hsList.getLength();

        } catch (Exception e) {
            Log.i("IGORXML", "READING XML WRONG: "+ e.toString());
            e.printStackTrace();}
        return -2;
    }





}
