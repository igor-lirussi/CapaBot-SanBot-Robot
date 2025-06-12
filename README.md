# CapaBot, A Sanbot Robot Project for Interaction

The code aims to give Sanbot robot basic interaction abilities. It wanders around avoiding obstacles, going back to the charging station when needed. The robot turns towards sources of noise, or voices, so its attention can be called.
During the movement. a face detection module is used to trigger a voice interaction. (it includes a speech recognition module, a conversational engine to formulate the answers and the speech synthesis). The dialogue can be purpose-less, this means that no task is pursued except a normal and pleasant interaction. It is capable also of a task-oriented interaction, implemented in many example modules. Some allow Sanbot to present itself, project a story (of the organization) show the events in a synced calendar, shake hands, tell the weather, give directions, save suggestions from the customers, display web pages for info.

#### You can find a more in deep discussion [here](https://amslaurea.unibo.it/19120/)

<!--
## Video of the Result
[![Sanbot Interaction](http://i3.ytimg.com/)](https://youtu.be/)
-->

## Pipeline Implemented
![Alt text](readme-images/Pipeline.jpg?raw=true "Pipeline")

## Install App and/or Code Repo
Robot has to be in Developer Mode, press the Light-Button on the head 7 times, in the Settings>About, tap on "Robot Version" till "Developer mode open". Connect the robot to the computer with the usb cable.
- You can upload in the robot the already [compiled apk](https://github.com/igor-lirussi/CapaBot-SanBot-Robot/releases/latest) with  `adb install –r –d CapaBot-SanBot-Robot-debug.apk` 
- To build it yourself from the code, clone the repository. <br>
Android studio is strongly suggested to open this project, the tablet in the robot runs with Android. <br>
Connect the robot to the computer with the cable, press the green play button :arrow_forward: this allows Android Studio, once compiled the code of the project, to upload the app in the robot.

## Run App from the Robot
After the first installation, the app will be available in the section “APP Market” -> ”Come into my life” -> “Purchased APP”


## Built With

*   Java
*   Sanbot OpenSDK
*   icalendar 4 java 3.0.0
*   Ab AIML engine

## Changelog
<br>**Version 1.0.0** - Initial release
<br>**Version 2.4.0** - added calendar and autonomously charge
<br>**Version 2.6.9** - added web activity
<br>**Version 3.0.3** - added AIML conversational engine
<br>**Version 3.0.8** - final Thesis version
<br>**Version 4.0.0** - fixed AIML and AIML conversational engine
<br>**Version 5.0.0** - passed at the new SDK 2.0.1.10

## Authors

*   **Igor Lirussi** @ ISR Institute for System and Robotics - University of Lisbon (PT)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details

## Acknowledgments
*   ALICE AI Foundation and Dr. Richard S. Wallace. - for the AIML engine
