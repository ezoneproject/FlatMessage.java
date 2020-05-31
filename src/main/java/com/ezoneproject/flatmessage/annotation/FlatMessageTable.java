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
 * 테이블 항목을 정의하는 어노테이션
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface FlatMessageTable {
    /**
     * 테이블명(사용자 임의 입력)
     */
    String value() default "";

    /**
     * 필드 순번 (필수, 필드 순번으로 필드 순서를 정렬)
     */
    int position();

    /**
     * 테이블 종류 (고정반복, 가변반복, TableType.ETC는 금지)
     */
    TableType type();

    /**
     * 테이블 종류가 고정반복 테이블(TableType.TABLE_FIXED)인 경우 테이블 반복 횟수 (고정반복 테이블은 필수)
     */
    int loopCount() default 0;

    /**
     * 테이블 종류가 가변반복 테이블(TableType.TABLE_VARIABLE)인 경우 반복 횟수를 가지고 있는 필드명 (가변반복 테이블은 필수)
     */
    String loopFieldName() default "";

    /**
     * 반복 테이블 클래스
     */
    Class<?> tableClass();
}
