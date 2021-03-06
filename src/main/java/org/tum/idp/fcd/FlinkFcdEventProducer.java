package org.tum.idp.fcd;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;

/*
 * Flink producer task to fetch data from HERE traffic flow API in every 1 minute
 * and sink them in to Kafka topic called "fcd-messages"
 */
public class FlinkFcdEventProducer {

    private static final String KAFKA_BROKER = "kafka:9092";

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        if (params.getNumberOfParameters() < 2) {
            throw new IllegalArgumentException("Must have either 'appid' or 'appcode' or 'bbox' as first argument. \n");
        }

        String appId = params.getRequired(Constants.APP_ID_PARAM_NAME);
        String appCode = params.getRequired(Constants.APP_CODE_PARAM_NAME);
        String bboxInput = params.get(Constants.BBOX_PARAM_NAME);
        String bbox = (bboxInput == null) ? "48.160250,11.551678;48.159462,11.558652" : bboxInput;

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        DataStream<FcdEvent> eventStream = env.addSource(new FcdEventSource("https://traffic.cit.api.here.com/traffic/6.2/flow.json?app_id=" + appId + "&app_code=" + appCode + "&bbox=" + bbox));
        //TODO:appid and appcde and bbx coordinates input and tmc data ingestion
        //taxiEventStream.print();
        FlinkKafkaProducer010<FcdEvent> producer = new FlinkKafkaProducer010<>(
                KAFKA_BROKER,
                Constants.KAFKA_TOPIC_PARAM_VALUE,
                new FcdEventSchema());
        eventStream.addSink(producer);
        env.execute("Ingestion of HERE FCD Traffic Flow Data");
    }
}
