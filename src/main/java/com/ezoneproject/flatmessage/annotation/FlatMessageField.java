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
     * 데이터 종류가 NUMERIC 인 경우 고정 소숫점의 자릿수를 지정, -1인 경우 데이터에 포함된 소수점으로 판단
     * <pre>
     *     고정소수점은 데이터에 소수점이 포함되지 않으며, 우측에서부터 소수점을 계산한다.
     *     scale > 0에서 데이터에 소수점이 있으면 오류 처리한다.
     *     scale = 0은 소수점이 없는 것을 의미한다.
     *     scale = -1은 데이터에 소수점이 포함되어 있는 경우이며, 직렬화 할 때도 별다른 통제 없이 유동 소수점으로 변환한다.
     *     유동 소수점을 직렬화 할 때 소수점 자릿수를 제한하려면 자료형으로 BigDecumal 클래스를 사용하고 setScale() 메서드로 소수점 자릿수를 설정한다.
     *     유동 소수점 값이 전체 길이를 초과하면 소수 값을 자른다.
     * </pre>
     */
    int scale() default -1;

    /**
     * 데이터 종류가 CLASS 인 경우 데이터 클래스
     */
    Class<?> dataClass() default Object.class;
}
