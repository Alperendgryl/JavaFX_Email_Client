package Email;

import java.io.File;
import java.util.Date;
//@author AlperenDGRYL

public class Email {

    private String senderName;
    private String subject;
    private String mailBody;
    private Date date;
    private String attachment;
    private File attachFile;

    public Email(String senderName, String subject, String mailBody, Date date, String attachment) {
        this.senderName = senderName;
        this.subject = subject;
        this.mailBody = mailBody;
        this.date = date;
        this.attachment = attachment;
    }

    public Email(String senderName, String subject, String mailBody, Date date, String attachment, File attachFile) {
        this.senderName = senderName;
        this.subject = subject;
        this.mailBody = mailBody;
        this.date = date;
        this.attachment = attachment;
        this.attachFile = attachFile;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMailBody() {
        return mailBody;
    }

    public void setMailBody(String mailBody) {
        this.mailBody = mailBody;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public File getAttachFile() {
        return attachFile;
    }

    public void setAttachFile(File attachFile) {
        this.attachFile = attachFile;
    }

    @Override
    public String toString() {
        return "Sender Name : " + senderName + "\nSubject : " + subject + "\nDate : " + date + "\nAttachment : " + attachment + "\n\n" + mailBody;
    }
}
