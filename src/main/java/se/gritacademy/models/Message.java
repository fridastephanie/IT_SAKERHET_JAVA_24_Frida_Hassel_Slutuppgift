package se.gritacademy.models;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String sender;
    @Column(nullable = false)
    private String receiver;
    @Column(nullable = false)
    private String encryptedMessage;
    @Column(nullable = false)
    private String nonce;
    @Column(nullable = false)
    private String authTag;
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    public Message() {
    }

    public Message(String sender, String receiver, String encryptedMessage, String nonce, String authTag, Date date) {
        this.sender = sender;
        this.receiver = receiver;
        this.encryptedMessage = encryptedMessage;
        this.nonce = nonce;
        this.authTag = authTag;
        this.date = date;
    }

    public Long getId() {
        return id;
    }
    public String getSender() {
        return sender;
    }
    public String getReceiver() {
        return receiver;
    }
    public String getEncryptedMessage() {
        return encryptedMessage;
    }
    public String getNonce() {
        return nonce;
    }
    public String getAuthTag() {
        return authTag;
    }
    public Date getDate() {
        return date;
    }
}
