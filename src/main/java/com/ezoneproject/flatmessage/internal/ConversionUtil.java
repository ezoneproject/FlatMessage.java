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
import java.math.RoundingMode;
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
                else if (b == '-') {
                    // 음수 기호는 맨 앞에 나오거나 유효한 숫자가 시작하기 전에 나와야 함 (0만 허용)
                    // 음수 기호는 전체 길이에 포함됨
                    for (int i = sb.length() - 1; i >= 0; i--) {
                        if (sb.charAt(i) != '0')
                            throw new DataConversionException("Negative sign position: " + clsName + " [" + new String(fieldData) + "]");
                    }
                    sb.insert(0, '-');
                } else if (b == '+')
                    // 양수 기호는 0으로 변환
                    sb.append('0');
                else
                    throw new DataConversionException("Non numeric value: " + clsName + " [" + new String(fieldData) + "]");
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
            // 음수 기호 또는 양수 기호는 전체 길이에 영향이 없으므로 소수점 처리도 변경되지 않음
            if (flatFieldInfo.scale > 0)
                sb.insert(sb.length() - flatFieldInfo.scale, ".");

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
        String stringValue = String.format("%1$0" + len + "d", value);
        if (stringValue.length() > len)
            throw new DataConversionException("Value loss occurs '" + value + "' to " + len + " bytes.");

        return stringValue.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * decimal(includes float, double) to byte[]
     */
    public static byte[] toBytes(BigDecimal value, int len, int scale) {
        StringBuilder formatStr = new StringBuilder("%1$0");
        formatStr.append(len);

        // scale 값이 있으면 셋팅
        if (scale >= 0) {
            // 소숫점 초과 자릿수는 버림
            value = value.setScale(scale, RoundingMode.DOWN);
            formatStr.append(".").append(scale);
        }

        formatStr.append("f");

        // String conversion
        String stringValue = String.format(formatStr.toString(), value);

        // 정수/소수 추출
        if (stringValue.contains(".")) {
            // 정수 추출
            String intValue = stringValue.substring(0, stringValue.indexOf('.'));
            // 소수 추출
            String decValue = stringValue.substring(stringValue.indexOf('.') + 1);

            // 소수점 고정이면 stringValue 보정
            if (scale > 0)
                stringValue = intValue + decValue;
            else {
                stringValue = stringValue.substring(0, len);
                // 정수가 잘리면 오류
                if (stringValue.length() < intValue.length())
                    throw new DataConversionException("Value loss occurs '" + value + "' to " + len + " bytes.");
            }
        }

        // 지정한 길이를 초과하는 경우 데이터 로스 발생
        if (stringValue.length() > len)
            throw new DataConversionException("Value loss occurs '" + value + "' to " + len + " bytes.");
        else if (stringValue.length() < len) {
            // 길이가 부족하면 보정
            if (stringValue.charAt(0) == '-')
                stringValue = "-0" + stringValue.substring(1);
            else
                stringValue = "0" + stringValue;
        }

        return stringValue.getBytes(StandardCharsets.ISO_8859_1);
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
                return toBytes(new BigDecimal(String.valueOf((float) obj)), len, scale);
            else if (type == Double.class)
                return toBytes(new BigDecimal(String.valueOf((double) obj)), len, scale);
            else if (type == BigInteger.class)
                return toBytes(((BigInteger) obj).longValue(), len);
            else if (type == BigDecimal.class)
                return toBytes((BigDecimal) obj, len, scale);
            else if (type == String.class)
                return toBytes((String) obj, len, charset);
            else
                return toBytes(obj.toString(), len, charset);
        }
    }

}
