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

package org.openo.client.cli.fw.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

import org.openo.client.cli.fw.OpenOCommand;
import org.openo.client.cli.fw.ad.OpenOCredentials;
import org.openo.client.cli.fw.ad.OpenOService;
import org.openo.client.cli.fw.cmd.OpenOHttpCommand;
import org.openo.client.cli.fw.cmd.OpenOSwaggerCommand;
import org.openo.client.cli.fw.conf.Constants;
import org.openo.client.cli.fw.error.OpenOCommandDiscoveryFailed;
import org.openo.client.cli.fw.error.OpenOCommandException;
import org.openo.client.cli.fw.error.OpenOCommandHelpFailed;
import org.openo.client.cli.fw.error.OpenOCommandHttpHeaderNotFound;
import org.openo.client.cli.fw.error.OpenOCommandHttpInvalidResponseBody;
import org.openo.client.cli.fw.error.OpenOCommandInvalidParameterType;
import org.openo.client.cli.fw.error.OpenOCommandInvalidParameterValue;
import org.openo.client.cli.fw.error.OpenOCommandInvalidPrintDirection;
import org.openo.client.cli.fw.error.OpenOCommandInvalidResultAttributeScope;
import org.openo.client.cli.fw.error.OpenOCommandInvalidSchema;
import org.openo.client.cli.fw.error.OpenOCommandInvalidSchemaVersion;
import org.openo.client.cli.fw.error.OpenOCommandParameterNameConflict;
import org.openo.client.cli.fw.error.OpenOCommandParameterNotFound;
import org.openo.client.cli.fw.error.OpenOCommandParameterOptionConflict;
import org.openo.client.cli.fw.error.OpenOCommandResultEmpty;
import org.openo.client.cli.fw.error.OpenOCommandResultMapProcessingFailed;
import org.openo.client.cli.fw.error.OpenOCommandSchemaNotFound;
import org.openo.client.cli.fw.http.HttpInput;
import org.openo.client.cli.fw.http.HttpResult;
import org.openo.client.cli.fw.input.OpenOCommandParameter;
import org.openo.client.cli.fw.input.ParameterType;
import org.openo.client.cli.fw.output.OpenOCommandResult;
import org.openo.client.cli.fw.output.OpenOCommandResultAttribute;
import org.openo.client.cli.fw.output.OpenOCommandResultAttributeScope;
import org.openo.client.cli.fw.output.PrintDirection;
import org.openo.client.cli.fw.output.ResultType;
import org.openo.client.cli.fw.run.OpenOCommandExecutor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Provides helper method to parse Yaml files and produce required objects.
 *
 */
public class OpenOCommandUtils {

    /**
     * Private constructor.
     */
    private OpenOCommandUtils() {

    }

    /**
     * Validates schema version.
     *
     * @param schemaName
     *            schema name
     * @param version
     *            schema version
     * @return map
     * @throws OpenOCommandInvalidSchemaVersion
     *             invalid schema version exception
     * @throws OpenOCommandInvalidSchema
     *             invalid schema
     * @throws OpenOCommandSchemaNotFound
     *             schema not found
     */
    public static Map<String, ?> validateSchemaVersion(String schemaName, String version) throws OpenOCommandException {
        InputStream inputStream = OpenOCommandUtils.class.getClassLoader().getResourceAsStream(schemaName);

        try {
            Resource resource = getExternalResource(schemaName, Constants.EXTERNAL_SCHEMA_PATH_PATERN);

            if (resource != null) {
                inputStream = resource.getInputStream();
            }

        } catch (IOException e) {
            throw new OpenOCommandSchemaNotFound(schemaName, e);
        }
        if (inputStream == null) {
            throw new OpenOCommandSchemaNotFound(schemaName);
        }

        Map<String, ?> values = null;
        try {
            values = (Map<String, ?>) new Yaml().load(inputStream);
        } catch (Exception e) {
            throw new OpenOCommandInvalidSchema(schemaName, e);
        }
        String schemaVersion = "";
        if (values.keySet().contains(Constants.OPENO_CMD_SCHEMA_VERSION)) {
            Object obj = values.get(Constants.OPENO_CMD_SCHEMA_VERSION);
            schemaVersion = obj.toString();
        }

        if (!version.equals(schemaVersion)) {
            throw new OpenOCommandInvalidSchemaVersion(schemaVersion);
        }

        return values;
    }

