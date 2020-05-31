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

package com.ezoneproject.flatmessage;

import com.ezoneproject.flatmessage.annotation.FieldDataType;
import com.ezoneproject.flatmessage.debug.FlatMessageDump;
import com.ezoneproject.flatmessage.debug.FlatStringUtil;
import com.ezoneproject.flatmessage.internal.AnnotationFields;
import com.ezoneproject.flatmessage.internal.ConversionUtil;
import com.ezoneproject.flatmessage.internal.FlatFieldInfo;
import com.ezoneproject.flatmessage.internal.ReflectionAccess;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 객체를 byte[] 로 변환
 */
public final class FlatMessageSerializer<T> {
    //private final Logger log = LoggerFactory.getLogger(FlatMessageSerializer.class);

    /**
     * 플랫메시지 저장할 클래스 모델
     */
    private final Class<?> jClass;
    /**
     * 문자열 인코딩
     */
    private final Charset charset;
    /**
     * 필드 목록
     */
    private final List<FlatFieldInfo> fieldsList;

    // ************************************************************
    // 내부에서 특정 logger를 사용하지 않기 때문에 디버깅 정보를 별도로 보관한다.
    // ************************************************************

    // 덤프(디버깅) 모드
    private boolean dumpMode = false;
    // 덤프(디버깅) 데이터 목록
    private final List<FlatMessageDump> fieldsDump = new ArrayList<>();
    // 덤프(디버깅) 테이블 레벨
    protected int tableLevel = 0;
    // 덤프(디버깅) 테이블명 (테이블 레벨 > 0)
    protected String tableName = "";
    // 덤프(디버깅) 테이블 행번호 (테이블 레벨 > 0)
    protected int tableRow = 0;

    /**
     * 플랫 메시지를 byte 배열로 변환한다.
     *
     * @param jClass  class 모형
     * @param charset 문자열 인코딩
     */
    public FlatMessageSerializer(final Class<?> jClass, final Charset charset) {
        this.jClass = jClass;
        this.charset = charset;
        fieldsList = AnnotationFields.getInstance().getFlatFieldInfoList(jClass);
    }

    public FlatMessageSerializer(final Class<?> jClass, final Charset charset, final boolean dumpMode) {
        this.jClass = jClass;
        this.charset = charset;
        fieldsList = AnnotationFields.getInstance().getFlatFieldInfoList(jClass);
        this.dumpMode = dumpMode;
    }

    /**
     * 자료 길이를 리턴한다.
     */
    @SuppressWarnings("unckecked")
    public int getLength(final T object) {
        int length = 0;

        for (FlatFieldInfo it : fieldsList) {
            if (it.itemType == FlatFieldInfo.FieldTableType.FIELD) {
                // class 처리
                if (it.dataType == FieldDataType.CLASS) {
                    FlatMessageSerializer<? super Object> subSerializer = new FlatMessageSerializer<>(it.dataClass, charset);
                    Object subObject = ReflectionAccess.getField(it, object);
                    if (subObject == null) {
                        try {
                            subObject = it.dataClass.newInstance();
                        } catch (Exception e) {
                            // Bean 클래스는 인자가 없는 빈 public 생성자가 있어야 한다.
                            throw new InstanceCreateException(it.dataClass.getCanonicalName(), e);
                        }
                    }

                    length += subSerializer.getLength(subObject);
                    continue;
                }

                if (it.length >= 0)
                    length += it.length;
                else if (it.dataType == FieldDataType.BLOCK) {
                    // it.length < 0 이고, 데이터 타입이 BLOCK 이면 길이 계산 없이 주어진 byte를 끝까지 더한다.
                    Object data = ReflectionAccess.getField(it, object);

                    // BLOCK으로 선언되어 있으면 항상 byte[] (어노테이션 점검시 검증함)
                    if (data != null) {
                        byte[] dataByte = (byte[]) data;
                        length += dataByte.length;
                    }
                } else
                    throw new FieldDataAccessException("Can not calculate data length: " +
                            FlatStringUtil.shortClassName(it.field.getDeclaringClass().getCanonicalName()) + "." + it.field.getName());

            } else if (it.itemType == FlatFieldInfo.FieldTableType.TABLE_FIXED ||
                    it.itemType == FlatFieldInfo.FieldTableType.TABLE_VARIABLE) {
                int loopCount = ReflectionAccess.getTableLoopCount(it, object, jClass);

                int tableLen;
                try {
                    FlatMessageSerializer<? super Object> tableProcess = new FlatMessageSerializer<>(it.tableClass, charset);

                    tableLen = tableProcess.getLength(it.tableClass.newInstance());
                } catch (Exception e) {
                    throw new FieldDataAccessException("Can not calculate table:" +
                            FlatStringUtil.shortClassName(it.tableClass.getCanonicalName()), e);
                }

                length += (tableLen * loopCount);
            }
        }

        return length;
    }

