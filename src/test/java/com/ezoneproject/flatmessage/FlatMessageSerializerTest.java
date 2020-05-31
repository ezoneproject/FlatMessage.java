/*
 * Copyright 2020 ezoneproject.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ezoneproject.flatmessage;

import com.ezoneproject.flatmessage.annotation.FieldDataType;
import com.ezoneproject.flatmessage.annotation.FlatMessageField;
import com.ezoneproject.flatmessage.annotation.FlatMessageTable;
import com.ezoneproject.flatmessage.annotation.TableType;
import com.ezoneproject.flatmessage.debug.FlatMessageDump;
import com.ezoneproject.flatmessage.debug.FlatStringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FlatMessageSerializerTest {
    private static final Logger log = LoggerFactory.getLogger(FlatMessageSerializerTest.class);

    @BeforeAll
    static void setUp() {
        log.info("Before Test ----------");

        // register class (optional)
        FlatMessageClassBuilder builder = new FlatMessageClassBuilder();
        builder.register(TestMessage.class).register(TestSubMessage.class);
    }

    @Test
    void serialize_n_deserializeTest() {
        log.info("Test Start ----------");

        TestMessage message = new TestMessage();
        message.length = 12345;
        message.rawData = "MSG".getBytes();
        message.stringData = "테스트";
        message.messageArray = new TestSubMessage[]{
                new TestSubMessage(),
                new TestSubMessage("SUB1", 3, "테이블")
        };

        // serialize
        FlatMessageSerializer<TestMessage> serializer = new FlatMessageSerializer<>(TestMessage.class, StandardCharsets.UTF_8, true);
        byte[] serializedData = serializer.objectToBytes(message);

        log.info("[" + new String(serializedData, StandardCharsets.UTF_8) + "] ");
        log.info("Bytes length " + serializedData.length);

        printDump(serializer.getFieldsDump());

        // deserialize
        FlatMessageDeserializer<TestMessage> deserializer = new FlatMessageDeserializer<>(TestMessage.class, StandardCharsets.UTF_8, true);
        TestMessage deserialized = deserializer.bytesToObject(serializedData, 0);

        log.info("Orignal hash: " + message.hashCode());
        log.info("Deserialized hash: " + deserialized.hashCode());
        log.info("equals: " + message.equals(deserialized));

        printDump(deserializer.getFieldsDump());

        //assertNotEquals(message, deserialized);
        assertEquals(message, deserialized);

        // final
        FlatMessageSerializer<TestMessage> serializer2 = new FlatMessageSerializer<>(TestMessage.class, StandardCharsets.UTF_8);
        byte[] serializedData2 = serializer2.objectToBytes(deserialized);

        log.info("[" + new String(serializedData2, StandardCharsets.UTF_8) + "] ");
        log.info("Bytes length " + serializedData2.length);

        assertArrayEquals(serializedData, serializedData2);

        log.info("Test End ----------");
    }

    private void printDump(List<FlatMessageDump> dumps) {
        log.debug("--------------------+-----+-----+-----------------------------------------------");
        log.debug("       FIELD        | Off | Len |   Data                                        ");
        log.debug("--------------------+-----+-----+-----------------------------------------------");
        for (FlatMessageDump d : dumps) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.getTableLevel(); i++) {
                sb.append(">");
            }

            if (d.getTableLevel() > 0)
                sb.append(FlatStringUtil.rpad(d.getDescription() + "#" + d.getTableRow(), 20 - d.getTableLevel()));
            else
                sb.append(FlatStringUtil.rpad(d.getDescription(), 20 - d.getTableLevel()));

            sb.append("|");
            sb.append(FlatStringUtil.lpad(Integer.toString(d.getAbsoluteOffset()), 5));
            sb.append("|");
            sb.append(FlatStringUtil.lpad(Integer.toString(d.getLength()), 5));
            sb.append("|");
            sb.append(d.getData());
            sb.append("<");
            log.debug(sb.toString());
        }
        log.debug("--------------------+-----+-----+-----------------------------------------------");

    }

    public static class TestMessage {
        @FlatMessageField(position = 1, length = 8, type = FieldDataType.NUMERIC)
        public int length = 0;

        @FlatMessageField(position = 2, length = 10, type = FieldDataType.BLOCK)
        public byte[] rawData = null;

        @FlatMessageField(position = 3, length = 30, type = FieldDataType.STRING)
        public String stringData = null;

        @FlatMessageTable(position = 4, type = TableType.TABLE_FIXED, loopCount = 5, tableClass = TestSubMessage.class)
        public TestSubMessage[] messageArray = null;

        @FlatMessageField(position = 5, length = 0, type = FieldDataType.CLASS, dataClass = TestSubMessage.class)
        public TestSubMessage subClass = null;

        // byte[] 배열은 패딩때문이 다른 결과가 나올 수 있어서 별도의 메서드 생성
        private boolean rawDataEquals(byte[] that) {
            int compareLength = Math.min(rawData.length, 10);

            // that.length 가 작은 경우 항상 실패
            if (that.length < rawData.length)
                return false;

            for (int i = 0; i < compareLength; i++) {
                if (rawData[i] != that[i])
                    return false;
            }
            return true;
        }

        private boolean messageArrayEquals(TestSubMessage[] that) {
            if (messageArray == null && that == null)
                return true;
            if (messageArray == null && that.length == 0)
                return true;

            int compareLength = Math.min(messageArray == null ? 0 : messageArray.length, 5);

            // that.length 가 작은 경우 항상 실패
            if (that.length < compareLength)
                return false;

            for (int i = 0; i < compareLength; i++) {
                if (!messageArray[i].equals(that[i]))
                    return false;
            }

            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestMessage)) return false;
            TestMessage that = (TestMessage) o;

            return length == that.length &&
                    rawDataEquals(that.rawData) &&
                    FlatStringUtil.rtrim(stringData).equals(FlatStringUtil.rtrim(that.stringData)) &&
                    messageArrayEquals(that.messageArray) &&
                    (subClass == null || subClass.equals(that.subClass));
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(length, FlatStringUtil.rtrim(stringData), subClass);
            result = 31 * result + Arrays.hashCode(rawData);
            result = 31 * result + Arrays.hashCode(messageArray);
            return result;
        }
    }


    public static class TestSubMessage {
        @FlatMessageField(position = 1, length = 10, type = FieldDataType.STRING)
        public String stringData1 = null;

        @FlatMessageField(position = 2, length = 5, type = FieldDataType.NUMERIC)
        public int intData = 0;

        @FlatMessageField(position = 3, length = 20, type = FieldDataType.STRING)
        public String stringData2 = null;

        //@FlatMessageField(position = 5, length = 0, type = FieldDataType.CLASS, dataClass = TestMessage.class)
        //public TestMessage subClass = null;

        public TestSubMessage() {
        }

        public TestSubMessage(String stringData1, int intData, String stringData2) {
            this.stringData1 = stringData1;
            this.intData = intData;
            this.stringData2 = stringData2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestSubMessage)) return false;
            TestSubMessage that = (TestSubMessage) o;

            return intData == that.intData &&
                    FlatStringUtil.rtrim(stringData1).equals(FlatStringUtil.rtrim(that.stringData1)) &&
                    FlatStringUtil.rtrim(stringData2).equals(FlatStringUtil.rtrim(that.stringData2));
        }

        @Override
        public int hashCode() {
            return Objects.hash(FlatStringUtil.rtrim(stringData1), intData, FlatStringUtil.rtrim(stringData2));
        }
    }

}
