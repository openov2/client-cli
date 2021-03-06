/*
 * Copyright 2017 Huawei Technologies Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openo.client.cli.fw.error;

/**
 * Command schema not found.
 *
 */
public class OpenOCommandSchemaNotFound extends OpenOCommandException {

    private static final long serialVersionUID = -3919580583845280200L;

    public OpenOCommandSchemaNotFound(String cmdName) {
        this(cmdName, new Exception(""));
    }

    public OpenOCommandSchemaNotFound(String cmdName, Throwable throwable) {
        super("0x0019", "Command schema " + cmdName + " is not found, " + throwable.getMessage());
    }
}
