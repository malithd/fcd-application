FROM bde2020/flink-maven-template:1.4.0-hadoop2.7

ENV FLINK_APPLICATION_JAR_NAME tum-idp-fcd-0.1-jar-with-dependencies
ENV FLINK_APPLICATION_MAIN_CLASS org.tum.idp.fcd.FlinkFcdEventProducer
ENV FLINK_APPLICATION_ARGS "--app_id *** --app_code ***  --bbox '48.160250,11.551678;48.159462,11.558652'"
