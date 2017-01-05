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

    //默认端口号和游戏房间数量
    private static final int SERVER_PORT = 12345;
    private static final int ROOM_SIZE = 9;

    //在线玩家列表
    static List<String> onlineuser;
    //房间信息
    static String[][] room;
    //所有输出类容器
    static Vector<BufferedWriter> writers;
    //用户名和输出类下表映射
    static HashMap<String, Integer> usermap;
    //数据库连接
    Connection conn;

    public static void main(String[] args) throws IOException {
        //初始化在线玩家列表
        onlineuser = new ArrayList<String>();
        onlineuser.clear();
        //初始化房间信息
        room = new String[ROOM_SIZE][2];
        for (int i = 0; i < ROOM_SIZE; i++) {
            room[i][0] = "";
            room[i][1] = "";
        }
        //初始化所有输出类
        writers = new Vector<BufferedWriter>(ROOM_SIZE * 2);
        for (int i = 0; i < ROOM_SIZE * 2; i++)
            writers.add(null);
        //初始化映射表
        usermap = new HashMap<String, Integer>();
        //开启服务器
        new Server();
    }

    public Server() throws IOException {
        super(SERVER_PORT);
        try {
            //连接数据库
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/othello";
            Scanner input = new Scanner(System.in);
            //输入用户名和密码连接数据库
            System.out.print("Input user name:");
            String user = input.nextLine();
            System.out.print("Input password:");
            String pwd = input.nextLine();
            input.close();
            //保存数据库连接
            conn = DriverManager.getConnection(url, user, pwd);
            System.out.println("Database connected! Server starting...");
            try {
                while (true) {
                    //有客户端进入则新建一个socket进行数据处理，并新建数据处理线程
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
        //输入输出类
        private InputStreamReader inSR;
        private OutputStreamWriter outSW;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        //当前线程用户名
        private String user;
        //用户房间号和房间位置
        private int userroom, useroffset;

        public CreateServerThread(Socket s) throws IOException {
            //保存socket
            client = s;
            //初始化用户相关信息
            user = "";
            userroom = -1;
            useroffset = -1;
            //获取输入输出类
            inSR = new InputStreamReader(client.getInputStream(), "UTF-8");
            bufferedReader = new BufferedReader(inSR);
            outSW = new OutputStreamWriter(client.getOutputStream(), "UTF-8");
            bufferedWriter = new BufferedWriter(outSW);
            //开启线程
            start();
        }

        public void run() {
            try {
                String line = "";
                while (!line.equals("exit")) {
                    line = bufferedReader.readLine();
                    //登录
                    if (line.equals("login")) {
                        do_login();
                    }
                    //获取游戏大厅数据
                    else if (line.equals("getroom")) {
                        do_getroom();
                    }
                    //进入房间
                    else if (line.equals("intoroom")) {
                        do_intoroom();
                    }
                    //退出房间
                    else if (line.equals("exitroom")) {
                        do_exitroom();
                    }
                    //聊天
                    else if (line.equals("chat")) {
                        do_chat();
                    }
                    //落子
                    else if (line.equals("play")) {
                        do_play();
                    }
                    //认输
                    else if (line.equals("giveup")) {
                        do_giveup();
                    }
                    //重新开始
                    else if (line.equals("restart")) {
                        do_restart();
                    }
                    //退出
                    else if (line.equals("exit")) {
                        //将玩家从在线列表中移出
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
                //连接出现问题则退出房间和服务器
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
                //获取用户名和密码的md5摘要
                String username = bufferedReader.readLine();
                String pwdmd5 = bufferedReader.readLine();
                if (onlineuser.contains(username)) {
                    bufferedWriter.write("online\n");
                    bufferedWriter.flush();
                    return;
                }
                //从数据库中取出玩家信息
                Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery("select * from player");
                //判断玩家是否存在
                while (resultSet.next()) {
                    String name = resultSet.getString("username");
                    //变量判断用户名是否存在
                    if (username.equals(name)) {
                        String pwd = resultSet.getString("pwdmd5");
                        //判断密码是否正确
                        if (!pwdmd5.equals(pwd)) {
                            bufferedWriter.write("pwderr\n");
                        }
                        else {
                            //登录成功，设置用户名并加入在线列表
                            bufferedWriter.write("done\n");
                            user = username;
                            onlineuser.add(user);
                            //记录登陆信息
                            insertlog(user, "login");
                            System.out.println(user + " login.");
                        }
                        bufferedWriter.flush();
                        resultSet.close();
                        statement.close();
                        return;
                    }
                }
                //注册一个新用户
                PreparedStatement preparedStatement = conn.prepareStatement("insert into player(username,pwdmd5) values(?,?)");
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, pwdmd5);
                preparedStatement.executeUpdate();
                //设置用户用并加入在线列表
                user = username;
                onlineuser.add(user);
                //记录登陆信息
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
                //发送游戏大厅数据
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
                //房间第一个位置有空
                if (room[num][0].isEmpty()) {
                    //玩家进入房间，更新相关信息
                    room[num][0] = user;
                    userroom = num;
                    useroffset = 0;
                    //记录进入房间信息
                    insertgameinfo(num + 1, user, "enter room");
                    bufferedWriter.write("done\n");
                    bufferedWriter.flush();
                    writers.set(num * 2, bufferedWriter);
                    usermap.put(user, num * 2);
                    //如果房间原本有玩家，发送进入信息
                    if (!room[num][1].isEmpty()) {
                        writers.get(num * 2 + 1).write("come\n");
                        writers.get(num * 2 + 1).write(user + "\n");
                        writers.get(num * 2 + 1).flush();
                    }
                }
                //房间第二个位置有空
                else if (room[num][1].isEmpty()) {
                    //玩家进入房间，更新相关信息
                    room[num][1] = user;
                    userroom = num;
                    useroffset = 0;
                    //记录进入房间信息
                    insertgameinfo(num + 1, user, "enter room");
                    bufferedWriter.write("done\n");
                    bufferedWriter.flush();
                    writers.set(num * 2 + 1, bufferedWriter);
                    usermap.put(user, num * 2 + 1);
                    //如果房间原本有玩家，发送进入信息
                    if (!room[num][0].isEmpty()) {
                        writers.get(num * 2).write("come\n");
                        writers.get(num * 2).write(user + "\n");
                        writers.get(num * 2).flush();
                    }
                }
                else {
                    //房间已满不能进入
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
                //用户在第一个位置退出
                int num = usermap.get(user) / 2;
                if (room[num][0].equals(user)) {
                    //玩家离开房间，更新相关信息
                    room[num][0] = "";
                    userroom = -1;
                    useroffset = -1;
                    writers.set(num * 2, null);
                    usermap.remove(user);
                    //记录退出房间信息
                    insertgameinfo(num + 1, user, "exit room");
                    //如果房间仍有玩家，发送退出信息
                    if (!room[num][1].isEmpty()) {
                        writers.get(num * 2 + 1).write("out\n");
                        writers.get(num * 2 + 1).flush();
                    }
                }
                //用户在第二个位置退出
                else if (room[num][1].equals(user)) {
                    //玩家离开房间，更新相关信息
                    room[num][1] = "";
                    userroom = -1;
                    useroffset = -1;
                    writers.set(num * 2 + 1, null);
                    usermap.remove(user);
                    //记录退出房间信息
                    insertgameinfo(num + 1, user, "exit room");
                    //如果房间仍有玩家，发送退出信息
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
                //接受聊天信息
                String msg = bufferedReader.readLine();
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                //记录聊天信息
                insertchatrecord(num + 1, user, msg);
                //如果房间存在另一个玩家，发送聊天信息
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
                //获取落子信息
                String x = bufferedReader.readLine();
                String y = bufferedReader.readLine();
                int x1 = Integer.valueOf(x).intValue() + 1;
                int y1 = Integer.valueOf(y).intValue() + 1;
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                String color = (oppo == 1) ? "black" : "white";
                //记录落子信息
                insertgameinfo(num + 1, user, color + " place in (" + x1 + ", " + y1 + ")");
                //如果房间存在另一个玩家，发送落子信息
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
                //认输
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                //记录认输信息
                insertgameinfo(num + 1, user, "give up");
                //如果房间存在另一个玩家，发送认输信息
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
                //重新开始
                int num = usermap.get(user) / 2;
                int oppo = (usermap.get(user) + 1) % 2;
                //记录重新开始信息
                insertgameinfo(num + 1, user, "restart");
                //如果房间存在另一个玩家，发送重新开始信息
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
            //将登陆、退出记录到数据库中
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
            //将游戏信息记录到数据库中
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
            //将聊天信息记录到数据库中
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
