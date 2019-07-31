@ECHO OFF

:: Variables used
SET ADDITIONAL_OPTS=-server
SET CLASSPATH=-classpath .\lib\*;.\config
SET MAIN_CLASS=com.intel.rfid.controller.Main

:: Run the VM
java %_JAVA_OPTIONS% %ADDITIONAL_OPTS% %CLASSPATH% %MAIN_CLASS%
