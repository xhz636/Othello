package othelloserver;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class Server extends ServerSocket {
    
    private static final int SERVER_PORT = 12345;
    private static final int ROOM_SIZE = 9;

    static List<String> onlineuser;
    static String[][] room;
    static Vector<BufferedWriter> writers;
    static HashMap<String, Integer> usermap;
    Connection conn;
    
    public static void main(String[] args) throws IOException {
        onlineuser = new ArrayList<String>();
        onlineuser.clear();
        room = new String[ROOM_SIZE][2];
        for (int i = 0; i < ROOM_SIZE; i++) {
            room[i][0] = "";
            room[i][1] = "";
        }
        writers = new Vector<BufferedWriter>(ROOM_SIZE * 2);
        for (int i = 0; i < ROOM_SIZE * 2; i++)
            writers.add(null);
        usermap = new HashMap<String, Integer>();
        new Server();
    }
    
    public Server() throws IOException {
        super(SERVER_PORT);
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/othello";
            Scanner input = new Scanner(System.in);
            System.out.print("Input user name:");
            String user = input.nextLine();
            System.out.print("Input password:");
            String pwd = input.nextLine();
            conn = DriverManager.getConnection(url, user, pwd);
            System.out.println("Database connected! Server starting...");
            try {
                while (true) {
                    Socket socket = accept();
                    new CreateServerThread(socket);
                }
            }
            catch (IOException e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
            finally {
                close();
            }
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
        }
    }
    
    class CreateServerThread extends Thread {
        
        private Socket client;
        private InputStreamReader inSR;
        private OutputStreamWriter outSW;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String user;
        
        public CreateServerThread(Socket s) throws IOException {
            client = s;
            inSR = new InputStreamReader(client.getInputStream(), "UTF-8");
            bufferedReader = new BufferedReader(inSR);
            outSW = new OutputStreamWriter(client.getOutputStream(), "UTF-8");
            bufferedWriter = new BufferedWriter(outSW);
            start();
        }
        
        public void run() {
            try {
                String line = "";
                while (!line.equals("exit")) {
                    line = bufferedReader.readLine();
                    if (line.equals("login")) {
                        do_login();
                    }
                    else if (line.equals("getroom")) {
                        do_getroom();
                    }
                    else if (line.equals("intoroom")) {
                        do_intoroom();
                    }
                    else if (line.equals("exitroom")) {
                        do_exitroom();
                    }
                    else if (line.equals("chat")) {
                        do_chat();
                    }
                    else if (line.equals("play")) {
                        do_play();
                    }
                    else if (line.equals("giveup")) {
                        do_giveup();
                    }
                    else if (line.equals("restart")) {
                        do_restart();
                    }
                    else if (line.equals("exit")) {
                        onlineuser.remove(user);
                    }
                }
                System.out.println(user + " exit.");
                bufferedWriter.close();
                bufferedReader.close();
                client.close();
            } catch (IOException e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }

        public void do_login() {
            try {
                String username = bufferedReader.readLine();
                String pwdmd5 = bufferedReader.readLine();
                if (onlineuser.contains(username)) {
                    bufferedWriter.write("online\n");
                    bufferedWriter.flush();
                    return;
                }
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery("select * from player");
                while (resultSet.next()) {
                    String name = resultSet.getString("username");
                    if (username.equals(name)) {
                        String pwd = resultSet.getString("pwdmd5");
                        if (!pwdmd5.equals(pwd)) {
                            bufferedWriter.write("pwderr\n");
                        }
                        else {
                            bufferedWriter.write("done\n");
                            user = username;
                            onlineuser.add(user);
                            System.out.println(user + " login.");
                        }
                        bufferedWriter.flush();
                        resultSet.close();
                        statement.close();
                        return;
                    }
                }
                PreparedStatement preparedStatement = conn.prepareStatement("insert into player(username,pwdmd5) values(?,?)");
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, pwdmd5);
                preparedStatement.executeUpdate();
                bufferedWriter.write("new\n");
                bufferedWriter.flush();
                resultSet.close();
                statement.close();
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }

        public void do_getroom() {
            try {
                for (int i = 0; i < ROOM_SIZE; i++) {
                    bufferedWriter.write(room[i][0] + "\n");
                    bufferedWriter.write(room[i][1] + "\n");
                }
                bufferedWriter.flush();
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }

        public void do_intoroom() {
            try {
                String roomnum = bufferedReader.readLine();
                int num = Integer.valueOf(roomnum).intValue();
                if (room[num][0].isEmpty()) {
                    room[num][0] = user;
                    bufferedWriter.write("done\n");
                    bufferedWriter.flush();
                    writers.set(num * 2, bufferedWriter);
                    usermap.put(user, num * 2);
                    if (!room[num][1].isEmpty()) {
                        writers.get(num * 2 + 1).write("come\n");
                        writers.get(num * 2 + 1).write(user + "\n");
                        writers.get(num * 2 + 1).flush();
                    }
                }
                else if (room[num][1].isEmpty()) {
                    room[num][1] = user;
                    bufferedWriter.write("done\n");
                    bufferedWriter.flush();
                    writers.set(num * 2 + 1, bufferedWriter);
                    usermap.put(user, num * 2 + 1);
                    if (!room[num][0].isEmpty()) {
                        writers.get(num * 2).write("come\n");
                        writers.get(num * 2).write(user + "\n");
                        writers.get(num * 2).flush();
                    }
                }
                else {
                    bufferedWriter.write("failed\n");
                    bufferedWriter.flush();
                }
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }

        public void do_exitroom() {
            try {
                int num = usermap.get(user) / 2;
                if (room[num][0].equals(user)) {
                    room[num][0] = "";
                    writers.set(num * 2, null);
                    usermap.remove(user);
                    if (!room[num][1].isEmpty()) {
                        writers.get(num * 2 + 1).write("out\n");
                        writers.get(num * 2 + 1).flush();
                    }
                }
                else if (room[num][1].equals(user)) {
                    room[num][1] = "";
                    writers.set(num * 2 + 1, null);
                    usermap.remove(user);
                    if (!room[num][0].isEmpty()) {
                        writers.get(num * 2).write("out\n");
                        writers.get(num * 2).flush();
                    }
                }
                bufferedWriter.write("exitroom\n");
                bufferedWriter.flush();
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }

        public void do_chat() {
            try {
                String msg = bufferedReader.readLine();
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                if (!room[num][oppo].isEmpty()) {
                    writers.get(num * 2 + oppo).write("chat\n");
                    writers.get(num * 2 + oppo).write(msg + "\n");
                    writers.get(num * 2 + oppo).flush();
                }
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }

        public void do_play() {
            try {
                String x = bufferedReader.readLine();
                String y = bufferedReader.readLine();
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                if (!room[num][oppo].isEmpty()) {
                    writers.get(num * 2 + oppo).write("play\n");
                    writers.get(num * 2 + oppo).write(x + "\n");
                    writers.get(num * 2 + oppo).write(y + "\n");
                    writers.get(num * 2 + oppo).flush();
                }
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }

        public void do_giveup() {
            try {
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                if (!room[num][oppo].isEmpty()) {
                    writers.get(num * 2 + oppo).write("giveup\n");
                    writers.get(num * 2 + oppo).flush();
                }
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }
        
        public void do_restart() {
            try {
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                if (!room[num][oppo].isEmpty()) {
                    writers.get(num * 2 + oppo).write("restart\n");
                    writers.get(num * 2 + oppo).flush();
                }
            }
            catch (Exception e) {
                // TODO: handle exception
                System.err.println(e.toString());
            }
        }
        
    }

}
