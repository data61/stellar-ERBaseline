FROM ubuntu:16.04

RUN apt-get update && \
    apt-get install -y software-properties-common python-software-properties

# Install Java.
RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# Define working directory.
WORKDIR /data

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

RUN mkdir -p /opt/ER/Baseline/
COPY target/Baseline-1.3.3-jar-with-dependencies.jar /opt/ER/Baseline/
ADD datasets/ACM_DBLP /opt/ER/Baseline/datasets/ACM_DBLP

EXPOSE 7000

CMD java -jar /opt/ER/Baseline/Baseline-1.3.3-jar-with-dependencies.jar -r  3>&1 1>>/var/log/er.log 2>&1

