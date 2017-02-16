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

package org.openo.client.cli.fw.input;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openo.client.cli.fw.error.OpenOCommandInvalidParameterType;

public class ParameterTypeTest {
    @Test
    public void paramTypeGetTest() {

        try {
            assertTrue(ParameterType.JSON.equals(ParameterType.get("json"))
                    && ParameterType.YAML.equals(ParameterType.get("yaml"))
                    && ParameterType.STRING.equals(ParameterType.get("string"))
                    && ParameterType.LONG.equals(ParameterType.get("long"))
                    && ParameterType.URL.equals(ParameterType.get("url"))
                    && ParameterType.BOOL.equals(ParameterType.get("bool")));
        } catch (OpenOCommandInvalidParameterType e) {
            fail("Shouldn't have thrown this exception : " + e.getMessage());
        }

        try {
            ParameterType.get("name");
        } catch (OpenOCommandInvalidParameterType e) {
            assertTrue("0x0003::Parameter type name is invalid".equals(e.getMessage()));
        }

    }

}