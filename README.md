# Android Parallel Tests Runner (APTR)
 
[![Version](https://img.shields.io/badge/version-0.1-brightgreen.svg)](https://pypi.python.org/pypi/robotframework-pabot)

A parallel executor for [Robot Framework](http://www.robotframework.org) tests destinate to Android devices. With APTR you can run tests on multiples devices in parallel and get report/log/screenshots correctly. 

![APTR Flow](https://i.imgur.com/oSFC74Z.jpg)

When I firstly discovered Pabot I was very excited. But there were problems when I tried to run my android device tests simultaneously. Sometimes sessions were override, or reports were not created correctly.
APTR is a tool that I did to simplify at maximum the parallel execution of android tests. It used a custom pabot version to get screenshots correctly.


## Installation:

You can get the compiled jar from release : 

Or build you own jar from the project

    cd /path/to/git/clone
    mvn clean compile assembly:single

## Dependencies:

APTR need a custom pabot version to run correctly. So at startup APTR will looks for a folder called "pabot"
which should contains pabot.py, pabotlib.py, ... "By chance" APTR do it automatically by cloning
my custom pabot repository. 
If the repo is already cloned, you can still force the upgrade by using --forceupdate argument.
 

## Configuration before running tests

### Workspace hierarchy

APTR needs some files and folder to run, please consider create them in your project to begin.
Create a "runner" directory into your workspace and follow this graph.

WORKSPACE_DIR

    ├── TestFileSuite1.robot  
    ├── TestFileSuite2.robot  
    ├── ....................  
    ├── runner   
    │   ├── devices_conf  
    │   │   ├── device1.dat  
    │   │   ├── .........     
    │   │   └── nexus.dat  
    │   └── output  

Note : the output dir is cleaned automatically when you run another test suite.


### Android configuration files (.dat)

All files into devices_conf directory are variable files passed to pabot. APTR uses --argumentfileX from pabot to run tests.  
So they must contain some information about devices. Minimum requirement are simple. See the example file bellow :

device1.dat

    --variable udid:3bc45f49 (necessary)   
    --variable name:Device1_s6 (necessary)  
    --variable appium:4470 (necessary => indicate on which port appium server will run for this device [localhost:PORT])  
    --variable appiumbp:9055 (necessary => indication bootstrap port for appium) 


Theses 4 variables have to be defined in .dat files.

If you need to pass other arguments to pabot/pybot, you can specify them in the .dat file.  
[Here you can see all possibilities.](http://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#argument-files)

**Notice that all these variables are accessible from test suites (${udid}, ${appium}, ...)**

## Command-line options

APTR has been created to simplify at maximum the parallelization. 

--directory (-d)     
  Specify the workspace directory (which contains .robot files and runner directory)

--file (-f)    
  Specify only one test file to run, you need to be on workspace before running -f
  
--testname (-t)
  Specify a general test name. It will be displayed into the final report/log

--forceupdate (-force)
  Force pabot update by deleting current directory, and cloning again from git. By default APTR uses a custom pabot version directly from a pabot directory, at same location of .jar.
  
--jenkins (-j)
  If you want to run test on jenkins you should indicate it. The output is formated specially for jenkins or for local execution. Please refer to jenkins section for more information.
  
--verbose (-v)
  Specify if you want to see processes output and add some verbose output.

## Running and examples:

Once you have 
Example usages:

*Run only one test*  

    user$ cd /path/to/robot/workspace/ 
    user$ java -jar APTR-0.1.jar --file Test_Suite.robot

*Run all tests from a directory (workspace)*

    user$ java -jar APTR-0.1.jar -d /path/to/robot/workspace

*Run all tests from a directory (workspace) and ensuring to have last custom pabot version*

    user$ java -jar APTR-0.1.jar -d /path/to/robot/workspace --forceupdate
 
## Jenkins, and configuration

![Basic jenkins configuration](https://i.imgur.com/TXoNSgH.png)

## Contributing to the project

There are several ways you can help in improving this tool:

   - Report an issue or an improvement idea to the [issue tracker](https://github.com/bastienjalbert/aptr/issues) 
   - Giving me a tip (bitcoin cash : 14b2UMY1kCnhPDRbegTEg3zATQapCoswkf)
