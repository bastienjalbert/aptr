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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Create pabot thread to execute tests on multiple devices. Only execute ONE test
 * on each devices, we're doing something like "execute all tests on all devices).
 * 
 * Pabot can launch a test on an android device that is already executing another test
 * if we try to "execute all tests on all devices". Even if we limit processes with args
 * This is why I chose the "execute one test on all devices".
 * 
 * @author bastienjalbert
 */
public class PabotThread implements Runnable {

    // attached devices to the appiumserver
    private final ArrayList<Device> devices; 

    // robot test name
    private final String robotName;

    public PabotThread(ArrayList<Device> device, String robotName) {
        this.devices = device; 
        this.robotName = robotName;
    }

    @Override
    public void run() {
        try {

            // prepare arguments for multiple device
            ArrayList<String> pabotArgs = new ArrayList<>();

            // basic process info to start 
            pabotArgs.add("python");
            pabotArgs.add("-m");
            pabotArgs.add("pabot.pabot");
            
            pabotArgs.add("--verbose");
            
            // ensuring to run just same number of // test as number of device
            // so there are never two (or more) tests launch on one device
            pabotArgs.add("--processes");
            pabotArgs.add(String.valueOf(devices.size()));
            
            pabotArgs.add("--pabotlib");
             
            // argumentfileX where X the index (represented with var i)
            int i = -1;
            for (Device oneDevice : devices) {
                pabotArgs.add("--argumentfile" + ++i); 
                pabotArgs.add(oneDevice.getConfFilePath());
            }
 
            // output results path of pabot
            pabotArgs.add("--outputdir");
            pabotArgs.add(launcher.OUTPUT_PATH);           

            // and last step, indicate which robot file to execute
            pabotArgs.add(robotName);  
            
            // prepare the process with all our args
            Process p;
            ProcessBuilder pb = new ProcessBuilder(pabotArgs);

            // ensure process will run into runnner directory
            pb.directory(new File(launcher.PATH_TO_TESTS));

            // ensuring no problems with proxy or cntlm
            Map<String, String> env = pb.environment();
            env.put("no_proxy", "127.0.0.1, localhost, 0.0.0.0");
            
            // redirect all stream from future pabot process
            pb.redirectErrorStream(true);
            
            // start test execution
            p = pb.start(); 
            
            System.out.println("INSTRUC  - " + pabotArgs);
            
            // print information before thread execution ... 
            System.out.println("INFO : Test execution of " + robotName + " started for devices :");
            for (Device oneDevice : devices) {
                System.out.println("       - " + oneDevice.getName());
            }

            // show pabot output (stdout)
            p.getOutputStream().flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            // capture line from standard output from kal execution
            String line = new String();
            while ((line = reader.readLine()) != null) { 
                System.out.println("PABOT OUTPUT : " + line); 
                System.out.flush();
            }
            
            p.destroy();
            p.destroyForcibly();

        } catch (Exception ex) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error appium server start : ", ex.getMessage()));
        }

    }

}
