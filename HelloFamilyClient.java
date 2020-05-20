package com.company;



import org.w3c.dom.ls.LSOutput;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

    /** обрабатывает сообщения, приходящие от сервера */
    class ServerMessageHandler implements Runnable {
        private  String nickName;
        private SocketChannel socketChannel;
        private Scanner fromServer;
        // console colors
        static final String RESET = "\u001b[0m";
        static final String BLUE = "\u001b[34m";
        static final String GREEN = "\u001b[32m";
        static final String RED = "\u001b[31m";

        public ServerMessageHandler(SocketChannel socketChannel, String nickName) {
            this.socketChannel = socketChannel;
            this.nickName = nickName;
            fromServer = new Scanner(socketChannel);
        }
         private boolean isMyMessage(String message)  {
            int index = message.indexOf(":");
            if(index > 0) {
                String userNick = message.substring(0, index);
                return userNick.equals(nickName);
            } else return false;
        }
        /** just receive messages from Server and print them in console */
        @Override
        public void run() {
            while (true) {
                if (fromServer.hasNext()) {
                    String message = fromServer.nextLine();
                    if (message.trim().equals("")) continue; // empty string is not printed
                    if(isMyMessage(message)) continue; //don't need reprint again
                    System.out.println(RED + message + RESET);
                    Date date = new Date();
                    System.out.println(BLUE + date + RESET);
                    System.out.flush();
                } else {
                    System.out.println(RED + "Server doesn't response. Bay!" + RESET);
                    break;
                }
            }
        }
    }

    public class HelloFamilyClient {
        private String Name;
        private String Nick;
        static final String RESET = "\u001b[0m";
        static final String BLUE = "\u001b[34m";
        static final String GREEN = "\u001b[32m";
        static final String RED = "\u001b[31m";
        private ByteBuffer outBuffer = ByteBuffer.allocateDirect(500);
        private Scanner forServer;  // for listen to client's messages

        public HelloFamilyClient(String name, String nick)  {
            Name = name;
            Nick = nick;
            forServer  = new Scanner(System.in);
        }

        private void sendMessage(SocketChannel socketChannel, String message) throws IOException {
            outBuffer.put(message.getBytes());
            outBuffer.flip();
            while (outBuffer.hasRemaining()) {
                socketChannel.write(outBuffer);
            }
            outBuffer.clear();
        }

        private boolean isStopWord(String message) {
            return (message.equals("stop"));
        }

        private boolean isEmptyString(String  message)  {
            return message.trim().equals("");
        }

        private void communicate(String infoToServer)  {
            /** клиент общается */
            PrintStream cons = System.out;
            cons.println(Nick + " write here. Enter stop to exit ... ");

            // 1. Creating SocketChanel
            try (SocketChannel socketChannel = SocketChannel.open()) {
                socketChannel.connect(new InetSocketAddress(InetAddress.getLocalHost(), 8189));

                String stringToServer = infoToServer+"\n";
                sendMessage(socketChannel, stringToServer);
                //Listen to server's messages
                ServerMessageHandler serMessH = new ServerMessageHandler(socketChannel, Nick);
                Thread clientListen = new Thread(serMessH);
                clientListen.start();
                // transfer messages
                while(true)  {
                    // Read info from console
                    if(forServer.hasNext()) {
                        stringToServer = forServer.nextLine();
                        if (isStopWord(stringToServer)) {  //stop word?
                            stringToServer+="\n";
                            sendMessage(socketChannel, stringToServer);
                            break;
                        }
                        if (isEmptyString(stringToServer)) { //empty string?
                            continue;
                        }
                        stringToServer+="\n";
                        sendMessage(socketChannel, stringToServer);
                    } //if
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void main(String[] args) {
            String myName,
                    myNick;
            if (args.length < 2)  {
                myName = "Anonim";
                Random random = new Random(100);
                myNick = myName + random.nextInt();
            } else  {
                myName = args[0];
                myNick = args[1];
            }
            HelloFamilyClient member = new HelloFamilyClient(myName,myNick);
            String introdusing = myName +"#"+myNick;
            member.communicate(introdusing);
        }
    }

