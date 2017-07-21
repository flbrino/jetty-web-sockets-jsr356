### Jetty 9.1 JSR-356 Kerberos
# Introduction

POC for Websocket Application using JSR356 API with Kerberos

# Acknowledgements and Related Work

[JSR 356, Java API for WebSocket] (http://www.oracle.com/technetwork/articles/java/jsr356-1937161.html)
[Package javax.websocket] (https://docs.oracle.com/javaee/7/api/javax/websocket/package-summary.html)
[How to build Java WebSocket Applications Using the JSR 356 API] (https://blog.openshift.com/how-to-build-java-websocket-applications-using-the-jsr-356-api/)
[Client/Server Hello World in Kerberos, Java and GSS](http://thejavamonkey.blogspot.com/2008/04/clientserver-hello-world-in-kerberos.html)
[Sun/Oracle official JAAS tutorials](http://java.sun.com/j2se/1.5.0/docs/guide/security/jgss/tutorials/index.html)
[ekoontz/jaas_and_kerberos] (https://github.com/ekoontz/jaas_and_kerberos)

# Prerequisites

## Kerberos server and client tools

Ready to use Kerberos server VM:
[Kerberos server VM] https://sourceforge.net/projects/kerberos-server-vm/

## JDK

Sun JDK version 1.8.0_131 used.

## MAVEN

Apache Maven 3.5.0 used.

# Setup Kerberos Server Infrastructure

## Choose realm name

In this example we are going to use "KRBREALM.ORG" as the Kerberos realm and "kerberos-server-vm" as the hostname of the machine running kerberos services:

## Edit /etc/krb5.conf

    [libdefaults]
           default_realm = KRBREALM.ORG

    [realms]
           KRBREALM.ORG = {
       		    kdc = kerberos-server-vm
                admin_server = kerberos-server-vm
           }
    [domain_realm]
           .krbrealm.org = KRBREALM.ORG
           krbrealm.org = KRBREALM.ORG

## Choose principal names.

For this example we are going to use "testclient" as the principal for the client and "testserver" as the principal for the server.

## Add principals using kadmin.local 

### Add server principal

On the Kerberos server execute the following commands:

	# kadmin.local
	kadmin.local: addprinc -randkey testserver/HOSTNAME
	kadmin.local: ktadd -k /tmp/testserver.keytab testserver/HOSTNAME
	kadmin.local: (Ctrl-D)

Don't forget to change HOSTNAME for the correct one. This should be the hostname of the machine where you are going to execute server class of the example code. Select a different directory for keytab file generation if you want.
After generating the keytab copy the file to the host identified and to the user area who will run the server class.
The option randkey on ktadd command specify that we don't want to use a password but keytab instead

### Add client principal

On the Kerberos server execute the following commands:

     # kadmin.local
     kadmin.local: addprinc testclient

Enter the password for this principal

# Test Kerberos Server Infrastructure

## Server principal

On the machine runing the server class of the example code execute the following command on the directory where you copy keytab:

     kinit -k -t testserver.keytab testserver/HOSTNAME

You shouldn't be asked for a password
## Client principal

     kinit testclient

Enter the password
# Compile Java example code

Run `mvn clean package`

# Configuration

## jaas.conf

Change`HOSTNAME` to the host where running the server class and the `REALM` to the choosen one.

	Client {
		   com.sun.security.auth.module.Krb5LoginModule required
		   debug=true
		   useKeyTab=false
		   useTicketCache=true
		   principal="testclient";
	};

	KerberizedServer {
		   com.sun.security.auth.module.Krb5LoginModule required
		   debug=true
		   useKeyTab=true
		   keyTab="C:/kerberized/testserver2.keytab"
		   useTicketCache=false
		   storeKey=true
		   doNotPrompt=true
		   principal="testserver/HOSTNAME@REALM";
	};

## client.properties

Set the server principal name
Set path to the jaas.conf file
If you change the port on the server.properties file set correct URL 


	sun.security.krb5.debug=true
	java.security.auth.login.config=./jaas.conf
	javax.security.auth.useSubjectCredsOnly=true
	jaas.configName=Client
	server.servername=testserver
	server.uri=ws://localhost:8080/broadcast

## server.properties	

Set path to the jaas.conf file


	sun.security.krb5.debug=true
	java.security.auth.login.config=./jaas.conf
	javax.security.auth.useSubjectCredsOnly=true
	jaas.configName=KerberizedServer
	server.port=8080

	
# Execute

	java -jar target/jetty-web-sockets-jsr356-0.0.1-SNAPSHOT-server.jar [path_to_server_properties_file]
	java -jar target/jetty-web-sockets-jsr356-0.0.1-SNAPSHOT-client.jar [path_to_client_properties_file] 