package com.ezoneproject.flatmessage.internal;

import com.ezoneproject.flatmessage.annotation.FieldDataType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

class ConversionUtilTest {
    private static final Logger log = LoggerFactory.getLogger(ConversionUtilTest.class);

    public double testValue;

    @Test
    void toObject() {
        log.info("Test Start ----------");

        FlatFieldInfo field = new FlatFieldInfo();
        field.dataType = FieldDataType.NUMERIC;
        try {
            field.field = ConversionUtilTest.class.getField("testValue");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        byte[] data = "00-100.01".getBytes();

        Object o = ConversionUtil.toObject(data, field, StandardCharsets.UTF_8);
        testValue = (double) o;

        log.info("Value : " + testValue);

        byte[] data2 = ConversionUtil.toBytes(testValue, 10, 3, StandardCharsets.UTF_8);
        log.info("String: " + new String(data2));
    }

    @Test
    void toBytes() {
        log.info("toBytes(long, int) Test Start ----------");

        byte[] data = ConversionUtil.toBytes(new BigDecimal("-123456.1234"), 9, -1);
        log.info("return [" + new String(data) + "]");
    }
}
