package com.almis.awe.service.data.builder;

import com.almis.awe.component.AweJmsDestination;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.listener.QueueListener;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queues.JmsConnectionInfo;
import com.almis.awe.model.entities.queues.MessageBuilder;
import com.almis.awe.model.entities.queues.Queue;
import com.almis.awe.model.entities.queues.RequestMessage;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.type.QueueMessageType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.processor.QueueProcessor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.Optional;

/**
 * Generate service datalist
 */
@Slf4j
public class QueueBuilder extends AbstractQueryBuilder {

  private Queue queue;

  // Autowired services
  private final AweJmsDestination jmsDestination;
  private final ConnectionFactory connectionFactory;
  private final PlatformTransactionManager transactionManager;
  private final BaseConfigProperties baseConfigProperties;

  private static final String ERROR_TITLE_BAD_QUEUE_DEFINITION_FORMAT = "ERROR_TITLE_BAD_QUEUE_DEFINITION_FORMAT";

  /**
   * Autowired constructor
   *
   * @param jmsDestination       JMS Destination
   * @param connectionFactory    Connection factory
   * @param transactionManager   Transaction manager
   * @param queryUtil            Query utilities
   * @param baseConfigProperties Base configuration properties
   */
  public QueueBuilder(AweJmsDestination jmsDestination, ConnectionFactory connectionFactory,
                      PlatformTransactionManager transactionManager, QueryUtil queryUtil, BaseConfigProperties baseConfigProperties) {
    super(queryUtil);
    this.jmsDestination = jmsDestination;
    this.connectionFactory = connectionFactory;
    this.transactionManager = transactionManager;
    this.baseConfigProperties = baseConfigProperties;
  }

  @Override
  public QueueBuilder setQuery(Query query) {
    super.setQuery(query);
    return this;
  }

  /**
   * Set the queue
   *
   * @param queue Queue
   * @return this
   */
  public QueueBuilder setQueue(Queue queue) {
    this.queue = queue;
    return this;
  }

  @Override
  public QueueBuilder setAddress(ComponentAddress address) {
    super.setAddress(address);
    return this;
  }

  @Override
  public QueueBuilder setVariables(Map<String, QueryParameter> parameterMap) {
    super.setVariables(parameterMap);
    return this;
  }

  @Override
  public QueueBuilder setParameters(ObjectNode parameters) {
    super.setParameters(parameters);
    return this;
  }

  /**
   * Call the service and returns the ServiceData
   *
   * @return serviceData
   * @throws AWException Error calling service
   */
  public ServiceData build() throws AWException {
    // Call service
    Map<String, QueryParameter> variables = queryUtil.getVariableMap(getQuery(), null);
    return launchQueue(extractValuesFromParameters(variables));
  }

  /**
   * Subscribe to a topic queue
   *
   * @return Service data
   * @throws AWException Error in subscription
   */
  public ServiceData subscribe() throws AWException {

    // Variable definition
    ServiceData serviceData = new ServiceData();

    try {
      // Check response object
      if (queue.getResponse() == null) {
        throw new AWException(getLocale(ERROR_TITLE_BAD_QUEUE_DEFINITION_FORMAT),
          getLocale("ERROR_MESSAGE_BAD_QUEUE_RESPONSE_DEFINITION_FORMAT", queue.getId()));
      }

      // Generate listener
      QueueListener listener = getBean(QueueListener.class)
        .setQuery(getQuery())
        .setAddress(getAddress())
        .setResponse(queue.getResponse());

      // Define container
      DefaultMessageListenerContainer jmsContainer = new DefaultMessageListenerContainer();
      jmsContainer.setConnectionFactory(connectionFactory);
      jmsContainer.setTransactionManager(transactionManager);
      jmsContainer.setPubSubDomain(true);
      jmsContainer.setDestination(jmsDestination.getDestination(queue.getResponse().getDestination()));
      jmsContainer.setMessageSelector(queue.getResponse().getSelector());
      jmsContainer.setMessageListener(listener);
      jmsContainer.afterPropertiesSet();
      jmsContainer.start();

      // Track session
      AweClientTracker clientTracker = getBean(AweClientTracker.class);
      JmsConnectionInfo connectionInfo = new JmsConnectionInfo()
        .setListenerContainer(jmsContainer)
        .setAddress(getAddress());
      clientTracker.track(connectionInfo);

    } catch (AWException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_RECEIVING_QUEUE_MESSAGE"),
        getLocale("ERROR_MESSAGE_RECEIVING_QUEUE_MESSAGE", queue.getId()),
        exc);
    }

