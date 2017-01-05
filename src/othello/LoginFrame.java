package othello;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.MessageDigest;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginFrame extends JFrame{

    //游戏大厅
    GameLobby gameLobby;
    //登录、退出按钮
    JButton btn_login, btn_exit;
    //服务器地址、端口、用户名、密码的输入框
    JTextField text_host, text_port, text_username;
    JPasswordField text_password;
    //各种提示标签和面板
    JLabel label_host, label_port, label_username, label_password;
    JPanel panel_host, panel_port, panel_username, panel_password, panel_button;
    //用户名、密码
    String username, password;
    //socket和对应的输入输出相关类
    Socket socket = null;
    DataInputStream dataInputStream;
    InputStreamReader inputStreamReader;
    BufferedReader bufferedReader;
    DataOutputStream dataOutputStream;
    OutputStreamWriter outputStreamWriter;
    BufferedWriter bufferedWriter;

    public LoginFrame() {
        //初始化窗口布局
        initLayout();
        //设置所需要的监视器
        initListener();
        //默认服务器地址和端口，作者已经部署了服务器，计划运行到2017年1月31日
        text_host.setText("112.74.161.123");
        text_port.setText("12345");
        //登录界面的一些基础设置
        this.setTitle("登录界面");
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void initLayout() {
        //服务器地址
        label_host = new JLabel("服务器地址：");
        text_host = new JTextField(16);
        //端口后
        label_port = new JLabel("端口号：");
        text_port = new JTextField(5);
        //用户名
        label_username = new JLabel("用户名：");
        text_username = new JTextField(16);
        //密码
        label_password = new JLabel("密    码：");
        text_password = new JPasswordField(16);
        //登录、退出按钮
        btn_login = new JButton("登录/注册");
        btn_exit = new JButton("退出");
        //将需要的控件放入面板
        panel_host = new JPanel();
        panel_port = new JPanel();
        panel_username = new JPanel();
        panel_password = new JPanel();
        panel_button = new JPanel();
        panel_host.add(label_host);
        panel_host.add(text_host);
        panel_port.add(label_port);
        panel_port.add(text_port);
        panel_username.add(label_username);
        panel_username.add(text_username);
        panel_password.add(label_password);
        panel_password.add(text_password);
        panel_button.add(btn_login);
        panel_button.add(btn_exit);
        //窗口布局
        this.setLayout(new GridLayout(5, 1));
        this.add(panel_host);
        this.add(panel_port);
        this.add(panel_username);
        this.add(panel_password);
        this.add(panel_button);
    }

    private void initListener() {
        //输入用户名处理
        text_username.addKeyListener(new KeyListener() {
            
            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
                char ch = e.getKeyChar();
                String str = text_username.getText();
                //限制输入字符为字母或数字
                if (ch != '\b' && !Character.isLetterOrDigit(ch)) {
                    e.consume();
                }
                //限制用户名长度
                else if (str.length() >= 16)
                    e.consume();
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        //输入密码处理
        text_password.addKeyListener(new KeyListener() {
            
            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
                char ch = e.getKeyChar();
                String str = String.valueOf(text_password.getPassword());
                //回车进行登录或注册
                if (ch == '\n') {
                    if (login()) {
                        gameLobby = new GameLobby(bufferedReader, bufferedWriter, text_username.getText());
                        dispose();
                    }
                }
                //限制输入字符为字母或数字
                else if (ch != '\b' && !Character.isLetterOrDigit(ch)) {
                    e.consume();
                }
                //限制密码长度
                else if (str.length() >= 16)
                    e.consume();
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        //登录或注册
        btn_login.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                if (login()) {
                    gameLobby = new GameLobby(bufferedReader, bufferedWriter, text_username.getText());
                    dispose();
                }
            }
        });
        //退出
        btn_exit.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                System.exit(0);
            }
        });
    }

    public boolean connect() {
        //已经连接上服务器
        if (socket != null)
            return true;
        String host = text_host.getText();
        String port = text_port.getText();
        try {
            //连接服务器，保存对应的输入输出类
            socket = new Socket(host, Integer.valueOf(port).intValue());
            socket.setSoTimeout(60000);
            dataInputStream = new DataInputStream(socket.getInputStream());
            inputStreamReader = new InputStreamReader(dataInputStream, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            outputStreamWriter = new OutputStreamWriter(dataOutputStream, "UTF-8");
            bufferedWriter = new BufferedWriter(outputStreamWriter);
            return true;
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
            return false;
        }
    }

    public void sendMsg(String str) {
        try {
            //向服务器发送数据
            bufferedWriter.write(str + "\n");
            bufferedWriter.flush();
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
        }
    }

    public String getMsg() {
        try {
            //从服务器接收数据
            return bufferedReader.readLine();
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
            return "";
        }
    }

    public boolean login() {
        //连接服务器
        if (!connect()) {
            JOptionPane.showMessageDialog(null, "无法连接至目标服务器！", "连接错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        //用户名判断
        username = text_username.getText();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(null, "用户名不能为空！", "登录", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        password = String.valueOf(text_password.getPassword());
        String pwdmd5;
        try {
            //用md5对密码生成摘要
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            byte b[] = md5.digest();
            StringBuffer buf = new StringBuffer();
            int num;
            for (int i = 0; i < b.length; i++) {
                num = b[i];
                if (num < 0)
                    num += 256;
                if (num < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(num));
            }
            pwdmd5 = buf.toString();
        }
        catch (Exception e) {
            // TODO: handle exception
            System.err.println(e.toString());
            return false;
        }
        //向服务器发送登录请求
        sendMsg("login");
        sendMsg(username);
        sendMsg(pwdmd5);
        //服务器响应
        String ans = getMsg();
        if (ans.equals("done")) {
            //登录成功
            JOptionPane.showMessageDialog(null, "登录成功！", "登录", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        else if (ans.equals("new")) {
            //注册成功
            JOptionPane.showMessageDialog(null, "注册成功！", "注册", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        else if (ans.equals("pwderr")) {
            //密码错误
            JOptionPane.showMessageDialog(null, "密码错误！", "登录", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if (ans.equals("online")) {
            //用户已登录
            JOptionPane.showMessageDialog(null, "该用户已登录！", "登录", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else {
            //其他
            JOptionPane.showMessageDialog(null, "其他错误！", "登录", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
}
