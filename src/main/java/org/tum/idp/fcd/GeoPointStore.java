package org.tum.idp.fcd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeoPointStore {
    private static final Map<String, GeoPoint> coordinateMap = new ConcurrentHashMap<>();

    public static void addEntry(String tmcCode, GeoPoint geoPoint){
            coordinateMap.put(tmcCode, geoPoint);
    }

    public static GeoPoint getGeoPoint(String tmcCode){
        return coordinateMap.get(tmcCode);
    }
}
