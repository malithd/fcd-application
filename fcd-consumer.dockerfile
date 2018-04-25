FROM bde2020/flink-maven-template:1.4.0-hadoop2.7

ENV FLINK_APPLICATION_JAR_NAME tum-idp-fcd-0.1-jar-with-dependencies
ENV FLINK_APPLICATION_MAIN_CLASS org.tum.idp.fcd.FlinkFcdEventConsumer