# Distributed MQTT+ Java Server
This is a brief guide to guide the users in the depoyment and the installation of a complete Distributed MQTT+ broker.
## Deployment instructions:
Prerequisites: Java, a C compiler, Make.
The broker is made up by two components:
1. A Java Server, available by cloning this repository.
2. A modified version of the Mosquitto MQTT broker, available [here](https://github.com/LeoStaglia/mosquitto) able to support the communication with the server.
Both have to be installed on the same physical machine: the Java Server is deployed throgh a JAR file that you can find following the path out/artifacts/MQTTPLUSWS_jar, while the Mosquitto broker can be compiled using make.
## Startup instructions:
It is recommended to start the Java server as first component of the broker, it can be done by using this command:
```
java -jar MQTTPLUSWS.jar *server_port* *broker_port* *distributed_flag*
```
As can be seen there are three mandatory arguments: the number of the port used by the Server to communicate with the broker, the number of the port the broker is using to communicate thorugh the MQTT protocol and a flag that enable or disable the distributed mode for the Java Server.

Upon the startup of the Java Server the Mosquitto broker can be started using the following command
```
mosquitto -p *broker_port* -ws *server_port* -distr *distributed_flag*
```
The three parameters have exactly the same meaning of the ones used to launch the JAR.

  
  
  
