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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date; 
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


/**
 * Main class of the launcher. Make a workspace, start // tests, copy right files 
 * and then make a report of all of this.
 * @author bastienjalbert
 * @version 0.1
 */
public class launcher {

    ///////////////// CONFIGURATIONS VARIABLES ////////////////////////////////
    //// Some var here are shared with other classes (like PabotThread) ///////
    
    // path to the robotframework workspace
    protected static String PATH_TO_TESTS;

    // Workspace where test will run (contains devices_conf folder)
    protected static String RUNNER_PATH;
    
    // Workspace where tests will run be temporally stored before reporting
    protected static String WORKING_PATH;
            
    // Contains all valuesXYZ.dat file (devices configuration)
    protected static String CONF_PATH;

    // Results output directory 
    protected static String OUTPUT_PATH;

    // Log file path
    protected static String LOG_FILE_PATH;
     
    // Directory path to appium screenshot for reports
    protected static String IMG_PATH;
  
    // general test name
    protected static String TESTS_NAME = "Default-Test";

    ///////////////////////////////////////////////////////////////////////////
    /**
     * Launcher for tests
     *
     * @param args see the list below
     */
    public static void main(String[] args) throws Exception {

        // prepare to get arguments.
        final Options options = configParameters();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine line = parser.parse(configParameters(), args);

        // configuring future test by parameters
        // start all tests of a directory
        if (line.hasOption("directory")) {
            PATH_TO_TESTS = line.getOptionValue("directory");
        } else if (line.hasOption("file")) { // or a signle robot test file
            PATH_TO_TESTS = System.getProperty("user.dir");
        } else { // bad args
            showUsage();
            System.exit(21);
        }
        
        // set the final test name if it exists
        if (line.hasOption("testname")) {
            TESTS_NAME = line.getOptionValue("testname");
        }

        // Set all working paths and clear old test reports in workspace
        Tools.getWorkingDir();
        
        // all configurations about devices
        ArrayList<Device> devices_conf = Tools.getDevicesDat();
        
        // if there are not configuration about devices, stop execution...
        if(devices_conf.size() <= 0) {
            System.out.println("You have to set some Android device configuration "
                    + "file into runner/devices_conf directory.\n"
                    + "Please read documentation.");
            System.exit(15);
        }

        // list of (future) appium server processes
        ArrayList<Thread> appium_threads = new ArrayList<>();
 
        // list of path to robot test files
        ArrayList<String> tests_suites = new ArrayList<>();

        // stop already running appium servers (if there are)
        AppiumThread.killallAppiumNode();

        /**
         * start one appium server for each device
         */
        for (Device device : devices_conf) {
            AppiumThread appium_server = new AppiumThread(device);
            Thread t = new Thread(appium_server);
            t.start();
            appium_threads.add(t);
        }

        // ensuring all appium servers are ready before running any test ... 
        Thread.sleep(5000);

        // prepare files to begin tests (populate the list of test file name)
        if (line.hasOption("directory")) {
            tests_suites = Tools.getRobotFrameworkTestFiles(PATH_TO_TESTS);
        } else { // just a file
            tests_suites.add(line.getOptionValue("file"));
        }
        
        /**
         * start tests
         */
        try { 
            // executing all test
            for (String oneTestFile : tests_suites) { 
                 
                // start one test suite on all devices
                Runnable pabot = new PabotThread(devices_conf, oneTestFile);
                Thread pabotLauncher = new Thread(pabot);
                pabotLauncher.start();
                // wait this test to finish before start another (new iteration)
                pabotLauncher.join();
                
                // when the test is terminated
                // copying and renamming each outputx.xml files 
                Tools.preparingOutputsToTmp(devices_conf, Tools.getOnlyTestNameFromFile(oneTestFile));
                 
            } 
            
            /* Creating final output.xml and report/log html files by aggregating *
             * all tests results of devices. Tell to rebot if it should format    *
             * output for jenkins or not (if argument is present is command line) */
            Runnable rebot = new RebotThread(devices_conf, tests_suites, line.hasOption("jenkins"));
            Thread rebotLauncher = new Thread(rebot);
            rebotLauncher.start();
            // wait to rebot finish before to continue
            rebotLauncher.join(); 

        } catch (Exception ex) {
            String date = new Date().toString();
            Tools.writeLog(Arrays.asList(date, "Error on test execution : ", ex.getLocalizedMessage()));
        } 
        // close all appium servers
        for (Thread appium : appium_threads) {
            appium.interrupt();
        } 
    }

    /**
     * Set all parameters options
     *
     * @return
     */
    private static Options configParameters() {

        final Option directory = Option.builder("d")
                .longOpt("directory") //
                .desc("Path that contains .robot files")
                .hasArg(true)
                .argName("dir")
                .required(false)
                .build();

        final Option file = Option.builder("f")
                .longOpt("file") //
                .desc("Path to one .robot file")
                .hasArg(true)
                .argName("file")
                .required(false)
                .build();

        final Option testname = Option.builder("t")
                .longOpt("testname")
                .desc("Final test name show in report/log")
                .hasArg(true)
                .required(false)
                .build();
        
        final Option jenkins = Option.builder("j")
                .longOpt("jenkins")
                .desc("If you're running test with jenkins specify it. Reporting behaviour are not the sames.")
                .hasArg(false)
                .required(false) 
                .build();
        
        final Option verbose = Option.builder("v")
                .longOpt("verbose")
                .desc("Show processes output and more information, be verbose.")
                .hasArg(false)
                .required(false)
                .build();

        final Options options = new Options();

        options.addOption(directory);
        options.addOption(file);
        options.addOption(testname);
        options.addOption(jenkins);
       options.addOption(verbose);

        return options;
    }

    public static void showUsage() {
        System.out.println("------------------------------------------------");
        System.out.println("Robot framework Android automatic parallel test runner.");
        System.out.println("");
        System.out.println("USAGE : java -jar [-d </path/robot/files/> or -f </path/to/one/robot/file.robot>] [-l <lang>]");
        System.out.println("-d,--directory             Select a folder that contains robot test files.");
        System.out.println("-f,--file                  Directory or file args, but not both, here just select one robot test file.");
        System.out.println("-t,--testname              Choose the final test name.");
        System.out.println("-j,--jenkins               Specify if you're running test with jenkins or locally.");
        System.out.println("-v,--verbose               Show more output from processes (pabot, rebot, ...). More verbose.");
        System.out.println("------------------------------------------------");

    }

}
