/*
Copyright 2017 Bastien Enjalbert - Orange

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal in 
the Software without restriction, including without limitation the rights to use, 
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the 
Software, and to permit persons to whom the Software is furnished to do so, 
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.orange.testgenappium.utility;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Functions to correct image url (<code><img src="...">...</code>) into xml outputs
 * By default all images are simply named, like if we execute only one test and 
 * not doing parallelization. So we need to edit the src attribute to see screenshots
 * for all tests and on all devices.
 * 
 * @author bastienjalbert
 */
public class ScreenshotUpdater {
    
    
    // used to change image file name in reports
    private static Node iterateNode = null;

    
    /**
     * Try to parse output.xml to generate correct image file name in future reports/logs
     * @param resultsAbsPath path to the xml output file
     * @param device_index the device index during test
     * @param testSuiteName the test name related to the output file
     * 
     * @return true if corrections have been done, false otherwhise (exception/error) 
     */
    protected static boolean reportImagesUpdaters(String resultsAbsPath, int device_index, String testSuiteName) {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new File(resultsAbsPath));

            // root document (<robot>...</robot> tag)
            Element racine = document.getDocumentElement(); 
            // get all childs nodes from <robot>...</robot> tag
            NodeList racineNoeuds = racine.getChildNodes();

            // root suite tag (<suite id="s1" name="Suites">)
            NodeList suiteNodes = null;

            // iterate into root node
            // try to find root suite tag
            for (int i = 0; i < racineNoeuds.getLength(); i++) {
                // if we're into a suite tag continue to parse :)
                if (racineNoeuds.item(i).getNodeType() == Node.ELEMENT_NODE
                        && racineNoeuds.item(i).getNodeName().equals("suite")) {
                    suiteNodes = racineNoeuds.item(i).getChildNodes(); 
                }
            }
             
            
            // one iteration = one device test suite execute by pybot
            // iterate into <suite id="s1" name="Suites">
            for (int i = 0; i < suiteNodes.getLength(); i++) {
                
                if (suiteNodes.item(i).getNodeType() == Node.ELEMENT_NODE
                        && suiteNodes.item(i).getNodeName().equals("test")) {
                    
                    // initialise the recursive variable 
                    // iterate into <test id="sx-tx" name="TestSuiteName"> fields 
                    iterateNode = suiteNodes.item(i);
                    
                    // The test suite name is like this : Test_Suite_Name
                    // but screenshots are name like this : 0.Test Suite Name...png
                    // So we need to replace underscores by spaces
                    testSuiteName = testSuiteName.replace("_"," ");
                    
                    // update all image file name with the device index and 
                    // the test suite name
                    updateImgFileNam(0, device_index, testSuiteName);
                }
                
            }

            // write the new xml output file  
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);

            StreamResult result = new StreamResult(new File(resultsAbsPath));
            transformer.transform(source, result);

        } catch (final ParserConfigurationException e) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error on parsing (reportImagesUpdaters function) : ", e.getLocalizedMessage()));
        } catch (final SAXException e) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error on SAX XML (reportImagesUpdaters function) : ", e.getLocalizedMessage()));
        } catch (final IOException e) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error of IO (reportImagesUpdaters function) : ", e.getLocalizedMessage()));
        } catch (TransformerConfigurationException e) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error on Transformer configuration (reportImagesUpdaters function) : ", e.getLocalizedMessage()));
        } catch (TransformerException e) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error on Transformer (reportImagesUpdaters function) : ", e.getLocalizedMessage()));
        }
        return true;
    }

    /**
     * Recusivelly change all image path into a test suite for a device Change
     * all images path to custom pabot results
     *
     * custom pabot path is like: X.Y.Z with: X = device id (ex: 0, 1, 2, ...) Y
     * = test suite name (ex: Add_client) Z = default name (ex:
     * appium-screenshot-15.png)
     *
     * @param level tag level
     * @param deviceIndex the current device index
     * @param testSuiteName and the current test suite name
     */
    public static void updateImgFileNam(int level, int deviceIndex, String testSuiteName) {

        // check if the node is a message (msg) node which contains an appium screenshot
        if (iterateNode.getNodeType() == Node.ELEMENT_NODE
                && iterateNode.getNodeName().equals("msg")
                && iterateNode.getTextContent().contains("appium-screenshot")) {
            // change href values
            Pattern p_href = Pattern.compile(".*(?:src=\\\")(.*?)(?:\\\").*");
            // note that (?:src=\\\") can also be (?:href=\\\") because we use
            // replace all after to change the content (replace all with appium-screenshot-x.png regex
            Matcher m_href = p_href.matcher(iterateNode.getTextContent());
            StringBuffer sb_href = new StringBuffer();
            /* changes all matches in the String with a correct appium screenshot name 
               why corrected ? sometimes the screen png is prefixed with the 
               test suite name */
            if (m_href.matches()) {
                // ensure the appium screenshot is correct, otherwise do nothing
                if (!extractAppiumSimpleScreenshotName(m_href.group(1)).equals("-1")) {
                    String cleanAppiumScreenName = extractAppiumSimpleScreenshotName(m_href.group(1));
                    m_href.appendReplacement(sb_href, m_href.group(0).replaceAll(Pattern.quote(m_href.group(1)), deviceIndex + "-" + testSuiteName + "-" + cleanAppiumScreenName));
                    m_href.appendTail(sb_href);
                }
            }
            // we did the change now in the xml file
            iterateNode.setTextContent(sb_href.toString());

        }
        // iterate again and again recursively 
        NodeList list = iterateNode.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            iterateNode = childNode;
            updateImgFileNam(level + 1, deviceIndex, testSuiteName);
        }
    }

    /**
     * Try to recover a clean appium screenshot name. This function try to find
     * the appium screenshot basic filename (appium-screenshot-[NUMBER].png)
     * from any String and return the cleaned version. For example the input
     * string "Suites.try test-appium-screenshot-5.png" will be converted to
     * "appium-screenshot-5.png". An other example is "appium-screenshot-5.png"
     * input String will not be modified, so if the input is
     * "appium-screenshot-5.png" than output will just be the same
     *
     * @param appiumMsg the input appium screenshot name
     * @return return the corrected screenshot name, or -1 in failure case
     */
    public static String extractAppiumSimpleScreenshotName(String appiumMsg) {
        int index = -1;
        Pattern p_href = Pattern.compile(".*appium-screenshot-([0-9]*).png.*");
        Matcher m_href = p_href.matcher(iterateNode.getTextContent());
        // try to find the match
        if (m_href.matches()) {
            index = Integer.parseInt(m_href.group(1));
        }

        return index != -1 ? "appium-screenshot-" + index + ".png" : "error";
    }

    // DEBUG FUNCTION
    public static void DEBUG_CHILDS(int level, int deviceIndex) {
        if (iterateNode.getNodeType() == Node.ELEMENT_NODE
                && iterateNode.getNodeName().equals("msg")
                && iterateNode.getTextContent().contains("png")) {
            System.out.println("DEVICE : " + deviceIndex + " MSG : " + iterateNode.getTextContent());
        }
        NodeList list = iterateNode.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            iterateNode = childNode;
            DEBUG_CHILDS(level + 1, deviceIndex);
        }
    }

}
