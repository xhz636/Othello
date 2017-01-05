package othelloserver;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class Server extends ServerSocket {

    //Ĭ�϶˿ںź���Ϸ��������
    private static final int SERVER_PORT = 12345;
    private static final int ROOM_SIZE = 9;

    //��������б�
    static List<String> onlineuser;
    //������Ϣ
    static String[][] room;
    //�������������
    static Vector<BufferedWriter> writers;
    //�û�����������±�ӳ��
    static HashMap<String, Integer> usermap;
    //���ݿ�����
    Connection conn;

    public static void main(String[] args) throws IOException {
        //��ʼ����������б�
        onlineuser = new ArrayList<String>();
        onlineuser.clear();
        //��ʼ��������Ϣ
        room = new String[ROOM_SIZE][2];
        for (int i = 0; i < ROOM_SIZE; i++) {
            room[i][0] = "";
            room[i][1] = "";
        }
        //��ʼ�����������
        writers = new Vector<BufferedWriter>(ROOM_SIZE * 2);
        for (int i = 0; i < ROOM_SIZE * 2; i++)
            writers.add(null);
        //��ʼ��ӳ���
        usermap = new HashMap<String, Integer>();
        //����������
        new Server();
    }

    public Server() throws IOException {
        super(SERVER_PORT);
        try {
            //�������ݿ�
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/othello";
            Scanner input = new Scanner(System.in);
            //�����û����������������ݿ�
            System.out.print("Input user name:");
            String user = input.nextLine();
            System.out.print("Input password:");
            String pwd = input.nextLine();
            input.close();
            //�������ݿ�����
            conn = DriverManager.getConnection(url, user, pwd);
            System.out.println("Database connected! Server starting...");
            try {
                while (true) {
                    //�пͻ��˽������½�һ��socket�������ݴ������½����ݴ����߳�
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

        //socket
        private Socket client;
        //���������
        private InputStreamReader inSR;
        private OutputStreamWriter outSW;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        //��ǰ�߳��û���
        private String user;
        //�û�����źͷ���λ��
        private int userroom, useroffset;

        public CreateServerThread(Socket s) throws IOException {
            //����socket
            client = s;
            //��ʼ���û������Ϣ
            user = "";
            userroom = -1;
            useroffset = -1;
            //��ȡ���������
            inSR = new InputStreamReader(client.getInputStream(), "UTF-8");
            bufferedReader = new BufferedReader(inSR);
            outSW = new OutputStreamWriter(client.getOutputStream(), "UTF-8");
            bufferedWriter = new BufferedWriter(outSW);
            //�����߳�
            start();
        }

        public void run() {
            try {
                String line = "";
                while (!line.equals("exit")) {
                    line = bufferedReader.readLine();
                    //��¼
                    if (line.equals("login")) {
                        do_login();
                    }
                    //��ȡ��Ϸ��������
                    else if (line.equals("getroom")) {
                        do_getroom();
                    }
                    //���뷿��
                    else if (line.equals("intoroom")) {
                        do_intoroom();
                    }
                    //�˳�����
                    else if (line.equals("exitroom")) {
                        do_exitroom();
                    }
                    //����
                    else if (line.equals("chat")) {
                        do_chat();
                    }
                    //����
                    else if (line.equals("play")) {
                        do_play();
                    }
                    //����
                    else if (line.equals("giveup")) {
                        do_giveup();
                    }
                    //���¿�ʼ
                    else if (line.equals("restart")) {
                        do_restart();
                    }
                    //�˳�
                    else if (line.equals("exit")) {
                        //����Ҵ������б����Ƴ�
                        onlineuser.remove(user);
                    }
                }
                insertlog(user, "exit");
                System.out.println(user + " exit.");
                bufferedWriter.close();
                bufferedReader.close();
                client.close();
            } catch (IOException e) {
                // TODO: handle exception
                System.err.println(e.toString());
                //���ӳ����������˳�����ͷ�����
                if (!user.isEmpty()) {
                    onlineuser.remove(user);
                    insertlog(user, "lost connection");
                    System.out.println(user + " lost connection.");
                }
                if (userroom >= 0 && useroffset >= 0)
                    room[userroom][useroffset] = "";
            }
        }

        public void do_login() {
            try {
                //��ȡ�û����������md5ժҪ
                String username = bufferedReader.readLine();
                String pwdmd5 = bufferedReader.readLine();
                if (onlineuser.contains(username)) {
                    bufferedWriter.write("online\n");
                    bufferedWriter.flush();
                    return;
                }
                //�����ݿ���ȡ�������Ϣ
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery("select * from player");
                //�ж�����Ƿ����
                while (resultSet.next()) {
                    String name = resultSet.getString("username");
                    //�����ж��û����Ƿ����
                    if (username.equals(name)) {
                        String pwd = resultSet.getString("pwdmd5");
                        //�ж������Ƿ���ȷ
                        if (!pwdmd5.equals(pwd)) {
                            bufferedWriter.write("pwderr\n");
                        }
                        else {
                            //��¼�ɹ��������û��������������б�
                            bufferedWriter.write("done\n");
                            user = username;
                            onlineuser.add(user);
                            //��¼��½��Ϣ
                            insertlog(user, "login");
                            System.out.println(user + " login.");
                        }
                        bufferedWriter.flush();
                        resultSet.close();
                        statement.close();
                        return;
                    }
                }
                //ע��һ�����û�
                PreparedStatement preparedStatement = conn.prepareStatement("insert into player(username,pwdmd5) values(?,?)");
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, pwdmd5);
                preparedStatement.executeUpdate();
                //�����û��ò����������б�
                user = username;
                onlineuser.add(user);
                //��¼��½��Ϣ
                insertlog(user, "login");
                System.out.println(user + " login.");
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
                //������Ϸ��������
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
                //�����һ��λ���п�
                if (room[num][0].isEmpty()) {
                    //��ҽ��뷿�䣬���������Ϣ
                    room[num][0] = user;
                    userroom = num;
                    useroffset = 0;
                    //��¼���뷿����Ϣ
                    insertgameinfo(num + 1, user, "enter room");
                    bufferedWriter.write("done\n");
                    bufferedWriter.flush();
                    writers.set(num * 2, bufferedWriter);
                    usermap.put(user, num * 2);
                    //�������ԭ������ң����ͽ�����Ϣ
                    if (!room[num][1].isEmpty()) {
                        writers.get(num * 2 + 1).write("come\n");
                        writers.get(num * 2 + 1).write(user + "\n");
                        writers.get(num * 2 + 1).flush();
                    }
                }
                //����ڶ���λ���п�
                else if (room[num][1].isEmpty()) {
                    //��ҽ��뷿�䣬���������Ϣ
                    room[num][1] = user;
                    userroom = num;
                    useroffset = 0;
                    //��¼���뷿����Ϣ
                    insertgameinfo(num + 1, user, "enter room");
                    bufferedWriter.write("done\n");
                    bufferedWriter.flush();
                    writers.set(num * 2 + 1, bufferedWriter);
                    usermap.put(user, num * 2 + 1);
                    //�������ԭ������ң����ͽ�����Ϣ
                    if (!room[num][0].isEmpty()) {
                        writers.get(num * 2).write("come\n");
                        writers.get(num * 2).write(user + "\n");
                        writers.get(num * 2).flush();
                    }
                }
                else {
                    //�����������ܽ���
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
                //�û��ڵ�һ��λ���˳�
                int num = usermap.get(user) / 2;
                if (room[num][0].equals(user)) {
                    //����뿪���䣬���������Ϣ
                    room[num][0] = "";
                    userroom = -1;
                    useroffset = -1;
                    writers.set(num * 2, null);
                    usermap.remove(user);
                    //��¼�˳�������Ϣ
                    insertgameinfo(num + 1, user, "exit room");
                    //�������������ң������˳���Ϣ
                    if (!room[num][1].isEmpty()) {
                        writers.get(num * 2 + 1).write("out\n");
                        writers.get(num * 2 + 1).flush();
                    }
                }
                //�û��ڵڶ���λ���˳�
                else if (room[num][1].equals(user)) {
                    //����뿪���䣬���������Ϣ
                    room[num][1] = "";
                    userroom = -1;
                    useroffset = -1;
                    writers.set(num * 2 + 1, null);
                    usermap.remove(user);
                    //��¼�˳�������Ϣ
                    insertgameinfo(num + 1, user, "exit room");
                    //�������������ң������˳���Ϣ
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
                //����������Ϣ
                String msg = bufferedReader.readLine();
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                //��¼������Ϣ
                insertchatrecord(num + 1, user, msg);
                //������������һ����ң�����������Ϣ
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
                //��ȡ������Ϣ
                String x = bufferedReader.readLine();
                String y = bufferedReader.readLine();
                int x1 = Integer.valueOf(x).intValue() + 1;
                int y1 = Integer.valueOf(y).intValue() + 1;
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                String color = (oppo == 1) ? "black" : "white";
                //��¼������Ϣ
                insertgameinfo(num + 1, user, color + " place in (" + x1 + ", " + y1 + ")");
                //������������һ����ң�����������Ϣ
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
                //����
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                //��¼������Ϣ
                insertgameinfo(num + 1, user, "give up");
                //������������һ����ң�����������Ϣ
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
                //���¿�ʼ
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                //��¼���¿�ʼ��Ϣ
                insertgameinfo(num + 1, user, "restart");
                //������������һ����ң��������¿�ʼ��Ϣ
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

    public void insertlog(String username, String operation) {
        try {
            //����½���˳���¼�����ݿ���
            PreparedStatement preparedStatement = conn.prepareStatement("insert into log(username,operation,optime) values(?,?,?)");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, operation);
            preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            preparedStatement.executeUpdate();
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
        }
    }

    public void insertgameinfo(int roomnum, String username, String info) {
        try {
            //����Ϸ��Ϣ��¼�����ݿ���
            PreparedStatement preparedStatement = conn.prepareStatement("insert into gameinfo(roomnum,username,info,optime) values(?,?,?,?)");
            preparedStatement.setInt(1, roomnum);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, info);
            preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            preparedStatement.executeUpdate();
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
        }
    }

    public void insertchatrecord(int roomnum, String username, String chat) {
        try {
            //��������Ϣ��¼�����ݿ���
            PreparedStatement preparedStatement = conn.prepareStatement("insert into chatrecord(roomnum,username,chat,optime) values(?,?,?,?)");
            preparedStatement.setInt(1, roomnum);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, chat);
            preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            preparedStatement.executeUpdate();
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
        }
    }

}