    /**
     * Retrieve OpenOCommand from schema.
     *
     * @param cmd
     *            OpenOCommand
     * @param schemaName
     *            schema name
     * @param includeDefault
     *            include if default
     * @throws OpenOCommandParameterNameConflict
     *             param name conflict exception
     * @throws OpenOCommandParameterOptionConflict
     *             param option conflict exception
     * @throws OpenOCommandInvalidParameterType
     *             invalid param type exception
     * @throws OpenOCommandInvalidPrintDirection
     *             invalid print direction exception
     * @throws OpenOCommandInvalidResultAttributeScope
     *             invalid scope exception
     * @throws OpenOCommandSchemaNotFound
     *             schema not found
     * @throws OpenOCommandInvalidSchema
     *             invalid schema
     * @throws OpenOCommandInvalidSchemaVersion
     *             invalid schema version
     */
    public static void loadSchema(OpenOCommand cmd, String schemaName, boolean includeDefault)
            throws OpenOCommandException {
        List<String> shortOptions = new ArrayList<>();
        List<String> longOptions = new ArrayList<>();
        List<String> names = new ArrayList<>();

        if (includeDefault) {
            loadSchema(cmd, Constants.DEFAULT_PARAMETER_FILE_NAME, shortOptions, longOptions, names);
        }

        loadSchema(cmd, schemaName, shortOptions, longOptions, names);

    }

    private static void loadSchema(OpenOCommand cmd, String schemaName, List<String> shortOptions,
            List<String> longOptions, List<String> names) throws OpenOCommandException {
        try {
            Map<String, ?> values = validateSchemaVersion(schemaName, cmd.getSchemaVersion());

            for (Map.Entry<String, ?> entry : values.entrySet()) {
                String key = entry.getKey();

                if (Constants.NAME.equals(key)) {
                    Object val = values.get(key);
                    cmd.setName(val.toString());
                } else if (Constants.DESCRIPTION.equals(key)) {
                    Object val = values.get(key);
                    cmd.setDescription(val.toString());
                } else if (Constants.SERVICE.equals(key)) {
                    Map<String, String> map = (Map<String, String>) values.get(key);
                    OpenOService srv = new OpenOService();

                    for (Map.Entry<String, String> entry1 : map.entrySet()) {
                        String key1 = entry1.getKey();

                        if (Constants.NAME.equals(key1)) {
                            srv.setName(map.get(key1));
                        } else if (Constants.VERSION.equals(key1)) {
                            srv.setVersion(map.get(key1));
                        } else if (Constants.NO_AUTH.equals(key1)) {
                            Object obj = map.get(key1);
                            srv.setNoAuth("true".equalsIgnoreCase(obj.toString()));
                        }
                    }

                    cmd.setService(srv);
                } else if (Constants.PARAMETERS.equals(key)) {
                    List<Map<String, String>> list = (ArrayList) values.get(key);

                    for (Map<String, String> map : list) {
                        OpenOCommandParameter param = new OpenOCommandParameter();

                        for (Map.Entry<String, String> entry1 : map.entrySet()) {
                            String key2 = entry1.getKey();

                            if (Constants.NAME.equals(key2)) {
                                if (names.contains(map.get(key2))) {
                                    throw new OpenOCommandParameterNameConflict(map.get(key2));
                                }
                                names.add(map.get(key2));
                                param.setName(map.get(key2));
                            } else if (Constants.DESCRIPTION.equals(key2)) {
                                param.setDescription(map.get(key2));
                            } else if (Constants.SHORT_OPTION.equals(key2)) {
                                if (shortOptions.contains(map.get(key2))) {
                                    throw new OpenOCommandParameterOptionConflict(map.get(key2));
                                }
                                shortOptions.add(map.get(key2));
                                param.setShortOption(map.get(key2));
                            } else if (Constants.LONG_OPTION.equals(key2)) {
                                if (longOptions.contains(map.get(key2))) {
                                    throw new OpenOCommandParameterOptionConflict(map.get(key2));
                                }
                                longOptions.add(map.get(key2));
                                param.setLongOption(map.get(key2));
                            } else if (Constants.DEFAULT_VALUE.equals(key2)) {
                                Object obj = map.get(key2);
                                param.setDefaultValue(obj.toString());
                            } else if (Constants.TYPE.equals(key2)) {
                                param.setParameterType(ParameterType.get(map.get(key2)));
                            } else if (Constants.IS_OPTIONAL.equals(key2)) {
                                if ("true".equalsIgnoreCase(String.valueOf(map.get(key2)))) {
                                    param.setOptional(true);
                                } else {
                                    param.setOptional(false);
                                }
                            } else if (Constants.IS_SECURED.equals(key2)) {
                                if ("true".equalsIgnoreCase(String.valueOf(map.get(key2)))) {
                                    param.setSecured(true);
                                } else {
                                    param.setSecured(false);
                                }
                            }
                        }
                        cmd.getParameters().add(param);

                    }
                } else if (Constants.RESULTS.equals(key)) {
                    Map<String, ?> valueMap = (Map<String, ?>) values.get(key);
                    OpenOCommandResult result = new OpenOCommandResult();
                    for (Map.Entry<String, ?> entry1 : valueMap.entrySet()) {
                        String key3 = entry1.getKey();

                        if (Constants.DIRECTION.equals(key3)) {
                            result.setPrintDirection(PrintDirection.get((String) valueMap.get(key3)));
                        } else if (Constants.ATTRIBUTES.equals(key3)) {
                            List<Map<String, String>> attrs = (ArrayList) valueMap.get(key3);

                            for (Map<String, String> map : attrs) {
                                OpenOCommandResultAttribute attr = new OpenOCommandResultAttribute();
                                for (Map.Entry<String, String> entry4 : map.entrySet()) {
                                    String key4 = entry4.getKey();

                                    if (Constants.NAME.equals(key4)) {
                                        attr.setName(map.get(key4));
                                    } else if (Constants.DESCRIPTION.equals(key4)) {
                                        attr.setDescription(map.get(key4));
                                    } else if (Constants.SCOPE.equals(key4)) {
                                        attr.setScope(OpenOCommandResultAttributeScope.get(map.get(key4)));
                                    } else if (Constants.TYPE.equals(key4)) {
                                        attr.setType(ParameterType.get(map.get(key4)));
                                    } else if (Constants.IS_SECURED.equals(key4)) {
                                        if ("true".equals(String.valueOf(map.get(key4)))) {
                                            attr.setSecured(true);
                                        } else {
                                            attr.setSecured(false);
                                        }
                                    }

                                }
                                result.getRecords().add(attr);
                            }
                        }
                    }
                    cmd.setResult(result);
                }
            }
        } catch (OpenOCommandException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenOCommandInvalidSchema(schemaName, e);
        }
    }

