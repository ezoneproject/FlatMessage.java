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
 * 어노테이션 사용법이 잘못된 경우 발생하는 에외
 */
public final class AnnotationDefineException extends RuntimeException {
    private static final long serialVersionUID = -6168930180877176095L;

    public AnnotationDefineException(String message) {
        super(message);
    }

    public AnnotationDefineException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnnotationDefineException(Throwable cause) {
        super(cause);
    }

    public AnnotationDefineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
