package org.tum.idp.fcd;

import com.google.common.io.Resources;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSink;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch2.RequestIndexer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.util.Collector;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

/*
 * Flink consumer task to subscribe to topic called "fcd-messages" and aggregate them in given window time and
 * calculate the average speed and sink in to elastic search
 */
public class FlinkFcdEventConsumer {

    private static final int MAX_EVENT_DELAY = 60;

    public static void main(String[] args) throws Exception {

        ParameterTool params = ParameterTool.fromArgs(args);
        if (params.getNumberOfParameters() < 1) {
            throw new IllegalArgumentException("Missing window argument. \n");
        }
        int timeWindow = params.getInt(Constants.TIME_WINDOW_PARAM_NAME);

        Properties properties;

        try (InputStream props = Resources.getResource("consumer.props").openStream()) {
            properties = new Properties();
            properties.load(props);

        }

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // create a Kafka consumer
        FlinkKafkaConsumer010<FcdEvent> consumer = new FlinkKafkaConsumer010<>(
                Constants.KAFKA_TOPIC_PARAM_VALUE,
                new FcdEventSchema(),
                properties);

        // assign a timestamp extractor to the consumer
        consumer.assignTimestampsAndWatermarks(new FcdEventTSExtractor());

        // create a FCD event data stream
        DataStream<FcdEvent> events = env.addSource(consumer);

        KeyedStream<FcdEvent, Integer> keyedEdits = events
                .keyBy(new KeySelector<FcdEvent, Integer>() {
                    @Override
                    public Integer getKey(FcdEvent event) {
                        return event.getTmcCode();
                    }
                });

        DataStream<Tuple5<Integer, Double, Double, Double, String>> result = keyedEdits
                .timeWindow(Time.minutes(timeWindow))
                .apply(new CalculateAverage());

        //result.print();

        // stores the data in Elasticsearch
        saveFcdEventDataToES(result);

        env.execute("Read Realtime FCD Data from Kafka");

    }


    /**
     * Assigns timestamps to FCD Taxi records. TODO:watermark handling
     */
    public static class FcdEventTSExtractor extends BoundedOutOfOrdernessTimestampExtractor<FcdEvent> {

        public FcdEventTSExtractor() {
            super(Time.seconds(MAX_EVENT_DELAY));
        }

        @Override
        public long extractTimestamp(FcdEvent event) {
            return event.getTimestamp();
        }
    }

    /**
     * Calculate the average speed..
     */
    public static class CalculateAverage implements WindowFunction<
            FcdEvent,
            Tuple5<Integer, Double, Double, Double, String>,
            Integer,
            TimeWindow> {
        private static transient DateTimeFormatter timeFormatter =
                DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");

        @SuppressWarnings("unchecked")
        @Override
        public void apply(
                Integer key,
                TimeWindow window,
                Iterable<FcdEvent> events,
                Collector<Tuple5<Integer, Double, Double, Double, String>> out) {

            double sum = 0.0;
            int count = 0;
            double avg = 0.0;
            for (FcdEvent event : events) {
                sum += event.getSpeed();
                count++;
            }

            if (count > 0) {
                avg = sum / count;
            }
            FcdEvent event = events.iterator().next();
            out.collect(new Tuple5<>(event.getTmcCode(), event.getLat(), event.getLon(), avg, timeFormatter.print(window.getEnd())));

        }
    }

    /**
     * Stores the data in Elasticsearch
     */
    private static void saveFcdEventDataToES(
            DataStream<Tuple5<Integer, Double, Double, Double, String>> inputStream) throws UnknownHostException {
        Map<String, String> config = new HashMap<>();
        config.put("bulk.flush.max.actions", "1");
        config.put("cluster.name", "elasticsearch");

        List<InetSocketAddress> transportAddresses = new ArrayList<>();
        //transportAddresses.add(new InetSocketAddress(InetAddress.getByName("localhost"), 9300));
        transportAddresses.add(new InetSocketAddress(InetAddress.getByName("elasticsearch"), 9300));

        inputStream.addSink(new ElasticsearchSink<Tuple5<Integer, Double, Double, Double, String>>(config, transportAddresses,
                new ElasticsearchSinkFunction<Tuple5<Integer, Double, Double, Double, String>>() {

                    public IndexRequest createIndexRequest(Tuple5<Integer, Double, Double, Double, String> record) {
                        Map<String, Object> json = new HashMap<>();
                        json.put("tmc_code", record.f0);
                        json.put("location", record.f1.toString() + "," + record.f2.toString()); // lat,lon
                        json.put("speed", record.f3);
                        json.put("timestamp", record.f4);

                        return Requests.indexRequest().index("munich").type("floating-cars").source(json);
                    }

                    @Override
                    public void process(Tuple5<Integer, Double, Double, Double, String> record, RuntimeContext ctx,
                                        RequestIndexer indexer) {
                        indexer.add(createIndexRequest(record));

                    }
                }));
    }
}
