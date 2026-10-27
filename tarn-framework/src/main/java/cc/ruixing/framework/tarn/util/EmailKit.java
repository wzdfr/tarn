package cc.ruixing.framework.tarn.util;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

/**
 * email tools of tarn support.
 */
public class EmailKit {
    private MimeMessage message;
    private Multipart mp = new MimeMultipart();
    private Session ssn;
    private String server;
    private String uid;
    private String pwd;
    public EmailKit() throws MessagingException {
        this("mail.wz2b.com", "tech@wz2b.com", "tech@wz2b.com", "Testdata2006");
    }

    public EmailKit(String server, String eml, String uid, String pwd) throws MessagingException {
        this.server = server;
        this.uid = uid;
        this.pwd = pwd;
        Properties props = new Properties();
        props.put("mail.smtp.host", server);
        props.put("mail.smtp.auth", "true");
        props.put("mail.transport.protocol", "smtp");
        CheckSendMail auth = new CheckSendMail(uid, pwd);
        ssn = Session.getInstance(props, auth);
        message = new MimeMessage(ssn);
        InternetAddress fromAddress = new InternetAddress(eml);
        message.setFrom(fromAddress);
    }

    /**
     * 设置发送邮件的标题.
     *
     * @param subject 邮件的标题字符串. 必须是纯文本的字符串,建议不要超过64个字符.
     * @return 当前的Eml对象
     * @throws javax.mail.MessagingException 发生错误时抛出此异常
     */
    public EmailKit setSubject(String subject) throws MessagingException {
        message.setSubject(subject);
        return this;
    }

    /**
     * 设置邮件的正文内容.
     *
     * @param html 邮件的正文内容. 标准的html格式.
     * @return 当前的Eml对象
     * @throws javax.mail.MessagingException 发生错误时抛出此异常
     */
    public EmailKit setBody(String html) throws MessagingException {
        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setContent(html, "text/html; charset=utf-8");
        mp.addBodyPart(mbp);
        return this;
    }

    /**
     * 设置邮件接收者的E-mail地址.
     *
     * @param to 邮件接收者的E-mail地址.
     * @return 当前的Eml对象
     * @throws javax.mail.MessagingException 发生错误时抛出此异常
     */
    public EmailKit addTo(String to) throws MessagingException {
        InternetAddress toAddress = new InternetAddress(to);
        message.addRecipient(Message.RecipientType.TO, toAddress);
        return this;
    }

    /**
     * 添加邮件接收的E-mail地址.
     *
     * @param tos 邮件接收者的E-mail地址数组.
     * @return 当前EmailKit对象
     * @throws javax.mail.MessagingException 发生错误时抛出此异常
     */
    public EmailKit addTo(String tos[]) throws MessagingException {
        InternetAddress toAddress[] = new InternetAddress[tos.length];
        for (int i = 0, n = tos.length; i < n; i++) {
            toAddress[i] = new InternetAddress(tos[i]);
        }
        message.addRecipients(Message.RecipientType.TO, toAddress);
        return this;
    }

    @Deprecated
    public EmailKit setTo(String to) throws MessagingException {
        return addTo(to);
    }

    /**
     * 添加邮件抄送的E-mail地址
     *
     * @param cc 邮件抄送者的E-mail地址.
     * @return 当前EmailKit对象
     * @throws javax.mail.MessagingException 发生错误时抛出此异常
     */
    public EmailKit addCc(String cc) throws MessagingException {
        InternetAddress toAddress = new InternetAddress(cc);
        message.addRecipient(Message.RecipientType.CC, toAddress);
        return this;
    }

    /**
     * 添加邮件暗送的E-mail地址
     *
     * @param bcc 邮件暗送者的E-mail地址.
     * @return 当前EmailKit对象
     * @throws javax.mail.MessagingException 发生错误时抛出此异常
     */
    public EmailKit addBcc(String bcc) throws MessagingException {
        InternetAddress toAddress = new InternetAddress(bcc);
        message.addRecipient(Message.RecipientType.BCC, toAddress);
        return this;
    }


    //该方法用于收集附件名
    public void addFile(String filename) throws MessagingException {
        MimeBodyPart mbp = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(filename);
        mbp.setDataHandler(new DataHandler(fds));
        mbp.setFileName(fds.getName());
        mp.addBodyPart(mbp);
    }

    /**
     * 添加邮件附件发送
     *
     * @param file 文件对象
     * @return 当前EmailKit对象
     * @throws javax.mail.MessagingException 发生错误时抛出此异常
     */
    //添加附件
    public void addFile(File file) throws MessagingException {
        addFile(file.getAbsolutePath());
    }

    /**
     * 开始发送邮件.
     * 在调用此方法之前,请确保己经设置好收件人的地址,邮件主题，邮件正文等内容.
     *
     * @throws javax.mail.MessagingException 当发送邮件时有错误时抛出此异常
     */
    @SuppressWarnings({"ConstantConditions"})
    public void send() throws MessagingException {
        message.setContent(mp);
        Transport transport = ssn.getTransport("smtp");
        transport.connect(server, uid, pwd);
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
    }

    class CheckSendMail extends Authenticator {
        String username = null;
        String password = null;

        public CheckSendMail(String user, String pass) {
            username = user;
            password = pass;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
