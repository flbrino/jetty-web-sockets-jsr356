package com.example.ws;

import com.example.config.KerberizedAppConfig;
import com.example.util.Util;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class KerberizedServerStarter {
  private static final Logger log = Logger.getLogger(KerberizedServerStarter.class.getName());

  public static void main(String[] args) throws Exception {
    try {
      String serverConfigPath = "./server.properties";
      InputStream input = null;

      if (args != null && args.length > 0) {
        serverConfigPath = args[0];
      }
      Properties properties = Util.getProperties(serverConfigPath);

      //Set Properties
      System.setProperty("sun.security.krb5.debug", (String) properties.getProperty("sun.security.krb5.debug"));
      System.setProperty("java.security.auth.login.config", (String) properties.getProperty("java.security.auth.login.config"));
      System.setProperty("javax.security.auth.useSubjectCredsOnly", (String) properties.getProperty("javax.security.auth.useSubjectCredsOnly"));
      System.setProperty("jaas.configName", (String) properties.getProperty("jaas.configName"));

      int port = 8080;
      if (properties.getProperty("server.port") != null && !properties.getProperty("server.port").isEmpty()) {
        port = Integer.parseInt(properties.getProperty("server.port"));
      }
      Server server = new Server(port);

      // Create the 'root' Spring application context
      final ServletContextHandler context = new ServletContextHandler();
      context.setContextPath("/");
      context.addEventListener(new ContextLoaderListener());
      context.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
      context.setInitParameter("contextConfigLocation", KerberizedAppConfig.class.getName());

      // Create default servlet (servlet api required)
      // The name of DefaultServlet should be set to 'defualt'.
      final ServletHolder defaultHolder = new ServletHolder("default", DefaultServlet.class);
      defaultHolder.setInitParameter("resourceBase", System.getProperty("user.dir"));
      context.addServlet(defaultHolder, "/");

      server.setHandler(context);
      WebSocketServerContainerInitializer.configureContext(context);

      server.start();
      server.join();
    } finally {
      System.exit(0);
    }
  }
}
