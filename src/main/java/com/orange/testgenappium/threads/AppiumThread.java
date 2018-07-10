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
package com.orange.testgenappium.threads;

import com.orange.testgenappium.model.Device;
import com.orange.testgenappium.utility.Tools;
import com.orange.testgenappium.launcher;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Create an appium server as a thread with device configuration. You can kill
 * node processes to kill appium servers that might still running.  
 * TODO : kill appium servers better ...
 * @author bastienjalbert
 */
public class AppiumThread implements Runnable {

    // attached device to the appiumserver
    private final Device device;

    public AppiumThread(Device device) {
        this.device = device;
    }

    @Override
    public void run() {
        try { 
            Process p = null;
            // start an appium server with the device target port and add 
            // a bootstrap port initialized with target port + 1000
            ProcessBuilder pb = new ProcessBuilder("appium", "-p", device.getAppiumPort(), "-bp", device.getAppiumBpPort());

            // ensuring no problem with proxy or cntlm
            Map<String, String> env = pb.environment();
            env.put("no_proxy", "127.0.0.1, localhost, 0.0.0.0");
            
            // ensure process will run into runnner directory
            pb.directory(new File(launcher.PATH_TO_TESTS));

            p = pb.start();

            // some information about the server
            System.out.println("INFO : Appium server started on 127.0.0.1:" + device.getAppiumPort());

        } catch (Exception ex) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error appium server start : ", ex.getMessage()));
        }

    }

    /**
     * Close all appium processes (node processes in real life...) 
     * TODO find a more proper way to kill appium processes without killing all node 
     * processes...
     */
    public static void killallAppiumNode() {
        try {
            Process p = null;
            ProcessBuilder pb = new ProcessBuilder("killall", "node");  
            p = pb.start();
        } catch (Exception ex) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error when attempting to close appium servers (node process) : ", ex.getMessage()));
        }
    }

}
