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
 
import static com.orange.testgenappium.Tools.getFileExtension;
import static com.orange.testgenappium.launcher.IMG_PATH;
import static com.orange.testgenappium.launcher.OUTPUT_PATH; 
import static com.orange.testgenappium.launcher.WORKING_PATH;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Running rebot process at the end of tests execution. This class provide only
 * a "run" function that do everything to obtain a final output.xml file and
 * the report.html and log.html files.
 * @author bastienjalbert
 */
public class RebotThread implements Runnable {
    
    // attached device to the appiumserver
    private final ArrayList<Device> devices; 

    // robot tests suite
    private final ArrayList<String> test_suites;
    
    // are we creating output for jenkins or only local reporting
    private final boolean jenkins;
    
    
    // create a rebot thread with informations (devices list, tests name list)
    /**
     * Initialize a Rebot thread to create final output/report/log with all executed
     * tests. 
     * @param devices the list of devices where tests has been executed
     * @param test_suites the list of executed tests
     * @param jenkins final output have to be formatted for jenkins (true) or not (false)
     */
    public RebotThread(ArrayList<Device> devices, ArrayList<String> test_suites, 
                       boolean jenkins) { 
        this.devices = devices;
        this.test_suites = test_suites;
        this.jenkins = jenkins;
    }
  
    /**
     * We generate output file for each devices (indexed by number) and then we 
     * merge all output in only one file (and create report/log files)
     */
    @Override
    public void run() {
        try { 
            
            // index of current device
            int x = 0;
            
            // generating one output for each devices
            for(Device device : devices) {

                // prepare arguments for multiple device
                ArrayList<String> rebotArgs = new ArrayList<>();

                // basic process info to start 
                rebotArgs.add("rebot");  
                
                // set the report name with test name from launcher config
                rebotArgs.add("--name");
                rebotArgs.add(device.getName());

                rebotArgs.add("-o");
                rebotArgs.add("output." + device.getUdid()+ ".xml");

                rebotArgs.add("--log");
                rebotArgs.add("NONE");
                
                rebotArgs.add("--report");
                rebotArgs.add("NONE");

                // add all test name to generate one output for all test of the same di
                for(String oneTestName : test_suites) {
                    rebotArgs.add("output" + x +  "." + Tools.getOnlyTestNameFromFile(oneTestName) + ".xml");
                } 
                
                // prepare the process with all our args
                Process p;
                ProcessBuilder pb = new ProcessBuilder(rebotArgs);

                // ensure process will run into working directory (here is all our output files)
                pb.directory(new File(launcher.WORKING_PATH));

                // ensuring no problems with proxy or cntlm
                Map<String, String> env = pb.environment();
                env.put("no_proxy", "127.0.0.1, localhost, 0.0.0.0");

                // redirect all stream from future pabot process
                pb.redirectErrorStream(true);

                // start test execution
                p = pb.start(); 

               // show pabot output (stdout)
                p.getOutputStream().flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

                // capture line from standard output from kal execution
                String line = new String();
                while ((line = reader.readLine()) != null) { 
                    System.out.println("REBOT OUTPUT : " + line); 
                    System.out.flush();
                }

                p.destroy();
                p.destroyForcibly();
                
                // pass to the outputX++.testName.xml next file
                x++;
            }
            
            
            //merge all output in only one file
            
             // prepare arguments for multiple device
            ArrayList<String> rebotArgs = new ArrayList<>();

            // basic process info to start 
            rebotArgs.add("rebot");  
            
            // set the report name with test name from launcher config
            rebotArgs.add("--name");
            rebotArgs.add(launcher.TESTS_NAME);
             
            rebotArgs.add("-o");
            rebotArgs.add("output-final.xml");
            
            rebotArgs.add("--report");
            rebotArgs.add("report.html");
            
            rebotArgs.add("--log");
            rebotArgs.add("log.html");
            
            // give to rebot all output files of each devices
            for(Device device : devices) {
                rebotArgs.add("output." + device.getUdid() + ".xml");
            }
             
            // prepare the process with all our args
            Process p;
            ProcessBuilder pb = new ProcessBuilder(rebotArgs);

            // ensure process will run into runnner directory
            pb.directory(new File(launcher.WORKING_PATH));

            // ensuring no problems with proxy or cntlm
            Map<String, String> env = pb.environment();
            env.put("no_proxy", "127.0.0.1, localhost, 0.0.0.0");
            
            // redirect all stream from future pabot process
            pb.redirectErrorStream(true);
            
            // start test execution
            p = pb.start(); 
 
            // show pabot output (stdout)
            p.getOutputStream().flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            // capture line from standard output from kal execution
            String line = new String();
            while ((line = reader.readLine()) != null) { 
                System.out.println("REBOT OUTPUT : " + line); 
                System.out.flush();
            }

            p.destroy();
            p.destroyForcibly();
                
            // if we executed tests locally (<=> means not on jenkins typically)
            if(!jenkins) {
                // create the final output directory
                final String FINAL_OUTPUT = OUTPUT_PATH + "/final";
                if(!(new File(FINAL_OUTPUT).exists())) {
                    new File(FINAL_OUTPUT).mkdir();
                } 
                
                // move the log and report html files to final directory 
                File logFile = new File(WORKING_PATH + "/log.html");
                logFile.renameTo(new File(FINAL_OUTPUT + "/log.html"));
                logFile.delete();
                File reportFile = new File(WORKING_PATH + "/report.html");
                reportFile.renameTo(new File(FINAL_OUTPUT + "/report.html"));
                reportFile.delete();
               
                // and finally copy all screenshots to the final directory
                // open the directory and get files into
                File folder = new File(IMG_PATH);
                File[] listOfFiles = folder.listFiles();

                for (int i = 0; i < listOfFiles.length; i++) { 
                    // we verify that the file is an image (.png)
                    if (listOfFiles[i].isFile() 
                            && getFileExtension(listOfFiles[i]).equals("png")) { 
                        // moving the image
                        listOfFiles[i].renameTo(new File(FINAL_OUTPUT + "/" + listOfFiles[i].getName())); 
                    } 
                }
                
                System.out.println("You can access to the FINAL report/log file at");
                System.out.println("Log:     " + new File(FINAL_OUTPUT).getAbsolutePath() + "/log.html");
                System.out.println("Report:  " + new File(FINAL_OUTPUT).getAbsolutePath() + "/report.html");
                
            }
  

        } catch (Exception ex) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error appium server start : ", ex.getMessage()));
        }

    }
 

}
