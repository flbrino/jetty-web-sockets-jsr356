package com.example.config;

import com.example.services.KerberizedBroadcastServerEndpoint;
import org.eclipse.jetty.websocket.jsr356.server.AnnotatedServerEndpointConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

@Configuration
public class KerberizedAppConfig {
  @Inject
  private WebApplicationContext context;
  private ServerContainer container;

  public class SpringServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
      return context.getAutowireCapableBeanFactory().createBean(endpointClass);
    }
  }

  @Bean
  public ServerEndpointConfig.Configurator configurator() {
    return new SpringServerEndpointConfigurator();
  }

  @PostConstruct
  public void init() throws DeploymentException {
    container = (ServerContainer) context.getServletContext().getAttribute(ServerContainer.class.getName());

    container.addEndpoint(
      new AnnotatedServerEndpointConfig(KerberizedBroadcastServerEndpoint.class, KerberizedBroadcastServerEndpoint.class.getAnnotation(ServerEndpoint.class)) {
        @Override
        public Configurator getConfigurator() {
          return configurator();
        }
      }
    );
  }
}
