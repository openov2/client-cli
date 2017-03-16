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
 * Command result initialization failed.
 *
 */
public class OpenOCommandResultInitialzationFailed extends OpenOCommandException {

    private static final long serialVersionUID = 8580121615330415123L;

    public OpenOCommandResultInitialzationFailed(String cmd, String error) {
        super("0x0022", "Command " + cmd + " result format is failed, " + error);
    }

    public OpenOCommandResultInitialzationFailed(String cmd, Throwable throwable) {
        this(cmd, throwable.getMessage());
    }
}
