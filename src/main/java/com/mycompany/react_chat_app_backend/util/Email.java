/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.react_chat_app_backend.util;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.json.JSONObject;

import java.security.*;

/**
 * Helper class that sends an email to a user, should be the friend request link.
 * 
 * This utilizes AWS SES (Simple Email Service).
 * 
 * @author chris
 */
public class Email {
    /**
     * Class Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Email.class.getName());
    /*
    SMTP user/role,     SMTP Username,    SMTP Password, are all stored in AWS Secret Manager
    */
    // Replace sender@example.com with your "From" address.
    // This address must be verified.
    private String FROM = "<Your email>";

 
    private String FROMNAME = "Sender Name";
	
    // Replace recipient@example.com with a "To" address. If your account 
    // is still in the sandbox, this address must be verified.
    private String TO = "recipient@example.com";
    
    // Replace smtp_username with your Amazon SES SMTP user name.
    private String SMTP_USERNAME = "smtp_username";
    
    // Replace smtp_password with your Amazon SES SMTP password.
    private String SMTP_PASSWORD = "smtp_password";
    
    // The name of the Configuration Set to use for this message.
    // If you comment out or remove this variable, you will also need to
    // comment out or remove the header below.
    private String CONFIGSET = "ConfigSet";
    
    // Amazon SES SMTP host name. This example uses the US West (Oregon) region.
    // See https://docs.aws.amazon.com/ses/latest/DeveloperGuide/regions.html#region-endpoints
    // for more information.
    private String HOST = "<your smtp endpoint>";
    
    // The port you will connect to on the Amazon SES SMTP endpoint. 
    private final int PORT = 587;
    
    private String SUBJECT = "Amazon SES test (SMTP interface accessed using Java)";
    
    private String BODY = String.join(
    	    System.getProperty("line.separator"),
    	    "<h1>Amazon SES SMTP Email Test</h1>",
    	    "<p>This email was sent with Amazon SES using the ", 
    	    "<a href='https://github.com/javaee/javamail'>Javamail Package</a>",
    	    " for <a href='https://www.java.com'>Java</a>.",
            " ---------------------------------------------",
            " test from Chris Duran."
    	);

    private Properties props;
 
    /**
     * 
     * @param emailToInvite 
     */
    public Email(String emailToInvite) {

            // Create a Properties object to contain connection configuration information.
            props = System.getProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.port", PORT);
            props.put("mail.smtp.starttls.enable", "false");// "true");
            props.put("mail.smtp.auth", "true");
            props.put("mail.debug", "true");

            TO = emailToInvite;

    }

    /**
     * Generates unique link for the recipient to click on.
     * 
     * @param requestId Integer
     * @return 
     */
    public boolean sendEmail(Integer requestId) {
        boolean retStatus = false;
        BODY = "Invite link sent http://localhost:3000/InviteAccepted/?rid=" + requestId;
        String smtpCreds = Secrets.getSecret();
        JSONObject jsonSmtpCreds = new JSONObject(smtpCreds);
        SMTP_USERNAME = jsonSmtpCreds.getString("smtpUsername");
        SMTP_PASSWORD = jsonSmtpCreds.getString("smtpPassword");
        try
        {
            // Create a Session object to represent a mail session with the specified properties.
            Session session = Session.getDefaultInstance(props);
            // Create a message with the specified information.
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM,FROMNAME));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
            msg.setSubject(SUBJECT);
            msg.setContent(BODY,"text/html");
            // Add a configuration set header. Comment or delete the
            // next line if you are not using a configuration set
            //msg.setHeader("X-SES-CONFIGURATION-SET", CONFIGSET);
            // Create a transport.
            Transport transport = session.getTransport();
            // Send the message.
            try
            {
                System.out.println("Sending...");

                // Connect to Amazon SES using the SMTP username and password you specified above.
                transport.connect(HOST, SMTP_USERNAME, SMTP_PASSWORD);

                // Send the email.
                transport.sendMessage(msg, msg.getAllRecipients());
                LOGGER.info("Email sent!");
                retStatus = true;
            }
            catch (Exception ex) {
                retStatus = false;
                LOGGER.severe("The email was not sent.");
                LOGGER.severe("Error message: " + ex.getMessage());
                ex.printStackTrace();

                // Should display available cipher suites????
                for (Provider provider: Security.getProviders()) {
                  //LOGGER.info("provider name: " + provider.getName());
                  for (String key: provider.stringPropertyNames()) {
                    //LOGGER.log(Level.INFO, "\t key: {0}\t property (key): {1}", new Object[]{key, provider.getProperty(key)});                      
                  }

                }
            }
            finally
            {
                // Close and terminate the connection.
                transport.close();
            }

        }
        catch (UnsupportedEncodingException | MessagingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return retStatus;
    }

}