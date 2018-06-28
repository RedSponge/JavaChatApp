package com.redsponge.networking.chat;

import javafx.util.Builder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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

    public ChatServer(String ip, int port) {
        socketThreads = new ArrayList<>();
        sockets = new ArrayList<>();
        connectedUsers = new ArrayList<>();
        try {
            server = new ServerSocket(port, Shared.server_backlog, InetAddress.getByName(ip));
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        boolean listening = true;
        try {
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
        new ChatServer(Shared.ip, Shared.port);
    }
}
