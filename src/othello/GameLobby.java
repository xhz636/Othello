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

    private final int ROOM_SIZE = 9;
    private final int CHESS_BLACK = 1;
    private final int CHESS_WHITE = 2;

    PlayRoom playRoom;
    volatile boolean connection = false;
    volatile boolean intoroom = false;
    String username;
    JLabel label_join;
    JButton btn_create;
    DefaultListModel<String> model;
    JList list_room;
    JPanel panel_create, panel_select;
    JScrollPane spane_select;
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    String[][] room;
    Timer refresh_room;

    public GameLobby(BufferedReader r, BufferedWriter w, String name) {
        bufferedReader = r;
        bufferedWriter = w;
        username = name;
        room = new String[ROOM_SIZE][2];
        initLayout();
        initListener();
        this.setTitle("游戏大厅 - 你好！" + username);
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
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
        label_join = new JLabel("点击选择所要进入的房间");
        btn_create = new JButton("进入房间");
        model = getModel();
        list_room = new JList();
        list_room.setModel(model);
        list_room.setPreferredSize(new Dimension(300, 200));
        panel_create = new JPanel();
        panel_select = new JPanel();
        spane_select = new JScrollPane(list_room);
        this.setLayout(new BorderLayout());
        panel_create.add(label_join);
        panel_create.add(btn_create);
        panel_select.add(spane_select);
        this.add(panel_create, BorderLayout.NORTH);
        this.add(panel_select, BorderLayout.CENTER);
    }

    private void initListener() {
        btn_create.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                int roomnum = list_room.getSelectedIndex();
                if (roomnum < 0) {
                    JOptionPane.showMessageDialog(null, "未选择房间！", "进入房间", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                else {
                    while (connection);
                    intoroom = true;
                    connection = true;
                    sendMsg("intoroom");
                    sendMsg(String.valueOf(roomnum));
                    String str = getMsg();
                    connection = false;
                    if (str.equals("done")) {
                        refresh_room.cancel();
                        if (room[roomnum][0].isEmpty()) {
                            playRoom = new PlayRoom(bufferedReader, bufferedWriter, roomnum, username, room[roomnum][1], CHESS_BLACK);
                        }
                        else if (room[roomnum][1].isEmpty()) {
                            playRoom = new PlayRoom(bufferedReader, bufferedWriter, roomnum, username, room[roomnum][0], CHESS_WHITE);
                        }
                        dispose();
                        return;
                    }
                    else {
                        intoroom = false;
                        JOptionPane.showMessageDialog(null, "房间已满，无法进入！", "进入房间", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
        });
        refresh_room = new Timer();
        Refresh rfroom = new Refresh();
        refresh_room.schedule(rfroom, 500, 500);
    }

    public DefaultListModel<String> getModel() {
        while (connection);
        if (intoroom)
            return new DefaultListModel<String>();
        connection = true;
        sendMsg("getroom");
        model = new DefaultListModel<String>();
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
        connection = false;
        return model;
    }

    public void sendMsg(String str) {
        try {
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
            int select = list_room.getSelectedIndex();
            model = getModel();
            list_room.setModel(model);
            if (select >= 0)
                list_room.setSelectedIndex(select);
        }
        
    }

}
