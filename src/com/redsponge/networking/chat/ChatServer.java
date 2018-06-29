package com.redsponge.networking.chat;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatServer {

    private ServerSocket server;
    private List<Thread> socketThreads;
    private List<Socket> sockets;
    private List<String> connectedUsers;

    public ChatServer(String ip, int port, boolean gui) {
        socketThreads = new ArrayList<>();
        sockets = new ArrayList<>();
        connectedUsers = new ArrayList<>();
        if(gui) createGui();
        try {
            server = new ServerSocket(port, Shared.server_backlog, InetAddress.getByName(ip));
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createGui() {
        JFrame frame = new JFrame("Chat App - Server");

        frame.setMinimumSize(new Dimension(500, 250));
        JTextArea console = new JTextArea();
        console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Console"));
        console.setEditable(false);
        PrintStream out = new PrintStream(new TextAreaOutputStream(console));

        System.setOut(out);
        System.setErr(out);

        frame.getContentPane().add(console);

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
        //TODO HANDLE
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
                s.close();
                thread.join();
            } catch (InterruptedException | IOException e) {
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
        boolean gui = false;
        if(args.length > 0) {
            if(args[0].equals("gui")) {
                gui = true;
            }
        }
        new ChatServer(Shared.ip, Shared.port, gui);
    }
}
