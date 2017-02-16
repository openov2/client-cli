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

package org.openo.client.cli.main;

import org.aspectj.lang.annotation.After;
import org.junit.Ignore;
import org.junit.Test;

public class OpenOCliMainTest {

    OpenOCli cli = null;

    @After(value = "")
    public void cleanup() {
        if (this.cli != null) {
            if (cli.getExitCode() != 0) {
                // Fail test case
            }
        }
    }

    private void handle(String[] args) {
        cli = new OpenOCli(args);
        cli.handle();
    }

    @Test
    public void testHelp() {
        this.handle(new String[] { "--help" });
    }

    @Test
    public void testHelpShort() {
        this.handle(new String[] { "-h" });
    }

    @Test
    public void testVersion() {
        this.handle(new String[] { "--version" });
    }

    @Test
    public void testVersionShort() {
        this.handle(new String[] { "--v" });
    }

    @Test
    public void testHelpSampleCommand() {
        this.handle(new String[] { "sample-test", "--help" });
    }

    @Test
    public void testHelpSampleCommandShort() {
        this.handle(new String[] { "sample-test", "-h" });
    }

    @Test
    public void testHelpServiceListCommandShort() {
        this.handle(new String[] { "sample-test", "-h" });
    }

    @Test
    public void testHelpUserCreateCommand() {
        this.handle(new String[] { "user-create", "--help" });
    }

    @Test
    @Ignore
    public void testUserCreateCommand() {
        this.handle(new String[] { "user-create", "-u", "admin", "-p", "Changeme_123", "-m", "http://192.168.99.100:80",
                "--username", "test", "--password", "sss", "--description", "test user" , "-d"});
    }

    @Test
    public void testHelpVimShowCommand() {
        this.handle(new String[] { "vim-show", "--help" });
    }

    @Test
    @Ignore
    public void testVimListCommand() {
        this.handle(new String[] { "vim-list", "-u", "admin", "-p", "Changeme_123", "-m", "http://192.168.99.100:80",
                "--long", "--format", "table" });
    }

    @Test
    public void tesVersionServiceListCommand() {
        this.handle(new String[] { "microservice-list", "--version" });
    }

    @Test
    public void tesVersionServiceListCommandShort() {
        this.handle(new String[] { "microservice-list", "-v" });
    }

    @Test
    public void tesVersionServiceListCommandShort1() {
        this.handle(new String[] { "microservice-list", "-h" });
    }

    @Test
    @Ignore
    public void testServiceListCommand() {
        this.handle(new String[] { "microservice-list", "-u", "admin", "-p", "Changeme_123", "-m",
                "http://192.168.99.100:80", "--long" });
    }

    @Test
    @Ignore
    public void testServiceCreateCommand() {
        this.handle(new String[] { "microservice-create", "-u", "admin", "-p", "Changeme_123", "-m",
                "http://192.168.99.100:80", "--service-name", "test-service", "--service-version", "v1",
                "--service-url", "/openapi/sampletest/v1", "127.0.0.1", "8080", "--debug", "--long" });
    }

    @Test
    @Ignore
    public void testServiceShowCommand() {
        this.handle(new String[] { "microservice-show", "-u", "admin", "-p", "Changeme_123", "-m",
                "http://192.168.99.100:80", "--service-name", "test-service", "--service-version", "v1", "--debug",
                "--long" });
    }

    @Test
    @Ignore
    public void testServiceDeleteCommand() {
        this.handle(new String[] { "microservice-delete", "-u", "admin", "-p", "Changeme_123", "-m",
                "http://192.168.99.100:80", "--service-name", "test-service", "--service-version", "v1", "--debug",
                "--long" });
    }

    @Test
    @Ignore
    public void testServiceCreateHelpCommand() {
        this.handle(new String[] { "microservice-create", "--help" });
    }

}
