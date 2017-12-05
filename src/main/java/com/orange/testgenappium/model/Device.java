/*
 * Orange, All right reserved. 2017
 */
package com.orange.testgenappium.model;

import com.orange.testgenappium.utility.Tools;
import java.io.File;
import java.io.IOException; 
import java.nio.charset.StandardCharsets;
import java.nio.file.Files; 
import java.util.Arrays;
import java.util.Date; 
import java.util.stream.Stream;

/**
 * Simple class to represent an Android device with some basic information
 *
 * @author bastien enjalbert
 */
public class Device {

    private String udid;

    private String name;

    private String type;

    private String appiumPort;
    
    private String fullPathConf;
    
    private String appiumBpPort;
    
    private String osVersion;

    public String getUdid() {
        return udid;
    }

    public void setUdid(String udid) {
        this.udid = udid;
    }
    
    public String getOs() {
        return osVersion;
    }

    public void setOs(String osVersion) {
        this.osVersion = osVersion;
    }
    
    public String getConfFilePath() {
        return fullPathConf;
    }

    public void setConfFilePath(String fullPathConf) {
        this.fullPathConf = fullPathConf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAppiumBpPort() {
        return appiumBpPort;
    }
    
    public void setAppiumBpPort(String port) {
        this.appiumBpPort = port;
    }  
    
    public String getAppiumPort() {
        return appiumPort;
    }

    public void setAppiumPort(String port) {
        this.appiumPort = port;
    } 

    /**
     * Read a file (value.dat) and convert informations to a Device object
     * @param filePath value file
     * @return null if read error / errors, the device object if no error
     */
    public static Device loadToFile(String filePath) {
        
        Device loadedConf = new Device();
        
        try {
            // load the file into a stream 
            Stream<String> confLine = Files.lines(new File(filePath).toPath(), StandardCharsets.UTF_8);
            // prepare for iteration (line by line)
            Iterable<String> iterable = confLine::iterator;
            // read the file line by line
            for (String s : iterable) {
                if(s.split(":").length == 2) { 
                    // load informations about configuration
                    switch (s.split((":"))[0]) {
                        case "--variable udid":
                            loadedConf.udid = s.split((":"))[1];
                            break;
                        case "--variable type":
                            loadedConf.type = s.split((":"))[1];
                            break;
                        case "--variable name":
                            loadedConf.name = s.split((":"))[1];
                            break;
                        case "--variable appium":
                            loadedConf.appiumPort = s.split((":"))[1];
                            break;
                        case "--variable appiumbp":
                            loadedConf.appiumBpPort = s.split((":"))[1];
                            break;
                        case "--variable osversion":
                            loadedConf.osVersion = s.split((":"))[1];
                            break;
                        default:
                            break;
                    }
                    
                }
            } 
            
            loadedConf.fullPathConf = filePath;
            
            return loadedConf;

        } catch (IOException ex) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error on reading device conf file (loadToFile function) : ", ex.getLocalizedMessage()));
            return null;
        }
 
    }

}
