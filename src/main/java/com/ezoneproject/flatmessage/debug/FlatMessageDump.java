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

/**
 * 디버그용 정보
 */
public class FlatMessageDump {
    /**
     * 필드설명
     */
    private String description = "";

    /**
     * 필드명
     */
    private String name = "";

    /**
     * 상대오프셋
     */
    private int offset = 0;

    /**
     * 절대오프셋
     */
    private int absoluteOffset = 0;

    /**
     * 길이
     */
    private int length = 0;

    /**
     * 문자열데이터
     */
    private String data = "";

    /**
     * 테이블레벨
     */
    private int tableLevel = 0;

    /**
     * 테이블명
     */
    private String tableName = "";

    /**
     * 테이블 반복 인덱스 (테이블인 경우 1부터 시작, 클래스의 경우 0)
     */
    private int tableRow = 0;

    private FlatMessageDump() {
    }

    public FlatMessageDump(final String name, final String description, final int offset,
                           final int absoluteOffset, final int length, final String data,
                           final int tableLevel, final String tableName, final int tableRow) {
        this.name = name;

        if (description == null || description.length() == 0)
            this.description = name;
        else
            this.description = description;

        this.offset = offset;
        this.absoluteOffset = absoluteOffset;
        this.length = length;
        this.data = data;
        this.tableLevel = tableLevel;
        this.tableName = tableName;
        this.tableRow = tableRow;
    }

    /**
     * @return 필드 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return 필드명
     */
    public String getName() {
        return name;
    }

    /**
     * @return 오프셋
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return 절대오프셋
     */
    public int getAbsoluteOffset() {
        return absoluteOffset;
    }

    /**
     * @return 필드 길이
     */
    public int getLength() {
        return length;
    }

    /**
     * @return 필드 데이터
     */
    public String getData() {
        return data;
    }

    /**
     * @return 테이블 중첩 레벨
     */
    public int getTableLevel() {
        return tableLevel;
    }

    /**
     * @return 테이블명
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return 테이블 row 인덱스
     */
    public int getTableRow() {
        return tableRow;
    }
}
