package com.redsponge.networking.chat;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ListIterator;

public class ChatClient {

    private Socket s;
    private JFrame frame;
    private JButton sendbutton;
    private JTextField inputBox;
    private JTextArea chat;
    private String username;
    private JTextArea onlineUsers;
    private String ip;
    private int port;

    private boolean connected;

    private PrintWriter out;

    public ChatClient() {
        ip = Shared.ip;
        port = Shared.port;
        setupGui();
        connect();
    }

    public void connect() {
        try {
            s = new Socket(ip, port);
            connected = true;
            out = new PrintWriter(s.getOutputStream(), true);
            receive();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupGui() {
        frame = new JFrame("Chat App");
        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(connected) {
                    disconnect();
                }
            }
        });

        sendbutton = new JButton("Send!");

        inputBox = new JTextField("Type Here!");
        inputBox.addActionListener(this::actionPerformed);
        //inputBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        sendbutton.addActionListener(this::actionPerformed);


        frame.setLayout(new BorderLayout());

        Container con = frame.getContentPane();
        JPanel chatPanel = new JPanel(new GridBagLayout());
        //chatPanel.setBackground(Color.BLUE);
        GridBagConstraints c = new GridBagConstraints();

        chat = new JTextArea();
        chat.setEditable(false);
        chat.setAutoscrolls(true);
        JScrollPane chatScroller = new JScrollPane(chat);
        chatScroller.setAutoscrolls(true);
        onlineUsers = new JTextArea();
        onlineUsers.setEditable(false);

        chat.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Chat"));
        onlineUsers.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Online Users"));

        c.weightx = 3;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        chatPanel.add(chatScroller, c);
        c.weightx = 1;
        chatPanel.add(onlineUsers, c);


        inputBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel send = new JPanel(new BorderLayout());

        send.add(inputBox, BorderLayout.CENTER);
        send.add(sendbutton, BorderLayout.EAST);

        con.add(chatPanel, BorderLayout.CENTER);
        con.add(send, BorderLayout.SOUTH);

        username = JOptionPane.showInputDialog(frame, "Username: ");
        if(username == null || username.trim().equals("")) {
            System.exit(-1);
        }

        JPanel userData = new JPanel(new GridLayout());
        userData.setBackground(new Color(0, 0, 0, 0));
        userData.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "User Data"));


        String[][] labels = {
            {"USERNAME:", username},
            {"IP:", ip},
            {"PORT:", Integer.toString(port)}
        };
        for(String[] entry : labels) {
            JTextArea label = new JTextArea(entry[1]);
            label.setEditable(false);
            label.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), entry[0]));
            userData.add(label);
        }

        con.add(userData, BorderLayout.NORTH);
        frame.setVisible(true);
    }

    public void send(String message) {
        System.out.println("SENDING " + message);
        out.println(message);
    }

    public void processMessage(String message){
        if(message.startsWith(Shared.command_prefix)) {
            disconnect();
        } else if(!message.trim().equals("")){
            send(Shared.message_prefix + message);
        }
    }

    public void receive() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            boolean running = true;
            while(running) {
                String data = br.readLine();
                System.out.println(data);
                if(data.startsWith(Shared.message_prefix)) {
                    data = data.substring(Shared.message_prefix.length());
                    printIntoChat(data);
                }
                else if(data.equals("USERNAME")) {
                    System.out.println("USERNAME: " + username);
                    send(username);
                }
                else if(data.equals("QUIT")) {
                    running = false;
                }
                else if(data.equals(Shared.user_update)) {
                    updateUserList(br);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Fully disconnected!");
        }
        System.exit(0);
    }

    private void updateUserList(BufferedReader in) {
        String[] users = getOnlineUsers(in);
        System.out.println("ONLINE USERS HERE");
        StringBuilder sb = new StringBuilder();
        for(String user : users) {
            sb.append("* ");
            sb.append(user);
            sb.append("\n");
        }
        System.out.println("BUILT STRING");
        String s = sb.toString();
        SwingUtilities.invokeLater(
                () -> {onlineUsers.setText("");onlineUsers.append(s);}
        );

        System.out.println("DONE");
    }

    public String[] getOnlineUsers(BufferedReader in) {
        String s = request(Shared.online_user_request, in);
        System.out.println("GOT ONLINE USERS: " + s);
        return s.split(Shared.ARRAY_JOIN_DELIMETER);
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
        send("QUIT");
    }

    public void printIntoChat(String s) {
        SwingUtilities.invokeLater(
            () -> chat.append(s + '\n')
        );
    }

    public static void main(String[] args) {
        new ChatClient();
    }

    private void actionPerformed(ActionEvent e) {
        processMessage(inputBox.getText());
        inputBox.setText("");
    }
}
