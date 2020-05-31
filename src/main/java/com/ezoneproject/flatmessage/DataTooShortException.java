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
 * 데이터가 지정한 길이보다 짧은 경우 발생하는 예외
 */
public class DataTooShortException extends RuntimeException {
    private static final long serialVersionUID = 2053297504142090662L;

    public DataTooShortException() {
        super();
    }

    public DataTooShortException(String message) {
        super(message);
    }

    public DataTooShortException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataTooShortException(Throwable cause) {
        super(cause);
    }

    public DataTooShortException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
