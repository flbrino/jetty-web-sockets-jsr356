package com.example.services;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.example.util.KerberosUtil;

@ServerEndpoint(
  value = "/broadcast",
  encoders = {Message.MessageEncoder.class},
  decoders = {Message.MessageDecoder.class}
)
public class KerberizedBroadcastServerEndpoint {
  private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());

  @OnOpen
  public void onOpen(final Session session) {
    if (session.isOpen()) {
      sessions.add(session);
    }
  }

  @OnClose
  public void onClose(final Session session) {
    sessions.remove(session);
  }

  @OnMessage
  public void onMessage(final Message message, final Session client) throws IOException, EncodeException {
    if (client.isOpen()) {
      for (final Session session : sessions) {
        session.getBasicRemote().sendObject(message);
      }
    }
  }

  @OnMessage
  public void onMessage(final byte[] token, final Session client) throws IOException, EncodeException {
    try {
      //TODO for now we are just closing the client session if something goes wrong with the authentication. We should inform the client
      if (!KerberosUtil.createServerGSSContext(System.getProperty("jaas.configName"), token)) {
        client.close();
      }
    } catch (RuntimeException e) {
      client.close();
      throw e;
    }
  }

}