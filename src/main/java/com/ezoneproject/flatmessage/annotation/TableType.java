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
 * 필드의 데이터 타입에 대한 선언
 */
public enum TableType {
    /**
     * 고정길이 테이블
     */
    TABLE_FIXED,
    /**
     * 가변길이 테이블
     */
    TABLE_VARIABLE
}
