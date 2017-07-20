package com.example.util;

import org.ietf.jgss.*;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KerberosUtil {
  private static final Logger log = Logger.getLogger(KerberosUtil.class.getName());
  private static Oid KRB_5_OID;
  public static final String KERBEROS_TOKEN_PARAM = "KRB5_CLIENT_TOKEN";

  static {
    try {
      KRB_5_OID = new Oid("1.2.840.113554.1.2.2");
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      System.exit(-1);
    }
  }

  public static Subject kerberosSubject(String configurationName) {
    //Login to the KDC.
    LoginContext loginCtx = null;
    Subject subject = new Subject();
    // "Client" references the corresponding JAAS configuration section in the jaas.conf file.
    try {
      loginCtx = new LoginContext(configurationName);
      loginCtx.login();
      subject = loginCtx.getSubject();
    } catch (LoginException e) {
      log.log(Level.SEVERE, "Client: There was an error during the JAAS login: " + e);
      e.printStackTrace();
      System.exit(-1);
    }
    return subject;
  }

  public static byte[] createClientGSSContext(String configurationName, String server) {
    try {
      GSSManager manager = GSSManager.getInstance();
      GSSName serverName = manager.createName(server, GSSName.NT_HOSTBASED_SERVICE);
      final List<byte[]> initialTokens = new ArrayList<>(1);
      final GSSContext context = manager.createContext(serverName,
        KRB_5_OID,
        null,
        GSSContext.DEFAULT_LIFETIME);

      // The GSS context initiation has to be performed as a privileged action.
      GSSContext serviceTicket = Subject.doAs(kerberosSubject(configurationName), new PrivilegedAction<GSSContext>() {
        public GSSContext run() {
          try {
            context.requestMutualAuth(false);
            context.requestCredDeleg(false);

            log.log(Level.INFO, "Client: Getting the client token from Kerberos Server");
            initialTokens.add(getClientToken(context));
            log.log(Level.INFO, "Client: Finalized getting the token from Kerberos Server");
            return context;
          } catch (GSSException e) {
            e.printStackTrace();
            return null;
          }
        }
      });

      return !initialTokens.isEmpty() ? initialTokens.get(0) : null;
    } catch (GSSException ex) {
      log.log(Level.SEVERE, "Exception getting client token" + ex.getMessage());
      throw new RuntimeException("Exception creating client GSSContext", ex);
    }
  }

  public static byte[] getClientToken(GSSContext context) {
    byte[] initialToken = new byte[0];

    if (!context.isEstablished()) {
      try {
        // token is ignored on the first call
        initialToken = context.initSecContext(initialToken, 0, initialToken.length);
        return initialToken;
        //return getTokenWithLengthPrefix(initialToken);
      } catch (GSSException ex) {
        log.log(Level.SEVERE, "Exception getting client token" + ex.getMessage());
        throw new RuntimeException("Exception getting client token", ex);
      }
    }
    return null;
  }

  public static boolean acceptServerToken(GSSContext context, byte[] token) {
    try {
      byte[] nextToken = context.acceptSecContext(token, 0, token.length);
      return nextToken == null;
    } catch (GSSException ex) {
      log.log(Level.SEVERE, "Exception accepting client token" + ex.getMessage());
      throw new RuntimeException("Exception accepting client token", ex);
    }
  }

  public static boolean createServerGSSContext(String jaasServerConfName, final byte[] token) {
    log.log(Level.INFO, "createServerGSSContext()...");
    try {
      GSSContext clientContext =
        Subject.doAs(kerberosSubject(jaasServerConfName), new PrivilegedAction<GSSContext>() {
            public GSSContext run() {
              try {
                GSSManager manager = GSSManager.getInstance();
                GSSContext context = manager.createContext((GSSCredential) null);
                while (!context.isEstablished()) {
                  log.log(Level.INFO, "KerberizedServer: context not yet established: accepting from client.");
                  if (acceptServerToken(context, token)) {
                    log.log(Level.INFO, "KerberizedServer: Yes the client token was accepted");
                  } else {
                    log.log(Level.INFO, "KerberizedServer: The client token wasn't accepted");
                    return null;
                  }
                }
                log.log(Level.INFO, "KerberizedServer: context established: communication with client accepted.");
                return context;
              } catch (Exception e) {
                e.printStackTrace();
                return null;
              }
            }
          }
        );
      return clientContext != null;
    } catch (Exception ex) {
      log.log(Level.SEVERE, "createServerGSSContext(), finished with exception:  " + ex.getMessage());
      throw new RuntimeException("Exception creating server GSSContext", ex);
    }
  }
}
