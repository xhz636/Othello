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

    //棋盘所用常量
    private final int CHESS_EMPTY = 0;
    private final int CHESS_BLACK = 1;
    private final int CHESS_WHITE = 2;

    //游戏大厅
    GameLobby gameLobby;
    //房间号
    int roomnum;
    //游戏中，线程执行标志
    boolean playing;
    //是否为当前回合
    boolean turn;
    //用户名和对手用户名
    String username, opponame;
    //向服务器传输数据所用输入输出类
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    //各种面板和提示标签
    JPanel panel_p1info, panel_p2info, panel_info;
    Canvas panel_othello;
    JPanel panel_center, panel_sendmsg, panel_game, panel_south, panel_chat;
    JLabel label_p1name, label_p2name;
    JLabel label_p1status, label_p2status;
    JLabel label_p1count, label_p2count;
    //游戏信息区和聊天区
    TextArea text_playinfo, text_getmsg;
    //聊天输入区
    TextField text_sendmsg;
    //按钮
    JButton btn_send, btn_giveup, btn_restart;
    //联机游戏信息线程
    PlayHandler playHandler;
    //棋盘
    int[][] chessboard;
    //用户颜色和对手颜色
    int usercolor, oppocolor;
    //画布、格子、棋子大小
    int canvassize, blocksize, piecesize;
    //画布位置和落子位置
    int left, top, highlightx, highlighty;

    public PlayRoom(BufferedReader r, BufferedWriter w, int num, String name, String oppo, int color) {
        //保存输入输出所用类
        bufferedReader = r;
        bufferedWriter = w;
        //游戏线程标志
        playing = true;
        //房间号
        roomnum = num;
        //用户名和对手用户名
        username = name;
        opponame = oppo;
        //用户颜色和对手颜色
        usercolor = color;
        oppocolor = color == CHESS_BLACK ? CHESS_WHITE : CHESS_BLACK;
        //棋盘
        chessboard = new int[8][8];
        //初始化棋盘
        initboard();
        //初始化窗口布局
        initLayout();
        //设置所需要的监视器
        initListener();
        //游戏房间基础设置
        this.setTitle("黑白棋");
        this.setSize(1000, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        //重写关闭窗口函数，退出房间并开启游戏大厅
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
        //判断当前房间内是否有玩家
        text_playinfo.append(username + "进入了房间。\n");
        if (opponame.isEmpty()) {
            //原先无玩家，等待玩家进入
            waitingstatus();
        }
        else if (usercolor == CHESS_BLACK) {
            //原先有玩家，执黑开始游戏
            text_playinfo.append(username + ": 开始游戏，执黑先行。\n");
            userturn();
        }
        else {
            //原先有玩家，执白开始游戏
            text_playinfo.append(username + ": 开始游戏，执白后行。\n");
            oppoturn();
        }
        //开启游戏数据处理线程
        playHandler = new PlayHandler();
        playHandler.start();
    }

    public void initboard() {
        //清空棋盘
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                chessboard[j][i] = CHESS_EMPTY;
        //放置初始的4个棋子
        chessboard[3][3] = chessboard[4][4] = CHESS_BLACK;
        chessboard[4][3] = chessboard[3][4] = CHESS_WHITE;
        //没有高亮
        highlightx = -1;
        highlighty = -1;
    }

    private void initLayout() {
        //各种面板和控件
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
        //用户信息布局
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
        //游戏信息和聊天信息布局
        text_playinfo = new TextArea(20, 20);
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
        panel_center.setLayout(new GridLayout(2, 1));
        panel_center.add(text_playinfo);
        panel_center.add(text_getmsg);
        text_playinfo.setEditable(false);
        text_getmsg.setEditable(false);
        panel_chat.setLayout(new BorderLayout());
        panel_chat.add(panel_center, BorderLayout.CENTER);
        panel_chat.add(panel_south, BorderLayout.SOUTH);
        //窗口布局
        this.setLayout(new BorderLayout());
        this.add(panel_info, BorderLayout.WEST);
        this.add(panel_othello, BorderLayout.CENTER);
        this.add(panel_chat, BorderLayout.EAST);
    }

    private void initListener() {
        //点击棋盘下棋
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
                //不是当前回合，不进行任何操作
                if (!turn)
                    return;
                //鼠标左键单击
                if (arg0.getButton() == arg0.BUTTON1) {
                    int cx, cy;
                    //获取相对棋盘坐标
                    cx = arg0.getX() - left;
                    cy = arg0.getY() - top;
                    //点击不在棋盘内
                    if (cx < 0 || cx > canvassize || cy < top || cy > canvassize)
                        return;
                    //点击到格子的边界线
                    if (cx % (blocksize + 1) == 0 || cy % (blocksize + 1) == 0)
                        return;
                    //计算对应棋盘行列
                    int x = cx / (blocksize + 1);
                    int y = cy / (blocksize + 1);
                    //检测当前位置是否可下
                    if (checkplace(x, y, usercolor)) {
                        //下棋并进行翻转
                        placepiece(x, y, usercolor);
                        //刷新棋盘
                        panel_othello.repaint();
                        //发送下棋信息
                        sendMsg("play");
                        sendMsg(String.valueOf(x));
                        sendMsg(String.valueOf(y));
                        text_playinfo.append(username + ": (" + (x + 1) + ", " + (y + 1) + ")\n");
                        //回合结束
                        oppoturn();
                    }
                    else {
                        //当前位置不可下提示
                        JOptionPane.showMessageDialog(null, "不能下在当前位置！", "黑白棋", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });
        //发送聊天信息
        btn_send.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                String str = text_sendmsg.getText();
                //空信息不发送
                if (str.isEmpty())
                    return;
                //发送聊天信息
                sendMsg("chat");
                sendMsg(str);
                //显示聊天信息
                text_getmsg.append(username + ": " + str + "\n");
                //清空输入框
                text_sendmsg.setText("");
            }
        });
        //输入聊天信息处理
        text_sendmsg.addKeyListener(new KeyListener() {
            
            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
                char ch = e.getKeyChar();
                //回车发送聊天信息
                if (ch == '\n') {
                    String str = text_sendmsg.getText();
                    //空信息不发送
                    if (str.isEmpty())
                        return;
                    sendMsg("chat");
                    sendMsg(str);
                    text_getmsg.append(username + ": " + str + "\n");
                    text_sendmsg.setText("");
                }
                //限制输入长度
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
        //认输按钮
        btn_giveup.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                //发送认输信息
                sendMsg("giveup");
                text_playinfo.append(username + ": 认输。\n");
                //进入游戏结束状态
                endstatus(oppocolor);
            }
        });
        //重新开始按钮
        btn_restart.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                //发送重新开始信息
                sendMsg("restart");
                //初始化棋盘数据
                initboard();
                //刷新棋盘
                panel_othello.repaint();
                //判断先手
                if (usercolor == CHESS_BLACK) {
                    text_playinfo.append(username + ": 重新开始，执黑先行。\n");
                    userturn();
                }
                else {
                    text_playinfo.append(username + ": 重新开始，执白后行。\n");
                    oppoturn();
                }
                JOptionPane.showMessageDialog(null, "重新开始！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public void waitingstatus() {
        //等待玩家进入
        label_p1name.setText(username);
        label_p1status.setText("等待玩家...");
        label_p1count.setText("棋子数量：2");
        label_p2name.setText("空位");
        label_p2status.setText("等待玩家...");
        label_p2count.setText("棋子数量：2");
        //认输、重新开始不可用
        btn_giveup.setEnabled(false);
        btn_restart.setEnabled(false);
        //当前用户不可操作
        turn = false;
    }

    public void userturn() {
        //更新玩家状态信息
        label_p1name.setText(username);
        label_p2name.setText(opponame);
        label_p1count.setText("棋子数量：" + getCount(usercolor));
        label_p2count.setText("棋子数量：" + getCount(oppocolor));
        //判断是否能够切换到用户回合
        if (checkturn(usercolor)) {
            //判断用户颜色，设置对应信息
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "的回合！");
                label_p2status.setText("执白，等待" + username + "落子！");
            }
            else {
                label_p1status.setText("执白，" + username + "的回合！");
                label_p2status.setText("执黑，等待" + username + "落子！");
            }
            //可以认输，不可重新开始
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //当前用户可操作
            turn = true;
        }
        //判断是否能够保存在对手回合
        else if (checkturn(oppocolor)) {
            //判断用户颜色，设置对应信息
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑（无处落子），等待" + opponame + "落子！");
                label_p2status.setText("执白，" + opponame + "的回合！");
            }
            else {
                label_p1status.setText("执白（无处落子），等待" + opponame + "落子！");
                label_p2status.setText("执黑，" + opponame + "的回合！");
            }
            //可以认输，不可重新开始
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //当前用户不可操作
            turn = false;
        }
        else {
            //获取比分
            int score = diff();
            if (score > 0) {
                //用户胜利，进入结束状态
                endstatus(usercolor);
                JOptionPane.showMessageDialog(null, "你赢了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else if (score < 0) {
                //对手胜利，进入结束状态
                endstatus(oppocolor);
                JOptionPane.showMessageDialog(null, "你输了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                //平局，不可认输，可以重新开始
                btn_giveup.setEnabled(false);
                btn_restart.setEnabled(true);
                //当前用户不可操作
                turn = false;
                JOptionPane.showMessageDialog(null, "平局！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void oppoturn() {
        //更新玩家状态信息
        label_p1name.setText(username);
        label_p2name.setText(opponame);
        label_p1count.setText("棋子数量：" + getCount(usercolor));
        label_p2count.setText("棋子数量：" + getCount(oppocolor));
        //判断是否能够切换到对手回合
        if (checkturn(oppocolor)) {
            //判断用户颜色，设置对应信息
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，等待" + opponame + "落子！");
                label_p2status.setText("执白，" + opponame + "的回合！");
            }
            else {
                label_p1status.setText("执白，等待" + opponame + "落子！");
                label_p2status.setText("执黑，" + opponame + "的回合！");
            }
            //可以认输，不可重新开始
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //当前用户不可操作
            turn = false;
        }
        //判断是否能够保存在用户回合
        else if (checkturn(usercolor)) {
            //判断用户颜色，设置对应信息
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "的回合！");
                label_p2status.setText("执白（无处落子），等待" + username + "落子！");
            }
            else {
                label_p1status.setText("执白，" + username + "的回合！");
                label_p2status.setText("执黑（无处落子），等待" + username + "落子！");
            }
            //可以认输，不可重新开始
            btn_giveup.setEnabled(true);
            btn_restart.setEnabled(false);
            //当前用户可操作
            turn = true;
        }
        else {
            //获取比分
            int score = diff();
            if (score > 0) {
                //用户胜利，进入结束状态
                endstatus(usercolor);
                JOptionPane.showMessageDialog(null, "你赢了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else if (score < 0) {
                //对手胜利，进入结束状态
                endstatus(oppocolor);
                JOptionPane.showMessageDialog(null, "你输了！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                //平局，不可认输，可以重新开始
                btn_giveup.setEnabled(false);
                btn_restart.setEnabled(true);
                //当前用户不可操作
                turn = false;
                JOptionPane.showMessageDialog(null, "平局！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void endstatus(int winnercolor) {
        //判断是否为用户胜利
        if (winnercolor == usercolor) {
            //判断用户颜色并输出对应胜负信息
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "胜利！");
                label_p2status.setText("执白，" + opponame + "失败！");
            }
            else {
                label_p1status.setText("执白，" + username + "胜利！");
                label_p2status.setText("执黑，" + opponame + "失败！");
            }
        }
        //对手胜利
        else {
            //判断用户颜色并输出对应胜负信息
            if (usercolor == CHESS_BLACK) {
                label_p1status.setText("执黑，" + username + "失败！");
                label_p2status.setText("执白，" + opponame + "胜利！");
            }
            else {
                label_p1status.setText("执白，" + username + "失败！");
                label_p2status.setText("执黑，" + opponame + "胜利！");
            }
        }
        //不可认输，可以重新开始
        btn_giveup.setEnabled(false);
        btn_restart.setEnabled(true);
        //当前用户不可操作
        turn = false;
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

    class Canvas extends JPanel {
        //棋盘画布
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            //获取当前棋盘的合适大小
            canvassize = Math.min(getWidth(), getHeight());
            //计算格子大小
            blocksize = (canvassize - 1) / 8 - 1;
            //计算棋子大小
            piecesize = blocksize * 4 / 5;
            //规范化棋盘大小
            canvassize = (blocksize + 1) * 8 + 1;
            //棋盘的左侧和顶部坐标
            left = (getWidth() - canvassize) / 2;
            top = (getHeight() - canvassize) / 2;
            //画棋盘背景
            g.setColor(Color.GREEN);
            g.fillRect(left, top, canvassize, canvassize);
            //画棋盘网格线
            g.setColor(Color.BLACK);
            for (int i = 0; i < 9; i++) {
                int pos = i * (blocksize + 1);
                g.drawLine(left + pos, top, left + pos, top + canvassize - 1);
                g.drawLine(left, top + pos, left + canvassize - 1, top + pos);
            }
            //画出当前棋盘
            for (int j = 0; j < 8; j++)
                for (int i = 0; i < 8; i++) {
                    //无棋子
                    if (chessboard[j][i] == CHESS_EMPTY)
                        continue;
                    //黑棋子
                    else if (chessboard[j][i] == CHESS_BLACK) {
                        g.setColor(Color.BLACK);
                    }
                    //白棋子
                    else if (chessboard[j][i] == CHESS_WHITE) {
                        g.setColor(Color.WHITE);
                    }
                    //画出棋子
                    g.fillOval(left + (blocksize + 1) * i + (blocksize - piecesize) / 2,
                               top + (blocksize + 1) * j + (blocksize - piecesize) / 2,
                               piecesize, piecesize);
                }
            //高亮落子位置
            if (highlightx >= 0 && highlighty >= 0) {
                g.setColor(Color.RED);
                g.drawRect(highlightx * (blocksize + 1) + left, highlighty * (blocksize + 1) + top, blocksize + 1, blocksize + 1);
            }
        }
    }

    public boolean checkplace(int x, int y, int color) {
        //当前位置有棋子，不可落子
        if (chessboard[y][x] != CHESS_EMPTY)
            return false;
        //遍历8个方向
        for (int offsetx = -1; offsetx <= 1; offsetx++)
            for (int offsety = -1; offsety <= 1; offsety++) {
                //遍历的起始位置
                int newx = x + offsetx;
                int newy = y + offsety;
                //本次遍历结束，继续判断下一个方向
                if ((offsetx == 0 && offsety == 0) || newx < 0 || newx >= 8 || newy < 0 || newy >= 8)
                    continue;
                //方向起始为相同颜色或者无棋子，该方向不满足条件，继续判断下一个方向
                if (chessboard[newy][newx] == color || chessboard[newy][newx] == CHESS_EMPTY)
                    continue;
                //沿着方向遍历每个棋子
                for (int i = 1; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                        && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                    //找到相同颜色棋子，可以落子
                    if (chessboard[newy + offsety * i][newx + offsetx * i] == color)
                        return true;
                    //中途存在空位置，该方向不满足条件
                    else if (chessboard[newy + offsety * i][newx + offsetx * i] == CHESS_EMPTY)
                        break;
            }
        return false;
    }

    public void placepiece(int x, int y, int color) {
        //落子并设置高亮
        chessboard[y][x] = color;
        highlightx = x;
        highlighty = y;
        boolean eat;
        //遍历8个方向
        for (int offsetx = -1; offsetx <= 1; offsetx++)
            for (int offsety = -1; offsety <= 1; offsety++) {
                //遍历的起始位置
                int newx = x + offsetx;
                int newy = y + offsety;
                //本次遍历结束，继续判断下一个方向
                if ((offsetx == 0 && offsety == 0) || newx < 0 || newx >= 8 || newy < 0 || newy >= 8)
                    continue;
                //方向起始为相同颜色或者无棋子，该方向不满足条件，继续判断下一个方向
                if (chessboard[newy][newx] == color || chessboard[newy][newx] == CHESS_EMPTY)
                    continue;
                eat = false;
                //沿着方向遍历每个棋子
                for (int i = 1; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                        && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                    //找到相同颜色棋子，该方向可以翻转
                    if (chessboard[newy + offsety * i][newx + offsetx * i] == color) {
                        eat = true;
                        break;
                    }
                    //中途存在空位置，该方向不满足条件
                    else if (chessboard[newy + offsety * i][newx + offsetx * i] == CHESS_EMPTY)
                        break;
                //翻转棋子
                if (eat) {
                    for (int i = 0; newx + offsetx * i >= 0 && newx + offsetx * i < 8
                            && newy + offsety * i >= 0 && newy + offsety * i < 8; i++)
                        //找到相同颜色棋子，翻转结束
                        if (chessboard[newy + offsety * i][newx + offsetx * i] == color) {
                            break;
                        }
                        //翻转棋子
                        else {
                            chessboard[newy + offsety * i][newx + offsetx * i] = color;
                        }
                }
            }
    }

    public boolean checkturn(int color) {
        //判断棋盘上每个位置是否可以落子
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                if (checkplace(i, j, color))
                    return true;
        return false;
    }

    public int getCount(int color) {
        //获取对应颜色棋子数量
        int count = 0;
        //遍历每个位置并计数
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++)
                if (chessboard[j][i] == color)
                    count++;
        return count;
    }

    public int diff() {
        //计算棋子分差
        return getCount(usercolor) - getCount(oppocolor);
    }

    public class PlayHandler extends Thread {
        
        //游戏信息接收处理线程
        public void run() {
            String line = "";
            //判断是否在游戏中
            while (playing) {
                line = getMsg();
                //玩家进入房间
                if (line.equals("come")) {
                    opponame = getMsg();
                    text_playinfo.append(opponame + "进入了房间。\n");
                    label_p2name.setText(opponame);
                    if (usercolor == CHESS_BLACK) {
                        text_playinfo.append(opponame + ": 开始游戏，执白后行。\n");
                        userturn();
                    }
                    else {
                        text_playinfo.append(opponame + ": 开始游戏，执黑先行。\n");
                        oppoturn();
                    }
                }
                //玩家退出房间
                else if (line.equals("out")) {
                    text_playinfo.append(opponame + "离开了房间。\n");
                    opponame = "";
                    initboard();
                    panel_othello.repaint();
                    waitingstatus();
                }
                //接收聊天信息
                else if (line.equals("chat")) {
                    String msg = getMsg();
                    text_getmsg.append(opponame + ": " + msg + "\n");
                }
                //对手落子信息
                else if (line.equals("play")) {
                    int x = Integer.valueOf(getMsg()).intValue();
                    int y = Integer.valueOf(getMsg()).intValue();
                    placepiece(x, y, oppocolor);
                    panel_othello.repaint();
                    text_playinfo.append(opponame + ": (" + (x + 1) + ", " + (y + 1) + ")\n");
                    userturn();
                }
                //对手认输
                else if (line.equals("giveup")) {
                    text_playinfo.append(opponame + ": 认输。\n");
                    endstatus(usercolor);
                    JOptionPane.showMessageDialog(null, "对方认输！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
                }
                //对手重新开始
                else if (line.equals("restart")) {
                    initboard();
                    panel_othello.repaint();
                    if (usercolor == CHESS_BLACK) {
                        text_playinfo.append(opponame + ": 重新开始，执白后行。\n");
                        userturn();
                    }
                    else {
                        text_playinfo.append(opponame + ": 重新开始，执黑先行。\n");
                        oppoturn();
                    }
                    JOptionPane.showMessageDialog(null, "重新开始！", "黑白棋", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        
    }

}
