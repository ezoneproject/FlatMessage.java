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

package com.ezoneproject.flatmessage.internal;

import com.ezoneproject.flatmessage.DataConversionException;
import com.ezoneproject.flatmessage.annotation.FieldDataType;
import com.ezoneproject.flatmessage.debug.FlatStringUtil;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class ConversionUtil {
    private ConversionUtil() {
    }

    /**
     * byte[] to Object
     *
     * @param fieldData     byte
     * @param flatFieldInfo Flat Field Info
     * @param charset       byte charset
     * @return Object
     */
    public static Object toObject(final byte[] fieldData, final FlatFieldInfo flatFieldInfo, final Charset charset) {
        final String clsName = FlatStringUtil.shortClassName(flatFieldInfo.field.getDeclaringClass().getCanonicalName()
                + "." + flatFieldInfo.name);

        if (flatFieldInfo.dataType == FieldDataType.BLOCK) {
            return fieldData;
        } else if (flatFieldInfo.dataType == FieldDataType.NUMERIC) {
            // 숫자 검증
            final StringBuilder sb = new StringBuilder();
            for (byte b : fieldData) {
                if (b == '.' || ((b & 0x00ff) >= (0x0030 & 0x00ff) && (b & 0x00ff) <= (0x0039 & 0x00ff)))
                    sb.append((char) b);
                else
                    throw new DataConversionException("Not numeric: " + clsName + " [" + new String(fieldData) + "]");
            }

            // 소수를 정수에 컨버전하려는 경우 오류 (원본은 소수, 타겟은 정수)
            if ((flatFieldInfo.scale > 0 || sb.indexOf(".") >= 0) &&
                    (flatFieldInfo.field.getType() == Integer.class || flatFieldInfo.field.getType() == int.class ||
                            flatFieldInfo.field.getType() == Long.class || flatFieldInfo.field.getType() == long.class ||
                            flatFieldInfo.field.getType() == BigInteger.class))
                throw new DataConversionException("Decimal to integer: " + clsName + " [" + sb.toString() + "]");

            // 고정소수점인데 소수점이 문자열에 포함되어 있으면 오류
            if (flatFieldInfo.scale > 0 && sb.indexOf(".") > 0)
                throw new DataConversionException("Decimal point detected: " + clsName + " [" + sb.toString() + "]");

            // 고정 소수점을 문자열에 삽입
            if (flatFieldInfo.scale > 0)
                sb.insert(sb.length() - flatFieldInfo.scale, ".");

            // 시작하는 0을 삭제
            while (sb.length() > 1 && sb.charAt(0) == '0')
                sb.deleteCharAt(0);

            // 0.xxx 에서 정수부 0이 삭제된 경우 0 보정
            if (sb.length() > 0 && sb.charAt(0) == '.')
                sb.insert(0, '0');

            if (flatFieldInfo.field.getType() == Integer.class || flatFieldInfo.field.getType() == int.class)
                return Integer.parseInt(sb.toString(), 10);
            else if (flatFieldInfo.field.getType() == Long.class || flatFieldInfo.field.getType() == long.class)
                return Long.parseLong(sb.toString(), 10);
            else if (flatFieldInfo.field.getType() == Float.class || flatFieldInfo.field.getType() == float.class)
                return Float.parseFloat(sb.toString());
            else if (flatFieldInfo.field.getType() == Double.class || flatFieldInfo.field.getType() == double.class)
                return Double.parseDouble(sb.toString());
            else if (flatFieldInfo.field.getType() == BigInteger.class)
                return new BigInteger(sb.toString());
            else if (flatFieldInfo.field.getType() == BigDecimal.class)
                return new BigDecimal(sb.toString());
            else
                throw new DataConversionException("Unknown target decimal type: " + clsName + " [" + flatFieldInfo.field.getType().getName() + "]");
        } else if (flatFieldInfo.dataType == FieldDataType.ALPHANUM) {
            // alphanum 은 ASCII 문자만 허용, 비ASCII 문자는 ?으로 변환함
            // 비ASCII 문자는 LOCAL 또는 UTF-8 사용
            StringBuilder sb = new StringBuilder();
            for (byte b : fieldData) {
                if ((b & 0x00ff) >= (0x0020 & 0x00ff) && (b & 0x00ff) < (0x0080 & 0x00ff))
                    sb.append((char) b);
                else
                    sb.append('?');
            }

            // right trim
            return FlatStringUtil.rtrim(sb.toString());
        } else if (flatFieldInfo.dataType == FieldDataType.STRING) {
            return FlatStringUtil.rtrim(new String(fieldData, charset));
        } else if (flatFieldInfo.dataType == FieldDataType.UTF8) {
            return FlatStringUtil.rtrim(new String(fieldData, StandardCharsets.UTF_8));
        } else
            throw new DataConversionException("Unknown FieldDataType: " + clsName);
    }

    /**
     * String to byte[]
     */
    public static byte[] toBytes(String str, int len, Charset charset) {
        byte[] source = str.getBytes(charset);
        byte[] target = new byte[len];

        if (source.length < target.length)
            Arrays.fill(target, (byte) ' ');

        // Ignore data loss
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
        return target;
    }

    /**
     * long to byte[]
     */
    public static byte[] toBytes(long value, int len) {
        return numericStringToBytes(String.valueOf(value), len);
    }

    /**
     * double to byte[]
     */
    public static byte[] toBytes(double value, int len, int scale) {
        String sourceStr = String.valueOf(value);
        int pointIdx = sourceStr.indexOf('.');

        // 소수점 보정
        if (scale > 0) {
            if (pointIdx < 0) {
                // 정수인 경우 소수점 아래 값만큼 0을 채움
                sourceStr = String.valueOf(value * (10 ^ scale));
            } else {
                // 소수점이 있는 경우 정수와 소수 분리
                String integerStr = sourceStr.substring(0, pointIdx);
                String minor = sourceStr.substring(pointIdx + 1);
                if (minor.length() < scale)
                    minor = String.valueOf(Integer.parseInt(minor) * (10 ^ (scale - minor.length())));
                else
                    minor = minor.substring(0, scale);

                sourceStr = integerStr + minor;
            }
        } else if (scale == 0 && pointIdx >= 0) {
            // 변환 대상에는 소수점이 없지만 데이터에 소수점이 있으면 소수점 아래 값 버림
            sourceStr = sourceStr.substring(0, pointIdx);
        }

        return numericStringToBytes(sourceStr, len);
    }

    /**
     * Object to byte[]
     */
    public static byte[] toBytes(Object obj, int len, int scale, Charset charset) {
        if (obj == null) {
            byte[] bytes = new byte[Math.max(len, 0)];
            Arrays.fill(bytes, (byte) ' ');
            return bytes;
        }

        Class<?> cls = obj.getClass();
        Type type = cls.isArray() ? cls.getComponentType() : cls;

        // byte[]
        if (cls.isArray()) {
            if (type == byte.class || type == Byte.class) {
                byte[] source = (byte[]) obj;

                int targetLen = (len < 0) ? source.length : len;
                byte[] target = new byte[targetLen];

                if (source.length < target.length)
                    Arrays.fill(target, (byte) ' ');

                System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
                return target;
            } else
                // 배열은 바이트 배열만 허용
                throw new DataConversionException("Only byte[]. [" + type.getTypeName() + "]");
        }
        // data type
        else {
            if (type == Integer.class)
                return toBytes(((Integer) obj).longValue(), len);
            else if (type == Long.class)
                return toBytes((long) obj, len);
            else if (type == Float.class)
                return toBytes(((Float) obj).doubleValue(), len, scale);
            else if (type == Double.class)
                return toBytes((double) obj, len, scale);
            else if (type == BigInteger.class)
                return toBytes(((BigInteger) obj).longValue(), len);
            else if (type == BigDecimal.class)
                return toBytes(((BigDecimal) obj).doubleValue(), len, scale);
            else if (type == String.class)
                return toBytes((String) obj, len, charset);
            else
                return toBytes(obj.toString(), len, charset);
        }
    }

    private static byte[] numericStringToBytes(String value, int len) {
        byte[] source = value.getBytes(StandardCharsets.ISO_8859_1);
        byte[] target = new byte[len];

        if (source.length < target.length)
            Arrays.fill(target, (byte) '0');

        // Data loss error
        if (source.length > target.length)
            throw new DataConversionException("Value loss occurs '" + value + "' to " + len + " bytes.");

        System.arraycopy(source, 0, target, target.length - source.length, source.length);
        return target;
    }
}
