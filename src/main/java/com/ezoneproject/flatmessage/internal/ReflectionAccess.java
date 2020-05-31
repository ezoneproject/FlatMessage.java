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

import com.ezoneproject.flatmessage.DataConversionException;
import com.ezoneproject.flatmessage.FieldDataAccessException;

public final class ReflectionAccess {
    private ReflectionAccess() {
    }

    /**
     * Set data
     */
    public static void setField(FlatFieldInfo field, Object target, Object data) {
        try {
            if (field.isPublic)
                field.field.set(target, data);
            else
                field.setterMethod.invoke(target, data);
        } catch (Exception e) {
            throw new FieldDataAccessException(field.name + " (" + (field.isPublic ? field.field.getName() + " field set" :
                    field.setterMethod.getName()) + ")", e);
        }
    }

    /**
     * Get data
     */
    public static Object getField(FlatFieldInfo field, Object source) {
        try {
            if (field.isPublic)
                return field.field.get(source);
            else
                return field.getterMethod.invoke(source);
        } catch (Exception e) {
            throw new FieldDataAccessException(field.name + " (" + (field.isPublic ? field.field.getName() + " field get" :
                    field.getterMethod.getName()) + ")", e);
        }
    }

    /**
     * Get table loop count
     */
    public static int getTableLoopCount(FlatFieldInfo it, Object object, Class<?> jClass) {
        int loopCount;
        if (it.itemType == FlatFieldInfo.FieldTableType.TABLE_FIXED)
            // 고정길이 테이블은 반복횟수가 고정됨
            loopCount = it.tableLoopCount;
        else
            // 가변길이 테이블은 필드값을 Get, 필드형태는 int 또는 Integer이어야 함
            loopCount = (int) ReflectionAccess.getField(it.tableLoopField, object);

        if (loopCount < 0)
            throw new DataConversionException("Table loop count is negative value[" + loopCount + "]: "
                    + it.tableLoopField.field.getName());

        return loopCount;
    }

}
