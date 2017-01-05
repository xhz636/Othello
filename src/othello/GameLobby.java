package othello;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class GameLobby extends JFrame{

    //游戏大厅和角色所用常量
    private final int ROOM_SIZE = 9;
    private final int CHESS_BLACK = 1;
    private final int CHESS_WHITE = 2;

    //游戏房间
    PlayRoom playRoom;
    //进入房间的锁，防止进入房间发送数据与刷新大厅的数据冲突
    volatile boolean connection = false;
    volatile boolean intoroom = false;
    //用户名
    String username;
    //进入房间提示
    JLabel label_join;
    JButton btn_enter;
    //游戏大厅数据和显示列表
    DefaultListModel<String> model;
    JList list_room;
    //布局面板
    JPanel panel_enter, panel_select;
    JScrollPane spane_select;
    //向服务器传输数据所用输入输出类
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    //房间信息
    String[][] room;
    //刷新游戏大厅定时器
    Timer refresh_room;

    public GameLobby(BufferedReader r, BufferedWriter w, String name) {
        //保存输入输出所用类
        bufferedReader = r;
        bufferedWriter = w;
        //用户名
        username = name;
        room = new String[ROOM_SIZE][2];
        //初始化窗口布局
        initLayout();
        //设置所需要的监视器
        initListener();
        //游戏大厅基础设置
        this.setTitle("游戏大厅 - 你好！" + username);
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        //重写关闭窗口函数，断开与服务器的连接
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                try {
                    refresh_room.cancel();
                    sendMsg("exit");
                    bufferedReader.close();
                    bufferedWriter.close();
                    System.exit(0);
                }
                catch (Exception e) {
                    // TODO: handle exception
                    System.err.println(e.toString());
                    System.exit(0);
                }
            }
        });
        this.setVisible(true);
    }

    private void initLayout() {
        //进入房间提示和按钮
        label_join = new JLabel("点击选择所要进入的房间");
        btn_enter = new JButton("进入房间");
        //游戏大厅显示列表
        model = getModel();
        list_room = new JList();
        list_room.setModel(model);
        list_room.setPreferredSize(new Dimension(300, 200));
        //将对应控件放入面板
        panel_enter = new JPanel();
        panel_select = new JPanel();
        spane_select = new JScrollPane(list_room);
        panel_enter.add(label_join);
        panel_enter.add(btn_enter);
        panel_select.add(spane_select);
        //窗口布局
        this.setLayout(new BorderLayout());
        this.add(panel_enter, BorderLayout.NORTH);
        this.add(panel_select, BorderLayout.CENTER);
    }

    private void initListener() {
        //进入房间
        btn_enter.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                int roomnum = list_room.getSelectedIndex();
                if (roomnum < 0) {
                    //未选择
                    JOptionPane.showMessageDialog(null, "未选择房间！", "进入房间", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                else {
                    //等待通信锁释放
                    while (connection);
                    //加上进入锁
                    intoroom = true;
                    //加上通信锁
                    connection = true;
                    sendMsg("intoroom");
                    sendMsg(String.valueOf(roomnum));
                    String str = getMsg();
                    //释放通信锁
                    connection = false;
                    if (str.equals("done")) {
                        refresh_room.cancel();
                        if (room[roomnum][0].isEmpty()) {
                            //执黑进入游戏房间
                            playRoom = new PlayRoom(bufferedReader, bufferedWriter, roomnum, username, room[roomnum][1], CHESS_BLACK);
                        }
                        else if (room[roomnum][1].isEmpty()) {
                            //执白进入游戏房间
                            playRoom = new PlayRoom(bufferedReader, bufferedWriter, roomnum, username, room[roomnum][0], CHESS_WHITE);
                        }
                        dispose();
                        return;
                    }
                    else {
                        //释放进入锁
                        intoroom = false;
                        JOptionPane.showMessageDialog(null, "房间已满，无法进入！", "进入房间", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
        });
        //每个500ms刷新一次游戏大厅数据
        refresh_room = new Timer();
        Refresh rfroom = new Refresh();
        refresh_room.schedule(rfroom, 500, 500);
    }

    public DefaultListModel<String> getModel() {
        //等待通信锁释放
        while (connection);
        //如果存在进入锁，不刷新游戏大厅
        if (intoroom)
            return new DefaultListModel<String>();
        //加上通信锁
        connection = true;
        //向服务器请求更新游戏大厅数据
        sendMsg("getroom");
        model = new DefaultListModel<String>();
        //获取游戏大厅数据并更新
        for (int i = 0; i < ROOM_SIZE; i++) {
            room[i][0] = getMsg();
            room[i][1] = getMsg();
            if (room[i][0].isEmpty() && room[i][1].isEmpty()) {
                model.addElement("room" + (i + 1));
            }
            else if (room[i][0].isEmpty() && !room[i][1].isEmpty()) {
                model.addElement("room" + (i + 1) + ": " + room[i][1] + " is waiting");
            }
            else if (!room[i][0].isEmpty() && room[i][1].isEmpty()) {
                model.addElement("room" + (i + 1) + ": " + room[i][0] + " is waiting");
            }
            else {
                model.addElement("room" + (i + 1) + ": " + room[i][0] + " vs " + room[i][1]);
            }
        }
        //释放通信锁
        connection = false;
        return model;
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

    class Refresh extends TimerTask {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            //保存之前的选择
            int select = list_room.getSelectedIndex();
            //刷新游戏大厅数据
            model = getModel();
            list_room.setModel(model);
            //还原之前的选择
            if (select >= 0)
                list_room.setSelectedIndex(select);
        }
        
    }

}
