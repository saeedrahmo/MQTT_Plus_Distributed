FROM openjdk:11

ENV WS_PORT 8080
ENV BROKER_PORT 1883
ENV DISTRIBUTED_FLAG true
ENV TOPOLOGY tree
ENV CLUSTER_SIZE 7
ENV BROKER_NUM 1
ENV LOCAL_FLAG false
ENV IP_ADDR 172.0.0.2

RUN apt-get update \ 
    && apt-get install -y --no-install-recommends cmake \
    protobuf-compiler \
    build-essential \
    libssl-dev \
    libcurl4-openssl-dev \
    xsltproc \
    docbook-xsl \
    net-tools \
    iputils-ping \
    iproute2 


RUN git clone https://github.com/LeoStaglia/mosquitto.git \
    && cd /mosquitto \
    && cmake . \
    && truncate -s-1  src/CMakeFiles/mosquitto.dir/link.txt \
    && echo "-lcurl" >> src/CMakeFiles/mosquitto.dir/link.txt \
    && make install


WORKDIR /home/mqttplus/

RUN git clone https://github.com/LeoStaglia/MQTT_Plus_Distributed.git

COPY start.sh /usr/bin/
RUN chmod +x /usr/bin/start.sh 

CMD ["/usr/bin/start.sh"]
