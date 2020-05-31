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

package com.ezoneproject.flatmessage.debug;

import java.nio.charset.Charset;

public final class FlatStringUtil {
    private static final Charset DEFAULT_CHARSET = Charset.forName("CP949");

    /**
     * 문자열 오른쪽을 지정한 길이만큼 공백으로 채운다. 길이를 오버하면 자른다.
     *
     * @param str 문자열
     * @param len bytes길이
     * @return 오른쪽을 공백으로 채운 문자열
     */
    public static String rpad(String str, final int len) {
        int dataLen = str.getBytes(DEFAULT_CHARSET).length;

        while (dataLen > len) {
            str = str.substring(0, str.length() - 1);
            dataLen = str.getBytes(DEFAULT_CHARSET).length;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(str);

        for (int i = 0; i < len - dataLen; i++) {
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * 문자열 왼쪽을 지정한 길이만큼 공백으로 채운다.
     *
     * @param str 문자열
     * @param len bytes길이
     * @return 왼쪽 공백이 포함된 문자열
     */
    public static String lpad(String str, final int len) {
        int dataLen = str.getBytes(DEFAULT_CHARSET).length;

        while (dataLen > len) {
            str = str.substring(1);
            dataLen = str.getBytes(DEFAULT_CHARSET).length;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len - dataLen; i++) {
            sb.append(" ");
        }
        sb.append(str);

        return sb.toString();
    }

    /**
     * 문자열 오른쪽의 공백을 삭제한다.
     *
     * @param str 문자열
     * @return 공백을 삭제한 문자열
     */
    public static String rtrim(String str) {
        // str.replaceAll("\\s+$", "") 을 사용하면 실제 길이가 줄어들지 않는다
        // length()가 변하지 않아서 String.equals() 가 실패함

        if (str == null)
            return "";

        char[] chars = str.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] != ' ' && chars[i] != '\u0000')
                return new String(chars, 0, i + 1);
            if (i == 0)
                return "";
        }
        return str;
    }

    /**
     * @param packageName 클래스 패키지명
     * @return 단축한 패키지명
     */
    public static String shortClassName(final String packageName) {
        String[] packagePath = packageName.split("\\.");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packagePath.length - 1; i++) {
            if (i < packagePath.length - 2)
                sb.append(packagePath[i].charAt(0)).append(".");
            else
                sb.append(packagePath[i]).append(".");
        }
        sb.append(packagePath[packagePath.length - 1]);
        return sb.toString();
    }

}
