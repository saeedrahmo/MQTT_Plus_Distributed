#! /bin/bash

echo "Executing the JAR ..."

java -jar /home/mqttplus/MQTT_Plus_Distributed/out/artifacts/MQTTPlusWS_jar/MQTTPlusWS.jar "${WS_PORT}" "${BROKER_PORT}" "${DISTRIBUTED_FLAG}"  "${TOPOLOGY}" "${CLUSTER_SIZE}" "${LOCAL_FLAG}" &

sleep 3

echo "Executing mosquitto-ST+ ..."

mosquitto -v -c /mosquitto/config/broker"${BROKER_NUM}".conf -ws "${WS_PORT}" -distr "${DISTRIBUTED_FLAG}"

exit 1