    /**
     * Load the schema.
     *
     * @param cmd
     *            OpenOSwaggerBasedCommand
     * @param schemaName
     *            schema name
     * @throws OpenOCommandParameterNameConflict
     *             param name conflict exception
     * @throws OpenOCommandParameterOptionConflict
     *             param option conflict exception
     * @throws OpenOCommandInvalidParameterType
     *             invalid param type exception
     * @throws OpenOCommandInvalidPrintDirection
     *             invalid print direction exception
     * @throws OpenOCommandInvalidResultAttributeScope
     *             invalid scope exception
     * @throws OpenOCommandSchemaNotFound
     *             schema not found
     * @throws OpenOCommandInvalidSchema
     *             invalid schema
     * @throws OpenOCommandInvalidSchemaVersion
     *             invalid schema version
     */
    public static void loadSchema(OpenOSwaggerCommand cmd, String schemaName) throws OpenOCommandException {
        try {
            Map<String, ?> values = (Map<String, ?>) validateSchemaVersion(schemaName, cmd.getSchemaVersion());
            Map<String, String> valueMap = (Map<String, String>) values.get(Constants.EXECUTOR);
            OpenOCommandExecutor exec = new OpenOCommandExecutor();

            for (Map.Entry<String, String> entry1 : valueMap.entrySet()) {
                String key1 = entry1.getKey();

                if (Constants.API.equals(key1)) {
                    exec.setApi(valueMap.get(key1));
                } else if (Constants.CLIENT.equals(key1)) {
                    exec.setClient(valueMap.get(key1));
                } else if (Constants.ENTITY.equals(key1)) {
                    exec.setEntity(valueMap.get(key1));
                } else if (Constants.EXCEPTION.equals(key1)) {
                    exec.setException(valueMap.get(key1));
                } else if (Constants.METHOD.equals(key1)) {
                    exec.setMethod(valueMap.get(key1));
                }
            }

            cmd.setExecutor(exec);
        } catch (OpenOCommandException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenOCommandInvalidSchema(schemaName, e);
        }
    }

