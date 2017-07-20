package com.example.ws;


import com.example.services.KerberizedBroadcastClientEndpoint;
import com.example.services.Message;
import com.example.util.Util;
import org.eclipse.jetty.util.component.LifeCycle;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

public class KerberizedClientStarter {
  public static void main(final String[] args) throws Exception {
    try {
      final String client = UUID.randomUUID().toString().substring(0, 8);
      final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
      String clientConfigPath = "./client.properties";

      Properties properties = Util.getProperties(clientConfigPath);

      //Set Properties
      System.setProperty("sun.security.krb5.debug", (String) properties.getProperty("sun.security.krb5.debug"));
      System.setProperty("java.security.auth.login.config", (String) properties.getProperty("java.security.auth.login.config"));
      System.setProperty("javax.security.auth.useSubjectCredsOnly", (String) properties.getProperty("javax.security.auth.useSubjectCredsOnly"));
      System.setProperty("jaas.configName", (String) properties.getProperty("jaas.configName"));
      System.setProperty("server.servername", (String) properties.getProperty("server.servername"));
      String uri = properties.getProperty("server.uri");

      try (Session session = container.connectToServer(KerberizedBroadcastClientEndpoint.class, URI.create(uri))) {
        //sends 10 messagens and receives back as result of the server broadcast
        for (int i = 1; i <= 10; ++i) {
          session.getBasicRemote().sendObject(new Message(client, "Message #" + i));
          Thread.sleep(1000);
        }
      }

      // JSR-356 has no concept of Container lifecycle.
      // (This is an oversight on the spec's part)
      // This stops the lifecycle of the Client WebSocketContainer
      if (container instanceof LifeCycle) {
        ((LifeCycle) container).stop();
      }
    } finally {
      System.exit(0);
    }
  }
}

