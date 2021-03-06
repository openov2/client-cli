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

package org.openo.client.cli.fw.ad;

/**
 * Open-O Service credentials.
 */
public class OpenOCredentials {

    /*
     * OpenO Service username.
     */
    private String username;

    /*
     * OpenO Service password
     */
    private String password;

    /*
     * OpenO Service msb-url
     */
    private String msbUrl;

    /**
     * Open-O credentials with username and password.
     *
     * @param username
     *            user name
     * @param password
     *            password
     * @param msbUrl
     *            msb url
     */
    public OpenOCredentials(String username, String password, String msbUrl) {
        super();
        this.username = username;
        this.password = password;
        this.msbUrl = msbUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getMsbUrl() {
        return msbUrl;
    }
}
