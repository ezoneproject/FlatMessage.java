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

/**
 * 필드 데이터 접근 오류 (필드에 억세스할 수 없거나 적절한 파라마터를 가지는 getter/setter가 없음)
 */
public final class FieldDataAccessException extends RuntimeException {
    private static final long serialVersionUID = 96899205054055997L;

    public FieldDataAccessException(String message) {
        super(message);
    }

    public FieldDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public FieldDataAccessException(Throwable cause) {
        super(cause);
    }

    public FieldDataAccessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
