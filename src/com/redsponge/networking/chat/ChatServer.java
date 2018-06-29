package com.redsponge.networking.chat;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatServer {

    private ServerSocket server;
    private List<Thread> socketThreads;
    private List<Socket> sockets;
    private List<String> connectedUsers;

    public ChatServer(int port, boolean gui) throws UnknownHostException {
        this(InetAddress.getLocalHost().getHostAddress(), port, gui);
    }

    public ChatServer(String ip, int port, boolean gui) {
        socketThreads = new ArrayList<>();
        sockets = new ArrayList<>();
        connectedUsers = new ArrayList<>();
        if(gui) createGui(ip, port);
        try {
            server = new ServerSocket(port, Shared.server_backlog, InetAddress.getByName(ip));
            System.out.println("Running on " + ip + ":" + port);
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createGui(String ip, int port) {
        JFrame frame = new JFrame("Chat App - Server");
        frame.setMinimumSize(new Dimension(500, 250));

        JTextArea console = new JTextArea();
        console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Console"));
        console.setEditable(false);
        console.setLineWrap(true);
        PrintStream out = new PrintStream(new TextAreaOutputStream(console));

        System.setOut(out);
        System.setErr(out);

        JScrollPane consoleScroller = new JScrollPane(console);

        JPanel info = new JPanel(new GridLayout());
        String[][] labels = {
                {"IP:", ip},
                {"PORT:", Integer.toString(port)}
        };
        for(String[] entry : labels) {
            JTextArea label = new JTextArea(entry[1]);
            label.setEditable(false);
            label.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), entry[0]));
            info.add(label);
        }

        Container c = frame.getContentPane();
        c.add(info, BorderLayout.NORTH);
        c.add(consoleScroller, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void listen() {
        boolean listening = true;
        try {
            System.out.println("Listening for clients!");
            while(listening) {
                Socket s = server.accept();
                int id = socketThreads.size();
                sockets.add(s);
                Thread t = new Thread(() -> handleSocket(id, s));
                socketThreads.add(t);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleSocket(int id, Socket s) {
        System.out.println("ID: " + id);
        Thread thread = socketThreads.get(id);
        boolean running = true;
        String username = "";
        try {
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            username = receive("USERNAME", out, in);
            System.out.println(username);
            connectedUsers.add(username);
            broadcast(Shared.user_update);
            broadcastExclude(Shared.JOIN_PREFIX + username, s);

            while(running) {
                String data = in.readLine();
                System.out.println("Got data " + data + " from socket " + s);
                if(data.startsWith(Shared.message_prefix))
                broadcast(Shared.message_prefix + "[" + username + "] " + data.substring(Shared.message_prefix.length()));
                if(data.equals("QUIT")) {
                    out.println("QUIT");
                    running = false;
                }
                if(data.equals(Shared.online_user_request)) {
                    out.println(String.join(Shared.ARRAY_JOIN_DELIMETER, connectedUsers));
                }
            }

            broadcastExclude(Shared.LEAVE_PREFIX + username, s);
            System.out.println("Stopping handling for socket with id " + id);
            socketThreads.remove(thread);

        } catch (IOException e) {
            if (e instanceof SocketException) System.out.println("Connection end!");
            else e.printStackTrace();
        } finally {
            System.out.println("Finished handling socket with id " + id + "!");
            connectedUsers.remove(username);
            broadcast(Shared.user_update);
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String receive(String data, PrintWriter out, BufferedReader in) {
        try {
            out.println(data);
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void broadcast(String data) {
        try {
            for (Socket s : sockets) {
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastExclude(String data, Socket... toExclude) {
        try {
            List<Socket> exclude = Arrays.asList(toExclude);
            for (Socket s : sockets) {
                if (!exclude.contains(s)) {
                    PrintWriter out = new PrintWriter(s.getOutputStream());
                    out.println(data);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        boolean gui = true;
        if(args.length > 0) {
            if(args[0].equals("nogui")) {
                gui = false;
            }
        }
        try {
            new ChatServer(Shared.port, gui);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
