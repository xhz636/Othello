package othello;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import javax.swing.*;

public class PlayRoom extends JFrame{

    //�������ó���
    private final int CHESS_EMPTY = 0;
    private final int CHESS_BLACK = 1;
    private final int CHESS_WHITE = 2;

    //��Ϸ����
    GameLobby gameLobby;
    //�����
    int roomnum;
    //��Ϸ�У��߳�ִ�б�־
    boolean playing;
    //�Ƿ�Ϊ��ǰ�غ�
    boolean turn;
    //�û����Ͷ����û���
    String username, opponame;
    //����������������������������
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    //����������ʾ��ǩ
    JPanel panel_p1info, panel_p2info, panel_info;
    Canvas panel_othello;
    JPanel panel_center, panel_sendmsg, panel_game, panel_south, panel_chat;
    JLabel label_p1name, label_p2name;
    JLabel label_p1status, label_p2status;
    JLabel label_p1count, label_p2count;
    //��Ϸ��Ϣ����������
    TextArea text_playinfo, text_getmsg;
    //����������
    TextField text_sendmsg;
    //��ť
    JButton btn_send, btn_giveup, btn_restart;
    //������Ϸ��Ϣ�߳�
    PlayHandler playHandler;
    //����
    int[][] chessboard;
    //�û���ɫ�Ͷ�����ɫ
    int usercolor, oppocolor;
    //���������ӡ����Ӵ�С
    int canvassize, blocksize, piecesize;
    //����λ�ú�����λ��
    int left, top, highlightx, highlighty;

