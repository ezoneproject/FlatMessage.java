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
 * 클래스 인스턴스를 생성할 수 없는 경우 (클래스 인스턴스는 인자가 없는 생성자가 있어야 함)
 */
public class InstanceCreateException extends RuntimeException {
    private static final long serialVersionUID = 7424628912000271226L;

    public InstanceCreateException(String message) {
        super(message);
    }

    public InstanceCreateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstanceCreateException(Throwable cause) {
        super(cause);
    }

    public InstanceCreateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
