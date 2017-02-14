FROM openjdk:8-jdk-alpine

RUN apk add --no-cache --quiet bash curl 

ENV NEO4J_TARBALL neo4j-community-3.1.1-unix.tar.gz
ARG NEO4J_URI=http://dist.neo4j.org/neo4j-community-3.1.1-unix.tar.gz

#RUN curl --fail --silent --show-error --location --remote-name ${NEO4J_URI} \
#    && tar --extract --file ${NEO4J_TARBALL} --directory /var/lib \
#    && mv /var/lib/neo4j-* /var/lib/neo4j \
#    && rm ${NEO4J_TARBALL}

COPY build/neo4j-community-3.1.1-unix.tar.gz /tmp/

RUN [ -f /tmp/${NEO4J_TARBALL} ] || curl --fail --silent --show-error --location --remote-name ${NEO4J_URI} -o /tmp/${NEO4J_TARBALL}

RUN mkdir -p /var/lib/mercator/lib

RUN cd /var/lib && tar --extract --file /tmp/${NEO4J_TARBALL} && mv /var/lib/neo4j-* /var/lib/neo4j 

COPY *.sh /
COPY build/libs/mercator*-all.jar /var/lib/mercator/lib

RUN chmod +x /*.sh

WORKDIR /var/lib/neo4j


RUN mv data /data \
    && ln -s /data && rm /tmp/*.gz

VOLUME /data

EXPOSE 7474 7473 7687

CMD ["/docker-init.sh"]
