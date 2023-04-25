package Email;

import java.util.ArrayList;
import java.util.Properties;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.stage.Stage;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.swing.JOptionPane;
//@author AlperenDGRYL

public class Controller extends Application {

    Properties props;
    Authenticator a;

    private MailManager mailManager;
    private ViewManager viewManager;

    Host currentUser;
    ArrayList<Host> userList = new ArrayList<>();

    public Controller() {
        currentUser = null; // Initialize currentUser as null since there's no current user at the start
        userList = new ArrayList<>(); // Initialize the userList as an empty ArrayList
        mailManager = new MailManager(currentUser, userList);
        viewManager = new ViewManager(mailManager, props, a, currentUser, userList);
    }

    @Override
    public void start(Stage stage) {
        String mailAddress;
        String password;
        String afterAt = "";
        while (true) {
            String tempfromMail;
            tempfromMail = JOptionPane.showInputDialog(null, "Enter Your Mail Address", "Mail Address", JOptionPane.INFORMATION_MESSAGE); //parent, question, title, message

            if (!tempfromMail.isEmpty() && tempfromMail.length() - 4 > 0) {
                afterAt = tempfromMail.substring(tempfromMail.length() - 4);
            }

            String errorMessage = "";
            if (tempfromMail.isEmpty()) {
                errorMessage = "Mail Address Cannot Be Empty!";
            } else if (!tempfromMail.contains("@")) {
                errorMessage = "Mail Address Must Contains '@'!";
            } else if (tempfromMail.substring(0).equals("@")) {
                errorMessage = "Mail Address Cannot Start With '@'!";
            } else if (!afterAt.contains(".com")) {
                errorMessage = "Mail Address Must Contains '.com'!";
            }

            if (errorMessage.isEmpty()) {
                mailAddress = tempfromMail;
                break;
            } else {
                JOptionPane.showMessageDialog(null, errorMessage, "Wrong Mail Address", JOptionPane.ERROR_MESSAGE);
            }
        }

        while (true) {
            String tempPassword = JOptionPane.showInputDialog(null, "Enter The Password", "Password", JOptionPane.INFORMATION_MESSAGE);

            if (tempPassword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Password Cannot Be Empty!", "Empty Password", JOptionPane.ERROR_MESSAGE);
            } else {
                password = tempPassword;
                break;
            }
        }

        Host currentUser = new Host(mailAddress, password, "587");
        userList.add(currentUser);

        createNewProps(mailAddress, password);
        viewManager.newHost(stage);
    }

    static void main(String[] args) {
        launch(args);
    }

    void createNewProps(String mailAddress, String password) {
        props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.store.protocol", "imaps");
        props.put("mail.smtp.auth", "true");

        props.put("mail.from", mailAddress);
        a = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailAddress, password);
            }
        };
    }
}
//REFERENCES
//https://docs.oracle.com/javafx/2/ui_controls/table-view.htm
//https://stackoverflow.com/questions/56495943/how-to-define-setonaction-for-tableview-rows
//https://docs.oracle.com/javafx/2/ui_controls/tree-view.htm 
//https://edencoding.com/force-refresh-scene/
