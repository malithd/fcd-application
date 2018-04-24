package org.tum.idp.fcd;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.util.serialization.DeserializationSchema;
import org.apache.flink.streaming.util.serialization.SerializationSchema;


public class FcdEventSchema implements DeserializationSchema<FcdEvent>, SerializationSchema<FcdEvent> {

    @Override
    public byte[] serialize(FcdEvent element) {
        return element.toBinary();
    }

    @Override
    public FcdEvent deserialize(byte[] message) {
        return FcdEvent.fromBinary(message);
    }

    @Override
    public boolean isEndOfStream(FcdEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<FcdEvent> getProducedType() {
        return TypeExtractor.getForClass(FcdEvent.class);
    }
}
