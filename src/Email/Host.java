package Email;
//@author AlperenDGRYL

public class Host {

    private String mailAddress;
    private String password;
    private String port;

    public Host(String mailAddress, String password, String port) {
        this.mailAddress = mailAddress;
        this.password = password;
        this.port = port;
    }

    public String getMailAddress() {
        return mailAddress;
    }

    public void setMailAddress(String mailAddress) {
        this.mailAddress = mailAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Host{" + "mailAddress=" + mailAddress + ", password=" + password + ", port=" + port + '}';
    }

}
