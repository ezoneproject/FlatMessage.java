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

import com.ezoneproject.flatmessage.annotation.FieldDataType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class FlatFieldInfo {
    /**
     * (어노테이션) value(description)
     */
    public String name = "";
    /**
     * 클래스 필드
     */
    public Field field = null;
    /**
     * public 여부(public 이면 필드에 직접 값을 셋팅하고 아니면 get/set 메서드를 통함)
     */
    public boolean isPublic = false;
    public Method getterMethod = null;
    public Method setterMethod = null;
    /**
     * (어노테이션) position
     */
    public int position = 0;
    /**
     * (어노테이션) 길이
     */
    public int length = 0;
    /**
     * (어노테이션) 소수점길이
     */
    public int scale = -1;
    /**
     * FieldDataType
     */
    public FieldDataType dataType = FieldDataType.BLOCK;
    /**
     * dataType 이 CLASS 인 경우 데이터 클래스
     */
    public Class<?> dataClass = null;

    /**
     * TableType
     */
    public FieldTableType itemType = FieldTableType.FIELD;
    /**
     * 고정테이블 반복횟수
     */
    public int tableLoopCount = 0;
    /**
     * 가변테이블 반복횟수 필드
     */
    public FlatFieldInfo tableLoopField = null;
    /**
     * 테이블 필드
     */
    public List<FlatFieldInfo> tableFieldInfo = null;
    /**
     * 테이블 클래스
     */
    public Class<?> tableClass = null;

    @Override
    public String toString() {
        return "FlatFieldInfo{" +
                "name='" + name + '\'' +
                ", field=" + field +
                ", isPublic=" + isPublic +
                ", getterMethod=" + getterMethod +
                ", setterMethod=" + setterMethod +
                ", position=" + position +
                ", length=" + length +
                ", dataType=" + dataType +
                ", itemType=" + itemType +
                ", tableLoopCount=" + tableLoopCount +
                ", tableLoopField=" + tableLoopField +
                ", tableFieldInfo=" + tableFieldInfo +
                '}';
    }

    public enum FieldTableType {
        /**
         * 고정길이 테이블
         */
        TABLE_FIXED,
        /**
         * 가변길이 테이블
         */
        TABLE_VARIABLE,
        /**
         * 필드
         */
        FIELD
    }
}
