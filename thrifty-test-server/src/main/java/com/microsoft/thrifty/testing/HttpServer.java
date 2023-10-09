package com.microsoft.thrifty.testing;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

import static com.microsoft.thrifty.testing.TestServer.getProtocolFactory;

public class HttpServer implements TestServerInterface {
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
}
