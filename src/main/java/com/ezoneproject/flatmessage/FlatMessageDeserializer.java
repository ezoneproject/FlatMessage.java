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
 * byte[]를 메시지 객체로 변환한다.
 * <pre>
 *     Class는 인자가 없는 빈 생성자가 있어야 한다.
 *     Class의 데이터 필드에 어노테이션이 선언되어야 한다. (메서드에 선언하면 안 됨)
 *     데이터 필드가 public이 아니면 getter/setter가 선언되어야 한다.
 *
 *     bytes 배열은 클래스의 데이터 필드 크기보다 같거나 커야 한다.
 *     bytes 배열이 클래스의 데이터 필드 크기보다 작으면 DataTooShortException이 발생한다.
 *     이것은 의도된 동작으로 부족한 데이터는 포맷 오류로 보기 때문이다.
 *     연속된 데이터를 처리하기 위해 데이터가 클래스의 필드 크기보다 더 큰 경우는 허용된다.
 * </pre>
 */
public final class FlatMessageDeserializer<T> {
    //private final Logger log = LoggerFactory.getLogger(FlatMessageDeserializer.class);

    private final Class<?> jClass;
    private final Charset charset;
    private final List<FlatFieldInfo> fieldsList;

    private int length = 0;

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
     * 역직렬화 클래스 생성
     *
     * @param jClass  역직렬화 대상 class
     * @param charset 기본 캐릭터셋
     */
    public FlatMessageDeserializer(final Class<?> jClass, final Charset charset) {
        this.jClass = jClass;
        this.charset = charset;
        fieldsList = AnnotationFields.getInstance().getFlatFieldInfoList(jClass);
    }

    /**
     * 역직렬화 클래스 생성
     *
     * @param jClass   역직렬화 대상 class
     * @param charset  기본 캐릭터셋
     * @param dumpMode 디버깅 모드
     */
    public FlatMessageDeserializer(final Class<?> jClass, final Charset charset, final boolean dumpMode) {
        this.jClass = jClass;
        this.charset = charset;
        fieldsList = AnnotationFields.getInstance().getFlatFieldInfoList(jClass);
        this.dumpMode = dumpMode;
    }

    /**
     * @return bytesToObject 수행 후 처리한 길이 (반드시 bytesToObject 수행 후 호출)
     */
    public int getLength() {
        return length;
    }

    /**
     * bytes[]를 객체로 역직렬화한다.
     *
     * @param data   데이터
     * @param offset 시작 offset
     * @return 데이터가 입력된 Object
     */
    public T bytesToObject(final byte[] data, final int offset) {
        return bytesToObject(data, offset, data.length - offset);
    }

