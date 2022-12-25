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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.swing.JOptionPane;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
//@author AlperenDGRYL

public class Controller extends Application {

    TextField currentHost;

    Properties props;
    Authenticator a;

    Host currentUser;
    ArrayList<Host> userList = new ArrayList<>();
    ObservableList<Email> inboxEmails = FXCollections.observableArrayList();
    ObservableList<Email> junkEmails = FXCollections.observableArrayList();
    ObservableList<Email> sentEmails = FXCollections.observableArrayList();
    TableView<Email> mailTable;

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

        currentUser = new Host(mailAddress, password, "587");
        userList.add(currentUser);

        createNewProps(mailAddress, password);
        mainPageUI(stage);
        receiveMail();
    }

    void mainPageUI(Stage stage) {
        VBox root = new VBox(10);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topPanel = new HBox(5);
        topPanel.prefWidthProperty().bind(stage.widthProperty());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> {
            mailTable.getItems().clear();
            refreshBtn.getScene().getRoot().requestLayout();
            showAlert(1, "Page Refreshed.");
            receiveMail();
        });

        Button composeNewBtn = new Button("Compose New");
        composeNewBtn.setOnAction(e -> {
            composeNew(stage);
        });

        Button replyBtn = new Button("Reply");
        replyBtn.setOnAction(e -> {
            reply(stage);
        });

        Button newHostBtn = new Button("New Host");
        newHostBtn.setOnAction(e -> {
            newHost(stage);
        });

        ComboBox hostComboBox = new ComboBox<>();
        hostComboBox.setMinWidth(150);
        for (int i = 0; i < userList.size(); i++) {
            hostComboBox.getItems().addAll(userList.get(i).getMailAddress());
        }
        hostComboBox.setPromptText("Change Your Host");
        hostComboBox.setOnAction(e -> {

            showAlert(2, "Host has changed to {" + hostComboBox.getValue().toString() + "}");
            currentHost.setText("Current Host : " + hostComboBox.getValue().toString());

            for (int i = 0; i < userList.size(); i++) {
                if (userList.get(i).getMailAddress().contains(hostComboBox.getValue().toString())) {
                    currentUser = userList.get(i);
                }
            }
            //CREATE NEW PROPS
            //CHANGE USER INFO
        });

        topPanel.getChildren().addAll(refreshBtn, composeNewBtn, replyBtn, newHostBtn, hostComboBox);
        topPanel.setAlignment(Pos.CENTER);

        ObservableList<Node> topButtons = topPanel.getChildren();
        topButtons.stream().filter((child) -> (child instanceof Button)).forEachOrdered((child) -> {
            ((Button) child).prefWidthProperty().bind(stage.widthProperty()); //for (Node child : topButtons) { if (child instanceof Button) { ((Button) child).prefWidthProperty().bind(stage.widthProperty()); }}
        });

        TextArea mailDetails = new TextArea();
        mailDetails.setPromptText("Selected message details will appear here.");
        mailDetails.setEditable(false);
        mailDetails.setPrefHeight(550);

        HBox bottomPanel = new HBox(5);
        bottomPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        mailTable = new TableView<>();
        mailTable.setEditable(false);
        mailTable.setMinWidth(600);
        mailTable.prefWidthProperty().bind(stage.widthProperty());
        mailTable.prefHeightProperty().bind(stage.heightProperty());

        TableColumn<Email, String> senderNameColumn = new TableColumn<>("Sender Name");
        TableColumn<Email, String> subjectColumn = new TableColumn<>("Subject");
        TableColumn<Email, Date> dateColumn = new TableColumn<>("Date");
        TableColumn<Email, String> attachmentColumn = new TableColumn<>("Attachment");

        senderNameColumn.setCellValueFactory(new PropertyValueFactory<>("senderName"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        attachmentColumn.setCellValueFactory(new PropertyValueFactory<>("attachment"));

        mailTable.getColumns().addAll(senderNameColumn, subjectColumn, dateColumn, attachmentColumn);

        ObservableList<TableColumn<Email, ?>> columns = mailTable.getColumns();
        columns.forEach((column) -> {
            column.prefWidthProperty().bind(mailTable.widthProperty().divide(columns.size()));
        });

        mailTable.setOnMouseClicked(e -> {
            Email selectedMail = mailTable.getSelectionModel().getSelectedItem();
            if (selectedMail != null) {
                mailDetails.setText(selectedMail.toString());
            }
        });

        TreeItem<String> rootItem = new TreeItem<>("Folders");
        TreeItem<String> inbox = new TreeItem<>("Inbox");
        TreeItem<String> junks = new TreeItem<>("Junks");
        TreeItem<String> sent = new TreeItem<>("Sent");

        rootItem.setExpanded(true);
        rootItem.getChildren().addAll(inbox, junks, sent);

        TreeView<String> folder = new TreeView<>(rootItem);

        folder.setOnMouseClicked(e -> {
            TreeItem<String> selectedItem = folder.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                int i = 0;
                switch (selectedItem.getValue()) {
                    case "Inbox":
                        mailTable.setItems(inboxEmails);
                        break;
                    case "Junks":
                        mailTable.setItems(junkEmails);
                        break;
                    case "Sent":
                        mailTable.setItems(sentEmails);
                        break;
                }
            }
        });
        folder.setPrefWidth(275);
        folder.prefHeightProperty().bind(stage.heightProperty());

        bottomPanel.getChildren().addAll(folder, mailTable);
        currentHost = new TextField("Current Host : " + currentUser.getMailAddress());
        currentHost.setPrefHeight(100);
        currentHost.setEditable(false);
        root.getChildren().addAll(topPanel, mailDetails, bottomPanel, currentHost);

        Scene scene = new Scene(root);
        stage.setTitle("Email Client");
        stage.setHeight(600);
        stage.setMinHeight(500);
        stage.setWidth(960);
        stage.setMinWidth(800);
        stage.setScene(scene);
        stage.show();
    }

    void composeNew(Stage stage) {
        Session session = Session.getDefaultInstance(props, a);

        VBox root = new VBox(10);// Create a VBox layout
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topPanel = new HBox(5);// (Spacing) Create a Hbox layout for buttons
        topPanel.prefWidthProperty().bind(stage.widthProperty());

        VBox detailsBox = new VBox(10);

        TextField to = new TextField();
        to.setPromptText("To");

        TextField subject = new TextField();
        subject.setPromptText("Subject");

        HBox attachmentBox = new HBox(5);

        TextField attachmentNameTF = new TextField();
        attachmentNameTF.setPromptText("Attachment");
        attachmentNameTF.setMinWidth(300);

        Button selectFileBtn = new Button("Select File");
        selectFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(new Stage());
            if (file != null) {
                attachmentNameTF.setText(file.getName());
            }
        });

        attachmentBox.getChildren().addAll(attachmentNameTF, selectFileBtn);

        detailsBox.getChildren().addAll(to, subject, attachmentBox);

        TextArea eMailBody = new TextArea();
        eMailBody.setPromptText("Message body will be written here");
        eMailBody.setEditable(true);
        eMailBody.prefHeightProperty().bind(stage.heightProperty());

        ComboBox pages = new ComboBox<>();
        pages.getItems().addAll("Main Page", "Compose New", "Reply", "New Host");
        pages.setPromptText("\uD83C\uDFE0");
        pages.setOnAction(e -> {
            System.out.println(pages.getValue().toString());
            if (pages.getValue().toString().equals("Main Page")) {
                mainPageUI(stage);
            }
            if (pages.getValue().toString().equals("Compose New")) {
                composeNew(stage);
            }
            if (pages.getValue().toString().equals("Reply")) {
                reply(stage);
            }
            if (pages.getValue().toString().equals("New Host")) {
                newHost(stage);
            }
        });
        pages.setMinWidth(60);

        ComboBox hostComboBox = new ComboBox<>();
        hostComboBox.setMinWidth(150);
        for (int i = 0; i < userList.size(); i++) {
            hostComboBox.getItems().addAll(userList.get(i).getMailAddress());
        }
        hostComboBox.setPromptText("Change Your Host");
        hostComboBox.setOnAction(e -> {
            showAlert(2, "Host has changed to {" + hostComboBox.getValue().toString() + "}");
            currentHost.setText("Current Host : " + hostComboBox.getValue().toString());
            for (int i = 0; i < userList.size(); i++) {
                if (userList.get(i).getMailAddress().contains(hostComboBox.getValue().toString())) {
                    currentUser = userList.get(i);
                }
            }
        });

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            System.out.println("Email Body Part : " + eMailBody.getText());
            String hasAttachment;
            try {
                if (!attachmentNameTF.getText().equals("")) { //With Attachment.
                    MimeMessage msg = new MimeMessage(session);
                    msg.setFrom(); //from is empty
                    msg.setRecipients(Message.RecipientType.TO, to.getText());
                    msg.setSubject(subject.getText());
                    msg.setSentDate(new Date());
                    Multipart mp = new MimeMultipart();
                    BodyPart bp = new MimeBodyPart();

                    bp.setText(eMailBody.getText());
                    mp.addBodyPart(bp);
                    BodyPart partForAtt = new MimeBodyPart();
                    String filename = attachmentNameTF.getText();
                    DataSource source = new FileDataSource(filename);
                    partForAtt.setDataHandler(new DataHandler(source));
                    partForAtt.setFileName(filename);
                    mp.addBodyPart(partForAtt);
                    msg.setContent(mp);
                    Transport.send(msg);
                    hasAttachment = filename + " \uD83D\uDCCE"; //TRUE, ATTACHMENT
                    System.out.println("E-mail Sent With Attachment!");
                } else { //Without Attachment.
                    Address toAddress = new InternetAddress(to.getText());
                    MimeMessage msg = new MimeMessage(session);
                    msg.setSubject(subject.getText());
                    msg.setContent("<p>" + eMailBody.getText() + "</p>", "text/html");
                    msg.setRecipient(Message.RecipientType.TO, toAddress);
                    Transport.send(msg);
                    hasAttachment = " X "; //FALSE, CROSS
                    System.out.println("E-mail Sent Without Attachment!");
                }
                Email sentMail = new Email(to.getText(), subject.getText(), eMailBody.getText(), new Date(), hasAttachment);
                sentEmails.add(sentMail);
                clearAll(to, subject, attachmentNameTF, eMailBody, "E-Mail Sent Successfully.");
            } catch (MessagingException mex) {
                System.out.println("Send failed, exception: " + mex);
            }
        });

        topPanel.getChildren().addAll(pages, sendButton, hostComboBox);
        topPanel.setAlignment(Pos.CENTER);

        ObservableList<Node> topButtons = topPanel.getChildren();
        topButtons.stream().filter((child) -> (child instanceof Button)).forEachOrdered((child) -> {
            ((Button) child).prefWidthProperty().bind(stage.widthProperty());
        });

        currentHost = new TextField("Current Host : " + currentUser.getMailAddress());
        currentHost.setPrefHeight(100);
        currentHost.setEditable(false);
        root.getChildren().addAll(topPanel, detailsBox, eMailBody, currentHost);

        Scene scene = new Scene(root);
        stage.setTitle("Compose New");
        stage.getIcons().add(new Image("https://static.thenounproject.com/png/3627054-200.png"));
        stage.setHeight(600);
        stage.setMinHeight(500);
        stage.setWidth(960);
        stage.setMinWidth(800);
        stage.setScene(scene);
        stage.show();
    }

    void reply(Stage stage) {
        Session session = Session.getDefaultInstance(props, a);

        VBox root = new VBox(10);// Create a VBox layout
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topPanel = new HBox(5);// (Spacing) Create a Hbox layout for buttons
        topPanel.prefWidthProperty().bind(stage.widthProperty());

        VBox detailsBox = new VBox(10);

        TextField to = new TextField();
        to.setPromptText("To");

        TextField subject = new TextField();
        subject.setPromptText("Subject");

        HBox attachmentBox = new HBox(5);

        TextField attachmentNameTF = new TextField();
        attachmentNameTF.setPromptText("Attachment");
        attachmentNameTF.setMinWidth(300);

        Button selectFileBtn = new Button("Select File");
        selectFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(new Stage());
            if (file != null) {
                attachmentNameTF.setText(file.getName());
            }
        });

        attachmentBox.getChildren().addAll(attachmentNameTF, selectFileBtn);

        detailsBox.getChildren().addAll(to, subject, attachmentBox);

        TextArea eMailBody = new TextArea();
        eMailBody.setPromptText("Message body will be written here");
        eMailBody.setEditable(true);
        eMailBody.setPrefHeight(550);

        ComboBox pages = new ComboBox<>();
        pages.getItems().addAll("Main Page", "Compose New", "Reply", "New Host");
        pages.setPromptText("\uD83C\uDFE0");
        pages.setOnAction(e -> {
            System.out.println(pages.getValue().toString());
            if (pages.getValue().toString().equals("Main Page")) {
                mainPageUI(stage);
            }
            if (pages.getValue().toString().equals("Compose New")) {
                composeNew(stage);
            }
            if (pages.getValue().toString().equals("Reply")) {
                reply(stage);
            }
            if (pages.getValue().toString().equals("New Host")) {
                newHost(stage);
            }
        });
        pages.setMinWidth(60);

        ComboBox hostComboBox = new ComboBox<>();
        hostComboBox.setMinWidth(150);
        for (int i = 0; i < userList.size(); i++) {
            hostComboBox.getItems().addAll(userList.get(i).getMailAddress());
        }
        hostComboBox.setPromptText("Change Your Host");
        hostComboBox.setOnAction(e -> {
            showAlert(2, "Host has changed to {" + hostComboBox.getValue().toString() + "}");
            currentHost.setText("Current Host : " + hostComboBox.getValue().toString());
            for (int i = 0; i < userList.size(); i++) {
                if (userList.get(i).getMailAddress().contains(hostComboBox.getValue().toString())) {
                    currentUser = userList.get(i);
                }
            }
        });

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            System.out.println("Email Body Part : " + eMailBody.getText());
            String hasAttachment;
            try {
                if (!attachmentNameTF.getText().equals("")) { //With Attachment.
                    MimeMessage msg = new MimeMessage(session);
                    msg.setFrom(); //from is empty
                    msg.setRecipients(Message.RecipientType.TO, to.getText());
                    msg.setSubject(subject.getText());
                    msg.setSentDate(new Date());
                    Multipart mp = new MimeMultipart();
                    BodyPart bp = new MimeBodyPart();

                    bp.setText(eMailBody.getText());
                    mp.addBodyPart(bp);
                    BodyPart partForAtt = new MimeBodyPart();
                    String filename = attachmentNameTF.getText();
                    DataSource source = new FileDataSource(filename);
                    partForAtt.setDataHandler(new DataHandler(source));
                    partForAtt.setFileName(filename);
                    mp.addBodyPart(partForAtt);
                    msg.setContent(mp);
                    Transport.send(msg);
                    hasAttachment = filename + " \uD83D\uDCCE"; //TRUE, ATTACHMENT
                    System.out.println("E-mail Sent With Attachment!");
                } else { //Without Attachment.
                    Address toAddress = new InternetAddress(to.getText());
                    MimeMessage msg = new MimeMessage(session);
                    msg.setSubject(subject.getText());
                    msg.setContent("<p>" + eMailBody.getText() + "</p>", "text/html");
                    msg.setRecipient(Message.RecipientType.TO, toAddress);
                    Transport.send(msg);
                    hasAttachment = " X "; //FALSE, CROSS
                    System.out.println("E-mail Sent Without Attachment!");
                }
                Email sentMail = new Email(to.getText(), subject.getText(), eMailBody.getText(), new Date(), hasAttachment);
                sentEmails.add(sentMail);
                clearAll(to, subject, attachmentNameTF, eMailBody, "E-Mail Sent Successfully.");
            } catch (MessagingException mex) {
                System.out.println("Send failed, exception: " + mex);
            }
        });

        topPanel.getChildren().addAll(pages, sendButton, hostComboBox);
        topPanel.setAlignment(Pos.CENTER);

        ObservableList<Node> topButtons = topPanel.getChildren();
        topButtons.stream().filter((child) -> (child instanceof Button)).forEachOrdered((child) -> {
            ((Button) child).prefWidthProperty().bind(stage.widthProperty());
        });

        HBox bottomPanel = new HBox(5);
        bottomPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        mailTable = new TableView<>();
        mailTable.setEditable(false);
        mailTable.setMinWidth(600);
        mailTable.prefWidthProperty().bind(stage.widthProperty());
        mailTable.prefHeightProperty().bind(stage.heightProperty());

        TableColumn<Email, String> senderNameColumn = new TableColumn<>("Sender Name");
        TableColumn<Email, String> subjectColumn = new TableColumn<>("Subject");
        TableColumn<Email, Date> dateColumn = new TableColumn<>("Date");
        TableColumn<Email, Boolean> attachmentColumn = new TableColumn<>("Attachment");

        senderNameColumn.setCellValueFactory(new PropertyValueFactory<>("senderName"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        attachmentColumn.setCellValueFactory(new PropertyValueFactory<>("attachment"));

        mailTable.getColumns().addAll(senderNameColumn, subjectColumn, dateColumn, attachmentColumn);

        ObservableList<TableColumn<Email, ?>> columns = mailTable.getColumns();
        columns.forEach((column) -> {
            column.prefWidthProperty().bind(mailTable.widthProperty().divide(columns.size()));
        });

        mailTable.setOnMouseClicked(e -> {
            Email selectedMail = mailTable.getSelectionModel().getSelectedItem();
            if (selectedMail != null) {
                if (selectedMail.getSenderName().contains("<") || selectedMail.getSenderName().contains(">")) {
                    String input = selectedMail.getSenderName();
                    int startIndex = input.indexOf("<");
                    int endIndex = input.indexOf(">", startIndex);
                    String output = input.substring(startIndex + 1, endIndex);
                    to.setText(output.trim());
                } else {
                    to.setText(selectedMail.getSenderName().trim());
                }
            }
        });

        TreeItem<String> rootItem = new TreeItem<>("Folders");
        TreeItem<String> inbox = new TreeItem<>("Inbox");
        TreeItem<String> junks = new TreeItem<>("Junks");
        TreeItem<String> sent = new TreeItem<>("Sent");

        rootItem.setExpanded(true);
        rootItem.getChildren().addAll(inbox, junks, sent);

        TreeView<String> folder = new TreeView<>(rootItem);

        folder.setOnMouseClicked(e -> {
            TreeItem<String> selectedItem = folder.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                int i = 0;
                switch (selectedItem.getValue()) {
                    case "Inbox":
                        mailTable.setItems(inboxEmails);
                        break;
                    case "Junks":
                        mailTable.setItems(junkEmails);
                        break;
                    case "Sent":
                        mailTable.setItems(sentEmails);
                        break;
                }
            }
        });
        folder.setPrefWidth(275);
        folder.prefHeightProperty().bind(stage.heightProperty());
        bottomPanel.getChildren().addAll(folder, mailTable);

        currentHost = new TextField("Current Host : " + currentUser.getMailAddress());
        currentHost.setPrefHeight(100);
        currentHost.setEditable(false);
        root.getChildren().addAll(topPanel, detailsBox, eMailBody, bottomPanel, currentHost);

        Scene scene = new Scene(root);
        stage.setTitle("Reply");
        stage.getIcons().add(new Image("https://cdn-icons-png.flaticon.com/512/1/1846.png"));
        stage.setHeight(600);
        stage.setMinHeight(500);
        stage.setWidth(960);
        stage.setMinWidth(800);
        stage.setScene(scene);
        stage.show();
    }

    void newHost(Stage stage) {
        VBox root = new VBox(30);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topPanel = new HBox(5);
        topPanel.prefWidthProperty().bind(stage.widthProperty());

        ComboBox pages = new ComboBox<>();
        pages.getItems().addAll("Main Page", "Compose New", "Reply", "New Host");
        pages.setPromptText("\uD83C\uDFE0");
        pages.setOnAction(e -> {
            System.out.println(pages.getValue().toString());
            if (pages.getValue().toString().equals("Main Page")) {
                mainPageUI(stage);
            }
            if (pages.getValue().toString().equals("Compose New")) {
                composeNew(stage);
            }
            if (pages.getValue().toString().equals("Reply")) {
                reply(stage);
            }
            if (pages.getValue().toString().equals("New Host")) {
                newHost(stage);
            }
        });
        pages.setMinWidth(40);
        pages.prefWidth(45);

        topPanel.getChildren().addAll(pages);
        topPanel.setAlignment(Pos.TOP_CENTER);
        topPanel.prefWidthProperty().bind(stage.widthProperty());

        Text protocolTXT = new Text("Receive Protocol");
        HBox protocolBox = new HBox(15);
        protocolBox.prefWidthProperty().bind(stage.widthProperty());
        RadioButton imap = new RadioButton("imap");
        RadioButton pop3 = new RadioButton("pop3");
        protocolBox.getChildren().addAll(imap, pop3);
        protocolBox.setAlignment(Pos.CENTER);

        Text portTXT = new Text("Ports");
        HBox portBox = new HBox(15);
        portBox.prefWidthProperty().bind(stage.widthProperty());
        TextField receivePort = new TextField();
        receivePort.setPromptText("Receive Port Address");
        TextField sendPort = new TextField();
        sendPort.setPromptText("Send Port Address");
        portBox.getChildren().addAll(receivePort, sendPort);
        portBox.setAlignment(Pos.CENTER);

        Text hostTXT = new Text("Host Addresses");
        HBox hostBox = new HBox(15);
        hostBox.prefWidthProperty().bind(stage.widthProperty());
        TextField toReceive = new TextField();
        toReceive.setPromptText("To Receive");
        TextField toSend = new TextField();
        toSend.setPromptText("To Send");
        hostBox.getChildren().addAll(toReceive, toSend);
        hostBox.setAlignment(Pos.CENTER);

        Text namePassTXT = new Text("User Name & Password");
        HBox namePassBox = new HBox(15);
        namePassBox.prefWidthProperty().bind(stage.widthProperty());
        TextField emailAddress = new TextField();
        emailAddress.setPromptText("Email Address");
        TextField password = new TextField();
        password.setPromptText("Password");
        namePassBox.getChildren().addAll(emailAddress, password);
        namePassBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Save");

        saveBtn.setOnAction(e -> {
            if (!emailAddress.getText().equals("") || !password.getText().equals("")) {
                Host newUser = new Host(emailAddress.getText(), password.getText(), "587");
                currentUser = newUser;
                userList.add(newUser);
                showAlert(2, "New Host Added Successfuly.\nYou Can Change Your Host!");
                mainPageUI(stage);
            }
        });

        root.getChildren().addAll(topPanel, protocolTXT, protocolBox, portTXT, portBox, hostTXT, hostBox, namePassTXT, namePassBox, saveBtn);
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root);
        stage.setTitle("New Host");
        stage.getIcons().add(new Image("https://static.thenounproject.com/png/3662728-200.png"));
        stage.setHeight(550);
        stage.setWidth(300);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    void receiveMail() {
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
            Message[] messages = inbox.getMessages();
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

        } catch (IOException | MessagingException ex) {
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

    void clearAll(TextField to, TextField subject, TextField attachmentName, TextArea eMailBody, String infoMsg) {
        to.clear();
        subject.clear();
        attachmentName.clear();
        eMailBody.clear();
        showAlert(1, infoMsg);
    }

    void showAlert(int duration, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.show();
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(duration),
                ae -> alert.close()
        ));
        timeline.play();
    }

    static void main(String[] args) {
        launch(args);
    }
}
//REFERENCES
//https://docs.oracle.com/javafx/2/ui_controls/table-view.htm
//https://stackoverflow.com/questions/56495943/how-to-define-setonaction-for-tableview-rows
//https://docs.oracle.com/javafx/2/ui_controls/tree-view.htm 
//https://edencoding.com/force-refresh-scene/