    /**
     * Load the schema.
     *
     * @param cmd
     *            OpenOHttpCommand
     * @param schemaName
     *            schema name
     * @throws OpenOCommandParameterNameConflict
     *             param name conflict exception
     * @throws OpenOCommandParameterOptionConflict
     *             param option conflict exception
     * @throws OpenOCommandInvalidParameterType
     *             invalid param type exception
     * @throws OpenOCommandInvalidPrintDirection
     *             invalid print direction exception
     * @throws OpenOCommandInvalidResultAttributeScope
     *             invalid scope exception
     * @throws OpenOCommandSchemaNotFound
     *             schema not found
     * @throws OpenOCommandInvalidSchema
     *             invalid schema
     * @throws OpenOCommandInvalidSchemaVersion
     *             invalid schema version
     */
    public static void loadSchema(OpenOHttpCommand cmd, String schemaName) throws OpenOCommandException {
        try {
            Map<String, ?> values = (Map<String, ?>) validateSchemaVersion(schemaName, cmd.getSchemaVersion());
            Map<String, ?> valMap = (Map<String, ?>) values.get(Constants.HTTP);

            for (Map.Entry<String, ?> entry1 : valMap.entrySet()) {
                String key1 = entry1.getKey();
                if (Constants.REQUEST.equals(key1)) {
                    Map<String, ?> map = (Map<String, ?>) valMap.get(key1);

                    for (Map.Entry<String, ?> entry2 : map.entrySet()) {
                        String key2 = entry2.getKey();

                        if (Constants.URI.equals(key2)) {
                            Object obj = map.get(key2);
                            cmd.getInput().setUri(obj.toString());
                        } else if (Constants.MERHOD.equals(key2)) {
                            Object obj = map.get(key2);
                            cmd.getInput().setMethod(obj.toString());
                        } else if (Constants.BODY.equals(key2)) {
                            Object obj = map.get(key2);
                            cmd.getInput().setBody(obj.toString());
                        } else if (Constants.HEADERS.equals(key2)) {
                            Map<String, String> head = (Map<String, String>) map.get(key2);
                            cmd.getInput().setReqHeaders(head);
                        } else if (Constants.QUERIES.equals(key2)) {
                            Map<String, String> query = (Map<String, String>) map.get(key2);

                            cmd.getInput().setReqQueries(query);
                        }
                    }
                } else if (Constants.SUCCESS_CODES.equals(key1)) {
                    cmd.setSuccessStatusCodes((ArrayList) valMap.get(key1));
                } else if (Constants.RESULT_MAP.equals(key1)) {
                    cmd.setResultMap((Map<String, String>) valMap.get(key1));
                } else if (Constants.SAMPLE_RESPONSE.equals(key1)) {
                    // (mrkanag) implement sample response handling
                }
            }

        } catch (OpenOCommandException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenOCommandInvalidSchema(schemaName, e);
        }
    }

    /**
     * Returns Help.
     *
     * @param cmd
     *            OpenOCommand
     * @return help string
     * @throws OpenOCommandHelpFailed
     *             help failed exception
     */
    public static String help(OpenOCommand cmd) throws OpenOCommandHelpFailed {
        String help = "usage: openo " + cmd.getName();

        // Add description
        help += "\n\n" + cmd.getDescription();

        // Add service
        help += "\n\nOpen-O service: " + cmd.getService();

        // Add whole command
        String commandOptions = "";

        // Add parameters
        OpenOCommandResult paramTable = new OpenOCommandResult();
        paramTable.setPrintDirection(PrintDirection.LANDSCAPE);
        paramTable.setType(ResultType.TABLE);
        paramTable.setIncludeTitle(false);
        paramTable.setIncludeSeparator(false);

        OpenOCommandResultAttribute attrName = new OpenOCommandResultAttribute();
        attrName.setName(Constants.NAME);
        attrName.setDescription(Constants.NAME);
        attrName.setScope(OpenOCommandResultAttributeScope.SHORT);
        paramTable.getRecords().add(attrName);

        OpenOCommandResultAttribute attrDescription = new OpenOCommandResultAttribute();
        attrDescription.setName(Constants.DESCRIPTION);
        attrDescription.setDescription(Constants.DESCRIPTION);
        attrDescription.setScope(OpenOCommandResultAttributeScope.SHORT);
        paramTable.getRecords().add(attrDescription);

        int newLineOptions = 0;
        for (OpenOCommandParameter param : cmd.getParameters()) {
            // First column Option or positional args
            String optFirstCol;
            if (newLineOptions == 3) {
                newLineOptions = 0;
                commandOptions += "\n";
            }

            if (param.getShortOption() != null || param.getLongOption() != null) {
                optFirstCol = OpenOCommandParameter.printShortOption(param.getShortOption()) + " | "
                        + OpenOCommandParameter.printLongOption(param.getLongOption());
                commandOptions += "[" + optFirstCol + "] ";
            } else {
                optFirstCol = param.getName();
                commandOptions += "<" + optFirstCol + "> ";
            }

            newLineOptions++;

            attrName.getValues().add(optFirstCol);

            // Second column description
            String optSecondCol = param.getDescription().trim();
            if (!optSecondCol.endsWith(".")) {
                optSecondCol += ".";
            }
            optSecondCol += " It is of type " + param.getParameterType().name() + ".";

            if (param.getParameterType().equals(ParameterType.JSON)
                    || param.getParameterType().equals(ParameterType.YAML)) {
                optSecondCol += " It's recommended to input the complete path of the file, which is having the value for it.";
            }
            if (param.isOptional()) {
                optSecondCol += " It is optional.";
            }

            String defaultMsg = " By default, it is ";
            if (param.isDefaultValueAnEnv()) {
                optSecondCol += defaultMsg + "read from environment variable " + param.getEnvVarNameFromDefaultValue()
                        + ".";
            } else if (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty()) {
                optSecondCol += defaultMsg + param.getDefaultValue() + ".";
            }

            if (param.isSecured()) {
                optSecondCol += " Secured.";
            }
            // (mrkanag) Add help msg for reading default value from env
            attrDescription.getValues().add(optSecondCol);
        }

        try {
            help += "\n\nOptions:\n" + commandOptions + "\nwhere,\n" + paramTable.print();
        } catch (OpenOCommandException e) {
            throw new OpenOCommandHelpFailed(e);
        }

        // Add results
        OpenOCommandResult resultTable = new OpenOCommandResult();
        resultTable.setPrintDirection(PrintDirection.PORTRAIT);
        resultTable.setType(ResultType.TABLE);
        resultTable.setIncludeTitle(false);
        resultTable.setIncludeSeparator(false);

        for (OpenOCommandResultAttribute attr : cmd.getResult().getRecords()) {
            OpenOCommandResultAttribute attrHelp = new OpenOCommandResultAttribute();
            attrHelp.setName(attr.getName());
            attrHelp.setDescription(attr.getDescription());
            String msg = attr.getDescription() + " and is of type " + attr.getType().name() + ".";
            if (attr.isSecured()) {
                msg += " It is secured.";
            }
            attrHelp.getValues().add(msg);
            attrHelp.setType(attr.getType());
            resultTable.getRecords().add(attrHelp);
        }
        try {
            help += "\n\nResults:\n" + resultTable.print();
        } catch (OpenOCommandException e) {
            throw new OpenOCommandHelpFailed(e);
        }

        // Error
        help += "\n\nError:\nOn error, it prints <HTTP STATUS CODE>::<ERROR CODE>::<ERROR MESSAGE>\n";
        return help;
    }

