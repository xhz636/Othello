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
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.event.IIOReadWarningListener;
import javax.swing.*;

public class PlayRoom extends JFrame{

    private final int CHESS_EMPTY = 0;
    private final int CHESS_BLACK = 1;
    private final int CHESS_WHITE = 2;

    GameLobby gameLobby;
    int roomnum;
    boolean playing;
    boolean turn;
    String username, opponame;
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    JPanel panel_p1info, panel_p2info, panel_info;
    Canvas panel_othello;
    JPanel panel_sendmsg, panel_game, panel_south, panel_chat;
    JLabel label_p1name, label_p2name;
    JLabel label_p1status, label_p2status;
    JLabel label_p1count, label_p2count;
    TextArea text_getmsg;
    TextField text_sendmsg;
    JButton btn_send, btn_giveup, btn_restart;
    PlayHandler playHandler;
    int[][] chessboard;
    int usercolor, oppocolor;
    int canvassize;
    int blocksize;
    int piecesize;
    int left, top;

    public PlayRoom(BufferedReader r, BufferedWriter w, int num, String name, String oppo, int color) {
        bufferedReader = r;
        bufferedWriter = w;
        playing = true;
        roomnum = num;
        username = name;
        opponame = oppo;
        usercolor = color;
        oppocolor = color == CHESS_BLACK ? CHESS_WHITE : CHESS_BLACK;
        chessboard = new int[8][8];
        initboard();
        initLayout();
        initListener();
        this.setTitle("黑白棋");
        this.setSize(1000, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
        text_getmsg.append(username + "进入了房间。\n");
        if (opponame.isEmpty()) {
            waitingstatus();
        }
        else if (usercolor == CHESS_BLACK) {
            userturn();
        }
        else {
            oppoturn();
        }
        playHandler = new PlayHandler();
        playHandler.start();
    }

    public void initboard() {
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                chessboard[j][i] = CHESS_EMPTY;
        chessboard[3][3] = chessboard[4][4] = CHESS_BLACK;
        chessboard[4][3] = chessboard[3][4] = CHESS_WHITE;
    }

    private void initLayout() {
        panel_p1info = new JPanel();
        panel_p2info = new JPanel();
        panel_info = new JPanel();
        panel_othello = new Canvas();
        panel_sendmsg = new JPanel();
        panel_game = new JPanel();
        panel_south = new JPanel();
        panel_chat = new JPanel();
        label_p1name = new JLabel();
        label_p1status = new JLabel();
        label_p1count = new JLabel();
        label_p2name = new JLabel();
        label_p2status = new JLabel();
        label_p2count = new JLabel();
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
        text_getmsg = new TextArea(20, 20);
        text_sendmsg = new TextField(20);
        btn_send = new JButton("发送");
        btn_giveup = new JButton("认输");
        btn_restart = new JButton("重新开始");
        panel_sendmsg.add(text_sendmsg);
        panel_sendmsg.add(btn_send);
        panel_game.add(btn_giveup);
        panel_game.add(btn_restart);
        panel_south.setLayout(new BorderLayout());
        panel_south.add(panel_sendmsg, BorderLayout.NORTH);
        panel_south.add(panel_game, BorderLayout.SOUTH);
        panel_chat.setLayout(new BorderLayout());
        panel_chat.add(text_getmsg, BorderLayout.CENTER);
        panel_chat.add(panel_south, BorderLayout.SOUTH);
        this.setLayout(new BorderLayout());
        this.add(panel_info, BorderLayout.WEST);
        this.add(panel_othello, BorderLayout.CENTER);
        this.add(panel_chat, BorderLayout.EAST);
        text_getmsg.setEditable(false);
    }

    private void initListener() {
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
                if (!turn)
                    return;
                if (arg0.getButton() == arg0.BUTTON1) {
                    int cx, cy;
                    cx = arg0.getX() - left;
                    cy = arg0.getY() - top;
                    if (cx < 0 || cx > canvassize || cy < top || cy > canvassize)
                        return;
                    if (cx % (blocksize + 1) == 0 || cy % (blocksize + 1) == 0)
                        return;
                    int x = cx / (blocksize + 1);
                    int y = cy / (blocksize + 1);
                    if (checkplace(x, y, usercolor)) {
                        placepiece(x, y, usercolor);
                        panel_othello.repaint();
                        sendMsg("play");
                        sendMsg(String.valueOf(x));
                        sendMsg(String.valueOf(y));
                        oppoturn();
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "不能下在当前位置！", "黑白棋", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });
        btn_send.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                String str = text_sendmsg.getText();
                if (str.isEmpty())
                    return;
                sendMsg("chat");
                sendMsg(str);
                text_getmsg.append(username + ": " + str + "\n");
                text_sendmsg.setText("");
            }
        });
        text_sendmsg.addKeyListener(new KeyListener() {
            
            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
                char ch = e.getKeyChar();
                if (ch == '\n') {
                    String str = text_sendmsg.getText();
                    if (str.isEmpty())
                        return;
                    sendMsg("chat");
                    sendMsg(str);
                    text_getmsg.append(username + ": " + str + "\n");
                    text_sendmsg.setText("");
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
        btn_giveup.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                sendMsg("giveup");
                endstatus(oppocolor);
            }
        });
        btn_restart.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                sendMsg("restart");
                initboard();
                panel_othello.repaint();
                if (usercolor == CHESS_BLACK) {
                    userturn();
                }
                else {
                    oppoturn();
                }
                JOptionPane.showMessageDialog(null, "重新开始！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public void waitingstatus() {
        label_p1name.setText(username);
        label_p1status.setText("等待玩家...");
        label_p1count.setText("棋子数量：2");
        label_p2name.setText("空位");
        label_p2status.setText("等待玩家...");
        label_p2count.setText("棋子数量：2");
        btn_giveup.setEnabled(false);
        btn_restart.setEnabled(false);
        turn = false;
    }

    public void userturn() {
        label_p1name.setText(username);
        label_p2name.setText(opponame);
        label_p1count.setText("棋子数量：" + getCount(usercolor));
        label_p2count.setText("棋子数量：" + getCount(oppocolor));
        if (checkturn(usercolor)) {
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "的回合！");
                label_p2status.setText("执白，等待" + username + "落子！");
            }
            else {
                label_p1status.setText("执白，" + username + "的回合！");
                label_p2status.setText("执黑，等待" + username + "落子！");
            }
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            turn = true;
        }
        else if (checkturn(oppocolor)) {
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑（无处落子），等待" + opponame + "落子！");
                label_p2status.setText("执白，" + opponame + "的回合！");
            }
            else {
                label_p1status.setText("执白（无处落子），等待" + opponame + "落子！");
                label_p2status.setText("执黑，" + opponame + "的回合！");
            }
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            turn = false;
        }
        else {
            int score = diff();
            if (score > 0) {
                endstatus(usercolor);
                JOptionPane.showMessageDialog(null, "你赢了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else if (score < 0) {
                endstatus(oppocolor);
                JOptionPane.showMessageDialog(null, "你输了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                btn_giveup.setEnabled(false);
                btn_restart.setEnabled(true);
                turn = false;
                JOptionPane.showMessageDialog(null, "平局！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void oppoturn() {
        label_p1name.setText(username);
        label_p2name.setText(opponame);
        label_p1count.setText("棋子数量：" + getCount(usercolor));
        label_p2count.setText("棋子数量：" + getCount(oppocolor));
        if (checkturn(oppocolor)) {
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，等待" + opponame + "落子！");
                label_p2status.setText("执白，" + opponame + "的回合！");
            }
            else {
                label_p1status.setText("执白，等待" + opponame + "落子！");
                label_p2status.setText("执黑，" + opponame + "的回合！");
            }
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            turn = false;
        }
        else if (checkturn(usercolor)) {
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "的回合！");
                label_p2status.setText("执白（无处落子），等待" + username + "落子！");
            }
            else {
                label_p1status.setText("执白，" + username + "的回合！");
                label_p2status.setText("执黑（无处落子），等待" + username + "落子！");
            }
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            turn = true;
        }
        else {
            int score = diff();
            if (score > 0) {
                endstatus(usercolor);
                JOptionPane.showMessageDialog(null, "你赢了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else if (score < 0) {
                endstatus(oppocolor);
                JOptionPane.showMessageDialog(null, "你输了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                btn_giveup.setEnabled(false);
                btn_restart.setEnabled(true);
                turn = false;
                JOptionPane.showMessageDialog(null, "平局！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    public void endstatus(int winnercolor) {
        if (winnercolor == usercolor) {
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "胜利！");
                label_p2status.setText("执白，" + opponame + "失败！");
            }
            else {
                label_p1status.setText("执白，" + username + "胜利！");
                label_p2status.setText("执黑，" + opponame + "失败！");
            }
        }
        else {
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "失败！");
                label_p2status.setText("执白，" + opponame + "胜利！");
            }
            else {
                label_p1status.setText("执白，" + username + "失败！");
                label_p2status.setText("执黑，" + opponame + "胜利！");
            }
        }
        btn_giveup.setEnabled(false);
        btn_restart.setEnabled(true);
        turn = false;
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

    class Canvas extends JPanel {

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            canvassize = Math.min(getWidth(), getHeight());
            blocksize = (canvassize - 1) / 8 - 1;
            piecesize = blocksize * 4 / 5;
            canvassize = (blocksize + 1) * 8 + 1;
            left = (getWidth() - canvassize) / 2;
            top = (getHeight() - canvassize) / 2;
            g.setColor(Color.GREEN);
            g.fillRect(left, top, canvassize, canvassize);
            g.setColor(Color.BLACK);
            for (int i = 0; i < 9; i++) {
                int pos = i * (blocksize + 1);
                g.drawLine(left + pos, top, left + pos, top + canvassize - 1);
                g.drawLine(left, top + pos, left + canvassize - 1, top + pos);
            }
            for (int j = 0; j < 8; j++)
                for (int i = 0; i < 8; i++) {
                    if (chessboard[j][i] == CHESS_EMPTY)
                        continue;
                    else if (chessboard[j][i] == CHESS_BLACK) {
                        g.setColor(Color.BLACK);
                    }
                    else if (chessboard[j][i] == CHESS_WHITE) {
                        g.setColor(Color.WHITE);
                    }
                    g.fillOval(left + (blocksize + 1) * i + (blocksize - piecesize) / 2,
                               top + (blocksize + 1) * j + (blocksize - piecesize) / 2,
                               piecesize, piecesize);
                }
        }
    }

    public boolean checkplace(int x, int y, int color) {
        if (chessboard[y][x] != CHESS_EMPTY)
            return false;
        for (int offsetx = -1; offsetx <= 1; offsetx++)
            for (int offsety = -1; offsety <= 1; offsety++) {
                int newx = x + offsetx;
                int newy = y + offsety;
                if ((offsetx == 0 && offsety == 0) || newx < 0 || newx >= 8 || newy < 0 || newy >= 8)
                    continue;
                if (chessboard[newy][newx] == color || chessboard[newy][newx] == CHESS_EMPTY)
                    continue;
                for (int i = 1; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                        && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                    if (chessboard[newy + offsety * i][newx + offsetx * i] == color)
                        return true;
                    else if (chessboard[newy + offsety * i][newx + offsetx * i] == CHESS_EMPTY)
                        break;
            }
        return false;
    }

    public void placepiece(int x, int y, int color) {
        chessboard[y][x] = color;
        boolean eat;
        for (int offsetx = -1; offsetx <= 1; offsetx++)
            for (int offsety = -1; offsety <= 1; offsety++) {
                int newx = x + offsetx;
                int newy = y + offsety;
                if ((offsetx == 0 && offsety == 0) || newx < 0 || newx >= 8 || newy < 0 || newy >= 8)
                    continue;
                if (chessboard[newy][newx] == color || chessboard[newy][newx] == CHESS_EMPTY)
                    continue;
                eat = false;
                for (int i = 1; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                        && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                    if (chessboard[newy + offsety * i][newx + offsetx * i] == color) {
                        eat = true;
                        break;
                    }
                    else if (chessboard[newy + offsety * i][newx + offsetx * i] == CHESS_EMPTY)
                        break;
                if (eat) {
                    for (int i = 0; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                            && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                        if (chessboard[newy + offsety * i][newx + offsetx * i] == color) {
                            break;
                        }
                        else {
                            chessboard[newy + offsety * i][newx + offsetx * i] = color;
                        }
                }
            }
    }

    public boolean checkturn(int color) {
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                if (checkplace(i, j, color))
                    return true;
        return false;
    }

    public int getCount(int color) {
        int count = 0;
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                if (chessboard[j][i] == color)
                    count++;
        return count;
    }

    public int diff() {
        return getCount(usercolor) - getCount(oppocolor);
    }

    public class PlayHandler extends Thread {

        public void run() {
            String line = "";
            while (playing) {
                line = getMsg();
                if (line.equals("come")) {
                    opponame = getMsg();
                    text_getmsg.append(opponame + "进入了房间。\n");
                    label_p2name.setText(opponame);
                    if (usercolor == CHESS_BLACK) {
                        userturn();
                    }
                    else {
                        oppoturn();
                    }
                }
                else if (line.equals("out")) {
                    text_getmsg.append(opponame + "离开了房间。\n");
                    opponame = "";
                    initboard();
                    panel_othello.repaint();
                    waitingstatus();
                }
                else if (line.equals("chat")) {
                    String msg = getMsg();
                    text_getmsg.append(opponame + ": " + msg + "\n");
                }
                else if (line.equals("play")) {
                    int x = Integer.valueOf(getMsg()).intValue();
                    int y = Integer.valueOf(getMsg()).intValue();
                    placepiece(x, y, oppocolor);
                    panel_othello.repaint();
                    userturn();
                }
                else if (line.equals("giveup")) {
                    endstatus(usercolor);
                    JOptionPane.showMessageDialog(null, "对方认输！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
                }
                else if (line.equals("restart")) {
                    initboard();
                    panel_othello.repaint();
                    if (usercolor == CHESS_BLACK) {
                        userturn();
                    }
                    else {
                        oppoturn();
                    }
                    JOptionPane.showMessageDialog(null, "重新开始！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

    }

}
