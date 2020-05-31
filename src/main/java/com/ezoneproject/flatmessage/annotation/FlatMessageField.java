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

import java.lang.annotation.*;

/**
 * 필드 항목을 정의하는 어노테이션
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface FlatMessageField {
    /**
     * 필드명(사용자 임의 입력, 선택)
     */
    String value() default "";

    /**
     * 필드 순번 (필수, 필드 순번으로 필드 순서를 정렬)
     */
    int position();

    /**
     * 바이트 길이 (필수, -1로 입력하면 데이터의 끝까지 읽으므로 마지막 필드로만 사용할 수 있음)
     */
    int length();

    /**
     * 데이터 종류 (기본값은 STRING)
     */
    FieldDataType type() default FieldDataType.STRING;

    /**
     * 데이터 종류가 NUMERIC 인 경우 고정 소숫점 자릿수, -1인 경우 데이터에 포함된 소수점으로 판단
     */
    int scale() default -1;

    /**
     * 데이터 종류가 CLASS 인 경우 데이터 클래스
     */
    Class<?> dataClass() default Object.class;
}
