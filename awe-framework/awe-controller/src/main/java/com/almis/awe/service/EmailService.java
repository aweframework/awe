package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.exception.AWException;
import com.almis.awe.factory.MailSenderFactory;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.entities.email.Email;
import com.almis.awe.model.entities.email.ParsedEmail;
import com.almis.awe.model.type.EmailMessageType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.builder.XMLEmailBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
public class EmailService extends ServiceConfig {

  // Constants
  private static final String CRLF = "\n";
  // Autowired services
  private final MailSenderFactory mailSenderFactory;
  private final BaseConfigProperties baseConfigProperties;
  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final UserDAOImpl userDAO;

  /**
   * Autowired constructor
   *
   * @param mailSenderFactory    Email sender factory
   * @param baseConfigProperties Base configuration properties
   */
  public EmailService(MailSenderFactory mailSenderFactory, BaseConfigProperties baseConfigProperties,
                      QueryService queryService, QueryUtil queryUtil, UserDAOImpl userDAO) {
    this.mailSenderFactory = mailSenderFactory;
    this.baseConfigProperties = baseConfigProperties;
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.userDAO = userDAO;
  }

  @Async("threadPoolTaskExecutor")
  public Future<ServiceData> sendEmail(String emailName, ObjectNode parameters) throws AWException {
    // Initialize needed variables variables
    ServiceData serviceData = new ServiceData();

    // Get mail sender
    JavaMailSender mailSender = getMailServer();

    // Send email
    sendParsedEmail(parseEmail(getElements().getEmail(emailName).copy(), parameters), mailSender);

    // Return ok
    return CompletableFuture.completedFuture(serviceData
      .setTitle("OK_TITLE_EMAIL_OPERATION")
      .setMessage("OK_MESSAGE_EMAIL_OPERATION"));
  }

  @Async("threadPoolTaskExecutor")
  public void sendEmail(ParsedEmail email) {
    sendParsedEmail(email, getMailServer());
  }

  /**
   * Reload mail servers (when updated)
   * @return
   * @throws AWException
   */
  public ServiceData reloadMailServers() throws AWException {
    mailSenderFactory.init();
    return new ServiceData();
  }

  /**
   * Parse email
   *
   * @param email      Email XML to parse
   * @param parameters Parameters to send
   * @return Parsed email
   * @throws AWException
   */
  private ParsedEmail parseEmail(Email email, ObjectNode parameters) throws AWException {
    // Build message
    return new XMLEmailBuilder(queryService, queryUtil, getElements())
      .setEmail(email)
      .setParameters(parameters)
      .parseEmail()
      .build();
  }

  private void sendParsedEmail(ParsedEmail email, JavaMailSender mailSender) {
    MimeMessage message = mailSender.createMimeMessage();

    try {
      // Set email recipients
      setRecipients(email, message);

      // Set message subject
      message.setSubject(getLocale(email.getSubject()));

      // Append to message
      message.setText(getLocale(email.getBody()).replace("\\n", CRLF), baseConfigProperties.getEncoding());
      message.setContent(generateMultipartMessage(email), "html");

      // Send mail
      mailSender.send(message);
    } catch (MessagingException | IOException exc) {
      log.error("Error sending email message", exc);
    }
  }

  /**
   * Generate email message parts
   *
   * @return Multipart
   * @throws MessagingException Error generating message
   * @throws IOException        Error retrieving file
   */
  protected Multipart generateMultipartMessage(ParsedEmail email) throws MessagingException, IOException {
    // Message content type
    String messageContentType = email.getMessageType() + "; charset=" + baseConfigProperties.getEncoding();

    // Set the message body
    Multipart multipart = new MimeMultipart();

    // creates body part for the message
    MimeBodyPart messageBodyPart = new MimeBodyPart();

    // Content type
    if (email.getMessageType() == EmailMessageType.HTML) {
      messageBodyPart.setText(getLocale(email.getBody()), null, messageContentType);
    } else {
      messageBodyPart.setText(getLocale(email.getBody()));
    }

    // Add body to multiPart
    multipart.addBodyPart(messageBodyPart);

    // Add attachments
    if (!email.getAttachments().isEmpty()) {
      generateMultipartAttachments(email, multipart);
    }

    return multipart;
  }

  /**
   * Append attachments
   *
   * @param multipart Attachments
   * @throws MessagingException  Message exception
   * @throws java.io.IOException IO exception
   */
  protected void generateMultipartAttachments(ParsedEmail email, Multipart multipart) throws MessagingException, IOException {
    for (String fileName : email.getAttachments().keySet()) {
      File file = email.getAttachments().get(fileName);

      // If extension is not the same as the one of the file, force extension
      if (!getFileExtension(fileName).equalsIgnoreCase(getFileExtension(file.getName()))) {
        fileName += getFileExtension(file.getName());
      }

      MimeBodyPart attachFilePart = new MimeBodyPart();
      attachFilePart.setDisposition(Part.ATTACHMENT);
      attachFilePart.attachFile(file);
      attachFilePart.setFileName(fileName);
      multipart.addBodyPart(attachFilePart);
    }
  }

  /**
   * Set email recipients
   *
   * @param email   Email
   * @param message Mime message
   * @throws MessagingException Error generating email
   */
  protected void setRecipients(ParsedEmail email, MimeMessage message) throws MessagingException {
    // Set from
    message.setFrom(email.getFrom());
    message.setSender(email.getFrom());

    // Set reply to
    if (!email.getReplyTo().isEmpty()) {
      message.setReplyTo(email.getReplyTo().toArray(new InternetAddress[0]));
    }

    // Set to
    message.setRecipients(Message.RecipientType.TO, email.getTo().toArray(new InternetAddress[0]));

    // Set cc
    if (!email.getCc().isEmpty()) {
      message.setRecipients(Message.RecipientType.CC, email.getCc().toArray(new InternetAddress[0]));
    }

    // Set Cco
    if (!email.getCco().isEmpty()) {
      message.setRecipients(Message.RecipientType.BCC, email.getCco().toArray(new InternetAddress[0]));
    }
  }

  /**
   * Get file extension
   *
   * @param fileName File name
   * @return Extension
   */
  private String getFileExtension(String fileName) {
    return fileName.substring(fileName.lastIndexOf('.'));
  }

  /**
   * Retrieve java mail server depending on user
   * @return Java Mail server
   */
  private JavaMailSender getMailServer() {
    String userName = Optional.ofNullable(getRequest().getParameterAsString("user"))
      .orElse(Optional.ofNullable(getSession()).map(AweSession::getUser)
        .orElse(null));

    if (userName != null) {
      User user = userDAO.findByUserName(userName);
      return mailSenderFactory.getMailSender(user.getEmailServer());
    }

    return mailSenderFactory.getMailSender();
  }
}