    /**
     * bytes[]를 객체로 역직렬화한다.
     *
     * @param data   데이터
     * @param offset 시작 offset
     * @param limit  최대 bytes, 최대 bytes를 초과하면 DataTooShortException 발생
     * @return 데이터가 입력된 Object
     * @throws InstanceCreateException  클래스 생성 실패
     * @throws DataTooShortException    데이터 길이가 짧음
     * @throws FieldDataAccessException 필드 데이터 접근 오류 (필드에 억세스할 수 없거나 적절한 파라마터를 가지는 getter/setter가 없음)
     * @throws DataConversionException  데이터 컨버전 오류
     */
    @SuppressWarnings("unchecked")
    public T bytesToObject(final byte[] data, final int offset, final int limit) {
        length = 0;
        fieldsDump.clear();
        int currentOffset = offset;

        // limit가 data의 길이보다 크면 오류
        if (limit < 0 || limit > (data.length - currentOffset))
            throw new DataTooShortException("limit: " + limit);

        final T object;
        try {
            object = (T) jClass.newInstance();
        } catch (Exception e) {
            // Bean 클래스는 인자가 없는 빈 public 생성자가 있어야 한다.
            throw new InstanceCreateException(jClass.getCanonicalName(), e);
        }

        // 각 필드별 처리
        for (FlatFieldInfo it : fieldsList) {
            final String clsName = FlatStringUtil.shortClassName(it.field.getDeclaringClass().getCanonicalName() + "." + it.name);
            final Object targetData;

            if (it.itemType == FlatFieldInfo.FieldTableType.FIELD) {
                // inner class 처리
                if (it.dataType == FieldDataType.CLASS) {
                    FlatMessageDeserializer<?> subClass = new FlatMessageDeserializer<>(it.dataClass, charset);

                    // 덤프(디버깅) 모드 셋팅
                    subClass.dumpMode = dumpMode;
                    subClass.tableLevel = tableLevel + 1;
                    subClass.tableName = it.dataClass.getSimpleName();

                    Object subObject = subClass.bytesToObject(data, currentOffset, limit - length);

                    currentOffset += subClass.getLength();
                    length += subClass.getLength();
                    if (length > limit)
                        throw new DataTooShortException(it.name + " (" + (length - limit) + " bytes short)");

                    ReflectionAccess.setField(it, object, subObject);

                    // 디버깅 데이터 처리
                    if (dumpMode) {
                        fieldsDump.addAll(subClass.getFieldsDump());
                    }
                    continue;
                }

                int itemLength = it.length;
                // item length가 미지정(-1)인 경우 limit만큼 끝까지 처리
                // 미지정 필드는 필드 목록의 마지막에 있어야 하며, 이후 필드가 존재하면 데이터 길이 부족으로 오류가 발생하므로 유의
                if (itemLength < 0) {
                    // length가 더 커서 오버런인 경우
                    if (limit < length)
                        throw new DataTooShortException(it.name + " (" + (length - limit) + " bytes short)");

                    itemLength = limit - length;
                }

                byte[] fieldData = new byte[itemLength];
                // 필드 길이보다 데이터 길이가 짧으면 오류
                // 오류는 의도한 동작이므로 라이브러리 수정 금지
                try {
                    System.arraycopy(data, currentOffset, fieldData, 0, fieldData.length);
                } catch (ArrayIndexOutOfBoundsException ae) {
                    // 필요한 데이터 bytes: itemLength, 남은 데이터 bytes: (data.length - offset)
                    throw new DataTooShortException(it.name + " (" + (fieldData.length - (data.length - currentOffset)) + " bytes short)", ae);
                }

                targetData = ConversionUtil.toObject(fieldData, it, charset);

                // 디버깅 데이터 생성
                if (dumpMode) {
                    String dumpData;
                    if (it.dataType == FieldDataType.BLOCK)
                        dumpData = new String(fieldData, charset);
                    else if (targetData instanceof String)
                        dumpData = (String) targetData;
                    else
                        dumpData = targetData.toString();

                    fieldsDump.add(new FlatMessageDump(it.field.getName(), it.name, length, currentOffset,
                            itemLength, dumpData, tableLevel, tableName, tableRow));
                }

                currentOffset += itemLength;
                length += itemLength;

            } else if (it.itemType == FlatFieldInfo.FieldTableType.TABLE_FIXED ||
                    it.itemType == FlatFieldInfo.FieldTableType.TABLE_VARIABLE) {
                int loopCount = ReflectionAccess.getTableLoopCount(it, object, jClass);

                try {
                    // 반복횟수만큼 생성
                    FlatMessageDeserializer<?> tableProcess = new FlatMessageDeserializer<>(it.tableClass, charset);

                    // 덤프(디버깅) 모드 셋팅
                    tableProcess.dumpMode = dumpMode;
                    tableProcess.tableLevel = tableLevel + 1;
                    tableProcess.tableName = it.tableClass.getSimpleName();

                    // array[] 인 경우
                    if (it.field.getType().isArray()) {
                        // array 생성
                        Object[] objArray = (Object[]) Array.newInstance(it.tableClass, loopCount);

                        for (int i = 0; i < loopCount; i++) {
                            tableProcess.tableRow++;
                            objArray[i] = it.tableClass.cast(tableProcess.bytesToObject(data, currentOffset, limit - length));

                            currentOffset += tableProcess.getLength();
                            length += tableProcess.getLength();

                            // 디버깅데이터
                            if (dumpMode) {
                                fieldsDump.addAll(tableProcess.getFieldsDump());
                            }
                        }
                        targetData = Array.newInstance(it.tableClass, 0).getClass().cast(objArray);
                    }
                    // List object인 경우
                    else {
                        List<Object> oList;

                        // List 인터페이스로 선언된 경우 ArrayList를 생성해서 처리
                        if (it.field.getType() == List.class) {
                            oList = new ArrayList<>();
                            for (int i = 0; i < loopCount; i++) {
                                tableProcess.tableRow++;
                                oList.add(it.tableClass.cast(tableProcess.bytesToObject(data, currentOffset, limit - length)));

                                currentOffset += tableProcess.getLength();
                                length += tableProcess.getLength();

                                // 디버깅데이터
                                if (dumpMode) {
                                    fieldsDump.addAll(tableProcess.getFieldsDump());
                                }
                            }
                        }
                        // List 상속한 클래스인 경우 해당 클래스로 생성
                        else {
                            // list.class 상속하지 않았으면 여기서 ClassCastException 발생함
                            oList = (List<Object>) it.field.getType().newInstance();
                            for (int i = 0; i < loopCount; i++) {
                                tableProcess.tableRow++;
                                oList.add(tableProcess.bytesToObject(data, currentOffset, limit - length));

                                currentOffset += tableProcess.getLength();
                                length += tableProcess.getLength();

                                // 디버깅데이터
                                if (dumpMode) {
                                    fieldsDump.addAll(tableProcess.getFieldsDump());
                                }
                            }
                        }

                        targetData = oList;
                    }

                } catch (Exception e) {
                    throw new DataConversionException(FlatStringUtil.shortClassName(jClass.getCanonicalName()) +
                            "." + it.field.getName() + ": " + e.getMessage(), e);
                }

            } else {
                throw new AnnotationDefineException("@FlatMessageTable(type = undefined): " +
                        FlatStringUtil.shortClassName(jClass.getCanonicalName()) + "." + it.field.getName());
            }

            if (length > limit)
                throw new DataTooShortException(it.name + " (" + (length - limit) + " bytes short)");

            // set data
            ReflectionAccess.setField(it, object, targetData);
        }

        return object;
    }

    /**
     * @param dumpMode 데이터 덤프(디버깅용) 셋팅여부, bytesToObject 호출 전에 셋팅해야 한다
     */
    public void setDumpMode(boolean dumpMode) {
        this.dumpMode = dumpMode;
    }

    /**
     * @return dumpMode가 활성화되어 있으면 마지막 bytesToObject에 대한 필드 덤프 목록
     */
    public List<FlatMessageDump> getFieldsDump() {
        return fieldsDump;
    }

}
