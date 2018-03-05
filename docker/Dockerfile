FROM wangzhen263/image_ojdk8_mvn3:0.0.1

# Define working directory.
WORKDIR /data

RUN mkdir -p /opt/ER/Baseline/
COPY target/Baseline-1.3.3-jar-with-dependencies.jar /opt/ER/Baseline/
ADD datasets/ACM_DBLP /opt/ER/Baseline/datasets/ACM_DBLP

EXPOSE 7000

CMD java -jar /opt/ER/Baseline/Baseline-1.3.3-jar-with-dependencies.jar -r  3>&1 1>>/var/log/er.log 2>&1

