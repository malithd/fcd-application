package org.tum.idp.fcd;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FcdEventSource implements SourceFunction<FcdEvent> {
    private static final Logger log = LoggerFactory.getLogger(FcdEventSource.class);
    private final String url;

    private transient HttpURLConnection con = null;
    private transient JsonParser parser = null;

    private static final Map<String, GeoPoint> coordinateMap = new ConcurrentHashMap<>();
    private volatile boolean isRunning;

    private transient DateTimeFormatter timeFormatter = null;

    public void addEntry(String tmcCode, GeoPoint geoPoint) {
        coordinateMap.put(tmcCode, geoPoint);
    }

    public GeoPoint getGeoPoint(String tmcCode) {
        return coordinateMap.get(tmcCode);
    }

    public FcdEventSource(String url) {
        this.isRunning = true;
        this.url = url;
    }

    @Override
    public void run(SourceContext<FcdEvent> sourceContext) throws Exception {
        URL obj = new URL(url);
        parser = new JsonParser();
        timeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        GeoPoint pointA = new GeoPoint(48.15980, 11.56305);
        addEntry("27944", pointA);
        GeoPoint pointB = new GeoPoint(48.17660, 11.56760);
        addEntry("27945", pointB);
        GeoPoint pointC = new GeoPoint(48.15950, 11.54740);
        addEntry("27960", pointC);
        GeoPoint pointD = new GeoPoint(48.14760, 11.55885);
        addEntry("27959", pointD);
        GeoPoint pointE = new GeoPoint(48.16590, 11.53790);
        addEntry("27961", pointE);
        try {
            while (this.isRunning) {
                con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                generateEvent(response.toString(), sourceContext, parser);
                Thread.sleep(60000);
            }
        } finally {
            con.disconnect();
        }
    }

    /*
     * Creates one event from a json string
     */
    public void generateEvent(String jsonString, SourceContext<FcdEvent> sourceContext, JsonParser parser) {
        JsonElement element = parser.parse(jsonString);
        JsonObject jsonRecord = element.getAsJsonObject();
        JsonArray rwsList = jsonRecord.get("RWS").getAsJsonArray();

        for (JsonElement rws : rwsList) {
            JsonArray rwList = rws.getAsJsonObject().get("RW").getAsJsonArray();
            for (JsonElement rw : rwList) {
                JsonObject rwRecord = rw.getAsJsonObject();
                FcdEvent event = new FcdEvent();
                event.setMid(rwRecord.get("mid").getAsString());
                String timestamp = rwRecord.get("PBT").getAsString();
                event.setTimestamp(DateTime.parse(timestamp, timeFormatter).getMillis());
                JsonArray fisList = rwRecord.get("FIS").getAsJsonArray();
                for (JsonElement fis : fisList) {
                    JsonArray fiList = fis.getAsJsonObject().get("FI").getAsJsonArray();
                    for (JsonElement fi : fiList) {
                        JsonObject fiRecord = fi.getAsJsonObject();
                        JsonElement tmc = fiRecord.get("TMC");
                        String tmcCode = tmc.getAsJsonObject().get("PC").getAsString();
                        GeoPoint geoPoint = getGeoPoint(tmcCode);
                        JsonArray fiDataList = fiRecord.get("CF").getAsJsonArray();
                        for (JsonElement fiData : fiDataList) {
                            JsonObject fiDataRecord = fiData.getAsJsonObject();
                            event.setSpeed(fiDataRecord.getAsJsonObject().get("SP").getAsDouble());
                            event.setTmcCode(Integer.valueOf(tmcCode));
                            event.setLat(geoPoint.getLatitude());
                            event.setLon(geoPoint.getLongitude());
                            log.info("Adding new Event ----------------------------------------------------------------");
                            sourceContext.collectWithTimestamp(event, event.getTimestamp());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void cancel() {
        this.isRunning = false;
    }
}
