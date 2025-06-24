package com.almis.awe.service.data.builder;

import com.almis.awe.component.AweJmsDestination;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.listener.QueueListener;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queues.*;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.type.QueueMessageType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.processor.QueueProcessor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QueueBuilder tests
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class QueueBuilderTest {

    @Mock
    private AweJmsDestination jmsDestination;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private QueryUtil queryUtil;

    @Mock
    private BaseConfigProperties baseConfigProperties;

    @Mock
    private BaseConfigProperties.Jms jmsProperties;

    @Mock
    private ApplicationContext context;

    @Mock
    private AweElements aweElements;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private QueueListener queueListener;

    @Mock
    private AweClientTracker clientTracker;

    @Mock
    private QueueProcessor queueProcessor;

    @Mock
    private MessageBuilder messageBuilder;

    @Mock
    private Message message;

    @Mock
    private Destination destination;

    @InjectMocks
    private QueueBuilder queueBuilder;

    /**
     * Initializes beans for tests
     */
    @BeforeEach
    void initBeans() throws Exception {
        queueBuilder.setApplicationContext(context);

        // Mock context getBean calls
        doReturn(aweElements).when(context).getBean(AweElements.class);
        doReturn(jmsTemplate).when(context).getBean(JmsTemplate.class);
        doReturn(queueListener).when(context).getBean(QueueListener.class);
        doReturn(clientTracker).when(context).getBean(AweClientTracker.class);
        doReturn(queueProcessor).when(context).getBean(QueueProcessor.class);
        doReturn(messageBuilder).when(context).getBean(MessageBuilder.class);

        // Mock queryUtil to handle any arguments
        Map<String, QueryParameter> variableMap = new HashMap<>();
        doReturn(variableMap).when(queryUtil).getVariableMap(any(Query.class), any(ObjectNode.class));

        // Mock baseConfigProperties
        when(baseConfigProperties.getJms()).thenReturn(jmsProperties);
        when(jmsProperties.getServiceTimeout()).thenReturn(Duration.ofMillis(5000));
        when(jmsProperties.getMessageTimeToLive()).thenReturn(Duration.ofMillis(10000));

        // Mock queueListener
        when(queueListener.setQuery(any())).thenReturn(queueListener);
        when(queueListener.setAddress(any())).thenReturn(queueListener);
        when(queueListener.setResponse(any(ResponseMessage.class))).thenReturn(queueListener);

        // Mock messageBuilder
        when(messageBuilder.setType(any())).thenReturn(messageBuilder);
        when(messageBuilder.setSelector(any())).thenReturn(messageBuilder);
        when(messageBuilder.setRequest(any())).thenReturn(messageBuilder);
        when(messageBuilder.setValueList(any())).thenReturn(messageBuilder);
        when(messageBuilder.getMessage()).thenReturn(message);

        // Mock message
        when(message.getJMSMessageID()).thenReturn("test-message-id");

        // Mock jmsDestination
        when(jmsDestination.getDestination(anyString())).thenReturn(destination);
    }

    /**
     * Test build method with sync queue
     */
    @Test
    void testBuildWithSyncQueue() throws Exception {
        // Prepare
        Query query = new Query();
        ComponentAddress address = new ComponentAddress();
        Map<String, Object> parameters = new HashMap<>();
        Map<String, QueryParameter> variableMap = new HashMap<>();

        Queue queue = new Queue();
        RequestMessage request = new RequestMessage();
        request.setDestination("test-destination");
        request.setType("TEXT");

        ResponseMessage response = new ResponseMessage();
        response.setDestination("test-response-destination");

        queue.setRequest(request);
        queue.setResponse(response);

        ServiceData expectedServiceData = new ServiceData();

        // Mock jmsTemplate
        doReturn(message).when(jmsTemplate).receiveSelected(any(Destination.class), anyString());

        // Mock queueProcessor
        doReturn(expectedServiceData).when(queueProcessor).parseResponseMessage(any(ResponseMessage.class), any(Message.class));

        // Run
        ServiceData result = queueBuilder
                .setQuery(query)
                .setAddress(address)
                .setQueue(queue)
                .build();

        // Assert
        assertNotNull(result);
        assertEquals(expectedServiceData, result);
        verify(jmsTemplate).send(any(Destination.class), any(MessageBuilder.class));
        verify(jmsTemplate).receiveSelected(any(Destination.class), anyString());
        verify(queueProcessor).parseResponseMessage(any(ResponseMessage.class), any(Message.class));
    }

    /**
     * Test build method with async queue
     */
    @Test
    void testBuildWithAsyncQueue() throws Exception {
        // Prepare
        Query query = new Query();
        ComponentAddress address = new ComponentAddress();
        Map<String, Object> parameters = new HashMap<>();
        Map<String, QueryParameter> variableMap = new HashMap<>();

        Queue queue = new Queue();
        RequestMessage request = new RequestMessage();
        request.setDestination("test-destination");
        request.setType("TEXT");

        queue.setRequest(request);
        // No response means async queue

        // Mock queryUtil
        doReturn(variableMap).when(queryUtil).getVariableMap(any(Query.class), any(ObjectNode.class));

        // Run
        ServiceData result = queueBuilder
                .setQuery(query)
                .setAddress(address)
                .setQueue(queue)
                .build();

        // Assert
        assertNotNull(result);
        verify(jmsTemplate).send(any(Destination.class), any(MessageBuilder.class));
        verify(jmsTemplate, never()).receiveSelected(any(Destination.class), anyString());
    }

    /**
     * Test subscribe method
     */
    @Test
    void testSubscribe() throws Exception {
        // Prepare
        Query query = new Query();
        ComponentAddress address = new ComponentAddress();

        Queue queue = new Queue();
        ResponseMessage response = new ResponseMessage();
        response.setDestination("test-response-destination");
        response.setSelector("test-selector");

        queue.setResponse(response);

        // Run
        ServiceData result = queueBuilder
                .setQuery(query)
                .setAddress(address)
                .setQueue(queue)
                .subscribe();

        // Assert
        assertNotNull(result);
        verify(queueListener).setQuery(query);
        verify(queueListener).setAddress(address);
        verify(queueListener).setResponse(response);
        verify(clientTracker).track(any(JmsConnectionInfo.class));
    }

    /**
     * Test sendMessage method
     */
    @Test
    void testSendMessage() throws Exception {
        // Prepare
        Queue queue = new Queue();
        RequestMessage request = new RequestMessage();
        request.setDestination("test-destination");
        request.setType("TEXT");

        queue.setRequest(request);

        Map<String, Object> parameters = new HashMap<>();

        queueBuilder.setQueue(queue);

        // Run
        String messageId = queueBuilder.sendMessage(parameters);

        // Assert
        assertEquals("test-message-id", messageId);
        verify(jmsTemplate).send(any(Destination.class), any(MessageBuilder.class));
        verify(messageBuilder).setType(QueueMessageType.TEXT);
        verify(messageBuilder).setRequest(request);
        verify(messageBuilder).setValueList(parameters);
    }

    /**
     * Test receiveMessage method
     */
    @Test
    void testReceiveMessage() throws Exception {
        // Prepare
        Queue queue = new Queue();
        ResponseMessage response = new ResponseMessage();
        response.setDestination("test-response-destination");

        queue.setResponse(response);

        ServiceData expectedServiceData = new ServiceData();

        queueBuilder.setQueue(queue);

        // Mock jmsTemplate
        doReturn(message).when(jmsTemplate).receiveSelected(any(Destination.class), anyString());

        // Mock queueProcessor
        doReturn(expectedServiceData).when(queueProcessor).parseResponseMessage(any(ResponseMessage.class), any(Message.class));

        // Run
        ServiceData result = queueBuilder.receiveMessage("test-correlation-id");

        // Assert
        assertEquals(expectedServiceData, result);
        verify(jmsTemplate).receiveSelected(any(Destination.class), eq("JMSCorrelationID = 'test-correlation-id'"));
        verify(queueProcessor).parseResponseMessage(response, message);
    }

    /**
     * Test receiveMessage method with timeout
     */
    @Test
    void testReceiveMessageTimeout() throws Exception {
        // Prepare
        Queue queue = new Queue();
        ResponseMessage response = new ResponseMessage();
        response.setDestination("test-response-destination");

        queue.setResponse(response);

        queueBuilder.setQueue(queue);

        // Mock jmsTemplate to return null (timeout)
        doReturn(null).when(jmsTemplate).receiveSelected(any(Destination.class), anyString());

        // Mock aweElements for locale
        doReturn("Error").when(aweElements).getLocaleWithLanguage(anyString(), any());
        doReturn(aweElements).when(context).getBean(AweElements.class);

        // Run & Assert
        assertThrows(AWException.class, () -> queueBuilder.receiveMessage("test-correlation-id"));
        verify(jmsTemplate).receiveSelected(any(Destination.class), eq("JMSCorrelationID = 'test-correlation-id'"));
    }

    /**
     * Test build method with missing queue
     */
    @Test
    void testBuildWithMissingQueue() throws Exception {
        // Prepare
        Query query = new Query();
        ComponentAddress address = new ComponentAddress();
        Map<String, QueryParameter> variableMap = new HashMap<>();

        doReturn(variableMap).when(queryUtil).getVariableMap(any(Query.class), any(ObjectNode.class));

        // Run & Assert
        assertThrows(AWException.class, () -> queueBuilder
                .setQuery(query)
                .setAddress(address)
                .build());
    }

    /**
     * Test subscribe method with missing response
     */
    @Test
    void testSubscribeWithMissingResponse() throws Exception {
        // Prepare
        Query query = new Query();
        ComponentAddress address = new ComponentAddress();

        Queue queue = new Queue();
        // No response defined

        queueBuilder
                .setQuery(query)
                .setAddress(address)
                .setQueue(queue);

        // Run & Assert
        assertThrows(AWException.class, () -> queueBuilder.subscribe());
    }

    /**
     * Test sendMessage method with missing request
     */
    @Test
    void testSendMessageWithMissingRequest() throws Exception {
        // Prepare
        Queue queue = new Queue();
        // No request defined

        Map<String, Object> parameters = new HashMap<>();

        queueBuilder.setQueue(queue);

        // Run & Assert
        assertThrows(AWException.class, () -> queueBuilder.sendMessage(parameters));
    }

    /**
     * Test receiveMessage method with missing response
     */
    @Test
    void testReceiveMessageWithMissingResponse() throws Exception {
        // Prepare
        Queue queue = new Queue();
        // No response defined

        queueBuilder.setQueue(queue);

        // Run & Assert
        assertThrows(AWException.class, () -> queueBuilder.receiveMessage("test-correlation-id"));
    }
}
