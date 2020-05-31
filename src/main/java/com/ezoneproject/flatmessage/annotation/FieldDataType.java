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

package com.ezoneproject.flatmessage.annotation;

/**
 * 필드의 데이터 타입에 대한 선언, 선언한 타입과 클래스의 데이터타입이 불일치하면 오류 발생.
 */
public enum FieldDataType {
    /**
     * Block: byte[]
     */
    BLOCK,

    /**
     * Numeric: 숫자, int, long, float, double, BigInteger, BigDecimal
     */
    NUMERIC,

    /**
     * Alpha Numeric: 영문자 또는 숫자(0x20 ~ 0x80), 한글 불가, String
     */
    ALPHANUM,

    /**
     * Local string: 지역언어(EUC-KR, MS949 ... etc) 문자열, String
     */
    STRING,

    /**
     * UTF-8, 언제나 항상 UTF-8을 사용함, String
     */
    UTF8,
    /**
     * 사용자 Class (length 속성 무시)
     */
    CLASS;

    FieldDataType() {
    }
}
