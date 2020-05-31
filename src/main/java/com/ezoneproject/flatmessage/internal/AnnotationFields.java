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

import com.ezoneproject.flatmessage.AnnotationDefineException;
import com.ezoneproject.flatmessage.annotation.FieldDataType;
import com.ezoneproject.flatmessage.annotation.FlatMessageField;
import com.ezoneproject.flatmessage.annotation.FlatMessageTable;
import com.ezoneproject.flatmessage.annotation.TableType;
import com.ezoneproject.flatmessage.debug.FlatStringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public final class AnnotationFields {
    //private final Logger log = LoggerFactory.getLogger(AnnotationCache.class);

    private static final AnnotationFields instance = new AnnotationFields();

    private final Map<String, List<FlatFieldInfo>> flatFieldInfoCache = new HashMap<>();

    private AnnotationFields() {
    }

    public static AnnotationFields getInstance() {
        return instance;
    }

    /**
     * 필드 어노테이션을 목록으로 생성하고 캐시
     */
    public <T> List<FlatFieldInfo> getFlatFieldInfoList(Class<T> jClass) {
        String className = jClass.getCanonicalName();

        List<FlatFieldInfo> result;
        synchronized (flatFieldInfoCache) {
            result = flatFieldInfoCache.get(className);
            if (result == null) {
                result = makeFlatFieldInfoList(jClass);
                flatFieldInfoCache.put(className, result);
            }
        }
        return result;
    }

    /**
     * 클래스에서 필드 어노테이션을 목록으로 생성
     */
    private <T> List<FlatFieldInfo> makeFlatFieldInfoList(Class<T> jClass) {
        List<FlatFieldInfo> resultFieldList = new ArrayList<>();

        if (jClass.getSuperclass() != null) {
            resultFieldList.addAll(makeFlatFieldInfoList(jClass.getSuperclass()));
        }

        // 정의된 모든 필드에 대해 처리 (private, superclass 포함)
        Field[] fields = jClass.getDeclaredFields();
        for (Field field : fields) {
            // 필드항목 정의
            FlatFieldInfo fieldInfo = fieldProcess(field);

            // 필드항목이 아닌 경우
            if (fieldInfo == null) {
                // 테이블 항목 정의
                fieldInfo = tableProcess(field, resultFieldList);
            }

            // 어노테이션이 없는 일반 필드는 생략
            if (fieldInfo == null)
                continue;

            // 필드가 public 이면 필드에 직접 억세스함
            if (Modifier.isPublic(field.getModifiers())) {
                fieldInfo.isPublic = true;
            }
            // public 이 아니면 필드 getter와 setter가 있어야 함
            else {
                fieldInfo.isPublic = false;

                String clsFldName = jClass.getCanonicalName() + "." + field.getName();
                String methodName = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);

                // getter method
                try {
                    fieldInfo.getterMethod = jClass.getMethod("get" + methodName);
                } catch (NoSuchMethodException e) {
                    throw new AnnotationDefineException("@FlatMessageTable() getter() method is not exist: " + clsFldName);
                }
                // setter method
                try {
                    fieldInfo.setterMethod = jClass.getMethod("set" + methodName, field.getType());
                } catch (NoSuchMethodException e) {
                    throw new AnnotationDefineException("@FlatMessageTable() setter(" + field.getType() + ") method is not exist: " + clsFldName);
                }
            }

            resultFieldList.add(fieldInfo);
        } // end for

        // position 기준으로 정렬
        resultFieldList.sort(Comparator.comparingInt(a -> a.position));

        /*
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder(jClass.getCanonicalName());
            sb.append("[");
            for (FlatFieldInfo fieldInfo : resultFieldList) {
                sb.append(fieldInfo.toString());
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]");

            log.debug(sb.toString());
        }
        */

        return resultFieldList;
    }

    /**
     * 필드 어노테이션 처리
     */
    private FlatFieldInfo fieldProcess(Field field) {
        FlatMessageField flatMsgField = field.getAnnotation(FlatMessageField.class);
        if (flatMsgField == null)
            return null;

        FlatFieldInfo fieldInfo = new FlatFieldInfo();
        Class<?> fieldType = field.getType();
        String clsFldName = field.getDeclaringClass().getCanonicalName() + "." + field.getName();

        fieldInfo.name = flatMsgField.value();
        fieldInfo.field = field;
        fieldInfo.position = flatMsgField.position();
        fieldInfo.length = flatMsgField.length();
        fieldInfo.scale = flatMsgField.scale();
        fieldInfo.dataType = flatMsgField.type();
        fieldInfo.dataClass = flatMsgField.dataClass(); // default Object.class
        fieldInfo.itemType = FlatFieldInfo.FieldTableType.FIELD;
        fieldInfo.tableLoopCount = 0;
        fieldInfo.tableLoopField = null;
        fieldInfo.tableFieldInfo = null;
        fieldInfo.tableClass = null;

        // 필드설명이 정의되지 않았으면 필드명으로 대체
        if (fieldInfo.name == null || fieldInfo.name.length() == 0)
            fieldInfo.name = field.getName();

        // length < 0인 경우는 필드 타입이 BLOCK 이어야 함
        if (fieldInfo.length < 0 && fieldInfo.dataType != FieldDataType.BLOCK)
            throw new AnnotationDefineException("@FlatMessageField(length < 0 allowed with type = BLOCK): " + clsFldName);

        // 소수점 위치는 필드 길이보다는 작아야 함 (정수 1자리는 반드시 필요)
        if (fieldInfo.dataType == FieldDataType.NUMERIC && fieldInfo.length <= fieldInfo.scale)
            throw new AnnotationDefineException("@FlatMessageField(length <= scale): " + clsFldName);

        // ---------------------------------
        // Valid field's type with FieldDataType
        // ---------------------------------
        if (fieldInfo.dataType == FieldDataType.BLOCK && fieldType != byte[].class)
            // Block 은 byte[]만 가능
            throw new AnnotationDefineException("@FlatMessageField(type = BLOCK) must byte[]: " + clsFldName);
        else if (fieldInfo.dataType == FieldDataType.NUMERIC &&
                (fieldType != int.class && fieldType != Integer.class &&
                        fieldType != long.class && fieldType != Long.class &&
                        fieldType != float.class && fieldType != Float.class &&
                        fieldType != double.class && fieldType != Double.class &&
                        fieldType != BigDecimal.class && fieldType != BigInteger.class))
            // Numeric 은 int, long, float, double, BigDecimal, BigInteger 만 가능
            throw new AnnotationDefineException("@FlatMessageField(type = NUMERIC) must number: " + clsFldName);
        else if (fieldInfo.dataType == FieldDataType.ALPHANUM && fieldType != String.class)
            // Alpha Numeric 은 String 만 가능
            throw new AnnotationDefineException("@FlatMessageField(type = ALPHANUM) must String: " + clsFldName);
        else if (fieldInfo.dataType == FieldDataType.STRING && fieldType != String.class)
            // Local String 은 String 만 가능
            throw new AnnotationDefineException("@FlatMessageField(type = LOCAL) must String: " + clsFldName);
        else if (fieldInfo.dataType == FieldDataType.UTF8 && fieldType != String.class)
            // UTF8 String 은 String 만 가능
            throw new AnnotationDefineException("@FlatMessageField(type = UTF8) must String: " + clsFldName);

        return fieldInfo;
    }

    /**
     * 테이블 어노테이션 처리
     */
    private FlatFieldInfo tableProcess(Field field, List<FlatFieldInfo> resultFieldList) {
        FlatMessageTable flatMsgTable = field.getAnnotation(FlatMessageTable.class);
        if (flatMsgTable == null)
            return null;

        FlatFieldInfo fieldInfo = new FlatFieldInfo();
        String clsFldName = field.getDeclaringClass().getCanonicalName() + "." + field.getName();

        //Class<?> fieldType = field.getType();
        // 테이블 어노테이션이 붙은 클래스는 array[] 타입이거나 List 또는 List를 상속해야 한다.
        if (!field.getType().isArray() && field.getType() != List.class) {
            try {
                List<?> listTest = (List<?>) field.getType().newInstance();
                listTest.clear();
            } catch (Exception e) {
                throw new AnnotationDefineException("@FlatMessageTable() is not array or subclass of java.util.List: " + clsFldName);
            }
        }

        fieldInfo.name = flatMsgTable.value();
        fieldInfo.field = field;
        fieldInfo.position = flatMsgTable.position();
        fieldInfo.length = 0;
        fieldInfo.scale = 0;
        fieldInfo.dataType = FieldDataType.BLOCK;
        fieldInfo.itemType = (flatMsgTable.type() == TableType.TABLE_FIXED) ?
                FlatFieldInfo.FieldTableType.TABLE_FIXED : FlatFieldInfo.FieldTableType.TABLE_VARIABLE;
        fieldInfo.tableClass = flatMsgTable.tableClass();

        // 항상 고정된 행 수를 가진 테이블
        if (fieldInfo.itemType == FlatFieldInfo.FieldTableType.TABLE_FIXED) {
            fieldInfo.tableLoopCount = flatMsgTable.loopCount();
            fieldInfo.tableLoopField = null;
        }
        // 다른 필드에 행 수가 있는 가변형 테이블
        else if (fieldInfo.itemType == FlatFieldInfo.FieldTableType.TABLE_VARIABLE) {
            fieldInfo.tableLoopCount = 0;

            String loopFieldName = flatMsgTable.loopFieldName().trim();
            if (loopFieldName.length() == 0)
                throw new AnnotationDefineException("@FlatMessageTable(loopFieldName?): " + clsFldName);

            // 가변 반복 테이블은 반복 횟수를 참조할 필드가 반드시 현 테이블 이전 필드에 정의
            fieldInfo.tableLoopField = null;
            for (FlatFieldInfo it : resultFieldList) {
                if (it.field.getName().equals(loopFieldName)) {
                    // 반복 횟수 필드는 숫자형 타입이어야 함
                    if (it.dataType != FieldDataType.NUMERIC)
                        throw new AnnotationDefineException("@FlatMessageTable(loopFieldName) is not numeric: " + clsFldName);

                    fieldInfo.tableLoopField = it;
                    break;
                }
            }
            if (fieldInfo.tableLoopField == null)
                throw new AnnotationDefineException("@FlatMessageTable(loopFieldName) is not defined: " + clsFldName);
        } else
            throw new AnnotationDefineException("@FlatMessageTable(type) is unknown: " + clsFldName);

        // 테이블 필드 처리는 재귀 호출
        if (flatMsgTable.tableClass().getCanonicalName().equals(field.getDeclaringClass().getCanonicalName()))
            throw new AnnotationDefineException("@FlatMessageTable(tableClass=self reference deteced): " +
                    FlatStringUtil.shortClassName(flatMsgTable.tableClass().getCanonicalName()) + "." + field.getName());

        fieldInfo.tableFieldInfo = makeFlatFieldInfoList(flatMsgTable.tableClass());
        // 테이블 선언하면 해당 클래스가 어노테이션을 하나 이상 포함해야 함
        if (fieldInfo.tableFieldInfo.size() == 0)
            throw new AnnotationDefineException("@FlatMessageTable(tableClass) is not flat message class: " + clsFldName);

        return fieldInfo;
    }
}
