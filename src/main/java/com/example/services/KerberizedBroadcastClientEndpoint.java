package com.example.services;

import com.example.services.Message.MessageDecoder;
import com.example.services.Message.MessageEncoder;
import com.example.util.KerberosUtil;

import javax.websocket.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

@ClientEndpoint(encoders = {MessageEncoder.class}, decoders = {MessageDecoder.class})
public class KerberizedBroadcastClientEndpoint {
  private static final Logger log = Logger.getLogger(KerberizedBroadcastClientEndpoint.class.getName());

  @OnOpen
  public void onOpen(final Session session) throws IOException, EncodeException {
    byte[] initialToken = KerberosUtil.createClientGSSContext(System.getProperty("jaas.configName"), System.getProperty("server.servername"));
    log.info("Client: Sending the token to server for validation");
    //TODO Move this code to Handshake protocol or to Filter. At the moment we are not receiving error if something goes wrong with the authentication
    session.getBasicRemote().sendBinary(ByteBuffer.wrap(initialToken));
    session.getBasicRemote().sendObject(new Message("Client", "Hello!"));
  }

  @OnMessage
  public void onMessage(final Message message) {
    log.info(String.format("Received message '%s' from '%s'",
      message.getMessage(), message.getUsername()));
  }

}