    /**
     * Helps to create OpenOCredentials from default params.
     *
     * @param params
     *            list of parameters
     * @return OpenOCredentials
     * @throws OpenOCommandInvalidParameterValue
     *             exception
     */
    public static OpenOCredentials fromParameters(List<OpenOCommandParameter> params)
            throws OpenOCommandInvalidParameterValue {
        Map<String, String> paramMap = new HashMap<>();

        for (OpenOCommandParameter param : params) {
            paramMap.put(param.getName(), param.getValue().toString());
        }

        return new OpenOCredentials(paramMap.get(Constants.DEAFULT_PARAMETER_USERNAME),
                paramMap.get(Constants.DEAFULT_PARAMETER_PASS_WORD),
                paramMap.get(Constants.DEAFULT_PARAMETER_MSB_URL));
    }

    /**
     * Create Dict from list of Parameters.
     *
     * @param inputs
     *            list of parameters
     * @return map
     */
    public static Map<String, OpenOCommandParameter> getInputMap(List<OpenOCommandParameter> inputs) {
        Map<String, OpenOCommandParameter> map = new HashMap<>();
        for (OpenOCommandParameter param : inputs) {
            map.put(param.getName(), param);
        }
        return map;
    }

    /**
     * Discover the Open-O commands.
     *
     * @return list
     */
    public static List<Class<OpenOCommand>> findOpenOCommands() {
        ServiceLoader<OpenOCommand> loader = ServiceLoader.load(OpenOCommand.class);
        List<Class<OpenOCommand>> clss = new ArrayList<>();
        for (OpenOCommand implClass : loader) {
            clss.add((Class<OpenOCommand>) implClass.getClass());
        }

        return clss;
    }

    /**
     * sort the set.
     *
     * @param col
     *            set
     * @return list
     */
    public static List<String> sort(Set<String> col) {
        List<String> results = new ArrayList<>();
        results.addAll(col);
        Collections.sort(results);
        return results;
    }

    /**
     * Flatten the json list.
     *
     * @param jsons
     *            list json strings
     * @return list
     */
    public static List<String> jsonFlatten(List<String> jsons) {
        List<String> results = new ArrayList<>();
        for (String json : jsons) {
            try {
                results.add(JsonPath.parse(json).jsonString());
            } catch (Exception e) { // NOSONAR
                results.add(json);
            }
        }

        return results;
    }

