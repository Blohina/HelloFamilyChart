package com.company;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


    class UserHandler implements Runnable {
        private SocketChannel clientChannel;
        private ByteBuffer outBuffer;
        private String stringToClient;
        private PrintStream console = System.out;
        private String nameUser = "Anonim";
        private ByteBuffer outBufferThread = ByteBuffer.allocateDirect(500);
        private HelloFamilyServer server;
        private Lock lock = new ReentrantLock();


        /**
         * @param clientChannel клиентский сокет
         * @param nameUser  имя подключившегося пользователя
         */
        public UserHandler(HelloFamilyServer server, SocketChannel clientChannel, String nameUser)  {
            this.server = server;
            this.clientChannel = clientChannel;
            this.nameUser = nameUser;
            outBuffer = ByteBuffer.allocateDirect(500);
        }

        void sendMessage(String  messageToUser) throws IOException {
            stringToClient = messageToUser + "\n";
            outBufferThread.put(stringToClient.getBytes());
            outBufferThread.flip();
            while (outBufferThread.hasRemaining()) {
                clientChannel.write(outBufferThread);
            }
            outBufferThread.clear();
        }

        @Override
        public void run() {
            String s;
            // 1. Creating Scanner for reading from socketChannel
            try (Scanner fromClient = new Scanner(clientChannel)) {
                // 2. Send a greeting message
                String greeting = "Hello " + nameUser;
                lock.lock();
                try {
                    if (HelloFamilyServer.haveUsers()) {
                        server.sendMessageToAll(greeting);
                    }
                } finally {
                    lock.unlock();
                }
                while (clientChannel.isConnected()) {
                    if(!clientChannel.isConnected()) { break;}
                    if (fromClient.hasNext()) {
                        s = fromClient.nextLine();

                        // Client said stop word?
                        if (s.trim().equals("stop")|| s == null) {
                            stringToClient = nameUser + " said bay! \n";
                            console.println(stringToClient);
                            HelloFamilyServer.reduceUserCount();
                            HelloFamilyServer.deleteFromChatList(this);
                            console.flush();
                            server.sendMessageToAll(stringToClient);
                            break;
                        }
                        stringToClient = nameUser + ": " + s + "\n";
                        console.print(stringToClient);
                        console.flush();
                        server.sendMessageToAll(stringToClient);
                    } else {  //  клиент похоже ушел...
                        System.out.println( nameUser + " has gone");
                        HelloFamilyServer.reduceUserCount();
                        lock.lock();
                        try {
                            HelloFamilyServer.deleteFromChatList(this);
                            if (HelloFamilyServer.haveUsers()) {
                                server.sendMessageToAll(nameUser + " has gone! ");
                            }

                            break;
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public class HelloFamilyServer {
        private static HashMap<String, String> usersMap = new HashMap<>(); // nick,name
        private static ArrayList<UserHandler> family = new ArrayList<>(); // list of all participants
        private static int userCount;

        static synchronized void reduceUserCount() {
            userCount--;
        }
        private synchronized static String  getNameUser(String message) {
            int index = message.lastIndexOf("#");
            String nameUser = message.substring(0,index);
            String nick = message.substring(index+1);
            usersMap.put(nick,nameUser);
            return nick;
        }
        /** Are there users in the chart?  */
        synchronized static boolean haveUsers()  {
            return userCount > 0 ? true : false;
        }

        synchronized static  boolean noUser()  {
            return (userCount == 0);
        }

        synchronized static void deleteFromChatList (UserHandler user)  {
            family.remove(user);
        }

        /**  Send message to all chat users */
        void sendMessageToAll(String message) throws IOException {
            for(UserHandler member : family)  {
                member.sendMessage(message);
            }

        }

        public HelloFamilyServer (int port) throws IOException { //port = 8189
            try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
                serverSocketChannel.socket().bind(new InetSocketAddress(port));
                System.out.println("Server is run ... ");
                String nameUser = "Anonim";
                while(true)  {
                    //  Listen to port and wait for user
                    SocketChannel clientSocket = serverSocketChannel.accept();

                    userCount++; // the number of users was increased
                    System.out.println("В чате нас  " + userCount + " человек!");

                    //  Creating Scanner for reading from socketChannel
                    Scanner fromClient = new Scanner(clientSocket);
                    //  We will read  from clients its personal information
                    if (fromClient.hasNext()) {
                        String messageFrom = fromClient.nextLine();
                        nameUser = getNameUser(messageFrom);
                    }
                    System.out.println(nameUser + " has come!");
                    System.out.flush();
                    UserHandler user = new UserHandler(this, clientSocket, nameUser);
                    family.add(user);
                    Thread userThread = new Thread(user);
                    userThread.start();

                } // wait for users

            } catch (IOException e )  {
                System.out.println("Something wrong with ServerSocketChannel");
                e.printStackTrace();
            }
        }



    }


