package com.redsponge.networking.chat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.ListIterator;

public class ChatClient {

    private Socket s;
    private Thread receiver;
    private JFrame frame;
    private JButton sendbutton;
    private JTextArea inputBox;
    private JTextArea chat;
    private String username;
    private JTextArea onlineUsers;

    private PrintWriter out;

    public ChatClient() {
        setupGui();
        connect();
    }

    public void connect() {
        try {
            s = new Socket(Shared.ip, Shared.port);
            out = new PrintWriter(s.getOutputStream(), true);
            receiver = new Thread(this::receive);
            receiver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupGui() {
        frame = new JFrame("Chat App");
        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        sendbutton = new JButton("Send!");

        inputBox = new JTextArea("Type Here!");
        //inputBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        sendbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ACTION");
                send(inputBox.getText());
                inputBox.setText("");
            }
        });


        frame.setLayout(new BorderLayout());

        Container con = frame.getContentPane();
        JPanel chatPanel = new JPanel(new GridBagLayout());
        //chatPanel.setBackground(Color.BLUE);
        GridBagConstraints c = new GridBagConstraints();

        chat = new JTextArea();
        chat.setEditable(false);

        onlineUsers = new JTextArea();
        onlineUsers.setEditable(false);

        chat.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Chat"));
        onlineUsers.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Online Users"));

        c.weightx = 3;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        chatPanel.add(chat, c);
        c.weightx = 1;
        chatPanel.add(onlineUsers, c);


        inputBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel send = new JPanel(new BorderLayout());

        send.add(inputBox, BorderLayout.CENTER);
        send.add(sendbutton, BorderLayout.EAST);

        con.add(chatPanel, BorderLayout.CENTER);
        con.add(send, BorderLayout.SOUTH);

        username = JOptionPane.showInputDialog(frame, "Username: ");
        frame.setVisible(true);
    }

    public void send(String message) {
        System.out.println("SENDING " + message);
        out.println(message);
    }

    public void receive() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            while(!s.isClosed()) {
                String data = br.readLine();
                System.out.println(data);
                if(data.startsWith(Shared.message_prefix)) {
                    data = data.substring(Shared.message_prefix.length());
                    printIntoChat(data);
                }
                if(data.equals("USERNAME")) {
                    System.out.println("USERNAME: " + username);
                    send(username);
                }
                if(data.equals("QUIT")) {
                    s.close();
                } if(data.equals(Shared.user_update)) {
                    updateUserList(br);
                }
            }
            System.out.println("Stopped listening to server! joining thread");
            receiver.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Couldn't join thread!", e);
        } finally {
            System.out.println("Fully disconnected!");
        }
        System.exit(0);
    }

    private void updateUserList(BufferedReader in) {
        onlineUsers.setText("");
        String[] users = getOnlineUsers(in);
        onlineUsers.setText(String.join("\n", users));
    }

    public String[] getOnlineUsers(BufferedReader in) {
        String s = request(Shared.online_user_request, in);
        ;System.out.println("GOT ONLINE USERS: " + s);
        return s.split("\n");
    }

    public String request(String data, BufferedReader in) {
        send(data);
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void disconnect() {
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printIntoChat(String s) {
        chat.append(s + "\n");
    }

    public static void main(String[] args) {
        new ChatClient();
    }
}