    /**
     * Construct method name.
     *
     * @param name
     *            name
     * @param prefix
     *            prefix
     * @return string
     */
    public static String formMethodNameFromAttributeName(String name, String prefix) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String methodName = prefix;
        for (String tk : name.split("-")) {
            methodName += Character.toString(tk.charAt(0)).toUpperCase();
            methodName += tk.substring(1);
        }
        return methodName;
    }

    private static String replaceLineFromInputParameters(String line, Map<String, OpenOCommandParameter> params)
            throws OpenOCommandException {
        String result = "";

        if (!line.contains("${")) {
            return line;
        }

        int currentIdx = 0;
        while (currentIdx < line.length()) {
            int idxS = line.indexOf("${", currentIdx);
            if (idxS == -1) {
                result += line.substring(currentIdx);
                break;
            }
            int idxE = line.indexOf("}", idxS);
            String paramName = line.substring(idxS + 2, idxE);
            paramName = paramName.trim();
            if (!params.containsKey(paramName)) {
                throw new OpenOCommandParameterNotFound(paramName);
            }

            String value = params.get(paramName).getValue().toString();

            OpenOCommandParameter param = params.get(paramName);
            if (ParameterType.ARRAY.equals(param.getParameterType())
                    || ParameterType.MAP.equals(param.getParameterType())
                    || ParameterType.JSON.equals(param.getParameterType())
                    || ParameterType.YAML.equals(param.getParameterType())) {
                // ignore the front and back double quotes in json body
                result += line.substring(currentIdx, idxS - 1) + value;
                currentIdx = idxE + 2;
            } else {
                result += line.substring(currentIdx, idxS) + value;
                currentIdx = idxE + 1;
            }
        }

        return result;
    }

    private static ArrayList<String> replaceLineFromOutputResults(String line, HttpResult resultHttp)
            throws OpenOCommandHttpHeaderNotFound, OpenOCommandHttpInvalidResponseBody,
            OpenOCommandResultMapProcessingFailed, OpenOCommandResultEmpty {
        String headerProcessedLine = "";

        ArrayList<String> result = new ArrayList<>();
        if (!line.contains("$b{") && !line.contains("$h{")) {
            result.add(line);
            return result;
        }

        /**
         * In case of empty response body [] or {}
         **/
        if (resultHttp.getBody().length() <= 2) {
            return result;
        }

        /**
         * Process headers macros : line: $h{abc}-$b{$.[*].xyz} , After processing line will be [abc's
         * value]-$b{$.[*].xyz}
         **/
        int currentIdx = 0;
        while (currentIdx < line.length()) {
            int idxS = line.indexOf("$h{", currentIdx);
            if (idxS == -1) {
                headerProcessedLine += line.substring(currentIdx);
                break;
            }
            int idxE = line.indexOf("}", idxS);
            String headerName = line.substring(idxS + 3, idxE);
            headerName = headerName.trim();
            if (!resultHttp.getRespHeaders().containsKey(headerName)) {
                throw new OpenOCommandHttpHeaderNotFound(headerName);
            }
            String value = resultHttp.getRespHeaders().get(headerName);

            headerProcessedLine += line.substring(currentIdx, idxS) + value;
            currentIdx = idxE + 1;
        }

        // Process body jsonpath macros
        List<Object> values = new ArrayList<>();
        String bodyProcessedPattern = "";
        currentIdx = 0;
        int maxRows = 1; // in normal case, only one row will be there
        while (currentIdx < headerProcessedLine.length()) {
            int idxS = headerProcessedLine.indexOf("$b{", currentIdx);
            if (idxS == -1) {
                bodyProcessedPattern += headerProcessedLine.substring(currentIdx);
                break;
            }
            int idxE = headerProcessedLine.indexOf("}", idxS);
            String jsonPath = headerProcessedLine.substring(idxS + 3, idxE);
            jsonPath = jsonPath.trim();
            try {
                // JSONArray or String
                Object value = JsonPath.read(resultHttp.getBody(), jsonPath);
                if (value instanceof JSONArray) {
                    JSONArray arr = (JSONArray) value;
                    if (arr.size() > maxRows) {
                        maxRows = arr.size();
                    }
                }
                bodyProcessedPattern += headerProcessedLine.substring(currentIdx, idxS) + "%s";
                values.add(value);
                currentIdx = idxE + 1;
            } catch (Exception e) {
                throw new OpenOCommandHttpInvalidResponseBody(jsonPath, e);
            }
        }

        if (bodyProcessedPattern.isEmpty()) {
            result.add(headerProcessedLine);
            return result;
        } else {
            for (int i = 0; i < maxRows; i++) {
                currentIdx = 0;
                String bodyProcessedLine = "";
                int positionalIdx = 0; // %s positional idx
                while (currentIdx < bodyProcessedPattern.length()) {
                    int idxS = bodyProcessedPattern.indexOf("%s", currentIdx);
                    if (idxS == -1) {
                        bodyProcessedLine += bodyProcessedPattern.substring(currentIdx);
                        break;
                    }
                    int idxE = idxS + 2; // %s
                    try {
                        Object value = values.get(positionalIdx);
                        String valueS = String.valueOf(value);
                        if (value instanceof JSONArray) {
                            JSONArray arr = (JSONArray) value;
                            if (!arr.isEmpty()) {
                                valueS = arr.get(i).toString();
                            } else {
                                throw new OpenOCommandResultEmpty();
                            }
                        }

                        bodyProcessedLine += bodyProcessedPattern.substring(currentIdx, idxS) + valueS;
                        currentIdx = idxE;
                        positionalIdx++;
                    } catch (OpenOCommandResultEmpty e) {
                        throw e;
                    } catch (Exception e) {
                        throw new OpenOCommandResultMapProcessingFailed(line, e);
                    }
                }
                result.add(bodyProcessedLine);
            }

            return result;
        }
    }

    /**
     * Set argument to param value.
     *
     * @param params
     *            map
     * @param input
     *            HttpInput
     * @return HttpInput
     * @throws OpenOCommandParameterNotFound
     *             exception
     * @throws OpenOCommandInvalidParameterValue
     *             exception
     */
    public static HttpInput populateParameters(Map<String, OpenOCommandParameter> params, HttpInput input)
            throws OpenOCommandException {
        HttpInput inp = new HttpInput();
        for (OpenOCommandParameter param : params.values()) {
            if (ParameterType.BINARY.equals(param.getParameterType())) {
                inp.setBinaryData(true);
                break;
            }
        }
        inp.setBody(replaceLineFromInputParameters(input.getBody(), params));
        inp.setUri(replaceLineFromInputParameters(input.getUri(), params));
        inp.setMethod(input.getMethod().toLowerCase());
        for (String h : input.getReqHeaders().keySet()) {
            String value = input.getReqHeaders().get(h);
            inp.getReqHeaders().put(h, replaceLineFromInputParameters(value, params));
        }

        for (String h : input.getReqQueries().keySet()) {
            String value = input.getReqQueries().get(h);
            inp.getReqQueries().put(h, replaceLineFromInputParameters(value, params));
        }

        return inp;
    }

    /**
     * Populate result.
     *
     * @param resultMap
     *            map
     * @param resultHttp
     *            HttpResult
     * @return map
     * @throws OpenOCommandHttpHeaderNotFound
     *             header not found exception
     * @throws OpenOCommandHttpInvalidResponseBody
     *             invalid response body exception
     * @throws OpenOCommandResultMapProcessingFailed
     *             map processing failed exception
     */
    public static Map<String, ArrayList<String>> populateOutputs(Map<String, String> resultMap, HttpResult resultHttp)
            throws OpenOCommandException {
        Map<String, ArrayList<String>> resultsProcessed = new HashMap<>();

        for (Entry<String, String> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            resultsProcessed.put(key, replaceLineFromOutputResults(resultMap.get(key), resultHttp));
        }

        return resultsProcessed;
    }

    /**
     * Find external schema files.
     *
     * @return list ExternalSchema
     * @throws OpenOCommandDiscoveryFailed
     *             exception
     * @throws OpenOCommandInvalidSchema
     *             exception
     */
    public static List<ExternalSchema> findAllExternalSchemas() throws OpenOCommandException {
        List<ExternalSchema> extSchemas = new ArrayList<>();
        try {
            Resource[] res = getExternalResources(Constants.EXTERNAL_SCHEMA_PATH_PATERN);
            if (res != null && res.length > 0) {
                Map<String, ?> resourceMap;
                for (Resource resource : res) {
                    resourceMap = getExternalSchemaMap(resource);
                    if (resourceMap != null && resourceMap.size() > 0) {
                        ExternalSchema schema = new ExternalSchema();
                        schema.setSchemaName(resource.getFilename());
                        schema.setCmdName((String) resourceMap.get(Constants.NAME));
                        Object obj = resourceMap.get(Constants.OPENO_CMD_SCHEMA_VERSION);
                        schema.setVersion(obj.toString());
                        extSchemas.add(schema);
                    }
                }
            }
        } catch (IOException e) {
            throw new OpenOCommandDiscoveryFailed(Constants.EXTERNAL_SCHEMA_DIRECTORY, e);
        }

        return extSchemas;
    }

    /**
     * Returns all resources available under certain directory in class-path.
     *
     * @param pattern
     *            search pattern
     * @return resources found resources
     * @throws IOException
     *             exception
     */
    public static Resource[] getExternalResources(String pattern) throws IOException {
        ClassLoader cl = OpenOCommandUtils.class.getClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
        return resolver.getResources("classpath*:" + pattern);
    }

    /**
     * Returns a resource available under certain directory in class-path.
     *
     * @param pattern
     *            search pattern
     * @return found resource
     * @throws IOException
     *             exception
     */
    public static Resource getExternalResource(String fileName, String pattern) throws IOException {
        Resource[] resources = getExternalResources(pattern);
        if (resources != null && resources.length > 0) {
            for (Resource res : resources) {
                if (res.getFilename().equals(fileName)) {
                    return res;
                }
            }
        }

        return null;
    }

    /**
     * Get schema map.
     *
     * @param resource
     *            resource obj
     * @return map
     * @throws OpenOCommandInvalidSchema
     *             exception
     */
    public static Map<String, ?> getExternalSchemaMap(Resource resource) throws OpenOCommandInvalidSchema {
        Map<String, ?> values = null;
        try {
            values = (Map<String, ?>) new Yaml().load(resource.getInputStream());
        } catch (Exception e) {
            throw new OpenOCommandInvalidSchema(resource.getFilename(), e);
        }
        return values;
    }

    /**
     * Persist the external schema details.
     *
     * @param schemas
     *            list
     * @throws OpenOCommandDiscoveryFailed
     *             exception
     */
    public static void persist(List<ExternalSchema> schemas) throws OpenOCommandDiscoveryFailed {
        if (schemas != null) {
            try {
                Resource[] resources = getExternalResources(Constants.EXTERNAL_DISCOVERY_DIRECTORY);
                if (resources != null && resources.length == 1) {
                    String path = resources[0].getURI().getPath();
                    File file = new File(path + File.separator + Constants.EXTERNAL_DISCOVERY_FILE);
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writerWithDefaultPrettyPrinter().writeValue(file, schemas);
                }
            } catch (IOException e1) {
                throw new OpenOCommandDiscoveryFailed(Constants.EXTERNAL_DISCOVERY_DIRECTORY,
                        Constants.EXTERNAL_DISCOVERY_FILE, e1);
            }
        }
    }

    /**
     * Check if json file discovered or not.
     *
     * @return boolean
     * @throws OpenOCommandDiscoveryFailed
     *             exception
     */
    public static boolean isJsonFileDiscovered() throws OpenOCommandDiscoveryFailed {
        Resource resource = null;
        try {
            resource = getExternalResource(Constants.EXTERNAL_DISCOVERY_FILE,
                    Constants.EXTERNAL_DISCOVERY_DIRECTORY_PATTERN);
            if (resource != null) {
                return true;
            }
        } catch (IOException e) {
            throw new OpenOCommandDiscoveryFailed(Constants.EXTERNAL_DISCOVERY_DIRECTORY,
                    Constants.EXTERNAL_DISCOVERY_FILE, e);
        }

        return false;
    }

    /**
     * Load the previous discovered json file.
     *
     * @return list
     * @throws OpenOCommandInvalidSchema
     *             exception
     * @throws OpenOCommandDiscoveryFailed
     *             exception
     */
    public static List<ExternalSchema> loadExternalSchemasFromJson() throws OpenOCommandException {
        List<ExternalSchema> schemas = new ArrayList<>();
        if (!isJsonFileDiscovered()) {
            schemas = findAllExternalSchemas();
            if (!schemas.isEmpty()) {
                persist(schemas);
            }
        } else {
            try {
                Resource resource = getExternalResource(Constants.EXTERNAL_DISCOVERY_FILE,
                        Constants.EXTERNAL_DISCOVERY_DIRECTORY_PATTERN);
                if (resource != null) {
                    File file = new File(resource.getURI().getPath());
                    ObjectMapper mapper = new ObjectMapper();
                    ExternalSchema[] list = mapper.readValue(file, ExternalSchema[].class);
                    schemas.addAll(Arrays.asList(list));
                }
            } catch (IOException e) {
                throw new OpenOCommandDiscoveryFailed(Constants.EXTERNAL_DISCOVERY_DIRECTORY,
                        Constants.EXTERNAL_DISCOVERY_FILE, e);
            }
        }

        return schemas;
    }

    /**
     * Fetch a particular schema details.
     *
     * @param cmd
     *            command name
     * @return ExternalSchema obj
     * @throws OpenOCommandInvalidSchema
     *             exception
     * @throws OpenOCommandDiscoveryFailed
     *             exception
     */
    public static ExternalSchema loadExternalSchemaFromJson(String cmd) throws OpenOCommandException {
        List<ExternalSchema> list = loadExternalSchemasFromJson();
        ExternalSchema schemaStr = null;
        if (list != null) {
            for (ExternalSchema schema : list) {
                if (cmd.equals(schema.getCmdName())) {
                    schemaStr = schema;
                    break;
                }
            }
        }
        return schemaStr;
    }
}
