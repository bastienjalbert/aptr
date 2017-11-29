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
package com.orange.testgenappium;

import static com.orange.testgenappium.launcher.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList; 
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger; 
import org.apache.commons.io.FileUtils;

/**
 * Utility class that contains some usefull functions to create automatically 
 * // tests on Android devices.
 * @author bastienjalbert
 */
public class Tools {
     

    /**
     * Get list of valuesXYZ.dat files into CONF_PATH/devices_conf, in other
     * words get list of all devices, and theirs configurations (udid, appium
     * port, ...). We get this from configuration path
     *
     * @return the list of theses files
     */
    protected static ArrayList<Device> getDevicesDat() {
        ArrayList<String> values_files = new ArrayList<>();

        File folder = new File(launcher.CONF_PATH);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            // check file extension, get only .dat files
            if (listOfFiles[i].getName().lastIndexOf(".") != -1
                    && listOfFiles[i].getName().lastIndexOf(".") != 0
                    && listOfFiles[i].getName().contains(".dat")) {
                values_files.add(listOfFiles[i].getAbsolutePath());
            }

        }

        ArrayList<Device> devices = new ArrayList<>();

        // get devices information 
        for (String pathValue : values_files) {
            devices.add(Device.loadToFile(pathValue));
        }

        return devices;
    }

    /**
     * Get a list of test file (.robot) from a directory
     *
     * @param path to test files (to .robot files)
     * @return the list of xxxx.robot found in the path
     */
    protected static ArrayList<String> getRobotFrameworkTestFiles(String path) {
        // prepate the return
        ArrayList<String> tests_files = new ArrayList<>();

        // open the directory and get files into
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                // we verify that the file is a robot file and isn't init file 
                if (getFileExtension(listOfFiles[i]).equals("robot") && !listOfFiles[i].getName().equals("__init__.txt")) {
                    tests_files.add(listOfFiles[i].getName());
                }

            }
        }

        return tests_files;
    }

    // http://www.journaldev.com/842/how-to-get-file-extension-in-java
    // get the file extension
    protected static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }

    /**
     * Write something into a file, dedicated for log entries
     *
     * @param toAdd the text to append at the end of log file
     */
    protected static void writeLog(List<String> toAdd) {

        try {
            Path file = Paths.get(launcher.LOG_FILE_PATH);
            // if log file doesn't exist, just create one
            if (!file.toFile().exists()) {
                file.toFile().createNewFile();
            }
            Files.write(file, toAdd, Charset.forName("UTF-8"), StandardOpenOption.APPEND);

        } catch (IOException ex) {
            Logger.getLogger(launcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    /**
     * Copy outputx.xml file (corresponding to one test suite result on one device)
     * to the tmp directory and editing the outputx.xml to outputx.testSuiteName.xml
     * Then process output files with image updater 
     * @param devices_conf
     * @param testSuiteName 
     */
    protected static void preparingOutputsToTmp(ArrayList<Device> devices_conf, String testSuiteName) {
        
        // copying outputx.xml files to tmp directory (x = number of device) 
        int x = 0;
        
        // we have to do this devices_conf.size() times because we have x outputx.xml files
        for(Device device : devices_conf) {
            // move the old outputX.xml file to the new outputX.TestSuite.xml file
            File outputFile = new File(OUTPUT_PATH + "/pabot_results/output" + x + ".xml");
            outputFile.renameTo(new File(WORKING_PATH + "/output" + x + "." + testSuiteName + ".xml"));
            outputFile.delete();
            // process this file to get updated src of screenshot
            ScreenshotUpdater.reportImagesUpdaters(WORKING_PATH + "/output" + x + "." + testSuiteName + ".xml", x, testSuiteName);
            x++;
        }
        
    }

    /**
     * Return the test name from a file name. Just delete the .robot extension
     * @param testFileName 
     * @return the string name without the .robot extension
     */
    protected static String getOnlyTestNameFromFile(String testFileName) {
        return testFileName.substring(0, testFileName.lastIndexOf('.'));
    }
    
    
    /**
     * Clear the workspace directory if it exists and then create needed folder
     */
    protected static void getWorkingDir() {
        
        
        // Set all working paths //
        
        LOG_FILE_PATH = RUNNER_PATH + "/error.log.txt"; 
        
        
        RUNNER_PATH = PATH_TO_TESTS + "/runner";
        if(!(new File(RUNNER_PATH).exists())) {
            new File(RUNNER_PATH).mkdir();
        }  
        
        // if the output directory exists just delete it (recursivelly) 
        // and create it again (empty) <=> clear workspace
        OUTPUT_PATH = RUNNER_PATH + "/output";
        if(!(new File(OUTPUT_PATH).exists())) {
            new File(OUTPUT_PATH).mkdir();
        } else {
            try {
                FileUtils.deleteDirectory(new File(OUTPUT_PATH));
                new File(OUTPUT_PATH).mkdir();
            } catch(IOException e) {
                String date = new Date().toString();
                Tools.writeLog(Arrays.asList(date, "Error while clearing workspace (can't delete runner directory) : ", e.getLocalizedMessage()));
            }
        }
        
        WORKING_PATH = OUTPUT_PATH + "/tmp";
        if(!(new File(WORKING_PATH).exists())) {
            new File(WORKING_PATH).mkdir();
        }
        
        IMG_PATH = OUTPUT_PATH + "/img";
        if(!(new File(IMG_PATH).exists())) {
            new File(IMG_PATH).mkdir();
        } 
        
        CONF_PATH = RUNNER_PATH + "/devices_conf";
        if(!(new File(CONF_PATH).exists())) {
            new File(CONF_PATH).mkdir();
        }
        
    }

}
