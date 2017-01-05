# Othello
 Java课程大作业——在线黑白棋

##简介
 提供一个在线多人联机下黑白棋的平台，游戏规则与黑白棋规则一致

##实现功能
 * 用户的注册和登录
 * 通过游戏大厅寻找玩家并进入房间
 * 黑白棋按照基本游戏规则编写，实时对战，具体规则可以参考网上资料
 * 实现游戏过程中聊天功能
 * 记录游戏相关的用户操作
 * 游戏状态和基本信息浏览
 * 服务端实现所有用户操作的记录

##有待改进
 * 界面太丑
 * 游戏房间内的功能不够齐全
 * 还有很多很多...

##服务端需求
 * 服务端需要安装Java运行环境和MySQL
 * 数据库需要提前建表，数据库名称为othello，所需表格式如下
 * player
 
 | Field    | Type        | Null | Key | Default | Extra |
 |----------|-------------|------|-----|---------|-------|
 | username | varchar(20) | NO   | PRI | NULL    |       |
 | pwdmd5   | char(32)    | YES  |     | NULL    |       |
 
 * log
 
 | Field     | Type        | Null | Key | Default | Extra |
 |-----------|-------------|------|-----|---------|-------|
 | username  | varchar(20) | YES  |     | NULL    |       |
 | operation | varchar(20) | YES  |     | NULL    |       |
 | optime    | datetime    | YES  |     | NULL    |       |

 * gameinfo
 
 | Field    | Type        | Null | Key | Default | Extra |
 |----------|-------------|------|-----|---------|-------|
 | roomnum  | int(1)      | YES  |     | NULL    |       |
 | username | varchar(20) | YES  |     | NULL    |       |
 | info     | varchar(50) | YES  |     | NULL    |       |
 | optime   | datetime    | YES  |     | NULL    |       |

 * chatrecord
 
 | Field    | Type         | Null | Key | Default | Extra |
 |----------|--------------|------|-----|---------|-------|
 | roomnum  | int(1)       | YES  |     | NULL    |       |
 | username | varchar(20)  | YES  |     | NULL    |       |
 | chat     | varchar(100) | YES  |     | NULL    |       |
 | optime   | datetime     | YES  |     | NULL    |       |


 
