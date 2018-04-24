package org.tum.idp.fcd;

import com.google.common.io.Resources;
import com.twitter.bijection.Injection;
import com.twitter.bijection.avro.GenericAvroCodecs;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;


public class FcdEvent {

    private static final String FCD_SCHEMA = "fcd-event-schema.avsc";
    private static Schema schema = null;

    private String mid;
    private long timestamp;
    private double lon = 0.0;
    private double lat = 0.0;
    private double speed = 0.0;
    private int tmcCode = 0;

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getTmcCode() {
        return tmcCode;
    }

    public void setTmcCode(int tmcCode) {
        this.tmcCode = tmcCode;
    }

    public FcdEvent() {
    }

    public static void setSchema() {
        try (
                InputStream schemaIs = Resources.getResource(FCD_SCHEMA).openStream()) {
            Schema.Parser parser = new Schema.Parser();
            schema = parser.parse(schemaIs);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public byte[] toBinary() {
        if (schema == null) {
            setSchema();
        }
        GenericData.Record avroRecord = new GenericData.Record(schema);
        Injection<GenericRecord, byte[]> binaryRecord = GenericAvroCodecs.toBinary(schema);
        avroRecord.put("mid", getMid());
        avroRecord.put("timestamp", getTimestamp());
        avroRecord.put("lon", getLon());
        avroRecord.put("lat", getLat());
        avroRecord.put("speed", getSpeed());
        avroRecord.put("tmc_code", getTmcCode());
        byte[] value = binaryRecord.apply(avroRecord);
        return value;
    }

    public static FcdEvent fromBinary(byte[] avro) {
        if (schema == null) {
            setSchema();
        }
        FcdEvent event = new FcdEvent();
        Injection<GenericRecord, byte[]> recordInjection = GenericAvroCodecs.toBinary(schema);
        GenericRecord jsonRecord = recordInjection.invert(avro).get();
        event.setMid(jsonRecord.get("mid").toString());
        event.setTimestamp((Long) jsonRecord.get("timestamp"));
        event.setLon((Double) jsonRecord.get("lon"));
        event.setLat((Double) jsonRecord.get("lat"));
        event.setSpeed((Double) jsonRecord.get("speed"));
        event.setTmcCode(((Integer) jsonRecord.get("tmc_code")));
        return event;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mid).append("\t");
        sb.append(timestamp).append("\t");
        sb.append(lon).append("\t");
        sb.append(lat).append("\t");
        sb.append(speed).append("\t");
        sb.append(tmcCode);

        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FcdEvent &&
                this.mid.equals(((FcdEvent) other).mid) &&
                this.tmcCode == ((FcdEvent) other).tmcCode &&
                this.timestamp == (((FcdEvent) other).timestamp);
    }

    @Override
    public int hashCode() {
        return this.tmcCode;
    }

}
