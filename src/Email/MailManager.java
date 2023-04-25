/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Email;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMultipart;
import javax.mail.Message;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Store;

/**
 *
 * @author AlperenDGRYL
 */
public class MailManager {

    ObservableList<Email> inboxEmails = FXCollections.observableArrayList();
    ObservableList<Email> junkEmails = FXCollections.observableArrayList();
    ObservableList<Email> sentEmails = FXCollections.observableArrayList();
    
    private final Host currentUser;
    private final ArrayList<Host> userList;

    public MailManager(Host currentUser, ArrayList<Host> userList) {
        this.currentUser = currentUser;
        this.userList = userList;
    }
    
    void receiveMail() throws NoSuchProviderException, javax.mail.MessagingException {
        String sender;
        String subject;
        Date date;
        String body;
        String attachmentName;
        File attachment;

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        Session session = Session.getInstance(properties, null);
        try {
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", currentUser.getMailAddress(), currentUser.getPassword());

            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_ONLY);
            Message[] messages = (Message[]) inbox.getMessages();
            for (Message message : messages) {
                Address[] from = message.getFrom();
                sender = from[0].toString();
                subject = message.getSubject();
                body = getTextFromMessage(message);
                date = message.getSentDate();
                attachmentName = "";
                InputStream attachmentStream = null;
                attachment = null;

                if (message.isMimeType("multipart/*")) {
                    MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                    if (body.contains("[") && body.contains("]") && body.contains(":")) {
                        int startIndex = body.indexOf(":");
                        int endIndex = body.indexOf("]");
                        attachmentName = body.substring(startIndex + 1, endIndex) + " \uD83D\uDCCE";
                    } else {
                        attachmentName = " X ";
                    }
                    attachment = getAttachmentFile(mimeMultipart);
                }
                if (attachmentStream != null) {
                    saveAttachment(attachmentStream, attachmentName);
                }

                Email newMail = new Email(sender, subject, body, date, attachmentName, attachment);
                if (body.contains("junk")) {
                    junkEmails.add(newMail);
                } else {
                    inboxEmails.add(newMail);
                }
            }
            inbox.close(false);
            store.close();

        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain") || message.isMimeType("text/html")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
                result = result + "\n" + bodyPart.getContent();
                break;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof String) {
                result = result + "\n" + bodyPart.getContent();
            }
        }
        return result;
    }

    static File getAttachmentFile(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        File result = null;
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                // Create a temporary file to store the attachment
                result = File.createTempFile("attachment", "tmp");
                // Write the attachment to the temporary file
                InputStream attachmentStream = bodyPart.getInputStream();
                Files.copy(attachmentStream, result.toPath(), StandardCopyOption.REPLACE_EXISTING);
                break;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = getAttachmentFile((MimeMultipart) bodyPart.getContent());
                break;
            }
        }
        return result;
    }

    static File saveAttachment(InputStream inputStream, String fileName) throws IOException {
        // Create a temporary file to store the attachment
        File result = File.createTempFile("attachment", "tmp");
        // Write the contents of the input stream to the temporary file
        FileOutputStream outputStream = new FileOutputStream(result);
        int read;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        inputStream.close();
        outputStream.close();
        return result;
    }
}
