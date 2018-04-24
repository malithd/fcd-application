package org.tum.idp.fcd;

import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;

public class FlinkFcdEventProducer {

    private static final String KAFKA_BROKER = "localhost:9092";

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        DataStream<FcdEvent> eventStream = env.addSource(new FcdEventSource("https://traffic.cit.api.here.com/traffic/6.2/flow.json?app_id=foo&app_code=bar&bbox=48.160250,11.551678;48.159462,11.558652"));
        //TODO:appid and appcde and bbx coordinates input and tmc data ingestion
        //taxiEventStream.print();
        FlinkKafkaProducer010<FcdEvent> producer = new FlinkKafkaProducer010<>(
                KAFKA_BROKER,
                "fcd-messages",
                new FcdEventSchema());
        eventStream.addSink(producer);
        env.execute("Ingestion of HERE FCD Traffic Flow Data");
    }
}
