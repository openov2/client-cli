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

package org.openo.client.cli.fw;

import org.openo.client.cli.fw.error.OpenOCommandException;
import org.openo.client.cli.fw.error.OpenOCommandExecutionFailed;
import org.openo.client.cli.fw.error.OpenOCommandInvalidParameterType;
import org.openo.client.cli.fw.error.OpenOCommandInvalidPrintDirection;
import org.openo.client.cli.fw.error.OpenOCommandInvalidResultAttributeScope;
import org.openo.client.cli.fw.error.OpenOCommandInvalidSchema;
import org.openo.client.cli.fw.error.OpenOCommandInvalidSchemaVersion;
import org.openo.client.cli.fw.error.OpenOCommandParameterNameConflict;
import org.openo.client.cli.fw.error.OpenOCommandParameterOptionConflict;
import org.openo.client.cli.fw.error.OpenOCommandSchemaNotFound;
import org.openo.client.cli.fw.http.HttpInput;
import org.openo.client.cli.fw.http.HttpResult;
import org.openo.client.cli.fw.output.OpenOCommandResultAttribute;
import org.openo.client.cli.fw.utils.OpenOCommandUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenO Command.
 *
 */
public class OpenOHttpCommand extends OpenOCommand {
    public static final String HTTP = "http";
    public static final String REQUEST = "request";
    public static final String SAMPLE_RESPONSE = "sample_response";
    public static final String SUCCESS_CODES = "success_codes";
    public static final String RESULT_MAP = "result_map";

    private static final String OPENO_CMD_SCHEMA_VERSION_VALUE = "1.0";

    private HttpInput input = new HttpInput();

    private ArrayList<Integer> successStatusCodes = new ArrayList<>();

    private Map<String, String> resultMap = new HashMap<>();

    public void setInput(HttpInput input) {
        this.input = input;
    }

    public String getSchemaVersion() {
        return OPENO_CMD_SCHEMA_VERSION_VALUE;
    }

    public void setSuccessStatusCodes(ArrayList<Integer> successStatusCodes) {
        this.successStatusCodes = successStatusCodes;
    }

    public void setResultMap(Map<String, String> resultMap) {
        this.resultMap = resultMap;
    }

    public HttpInput getInput() {
        return input;
    }

    public ArrayList<Integer> getSuccessStatusCodes() {
        return successStatusCodes;
    }

    public Map<String, String> getResultMap() {
        return resultMap;
    }

    @Override
    protected void initializeProfileSchema() throws OpenOCommandParameterNameConflict,
            OpenOCommandParameterOptionConflict, OpenOCommandInvalidParameterType, OpenOCommandInvalidPrintDirection,
            OpenOCommandInvalidResultAttributeScope, OpenOCommandSchemaNotFound, OpenOCommandInvalidSchema,
            OpenOCommandInvalidSchemaVersion {
        OpenOCommandUtils.loadSchema(this, this.getSchemaName());
    }

    @Override
    protected void run() throws OpenOCommandException {
        HttpInput input = OpenOCommandUtils.populateParameters(this.getParametersMap(), this.getInput());
        input.setUri(this.authClient.getServiceBasePath(this.getService()) + input.getUri());

        HttpResult output = this.authClient.run(input);

        this.getResult().setOutput(output);
        if (!this.getSuccessStatusCodes().contains(output.getStatus())) {
            throw new OpenOCommandExecutionFailed(this.getName(), output.getBody(), output.getStatus());
        }

        Map<String, ArrayList<String>> results = OpenOCommandUtils.populateOutputs(this.getResultMap(), output);

        for (OpenOCommandResultAttribute attr : this.getResult().getRecords()) {
            attr.setValues(results.get(attr.getName()));
        }
    }
}