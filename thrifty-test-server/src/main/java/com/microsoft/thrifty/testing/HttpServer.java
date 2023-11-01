/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.testing;

import java.io.Closeable;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

import static com.microsoft.thrifty.testing.TestServer.getProtocolFactory;

public class HttpServer implements TestServerInterface, Closeable {
    private Tomcat tomcat;

    @Override
    public void run(ServerProtocol protocol, ServerTransport transport) {
        if (transport != ServerTransport.HTTP) {
            throw new IllegalArgumentException("only http transport supported");
        }
        this.tomcat = new Tomcat();
        tomcat.setBaseDir(System.getProperty("user.dir") + "\\build");
        tomcat.setPort(0);
        tomcat.getHost().setAutoDeploy(false);

        String contextPath = "/test";
        StandardContext context = new StandardContext();
        context.setPath(contextPath);
        context.addLifecycleListener(new Tomcat.FixContextListener());
        tomcat.getHost().addChild(context);
        tomcat.addServlet(contextPath, "testServlet", new TestServlet(getProtocolFactory(protocol)));
        context.addServletMappingDecoded("/service", "testServlet");
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    @Override
    public void close() {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    public void await() {
        tomcat.getServer().await();
    }
}
