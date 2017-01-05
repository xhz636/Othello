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

    //��Ϸ�����ͽ�ɫ���ó���
    private final int ROOM_SIZE = 9;
    private final int CHESS_BLACK = 1;
    private final int CHESS_WHITE = 2;

    //��Ϸ����
    PlayRoom playRoom;
    //���뷿���������ֹ���뷿�䷢��������ˢ�´��������ݳ�ͻ
    volatile boolean connection = false;
    volatile boolean intoroom = false;
    //�û���
    String username;
    //���뷿����ʾ
    JLabel label_join;
    JButton btn_enter;
    //��Ϸ�������ݺ���ʾ�б�
    DefaultListModel<String> model;
    JList list_room;
    //�������
    JPanel panel_enter, panel_select;
    JScrollPane spane_select;
    //����������������������������
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    //������Ϣ
    String[][] room;
    //ˢ����Ϸ������ʱ��
    Timer refresh_room;

    public GameLobby(BufferedReader r, BufferedWriter w, String name) {
        //�����������������
        bufferedReader = r;
        bufferedWriter = w;
        //�û���
        username = name;
        room = new String[ROOM_SIZE][2];
        //��ʼ�����ڲ���
        initLayout();
        //��������Ҫ�ļ�����
        initListener();
        //��Ϸ������������
        this.setTitle("��Ϸ���� - ��ã�" + username);
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        //��д�رմ��ں������Ͽ��������������
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
        //���뷿����ʾ�Ͱ�ť
        label_join = new JLabel("���ѡ����Ҫ����ķ���");
        btn_enter = new JButton("���뷿��");
        //��Ϸ������ʾ�б�
        model = getModel();
        list_room = new JList();
        list_room.setModel(model);
        list_room.setPreferredSize(new Dimension(300, 200));
        //����Ӧ�ؼ��������
        panel_enter = new JPanel();
        panel_select = new JPanel();
        spane_select = new JScrollPane(list_room);
        panel_enter.add(label_join);
        panel_enter.add(btn_enter);
        panel_select.add(spane_select);
        //���ڲ���
        this.setLayout(new BorderLayout());
        this.add(panel_enter, BorderLayout.NORTH);
        this.add(panel_select, BorderLayout.CENTER);
    }

    private void initListener() {
        //���뷿��
        btn_enter.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                int roomnum = list_room.getSelectedIndex();
                if (roomnum < 0) {
                    //δѡ��
                    JOptionPane.showMessageDialog(null, "δѡ�񷿼䣡", "���뷿��", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                else {
                    //�ȴ�ͨ�����ͷ�
                    while (connection);
                    //���Ͻ�����
                    intoroom = true;
                    //����ͨ����
                    connection = true;
                    sendMsg("intoroom");
                    sendMsg(String.valueOf(roomnum));
                    String str = getMsg();
                    //�ͷ�ͨ����
                    connection = false;
                    if (str.equals("done")) {
                        refresh_room.cancel();
                        if (room[roomnum][0].isEmpty()) {
                            //ִ�ڽ�����Ϸ����
                            playRoom = new PlayRoom(bufferedReader, bufferedWriter, roomnum, username, room[roomnum][1], CHESS_BLACK);
                        }
                        else if (room[roomnum][1].isEmpty()) {
                            //ִ�׽�����Ϸ����
                            playRoom = new PlayRoom(bufferedReader, bufferedWriter, roomnum, username, room[roomnum][0], CHESS_WHITE);
                        }
                        dispose();
                        return;
                    }
                    else {
                        //�ͷŽ�����
                        intoroom = false;
                        JOptionPane.showMessageDialog(null, "�����������޷����룡", "���뷿��", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
        });
        //ÿ��500msˢ��һ����Ϸ��������
        refresh_room = new Timer();
        Refresh rfroom = new Refresh();
        refresh_room.schedule(rfroom, 500, 500);
    }

    public DefaultListModel<String> getModel() {
        //�ȴ�ͨ�����ͷ�
        while (connection);
        //������ڽ���������ˢ����Ϸ����
        if (intoroom)
            return new DefaultListModel<String>();
        //����ͨ����
        connection = true;
        //����������������Ϸ��������
        sendMsg("getroom");
        model = new DefaultListModel<String>();
        //��ȡ��Ϸ�������ݲ�����
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
        //�ͷ�ͨ����
        connection = false;
        return model;
    }

    public void sendMsg(String str) {
        try {
            //���������������
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
            //�ӷ�������������
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
            //����֮ǰ��ѡ��
            int select = list_room.getSelectedIndex();
            //ˢ����Ϸ��������
            model = getModel();
            list_room.setModel(model);
            //��ԭ֮ǰ��ѡ��
            if (select >= 0)
                list_room.setSelectedIndex(select);
        }
        
    }

}
