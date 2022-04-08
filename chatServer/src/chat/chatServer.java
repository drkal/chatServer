package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class chatServer extends Thread implements Runnable {

    public static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    public static SimpleDateFormat dat = new SimpleDateFormat("d/m/y HH:mm:ss");
    private static final int PORT = 8080;
    public static int i = 1;
    public static int j;
    public static int z;
    public Handler child;
    private static HashSet<String> names = new HashSet<String>();
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    public static Hashtable<Integer, Handler> handlers = new Hashtable<Integer, Handler>();
    public ServerSocket listener;

    public void destroyAll() throws IOException {
        Enumeration<Handler> e = this.handlers.elements();
        while (e.hasMoreElements()) {
            Handler hand = e.nextElement();
            hand.in.close();
            hand.out.flush();
            hand.out.close();
            hand.socket.close();
            handlers.remove(hand);
        }
        this.handlers = new Hashtable<Integer, Handler>();
    }

    public static void jason(Handler hand) {
        handlers.remove(hand.currentThread());
        hand.currentThread().interrupt();
        hand = null;

    }

    @Override
    public void run() {

        try {
            System.out.println("The chat server is running.");
            listener = new ServerSocket(PORT);

            try {
                while (true) {
                    child = new Handler(listener.accept());
                    this.handlers.put(child.id, child);
                    child.start();
                    j = handlers.size();
                    ServerGui.servg.conNum.setText(j + "");
                }
            }catch(SocketException s){
                System.out.println("socket closed");   
            
            }finally {
                try{
                this.listener.close();
                this.currentThread().interrupt();
                }catch(NullPointerException n){
                System.out.println("final");
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(chatServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static class Handler extends Thread implements Runnable {

        private String name;
        public Socket socket;
        public BufferedReader in;
        public PrintWriter out;
        public static int ID = 0;
        public int id = 0;
        private static final String URL = "jdbc:mysql://localhost/chat_data";
        private static final String username = "root";
        private static final String password = "";
        private static Connection c;

        public Handler(Socket socket) {
            this.socket = socket;
            this.id = Handler.ID;
            Handler.ID++;
        }

        private static void connect() {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                c = DriverManager.getConnection(URL, username, password);
            } catch (ClassNotFoundException | SQLException ex) {
                Logger.getLogger(chatServer.class.getName()).log(Level.SEVERE, null, ex);
            }


        }

        private static void disconnect() {
            try {
                c.close();
            } catch (SQLException ex) {
                Logger.getLogger(chatServer.class.getName()).log(Level.SEVERE, null, ex);
            }


        }

        public static void insert(String user, String date, String ip, String message) {
            try {
                connect();

                Statement s = c.createStatement();
                String sql = "INSERT INTO msg (user, date, ip, message) VALUES ('" + user + "', '" + date + "', '" + ip + "', '" + message + "')";
                System.out.println(sql);
                s.executeUpdate(sql);
                disconnect();
            } catch (SQLException ex) {
                Logger.getLogger(chatServer.class.getName()).log(Level.SEVERE, null, ex);
            }


        }

        public void disConnection() {

            for (PrintWriter writer : writers) {
                writer.println("DISCONNECT%%$$");
            }

        }

    
        

        @Override
        public void run() {
            try {


                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);


                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                 
                            for (String nameSend : names) {                    
                           out.println("NAMES" + nameSend);          
                        }
                             ServerGui.servg.clientNum.setText(names.size() + "");
                           break;  
                            }  
                           
                        }
                    out.println("REJECTION%%##");
                    j=j-1;
                    ServerGui.servg.conNum.setText(j + "");
                    }
                


                out.println("NAMEACCEPTED");
                writers.add(out);
                 for (PrintWriter writer : writers) {
                  for (String nameSend : names) {
                      writer.println("NAMES" +nameSend);
                  }
                 
                 }
                System.out.println(sdf.format(new Date()) + " " + socket.getRemoteSocketAddress() + " " + name);



                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    } else if (input.startsWith("EXIT%%$")) {                                                                    
                        names.remove(this.name);
                         in.close();
                        out.flush();
                        out.close();
                        socket.close();
                        writers.remove(this.out);
                          for (PrintWriter writer : writers) {
                  for (String nameSend : names) {                     
                      writer.println("MESSAGE " + this.name + " DISCONNECTED");
                      writer.println("NAMES" +nameSend);
                  }
                        }   
                       
                        
                        
                        jason(this);

                        input = " DISCONNECTED";
                        z++;
                        ServerGui.LogArea.append(name + " DISCONNECTED\n");
                        ServerGui.clientNum.setText((handlers.size()-1)+"");
                        ServerGui.DisConNum.setText(z + "");
                      
                    }else{
                    for (PrintWriter writer : writers) {

                        writer.println("MESSAGE " + sdf.format(new Date()) + " " + name + ": " + input);


                        ServerGui.servg.messageNum.setText(i + "");
                        for (String nameSend : names) {
                            writer.println("NAMES" + nameSend);
                            ServerGui.servg.clientNum.setText(names.size() + "");
                            System.out.println(nameSend + "\n");
                        }

                    }

                    i++;

                    insert(name, dat.format(new java.util.Date()), socket.getRemoteSocketAddress() + "", input);

                    ServerGui.LogArea.append(sdf.format(new Date()) + " : " + name + " send : " + input + "\nfrom :" + socket.getRemoteSocketAddress() + "\n");
                }
                    
                  
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {

                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    in.close();
                    out.flush();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}