    // Return subscription out ok
    return serviceData;
  }

  /**
   * Launch a queue and retrieve response
   *
   * @param parameterList Parameters
   * @return Service data
   * @throws AWException Error launching queue
   */
  private ServiceData launchQueue(Map<String, Object> parameterList) throws AWException {

    // Find queue and launch it
    ServiceData serviceData;

    // Check if queue exists
    if (queue == null) {
      throw new AWException(getLocale("ERROR_TITLE_LAUNCHING_QUEUE"),
        getLocale("ERROR_MESSAGE_QUEUE_NOT_FOUND", ""));
    }

    // Waits for response
    if (queue.getResponse() != null) {
      // Launch queue
      serviceData = sendMessageSync(parameterList);
    } else {
      // Launch async queue
      serviceData = sendMessageAsync(parameterList);
    }

    return serviceData;
  }

  /**
   * Send a sync message
   *
   * @param parameterList Parameters
   * @return Service data
   * @throws AWException Error sending sync message
   */
  private ServiceData sendMessageSync(Map<String, Object> parameterList) throws AWException {
    // Send message
    String messageCorrelationId = sendMessage(parameterList);

    // Receive message
    return receiveMessage(messageCorrelationId);
  }

  /**
   * Send a sync message
   *
   * @param parameterList Parameters
   * @return Service data
   * @throws AWException Error sending sync message
   */
  private ServiceData sendMessageAsync(Map<String, Object> parameterList) throws AWException {
    // Send message and return ok
    sendMessage(parameterList);
    return new ServiceData();
  }

  /**
   * Send a message to a queue
   *
   * @param parameterList parameters
   * @return Correlation ID
   * @throws AWException Error sending message
   */
  public String sendMessage(Map<String, Object> parameterList) throws AWException {

    // Variable definition
    String messageId;

    try {
      // Check request object
      if (queue.getRequest() == null) {
        throw new AWException(getLocale(ERROR_TITLE_BAD_QUEUE_DEFINITION_FORMAT),
          getLocale("ERROR_MESSAGE_BAD_QUEUE_REQUEST_DEFINITION_FORMAT", queue.getId()));
      }

      // Get jmsTemplate
      JmsTemplate template = getBean(JmsTemplate.class);

      // Read timeouts
      long requestTimeout = Optional.ofNullable(queue.getRequest().getTimeout()).map(Long::parseLong).orElse(baseConfigProperties.getJms().getServiceTimeout().toMillis());

      template.setDeliveryMode(JmsProperties.DeliveryMode.NON_PERSISTENT.getValue());
      template.setTimeToLive(baseConfigProperties.getJms().getMessageTimeToLive().toMillis());
      template.setReceiveTimeout(requestTimeout);

      // Send message & store correlation id
      messageId = sendMessage(template, queue.getRequest(), parameterList);

      // Log sent message
      log.info("Message {} sent to queue {} with messageId {}. Contents: {}",
        queue.getId(), queue.getRequest().getDestination(), messageId, parameterList);

    } catch (AWException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_SENDING_QUEUE_MESSAGE"),
        getLocale("ERROR_MESSAGE_SENDING_QUEUE_MESSAGE", queue.getId()), exc);
    }

    return messageId;
  }

  /**
   * Receive message from a queue
   *
   * @param correlationId Correlation ID
   * @return Service data
   * @throws AWException Error receiving message
   */
  public ServiceData receiveMessage(String correlationId) throws AWException {

    // Variable definition
    ServiceData serviceData;
    String selector = "JMSCorrelationID = '" + correlationId + "'";

    try {
      // Check response object
      if (queue.getResponse() == null) {
        throw new AWException(getLocale(ERROR_TITLE_BAD_QUEUE_DEFINITION_FORMAT), getLocale(
          "ERROR_MESSAGE_BAD_QUEUE_RESPONSE_DEFINITION_FORMAT", queue.getId()));
      }

      // Get jmsTemplate
      JmsTemplate template = getBean(JmsTemplate.class);

      // Read timeouts
      long responseTimeout = Optional.ofNullable(queue.getResponse().getTimeout()).map(Long::parseLong).orElse(baseConfigProperties.getJms().getServiceTimeout().toMillis());
      template.setReceiveTimeout(responseTimeout);
      template.setTimeToLive(baseConfigProperties.getJms().getMessageTimeToLive().toMillis());

      // Retrieve consumer object
      Message responseMessage = template.receiveSelected(jmsDestination.getDestination(queue.getResponse().getDestination()), selector);

      // Check if response is null or exceded timeout
      if (responseMessage == null) {
        throw new AWException(getElements().getLocaleWithLanguage("ERROR_TITLE_JMS_TIMEOUT", getElements().getLanguage()),
          getElements().getLocaleWithLanguage("ERROR_MESSAGE_JMS_TIMEOUT", getElements().getLanguage(),
            queue.getId(), responseTimeout));
      }

      // Parse request parameters
      QueueProcessor responseProcessor = getBean(QueueProcessor.class);
      serviceData = responseProcessor.parseResponseMessage(queue.getResponse(), responseMessage);
    } catch (NumberFormatException exc) {
      throw new AWException(getLocale("ERROR_TITLE_RECEIVING_QUEUE_MESSAGE"),
        getLocale("ERROR_MESSAGE_RECEIVING_QUEUE_MESSAGE", queue.getId()),
        exc);
    }

    return serviceData;
  }

  /**
   * Send JMS Message
   *
   * @param template      JMS Template
   * @param request       Request
   * @param parameterList Parameter list
   * @return Correlation ID
   * @throws AWException Error sending message
   */
  public String sendMessage(JmsTemplate template, RequestMessage request, Map<String, Object> parameterList) throws AWException {
    // Variable definition
    MessageBuilder messageCreator;
    String correlationId;

    try {
      // Create message
      messageCreator = getBean(MessageBuilder.class)
        .setType(QueueMessageType.valueOf(request.getType().toUpperCase()))
        .setSelector(request.getSelector())
        .setRequest(request)
        .setValueList(parameterList);
      template.send(jmsDestination.getDestination(request.getDestination()), messageCreator);
      correlationId = messageCreator.getMessage().getJMSMessageID();
    } catch (JMSException exc) {
      throw new AWException(getLocale("ERROR_TITLE_PARSING_REQUEST_MESSAGE"),
        getLocale("ERROR_MESSAGE_PARSING_REQUEST_MESSAGE"), exc);
    }

    return correlationId;
  }
}