    public PlayRoom(BufferedReader r, BufferedWriter w, int num, String name, String oppo, int color) {
        //�����������������
        bufferedReader = r;
        bufferedWriter = w;
        //��Ϸ�̱߳�־
        playing = true;
        //�����
        roomnum = num;
        //�û����Ͷ����û���
        username = name;
        opponame = oppo;
        //�û���ɫ�Ͷ�����ɫ
        usercolor = color;
        oppocolor = color == CHESS_BLACK ? CHESS_WHITE : CHESS_BLACK;
        //����
        chessboard = new int[8][8];
        //��ʼ������
        initboard();
        //��ʼ�����ڲ���
        initLayout();
        //��������Ҫ�ļ�����
        initListener();
        //��Ϸ�����������
        this.setTitle("�ڰ���");
        this.setSize(1000, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        //��д�رմ��ں������˳����䲢������Ϸ����
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                playing = false;
                sendMsg("exitroom");
                gameLobby = new GameLobby(bufferedReader, bufferedWriter, username);
                dispose();
            }
        });
        this.setVisible(true);
        //�жϵ�ǰ�������Ƿ������
        text_playinfo.append(username + "�����˷��䡣\n");
        if (opponame.isEmpty()) {
            //ԭ������ң��ȴ���ҽ���
            waitingstatus();
        }
        else if (usercolor == CHESS_BLACK) {
            //ԭ������ң�ִ�ڿ�ʼ��Ϸ
            text_playinfo.append(username + ": ��ʼ��Ϸ��ִ�����С�\n");
            userturn();
        }
        else {
            //ԭ������ң�ִ�׿�ʼ��Ϸ
            text_playinfo.append(username + ": ��ʼ��Ϸ��ִ�׺��С�\n");
            oppoturn();
        }
        //������Ϸ���ݴ����߳�
        playHandler = new PlayHandler();
        playHandler.start();
    }

    public void initboard() {
        //�������
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                chessboard[j][i] = CHESS_EMPTY;
        //���ó�ʼ��4������
        chessboard[3][3] = chessboard[4][4] = CHESS_BLACK;
        chessboard[4][3] = chessboard[3][4] = CHESS_WHITE;
        //û�и���
        highlightx = -1;
        highlighty = -1;
    }

    private void initLayout() {
        //�������Ϳؼ�
        panel_p1info = new JPanel();
        panel_p2info = new JPanel();
        panel_info = new JPanel();
        panel_othello = new Canvas();
        panel_sendmsg = new JPanel();
        panel_center = new JPanel();
        panel_game = new JPanel();
        panel_south = new JPanel();
        panel_chat = new JPanel();
        label_p1name = new JLabel();
        label_p1status = new JLabel();
        label_p1count = new JLabel();
        label_p2name = new JLabel();
        label_p2status = new JLabel();
        label_p2count = new JLabel();
        //�û���Ϣ����
        panel_p1info.setLayout(new BorderLayout());
        panel_p1info.add(label_p1name, BorderLayout.NORTH);
        panel_p1info.add(label_p1status, BorderLayout.CENTER);
        panel_p1info.add(label_p1count, BorderLayout.SOUTH);
        panel_p2info.setLayout(new BorderLayout());
        panel_p2info.add(label_p2name, BorderLayout.NORTH);
        panel_p2info.add(label_p2status, BorderLayout.CENTER);
        panel_p2info.add(label_p2count, BorderLayout.SOUTH);
        panel_info.setLayout(new BorderLayout());
        panel_info.add(panel_p1info,  BorderLayout.SOUTH);
        panel_info.add(panel_p2info,  BorderLayout.NORTH);
        panel_info.setPreferredSize(new Dimension(200, 400));
        //��Ϸ��Ϣ��������Ϣ����
        text_playinfo = new TextArea(20, 20);
        text_getmsg = new TextArea(20, 20);
        text_sendmsg = new TextField(20);
        btn_send = new JButton("����");
        btn_giveup = new JButton("����");
        btn_restart = new JButton("���¿�ʼ");
        panel_sendmsg.add(text_sendmsg);
        panel_sendmsg.add(btn_send);
        panel_game.add(btn_giveup);
        panel_game.add(btn_restart);
        panel_south.setLayout(new BorderLayout());
        panel_south.add(panel_sendmsg, BorderLayout.NORTH);
        panel_south.add(panel_game, BorderLayout.SOUTH);
        panel_center.setLayout(new GridLayout(2, 1));
        panel_center.add(text_playinfo);
        panel_center.add(text_getmsg);
        text_playinfo.setEditable(false);
        text_getmsg.setEditable(false);
        panel_chat.setLayout(new BorderLayout());
        panel_chat.add(panel_center, BorderLayout.CENTER);
        panel_chat.add(panel_south, BorderLayout.SOUTH);
        //���ڲ���
        this.setLayout(new BorderLayout());
        this.add(panel_info, BorderLayout.WEST);
        this.add(panel_othello, BorderLayout.CENTER);
        this.add(panel_chat, BorderLayout.EAST);
    }

    private void initListener() {
        //�����������
        panel_othello.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseReleased(MouseEvent arg0) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void mousePressed(MouseEvent arg0) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void mouseExited(MouseEvent arg0) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void mouseEntered(MouseEvent arg0) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void mouseClicked(MouseEvent arg0) {
                // TODO Auto-generated method stub
                //���ǵ�ǰ�غϣ��������κβ���
                if (!turn)
                    return;
                //����������
                if (arg0.getButton() == arg0.BUTTON1) {
                    int cx, cy;
                    //��ȡ�����������
                    cx = arg0.getX() - left;
                    cy = arg0.getY() - top;
                    //�������������
                    if (cx < 0 || cx > canvassize || cy < top || cy > canvassize)
                        return;
                    //��������ӵı߽���
                    if (cx % (blocksize + 1) == 0 || cy % (blocksize + 1) == 0)
                        return;
                    //�����Ӧ��������
                    int x = cx / (blocksize + 1);
                    int y = cy / (blocksize + 1);
                    //��⵱ǰλ���Ƿ����
                    if (checkplace(x, y, usercolor)) {
                        //���岢���з�ת
                        placepiece(x, y, usercolor);
                        //ˢ������
                        panel_othello.repaint();
                        //����������Ϣ
                        sendMsg("play");
                        sendMsg(String.valueOf(x));
                        sendMsg(String.valueOf(y));
                        text_playinfo.append(username + ": (" + (x + 1) + ", " + (y + 1) + ")\n");
                        //�غϽ���
                        oppoturn();
                    }
                    else {
                        //��ǰλ�ò�������ʾ
                        JOptionPane.showMessageDialog(null, "�������ڵ�ǰλ�ã�", "�ڰ���", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });
        //����������Ϣ
        btn_send.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                String str = text_sendmsg.getText();
                //����Ϣ������
                if (str.isEmpty())
                    return;
                //����������Ϣ
                sendMsg("chat");
                sendMsg(str);
                //��ʾ������Ϣ
                text_getmsg.append(username + ": " + str + "\n");
                //��������
                text_sendmsg.setText("");
            }
        });
        //����������Ϣ����
        text_sendmsg.addKeyListener(new KeyListener() {
            
            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
                char ch = e.getKeyChar();
                //�س�����������Ϣ
                if (ch == '\n') {
                    String str = text_sendmsg.getText();
                    //����Ϣ������
                    if (str.isEmpty())
                        return;
                    sendMsg("chat");
                    sendMsg(str);
                    text_getmsg.append(username + ": " + str + "\n");
                    text_sendmsg.setText("");
                }
                //�������볤��
                else if (text_sendmsg.getText().length() >= 100) {
                    e.consume();
                }
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
        //���䰴ť
        btn_giveup.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                //����������Ϣ
                sendMsg("giveup");
                text_playinfo.append(username + ": ���䡣\n");
                //������Ϸ����״̬
                endstatus(oppocolor);
            }
        });
        //���¿�ʼ��ť
        btn_restart.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                //�������¿�ʼ��Ϣ
                sendMsg("restart");
                //��ʼ����������
                initboard();
                //ˢ������
                panel_othello.repaint();
                //�ж�����
                if (usercolor == CHESS_BLACK) {
                    text_playinfo.append(username + ": ���¿�ʼ��ִ�����С�\n");
                    userturn();
                }
                else {
                    text_playinfo.append(username + ": ���¿�ʼ��ִ�׺��С�\n");
                    oppoturn();
                }
                JOptionPane.showMessageDialog(null, "���¿�ʼ��", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public void waitingstatus() {
        //�ȴ���ҽ���
        label_p1name.setText(username);
        label_p1status.setText("�ȴ����...");
        label_p1count.setText("����������2");
        label_p2name.setText("��λ");
        label_p2status.setText("�ȴ����...");
        label_p2count.setText("����������2");
        //���䡢���¿�ʼ������
        btn_giveup.setEnabled(false);
        btn_restart.setEnabled(false);
        //��ǰ�û����ɲ���
        turn = false;
    }

    public void userturn() {
        //�������״̬��Ϣ
        label_p1name.setText(username);
        label_p2name.setText(opponame);
        label_p1count.setText("����������" + getCount(usercolor));
        label_p2count.setText("����������" + getCount(oppocolor));
        //�ж��Ƿ��ܹ��л����û��غ�
        if (checkturn(usercolor)) {
            //�ж��û���ɫ�����ö�Ӧ��Ϣ
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("ִ�ڣ�" + username + "�Ļغϣ�");
                label_p2status.setText("ִ�ף��ȴ�" + username + "���ӣ�");
            }
            else {
                label_p1status.setText("ִ�ף�" + username + "�Ļغϣ�");
                label_p2status.setText("ִ�ڣ��ȴ�" + username + "���ӣ�");
            }
            //�������䣬�������¿�ʼ
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //��ǰ�û��ɲ���
            turn = true;
        }
        //�ж��Ƿ��ܹ������ڶ��ֻغ�
        else if (checkturn(oppocolor)) {
            //�ж��û���ɫ�����ö�Ӧ��Ϣ
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("ִ�ڣ��޴����ӣ����ȴ�" + opponame + "���ӣ�");
                label_p2status.setText("ִ�ף�" + opponame + "�Ļغϣ�");
            }
            else {
                label_p1status.setText("ִ�ף��޴����ӣ����ȴ�" + opponame + "���ӣ�");
                label_p2status.setText("ִ�ڣ�" + opponame + "�Ļغϣ�");
            }
            //�������䣬�������¿�ʼ
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //��ǰ�û����ɲ���
            turn = false;
        }
        else {
            //��ȡ�ȷ�
            int score = diff();
            if (score > 0) {
                //�û�ʤ�����������״̬
                endstatus(usercolor);
                JOptionPane.showMessageDialog(null, "��Ӯ�ˣ�", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
            }
            else if (score < 0) {
                //����ʤ�����������״̬
                endstatus(oppocolor);
                JOptionPane.showMessageDialog(null, "�����ˣ�", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                //ƽ�֣��������䣬�������¿�ʼ
                btn_giveup.setEnabled(false);
                btn_restart.setEnabled(true);
                //��ǰ�û����ɲ���
                turn = false;
                JOptionPane.showMessageDialog(null, "ƽ�֣�", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void oppoturn() {
        //�������״̬��Ϣ
        label_p1name.setText(username);
        label_p2name.setText(opponame);
        label_p1count.setText("����������" + getCount(usercolor));
        label_p2count.setText("����������" + getCount(oppocolor));
        //�ж��Ƿ��ܹ��л������ֻغ�
        if (checkturn(oppocolor)) {
            //�ж��û���ɫ�����ö�Ӧ��Ϣ
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("ִ�ڣ��ȴ�" + opponame + "���ӣ�");
                label_p2status.setText("ִ�ף�" + opponame + "�Ļغϣ�");
            }
            else {
                label_p1status.setText("ִ�ף��ȴ�" + opponame + "���ӣ�");
                label_p2status.setText("ִ�ڣ�" + opponame + "�Ļغϣ�");
            }
            //�������䣬�������¿�ʼ
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //��ǰ�û����ɲ���
            turn = false;
        }
        //�ж��Ƿ��ܹ��������û��غ�
        else if (checkturn(usercolor)) {
            //�ж��û���ɫ�����ö�Ӧ��Ϣ
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("ִ�ڣ�" + username + "�Ļغϣ�");
                label_p2status.setText("ִ�ף��޴����ӣ����ȴ�" + username + "���ӣ�");
            }
            else {
                label_p1status.setText("ִ�ף�" + username + "�Ļغϣ�");
                label_p2status.setText("ִ�ڣ��޴����ӣ����ȴ�" + username + "���ӣ�");
            }
            //�������䣬�������¿�ʼ
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //��ǰ�û��ɲ���
            turn = true;
        }
        else {
            //��ȡ�ȷ�
            int score = diff();
            if (score > 0) {
                //�û�ʤ�����������״̬
                endstatus(usercolor);
                JOptionPane.showMessageDialog(null, "��Ӯ�ˣ�", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
            }
            else if (score < 0) {
                //����ʤ�����������״̬
                endstatus(oppocolor);
                JOptionPane.showMessageDialog(null, "�����ˣ�", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                //ƽ�֣��������䣬�������¿�ʼ
                btn_giveup.setEnabled(false);
                btn_restart.setEnabled(true);
                //��ǰ�û����ɲ���
                turn = false;
                JOptionPane.showMessageDialog(null, "ƽ�֣�", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void endstatus(int winnercolor) {
        //�ж��Ƿ�Ϊ�û�ʤ��
        if (winnercolor == usercolor) {
            //�ж��û���ɫ�������Ӧʤ����Ϣ
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("ִ�ڣ�" + username + "ʤ����");
                label_p2status.setText("ִ�ף�" + opponame + "ʧ�ܣ�");
            }
            else {
                label_p1status.setText("ִ�ף�" + username + "ʤ����");
                label_p2status.setText("ִ�ڣ�" + opponame + "ʧ�ܣ�");
            }
        }
        //����ʤ��
        else {
            //�ж��û���ɫ�������Ӧʤ����Ϣ
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("ִ�ڣ�" + username + "ʧ�ܣ�");
                label_p2status.setText("ִ�ף�" + opponame + "ʤ����");
            }
            else {
                label_p1status.setText("ִ�ף�" + username + "ʧ�ܣ�");
                label_p2status.setText("ִ�ڣ�" + opponame + "ʤ����");
            }
        }
        //�������䣬�������¿�ʼ
        btn_giveup.setEnabled(false);
        btn_restart.setEnabled(true);
        //��ǰ�û����ɲ���
        turn = false;
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

    class Canvas extends JPanel {
        //���̻���
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            //��ȡ��ǰ���̵ĺ��ʴ�С
            canvassize = Math.min(getWidth(), getHeight());
            //������Ӵ�С
            blocksize = (canvassize - 1) / 8 - 1;
            //�������Ӵ�С
            piecesize = blocksize * 4 / 5;
            //�淶�����̴�С
            canvassize = (blocksize + 1) * 8 + 1;
            //���̵����Ͷ�������
            left = (getWidth() - canvassize) / 2;
            top = (getHeight() - canvassize) / 2;
            //�����̱���
            g.setColor(Color.GREEN);
            g.fillRect(left, top, canvassize, canvassize);
            //������������
            g.setColor(Color.BLACK);
            for (int i = 0; i < 9; i++) {
                int pos = i * (blocksize + 1);
                g.drawLine(left + pos, top, left + pos, top + canvassize - 1);
                g.drawLine(left, top + pos, left + canvassize - 1, top + pos);
            }
            //������ǰ����
            for (int j = 0; j < 8; j++)
                for (int i = 0; i < 8; i++) {
                    //������
                    if (chessboard[j][i] == CHESS_EMPTY)
                        continue;
                    //������
                    else if (chessboard[j][i] == CHESS_BLACK) {
                        g.setColor(Color.BLACK);
                    }
                    //������
                    else if (chessboard[j][i] == CHESS_WHITE) {
                        g.setColor(Color.WHITE);
                    }
                    //��������
                    g.fillOval(left + (blocksize + 1) * i + (blocksize - piecesize) / 2,
                               top + (blocksize + 1) * j + (blocksize - piecesize) / 2,
                               piecesize, piecesize);
                }
            //��������λ��
            if (highlightx >= 0 && highlighty >= 0) {
                g.setColor(Color.RED);
                g.drawRect(highlightx * (blocksize + 1) + left, highlighty * (blocksize + 1) + top, blocksize + 1, blocksize + 1);
            }
        }
    }

    public boolean checkplace(int x, int y, int color) {
        //��ǰλ�������ӣ���������
        if (chessboard[y][x] != CHESS_EMPTY)
            return false;
        //����8������
        for (int offsetx = -1; offsetx <= 1; offsetx++)
            for (int offsety = -1; offsety <= 1; offsety++) {
                //��������ʼλ��
                int newx = x + offsetx;
                int newy = y + offsety;
                //���α��������������ж���һ������
                if ((offsetx == 0 && offsety == 0) || newx < 0 || newx >= 8 || newy < 0 || newy >= 8)
                    continue;
                //������ʼΪ��ͬ��ɫ���������ӣ��÷������������������ж���һ������
                if (chessboard[newy][newx] == color || chessboard[newy][newx] == CHESS_EMPTY)
                    continue;
                //���ŷ������ÿ������
                for (int i = 1; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                        && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                    //�ҵ���ͬ��ɫ���ӣ���������
                    if (chessboard[newy + offsety * i][newx + offsetx * i] == color)
                        return true;
                    //��;���ڿ�λ�ã��÷�����������
                    else if (chessboard[newy + offsety * i][newx + offsetx * i] == CHESS_EMPTY)
                        break;
            }
        return false;
    }

    public void placepiece(int x, int y, int color) {
        //���Ӳ����ø���
        chessboard[y][x] = color;
        highlightx = x;
        highlighty = y;
        boolean eat;
        //����8������
        for (int offsetx = -1; offsetx <= 1; offsetx++)
            for (int offsety = -1; offsety <= 1; offsety++) {
                //��������ʼλ��
                int newx = x + offsetx;
                int newy = y + offsety;
                //���α��������������ж���һ������
                if ((offsetx == 0 && offsety == 0) || newx < 0 || newx >= 8 || newy < 0 || newy >= 8)
                    continue;
                //������ʼΪ��ͬ��ɫ���������ӣ��÷������������������ж���һ������
                if (chessboard[newy][newx] == color || chessboard[newy][newx] == CHESS_EMPTY)
                    continue;
                eat = false;
                //���ŷ������ÿ������
                for (int i = 1; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                        && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                    //�ҵ���ͬ��ɫ���ӣ��÷�����Է�ת
                    if (chessboard[newy + offsety * i][newx + offsetx * i] == color) {
                        eat = true;
                        break;
                    }
                    //��;���ڿ�λ�ã��÷�����������
                    else if (chessboard[newy + offsety * i][newx + offsetx * i] == CHESS_EMPTY)
                        break;
                //��ת����
                if (eat) {
                    for (int i = 0; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                            && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                        //�ҵ���ͬ��ɫ���ӣ���ת����
                        if (chessboard[newy + offsety * i][newx + offsetx * i] == color) {
                            break;
                        }
                        //��ת����
                        else {
                            chessboard[newy + offsety * i][newx + offsetx * i] = color;
                        }
                }
            }
    }

    public boolean checkturn(int color) {
        //�ж�������ÿ��λ���Ƿ��������
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                if (checkplace(i, j, color))
                    return true;
        return false;
    }

    public int getCount(int color) {
        //��ȡ��Ӧ��ɫ��������
        int count = 0;
        //����ÿ��λ�ò�����
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                if (chessboard[j][i] == color)
                    count++;
        return count;
    }

    public int diff() {
        //�������ӷֲ�
        return getCount(usercolor) - getCount(oppocolor);
    }

    public class PlayHandler extends Thread {
        
        //��Ϸ��Ϣ���մ����߳�
        public void run() {
            String line = "";
            //�ж��Ƿ�����Ϸ��
            while (playing) {
                line = getMsg();
                //��ҽ��뷿��
                if (line.equals("come")) {
                    opponame = getMsg();
                    text_playinfo.append(opponame + "�����˷��䡣\n");
                    label_p2name.setText(opponame);
                    if (usercolor == CHESS_BLACK) {
                        text_playinfo.append(opponame + ": ��ʼ��Ϸ��ִ�׺��С�\n");
                        userturn();
                    }
                    else {
                        text_playinfo.append(opponame + ": ��ʼ��Ϸ��ִ�����С�\n");
                        oppoturn();
                    }
                }
                //����˳�����
                else if (line.equals("out")) {
                    text_playinfo.append(opponame + "�뿪�˷��䡣\n");
                    opponame = "";
                    initboard();
                    panel_othello.repaint();
                    waitingstatus();
                }
                //����������Ϣ
                else if (line.equals("chat")) {
                    String msg = getMsg();
                    text_getmsg.append(opponame + ": " + msg + "\n");
                }
                //����������Ϣ
                else if (line.equals("play")) {
                    int x = Integer.valueOf(getMsg()).intValue();
                    int y = Integer.valueOf(getMsg()).intValue();
                    placepiece(x, y, oppocolor);
                    panel_othello.repaint();
                    text_playinfo.append(opponame + ": (" + (x + 1) + ", " + (y + 1) + ")\n");
                    userturn();
                }
                //��������
                else if (line.equals("giveup")) {
                    text_playinfo.append(opponame + ": ���䡣\n");
                    endstatus(usercolor);
                    JOptionPane.showMessageDialog(null, "�Է����䣡", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
                }
                //�������¿�ʼ
                else if (line.equals("restart")) {
                    initboard();
                    panel_othello.repaint();
                    if (usercolor == CHESS_BLACK) {
                        text_playinfo.append(opponame + ": ���¿�ʼ��ִ�׺��С�\n");
                        userturn();
                    }
                    else {
                        text_playinfo.append(opponame + ": ���¿�ʼ��ִ�����С�\n");
                        oppoturn();
                    }
                    JOptionPane.showMessageDialog(null, "���¿�ʼ��", "�ڰ���", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        
    }

}