    /**
     * 객체를 byte[]로 변환 (serialize)
     */
    public byte[] objectToBytes(final T object) {
        byte[] buffer = new byte[getLength(object)];
        int offset = 0;
        fieldsDump.clear();

        for (FlatFieldInfo it : fieldsList) {
            if (it.itemType == FlatFieldInfo.FieldTableType.FIELD) {
                Object valueObject = ReflectionAccess.getField(it, object);

                byte[] value;
                // class 처리
                if (it.dataType == FieldDataType.CLASS) {
                    FlatMessageSerializer<? super Object> subSerializer = new FlatMessageSerializer<>(it.dataClass, charset);
                    if (valueObject == null) {
                        try {
                            valueObject = it.dataClass.newInstance();
                        } catch (Exception e) {
                            // Bean 클래스는 인자가 없는 빈 public 생성자가 있어야 한다.
                            throw new InstanceCreateException(it.dataClass.getCanonicalName(), e);
                        }
                    }

                    // 덤프(디버깅) 모드 셋팅
                    subSerializer.dumpMode = dumpMode;
                    subSerializer.tableLevel = tableLevel + 1;
                    subSerializer.tableName = it.dataClass.getSimpleName();

                    value = subSerializer.objectToBytes(valueObject);

                    // 디버깅 데이터 처리
                    if (dumpMode) {
                        fieldsDump.addAll(subSerializer.getFieldsDump());
                    }
                } else {
                    value = ConversionUtil.toBytes(valueObject, it.length, it.scale, charset);

                    // 디버깅 데이터 생성
                    if (dumpMode) {
                        fieldsDump.add(new FlatMessageDump(it.field.getName(), it.name, offset, offset,
                                value.length, new String(value, charset), tableLevel, tableName, tableRow));
                    }
                }

                System.arraycopy(value, 0, buffer, offset, value.length);
                offset += value.length;
            } else if (it.itemType == FlatFieldInfo.FieldTableType.TABLE_FIXED ||
                    it.itemType == FlatFieldInfo.FieldTableType.TABLE_VARIABLE) {
                // 필드에 정의된 데이터 건수
                int loopCount = ReflectionAccess.getTableLoopCount(it, object, jClass);
                // 실제 데이터 건수
                int arrayCount;
                boolean isArray = it.field.getType().isArray();
                Object target = ReflectionAccess.getField(it, object);
                List<?> listObject = null;

                if (target == null)
                    arrayCount = 0;
                else if (isArray)
                    arrayCount = Array.getLength(target);
                else {
                    listObject = (List<?>) target;
                    arrayCount = listObject.size();
                }

                FlatMessageSerializer<? super Object> tableProcess = new FlatMessageSerializer<>(it.tableClass, charset);

                // 덤프(디버깅) 모드 셋팅
                tableProcess.dumpMode = dumpMode;
                tableProcess.tableLevel = tableLevel + 1;
                tableProcess.tableName = it.tableClass.getSimpleName();

                // 필드에 정의한 데이터 수만큼 처리하고 데이터가 더 많으면 나머지는 버림
                for (int i = 0; i < loopCount; i++) {
                    Object tableClassObj;
                    if (i < arrayCount) {
                        // 실제 데이터 처리
                        if (isArray)
                            tableClassObj = Array.get(target, i);
                        else
                            tableClassObj = listObject.get(i);
                    } else {
                        // 선언한 데이터 건수보다 실제 데이터가 적은 경우 빈 데이터를 생성해서 채움
                        try {
                            tableClassObj = it.tableClass.newInstance();
                        } catch (Exception e) {
                            throw new AnnotationDefineException("Can not create instance: " + it.tableClass.getCanonicalName());
                        }
                    }

                    tableProcess.tableRow++;
                    byte[] value = tableProcess.objectToBytes(tableClassObj);

                    // 디버깅데이터
                    if (dumpMode) {
                        fieldsDump.addAll(tableProcess.getFieldsDump());
                    }

                    System.arraycopy(value, 0, buffer, offset, value.length);
                    offset += value.length;
                }
            }
        } // end for

        return buffer;
    }

    public void setDumpMode(boolean dumpMode) {
        this.dumpMode = dumpMode;
    }

    public List<FlatMessageDump> getFieldsDump() {
        return fieldsDump;
    }

}
