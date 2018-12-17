# Sensor NFC Application

The Sensor NFC Application Reference Design is one component of the Intel® RSP SW Toolkit.  This application is used to program security credentials into the Intel® RFID Sensor Platform (Intel® RSP).  The features included in this reference design are intended to provide the minimum functionality required to program the Intel® RSP.

**_THIS SOFTWARE IS NOT INTENDED TO BE A MARKET READY SOLUTION._**

#### Development Environment
The application was developed with with [Android Studio](https://developer.android.com/studio/index.html). Installing Android Studio will install additional dependencies such as Gradle support and the Android SDK. As of the time of writing this README, the app targeted Android 8.0 (Oreo) which is version 26. Check the [Android Manifest](app/src/main/AndroidManifest.xml) to figure out which versions of the Android SDK to install.

#### Security Credentials
The security credentials for a sensor consist of a root CA certificate and a provisioning token. With these items a sensor and gateway can mutually authenticate when connecting. securely provision the sensor should be available. Generating the credentials is a function of the [Intel® RFID Sensor Platform Gateway](../gateway). If these files have not been generated, please see the Gateway project for more details.


#### Getting Started

* Clone this project
* Install Android Studio
* Open Studio and **_Import_** this project
* Connect an Android device via USB
* Copy the security credentials to the devices download folder
  * Recommend using Android Studio's Device File Explorer or any other file transfer
  * Certificates must have a .crt file extension
  * Provisioning tokens must have token in the filename and have a .json file extension
* Build and Run the app module on the device using Android Studio
  * Menu -> Run -> Run 'app' 


