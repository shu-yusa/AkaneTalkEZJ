import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.rms.*;
import javax.microedition.lcdui.*;
//あかねと〜く(キャンバス)
final class AkaneTalkCanvas extends Canvas
    implements Runnable,CommandListener {
    //デバイス・乱数・暦
    //--------------------
    private Random   rand=new Random();          //乱数
    private Calendar cal =Calendar.getInstance();//暦

    //イメージ・文字列
    //--------------------
    private Image[]  charImg=new Image[4]; //キャライメージ
    private Image[]  charBuf=new Image[4]; //キャラバッファ
    private Image[]  resImg =new Image[10];//リソースイメージ
    private String[] resStr;               //リソース文字列

    //定数
    //--------------------
    //サイズ
    //--------------------
    private static final int WIDTH0  = 120;
    private static final int HEIGHT0 = 165;
    private static final int WIDTH  = 480;
    private static final int HEIGHT = 696;
    private static final int SCALE  = WIDTH/WIDTH0;
    //フォント
    //--------------------
    private static final Font FONT_TYPE = Font.getFont(Font.FACE_MONOSPACE,Font.STYLE_PLAIN,Font.SIZE_LARGE);

    //スリープ
    //--------------------
    private final static int SLEEP = 80;
    //共通
    //--------------------
    private static final int EVENT_NONE =  -222;
    private static final int SOFT1 = 1001;
    private static final int SOFT2 = 1002;
    private static final int L_T = Graphics.LEFT|Graphics.TOP;
    //--------------------
    //シーン
    //--------------------
    private final static int S_ERROR   = -2;
    private final static int S_END     = -1;
    private final static int S_NONE    = 0;
    private final static int S_READY   = 1;
    private final static int S_TITLE   = 2;
    private final static int S_COMMAND = 3;
    private final static int S_TALK    = 4;
    private final static int S_BUTTON  = 5;
    private final static int S_SELECT  = 6;
    private final static int S_KAISOU  = 7;

    //処理
    //--------------------
    private final static String M_INIT  = "i";
    private final static String M_STATE = "s";
    private final static String M_AKANE = "a";

    //コントロール
    //--------------------
    private final static int C_NONE    = 0; //なし
    private final static int C_VIBRATE = 1; //バイブレーション
    private final static int C_READY0  = 2; //タスクバー開始
    private final static int C_READY1  = 3; //タスクバー終了
    private final static int C_ACCESS  = 4; //通信

    //システム
    //--------------------
    private int     scene;  //シーン
    private int     init=-1;//初期化
    private int     tick;   //時間経過
    private int     tick2=0;   //時間経過2
    private int     event;  //イベント
    private int     control;//コントロール
    private int     sleep;  //スリープ
    private int     key;    //キー
    private int     mode;   //モード
    private boolean pause;  //ポーズ

    //コマンド
    //--------------------
    private int      charPos;//キャラ位置
    private String[] cmd;    //コマンド
    private String   cmdCash;//コマンドキャッシュ
    private int      cmdPos; //コマンド位置
    private String[] talk;   //トーク
    private int      talkPos;//トーク位置
    private int      talkNum;//トーク数

    //ステータス
    //--------------------
    private int    akaneType; //茜タイプ
    private int    akaneTypeN;//茜タイプ(次)
    private int    friend;    //友好度
    private int    friendN;   //友好度(次)
    private String urlN;      //URL(次)

    //バッファ
    //--------------------
    private Image    imgOff;//イメージ
    private Graphics graOff;//グラフィクス

    //コマンド
    //--------------------
    private Command cmdMode =new Command("じかん",Command.SCREEN,0);
    private Command cmdKaiso=new Command("思いで",Command.SCREEN,1);
    private Command cmdPrev =new Command("戻る",Command.SCREEN,2);

    private String fe = "";


//====================
//実行
//====================
    void exe() {
        manage(M_INIT,true);
        while (scene>=0) {
            try {
                //描画
                //--------------------
                repaint();

                //通信
                //--------------------
                if (urlN!=null) {
                    manage(urlN,true);
                    init=S_COMMAND;
                    urlN=null;
                }
                //茜画像読み込み
                //--------------------
                else if (akaneTypeN>=0) {
                    if (akaneTypeN!=akaneType) manage(M_AKANE,true);
                    init      =S_TALK;
                    akaneTypeN=-1;
                }
                //トーク待ち
                //--------------------
                else if (scene==S_TALK && init<0 && talkPos>=60) {
                    for (int i=45;i>=0;i--) {
                        Thread.sleep(100);
                        if (event==FIRE  || event==SOFT1 || 
                            event==SOFT2 || pause) {
                          break;
                        }
                    }
                    event=EVENT_NONE;
                    init   =S_TALK;
                    talkPos=0;
                }
                //スリープ
                //--------------------
                else {
                    Thread.sleep(sleep);
                    sleep=SLEEP;
                }
            } catch (Exception e) {
            }
        }
        //アプリ終了
        //--------------------
        if (scene==S_ERROR) {
            repaint();
            event=EVENT_NONE;
            while (event!=FIRE) {
              Thread.yield();
            }
        }
        AkaneTalk.a.destroyApp(false);
        AkaneTalk.a.notifyDestroyed();
    }


    //描画
    public synchronized void paint(Graphics g) {
//====================
//エラー
//====================
        if (scene==S_ERROR) {
            control=C_NONE;
            graOff.setColor((255<<16)+(255<<8)+255);
            graOff.fillRect(0,0,WIDTH,HEIGHT);
            graOff.setColor((0<<255)+(100<<255)+150);
            graOff.drawString("通信失敗",15,48,L_T);
            g.drawImage(imgOff,0,0,L_T);
            return;
        }
//====================
//通信
//====================
        else if (control>=C_READY0) {
            g.drawImage(imgOff,0,0,L_T);
        }
//====================
//非表示
//====================
        if (scene<=S_NONE || urlN!=null ||
            akaneTypeN>=0 || talkPos>=60) return;
        int i,j;
        String[] strs;
//====================
//初期化
//====================
        if (init>=0) {
            scene=init;
            //ボタン・選択肢
            //--------------------
            if (scene==S_BUTTON || scene==S_SELECT) {
                if (charPos<=3) {
                    graOff.drawImage(charImg[charPos],0,0,L_T);
                } else {
                    graOff.setColor(charPos-10);
                    graOff.fillRect(0,0,WIDTH,HEIGHT);
                }
                drawMode();
                if (friendN>=30 && scene==S_BUTTON) {
                    removeCommand(cmdPrev);
                    addCommand(cmdKaiso);
                }
            }
            //コマンド
            //--------------------
            if (scene==S_COMMAND) {
                charPos=0;
                cmdPos =0;
                scene  =S_TALK;
            }
            //トーク
            //--------------------
            if (scene==S_TALK) {
                cmdPos++;
                //最終行
                //--------------------
                if (cmdPos==cmd.length-1) {
                    //ボタン
                    if (cmd[cmdPos].equals("b")) {
                        charPos=0;
                        init   =S_BUTTON;
                    }
                    //終了
                    else if (cmd[cmdPos].equals("e")) {
                        scene=S_END;
                    }
                    //ジャンプ
                    else if (cmd[cmdPos].startsWith("j")) {
                        urlN="talk/"+cmd[cmdPos].substring(1);
                    }
                    //選択肢
                    else {
                        init=S_SELECT;
                        talk=parseString(cmd[cmdPos],'\t');
                        for (i=1;i<talk.length;i+=2) {
                            talk[i]="talk/"+talk[i];
                        }
                    }
                    return;
                }
                //イベント画像表示
                //--------------------
                else if (cmd[cmdPos].startsWith("#i")) {
                    charPos=Integer.parseInt(cmd[cmdPos].substring(2));
                    init=S_TALK;
                    return;
                }
                //塗り潰し
                //--------------------
                else if (cmd[cmdPos].startsWith("#c")) {
                    charPos=10+Integer.parseInt(cmd[cmdPos].substring(2,8),16);
                    init=S_TALK;
                    return;
                }
                //茜画像変更
                //--------------------
                else if (cmd[cmdPos].startsWith("#a")) {
                    strs=parseString(cmd[cmdPos],'\t');
                    akaneTypeN=Integer.parseInt(strs[0].substring(2));

                    //描画
                    if (akaneTypeN!=akaneType) {
                        if (charPos<=3) {
                            graOff.drawImage(charImg[charPos],0,0,L_T);
                        } else {
                            graOff.setColor(charPos-10);
                            graOff.fillRect(0,0,WIDTH,HEIGHT);
                        }
                        drawBox((HEIGHT0-49)*SCALE,189);
                        graOff.drawString(strs[1],5*SCALE,(HEIGHT0-46)*SCALE,L_T);
                        graOff.drawImage(resImg[8],((WIDTH0-97)*SCALE)/2,(HEIGHT0-20)*SCALE,L_T);
                        graOff.setColor((255<<16)+(204<<8)+204);
                        graOff.fillRect(10*SCALE,(HEIGHT0-7)*SCALE,100*SCALE,2*SCALE);
                        g.drawImage(imgOff,0,0,L_T);
                    }
                    return;
                }
                //友好度変更
                //--------------------
                else if (cmd[cmdPos].startsWith("#f")) {
                    friend+=Integer.parseInt(cmd[cmdPos].substring(2));
                    if (friend>=100) friend=100;
                    if (friend<0)    friend=0;
                    manage(M_STATE,false);
                    init=S_TALK;
                    return;
                }
                //友好度ジャンプ
                //--------------------
                else if (cmd[cmdPos].startsWith("#j")) {
                    init=S_TALK;
                    strs=parseString(cmd[cmdPos],'\t');
                    if (Integer.parseInt(strs[0].substring(2))<=friend) {
                        urlN="talk/"+strs[1];
                    }
                    return;
                }
                //バイブレーション
                //--------------------
                else if (cmd[cmdPos].startsWith("#v")) {
                    control=C_VIBRATE;
                    init   =S_TALK;
                    return;
                }
                //描画
                //--------------------
                if (charPos<=3) {
                  if (fe.startsWith("fe8_b") || fe.startsWith("fe8_c")) {
                    graOff.setColor(255,255,255);
                    graOff.fillRect(0,0,WIDTH,HEIGHT);
                  }
                  else if (fe.startsWith("fe0")) {
                  }
                  else if (fe.startsWith("fe")) {
                    graOff.setColor(0,0,0);
                    graOff.fillRect(0,0,WIDTH,HEIGHT);
                  }
                  else if (fe.startsWith("e9")) {
                    graOff.setColor(0,0,0);
                    graOff.fillRect(0,45,WIDTH,115);
                    graOff.fillRect(0,444,WIDTH,HEIGHT);
                  }
                  graOff.drawImage(charImg[charPos],0,0,L_T);
                } else {
                    graOff.setColor(charPos-10);
                    graOff.fillRect(0,0,WIDTH,HEIGHT);
                }
                drawMode();

                //トーク
                //--------------------
                talk   =parseString(cmd[cmdPos],'\t');
                talkPos=0;
                graOff.setColor((255<<16)+(255<<8)+255);
                g.drawImage(imgOff,0,0,L_T);
            }
//====================
//回想モード
//====================
            if (scene==S_KAISOU) {
                j=(friendN-30)/10;
                if (friendN<=50) j=friendN/20;
                talk=new String[j*2];
                for (i=0;i<j;i++) {
                    talk[i*2]  =resStr[i+15];
                    talk[i*2+1]="friend/"+(i+1);
                }
                removeCommand(cmdKaiso);
                addCommand(cmdPrev);
            }
            //後処理
            //--------------------
            event=EVENT_NONE;
            key  =0;
            tick =0;
            init =-1;
            return;
        }

//====================
//タイトル
//====================
        if (scene==S_TITLE) {
            //描画
            //--------------------
            graOff.setColor((255<<16)+(255<<8)+255);
            graOff.fillRect(0,0,WIDTH,HEIGHT);
            graOff.drawImage(resImg[2],(WIDTH-374)/2,40,L_T);
            graOff.drawImage(resImg[9],0,HEIGHT-466,L_T);
            if (tick%16<8) graOff.drawImage(resImg[3],7*SCALE,(HEIGHT0-12)*SCALE,L_T);

            //操作
            //--------------------
            if (event==FIRE) {
                graOff.setColor((255<<16)+(255<<8)+255);
                graOff.fillRect(0,0,WIDTH,HEIGHT);
                addCommand(cmdMode);
                mode  =0;
                init  =S_COMMAND;
                resImg[9]=null;
                for (i=0;i<=3;i++) resImg[i]=null;
            }
            if (++tick>999) tick=0;
        }

//====================
//キーイベント
//====================
        if (event>=0 && scene!=S_TITLE) {
            //ポーズ
            //--------------------
            if (pause) {
                pause=false;
            } else if (event==KEY_NUM0) {
                pause=true;
            }
            //ボタン
            //--------------------
            else if (scene==S_BUTTON) {
                if (event==LEFT && key==1) {
                    key=0;
                } else if (event==RIGHT && key==0) {
                    key=1;
                } else if (event==FIRE) {
                    if (key==0) {
                        //友好度イベント
                        //--------------------
                        if (friend>=friendN) {
                            if (friendN==10 || friendN==30) friendN+=10;
                            friendN+=10;
                            i=(friendN-30)/10;
                            if (friendN<=50) i=friendN/20;
                            manage(M_STATE,false);
                            urlN="friend/"+i;
                        }
                        //トークイベント
                        //--------------------
                        else {
                            urlN="talk/"+((rand.nextInt()>>>1)%talkNum);
                        }
                    } else {
                        cmd=new String[]{"",resStr[resStr.length-1],"e"};
                        init=S_COMMAND;
                    }
                    return;
                }
                //ifdef emu
                else if (event==KEY_NUM1) {
                    urlN="test";
                }
                //endif
            }
            //選択肢
            //--------------------
            else if (scene==S_SELECT || scene==S_KAISOU) {
                j=event;
                if (event==UP && key>0) {
                    key--;
                    tick2 = 0;
                } else if (event==DOWN && key<talk.length/2-1) {
                    key++;
                    tick2 = 0;
                } else if (event==FIRE) {
                    urlN=talk[key*2+1];
                    return;
                }
            }
            //トーク
            //--------------------
            if (event==FIRE) {
              if (tick==0) talkPos=60;
            }

            //後処理
            //--------------------
            tick =0;
            event=EVENT_NONE;
        }

//====================
//ボタン・トーク
//====================
        if (scene==S_TALK || (scene>=S_BUTTON && tick%50==0)) {
            //茜
            //--------------------
            if (charPos>3) {
                graOff.setColor(charPos-10);
                graOff.fillRect(0,0,WIDTH,HEIGHT);
                graOff.setColor((255<<16)+(255<<8)+255);
            } else if (scene!=S_TALK || pause || tick<1) {
                graOff.drawImage(charImg[charPos],0,0,L_T);
            }
            if (!pause) {
                //ボタン
                //--------------------
                drawMode();
                if (scene==S_BUTTON) {
//                  graOff.drawImage(resImg[5+key],3,HEIGHT-46,L_T);
                    if (key == 0) {
                      graOff.drawImage(resImg[5],5,
                          (HEIGHT0-15)*SCALE+(int)(5*Math.sin((double)tick2++/3)),L_T);
                      graOff.drawImage(resImg[6],WIDTH-230-5,(HEIGHT0-15)*SCALE,L_T);
                    } else {
                      graOff.drawImage(resImg[5],5,(HEIGHT0-15)*SCALE,L_T);
                      graOff.drawImage(resImg[6],WIDTH-230-5,
                          (HEIGHT0-15)*SCALE+(int)(5*Math.sin((double)tick2++/3)),L_T);
                    }
                    if (tick2 == 999) tick2 = 0;
                }
                //選択肢
                //--------------------
                else if (scene==S_SELECT) {
                    drawBox((HEIGHT0-49)*SCALE,189);
                    graOff.setColor((255<<16)+(130<<8)+130);
                    graOff.fillRoundRect(5*SCALE,(HEIGHT0-45+key*13)*SCALE,
                        Math.min(talk[key*2].getBytes().length*6*SCALE,440),13*SCALE,5,5);
//                  graOff.fillRect(5*SCALE,(HEIGHT0-43+key*13)*SCALE,
//                      talk[key*2].getBytes().length*6*SCALE,13*SCALE);
                    graOff.setColor((255<<16)+(255<<8)+255);
                    for (i=0;i<talk.length/2;i++) {
                        graOff.drawString(talk[i*2],5*SCALE,(HEIGHT0-44+i*13)*SCALE,L_T);
                    }
                }
                //トーク
                //--------------------
                else if (scene==S_TALK) {
                    talkPos++;
                    drawBox((HEIGHT0-49)*SCALE,189);
                    if (talkPos<20) {
                        graOff.drawString(talk[0].substring(0,talkPos),
                            5*SCALE,(HEIGHT0-44)*SCALE,L_T);
                        if (talkPos>=talk[0].length()) {
                            talkPos=20;
                            if (talk.length<=1) talkPos=60;
                        }
                    } else if (talkPos<40) {
                        graOff.drawString(talk[0],5*SCALE,(HEIGHT0-44)*SCALE,L_T);
                        graOff.drawString(talk[1].substring(0,talkPos-20),
                            5*SCALE,(HEIGHT0-44+13)*SCALE,L_T);
                        if (talkPos>=talk[1].length()+20) {
                            talkPos=40;
                            if (talk.length<=2) talkPos=60;
                        }
                    } else if (talkPos<60) {
                        graOff.drawString(talk[0],5*SCALE,(HEIGHT0-44)*SCALE,L_T);
                        graOff.drawString(talk[1],5*SCALE,(HEIGHT0-44+13)*SCALE,L_T);
                        graOff.drawString(talk[2].substring(0,talkPos-40),
                            5*SCALE,(HEIGHT0-44+26)*SCALE,L_T);
                        if (talkPos>=talk[2].length()+40) {
                            talkPos=60;
                        }
                    } else {
                        graOff.drawString(talk[0],5*SCALE,(HEIGHT0-44)*SCALE,L_T);
                        if (talk.length>=2)
                            graOff.drawString(talk[1],5*SCALE,(HEIGHT0-44+13)*SCALE,L_T);
                        if (talk.length>=3)
                            graOff.drawString(talk[2],5*SCALE,(HEIGHT0-44+26)*SCALE,L_T);
                    }
                }

                //回想モード
                //--------------------
                else if (scene==S_KAISOU) {

                    /*
                    graOff.drawImage(resImg[7],3*SCALE,(HEIGHT0-118)*SCALE,L_T);
                    graOff.drawImage(resImg[7],3*SCALE,(HEIGHT0-46)*SCALE,L_T);
                    graOff.setColor((107<<16)+(109<<8)+255);
                    graOff.fillRect(3*SCALE,(HEIGHT0-110)*SCALE,114*SCALE,100*SCALE);

                    // left margin
                    graOff.setColor(105,194,253);
                    graOff.drawLine(3*SCALE,(HEIGHT0-110)*SCALE,3*SCALE,(HEIGHT0-10)*SCALE);
                    graOff.setColor(113,171,254);
                    graOff.drawLine(3*SCALE+1,(HEIGHT0-110)*SCALE,3*SCALE+1,(HEIGHT0-10)*SCALE);
                    graOff.setColor(103,155,255);
                    graOff.drawLine(3*SCALE+2,(HEIGHT0-110)*SCALE,3*SCALE+2,(HEIGHT0-10)*SCALE);
                    graOff.setColor(102,140,255);
                    graOff.drawLine(3*SCALE+3,(HEIGHT0-110)*SCALE,3*SCALE+3,(HEIGHT0-10)*SCALE);
                    graOff.setColor(110,125,254);
                    graOff.drawLine(3*SCALE+4,(HEIGHT0-110)*SCALE,3*SCALE+4,(HEIGHT0-10)*SCALE);

                    // right margin
                    graOff.setColor(105,194,253);
                    graOff.drawLine(3*SCALE+455,(HEIGHT0-110)*SCALE,3*SCALE+455,(HEIGHT0-10)*SCALE);
                    graOff.setColor(113,171,254);
                    graOff.drawLine(3*SCALE+454,(HEIGHT0-110)*SCALE,3*SCALE+454,(HEIGHT0-10)*SCALE);
                    graOff.setColor(103,155,255);
                    graOff.drawLine(3*SCALE+453,(HEIGHT0-110)*SCALE,3*SCALE+453,(HEIGHT0-10)*SCALE);
                    graOff.setColor(102,140,255);
                    graOff.drawLine(3*SCALE+452,(HEIGHT0-110)*SCALE,3*SCALE+452,(HEIGHT0-10)*SCALE);
                    graOff.setColor(110,125,254);
                    graOff.drawLine(3*SCALE+451,(HEIGHT0-110)*SCALE,3*SCALE+451,(HEIGHT0-10)*SCALE);

                    */

                    drawBox((HEIGHT0-118)*SCALE, 188+69*4);
                    graOff.setColor((255<<16)+(130<<8)+130);
//                  graOff.fillRoundRect(5*SCALE,(HEIGHT0-120+8+key*13)*SCALE,
//                      (int)(FONT_TYPE.stringWidth(talk[key*2])),13*SCALE,5,5);
                    graOff.fillRoundRect(5*SCALE,(HEIGHT0-120+7+key*13)*SCALE,
                        talk[key*2].getBytes().length*5,13*SCALE,5,5);
                        
                    graOff.setColor((255<<16)+(255<<8)+255);
                    for (i=0;i<talk.length/2;i++) {
                        graOff.drawString(talk[i*2],5*SCALE,(HEIGHT0-120+8+i*13)*SCALE,L_T);
                    }
                }
            }
        }

//====================
//表示
//====================
        g.drawImage(imgOff,0,0,L_T);
    }

    private void drawBox(int top, int h) {
      int color = graOff.getColor();
      graOff.setColor(105,194,253);
      graOff.fillRoundRect(3*SCALE,top,456,h,20,20);
      graOff.setColor(113,171,254);
      graOff.fillRoundRect(3*SCALE+1,top+1,454,h-2,20,20);
      graOff.setColor(103,155,255);
      graOff.fillRoundRect(3*SCALE+2,top+2,452,h-4,20,20);
      graOff.setColor(102,140,255);
      graOff.fillRoundRect(3*SCALE+3,top+3,450,h-6,20,20);
      graOff.setColor(110,125,254);
      graOff.fillRoundRect(3*SCALE+4,top+4,448,h-8,20,20);
      graOff.setColor(113,116,255);
      graOff.fillRoundRect(3*SCALE+5,top+5,446,h-10,20,20);
      graOff.setColor(107,110,254);
      graOff.fillRoundRect(3*SCALE+6,top+6,444,h-12,20,20);
      graOff.setColor(color);
    }

    //モードの描画
    private void drawMode() {
        String str0=c(friend,3);
        int posX = WIDTH-8-FONT_TYPE.stringWidth("000");
        int posY = 8 + FONT_TYPE.getHeight()/2;
        graOff.drawImage(resImg[7],posX-35-8,posY-30/2,L_T);
        graOff.setColor(240,114,113);
        graOff.drawString(str0,posX,posY-FONT_TYPE.getHeight()/2,L_T);
        graOff.setColor(255,255,255);

        if (mode!=0) {
            String str=null;

            //友好度
            //--------------------
            if (mode==0) {
//          if (mode==1) {
                str=c(friend,3);
            }

            //時分・年月日
            //--------------------
            else {
                cal=Calendar.getInstance();
//              if (mode==2) {
                if (mode==1) {
                    str=c(cal.get(Calendar.HOUR_OF_DAY),2)+":"+
                        c(cal.get(Calendar.MINUTE),2);
                } else {
                    str=cal.get(Calendar.YEAR)+"/"+
                        c(cal.get(Calendar.MONTH)+1,2)+"/"+
                        c(cal.get(Calendar.DAY_OF_MONTH),2);
                }
            }

            //表示
            //--------------------
            graOff.setColor((100<<16)+(100<<8)+100);
            graOff.drawString(str,5,2,L_T);
            graOff.setColor((255<<16)+(255<<8)+255);
            graOff.drawString(str,5,2,L_T);
        }
    }

    //処理
    private void manage(String type,boolean error) {
        int i,j;
        byte[]       data;
        String[]     str;
        StringBuffer sb;

        //ストリーム
        //--------------------
        RecordStore    rs=null;

        //コマンド
        //--------------------
        removeCommand(cmdKaiso);
        removeCommand(cmdPrev);
        try {
//====================
//初期化
//====================
            if (type.equals(M_INIT)) {
                //リスナ
                //--------------------
                sleep=SLEEP;
                setCommandListener(this);

                //バッファ
                //--------------------
                imgOff=Image.createImage(WIDTH,HEIGHT);
                graOff=imgOff.getGraphics();
                graOff.setColor((247<<16)+(162<<8)+181);
                graOff.fillRect(0,0,WIDTH,HEIGHT);
                graOff.setFont(FONT_TYPE);

                //リソースイメージ
                //--------------------
                for (i=9;i>=0;i--) {
//              for (i=10;i>=0;i--) {
                    resImg[i]=Image.createImage("/"+i+".png");
                }

                //ロード画面
                //--------------------
                graOff.setColor((255<<16)+(255<<8)+255);
                graOff.fillRect(0,0,WIDTH,HEIGHT);
                graOff.drawImage(resImg[0],((WIDTH0-57)*SCALE)/2,((HEIGHT0-32)*SCALE)/2,L_T);
                graOff.drawImage(resImg[1],((WIDTH0-89)*SCALE)/2,(HEIGHT0-20)*SCALE,L_T);
                graOff.setColor((255<<16)+(204<<8)+204);
                graOff.fillRect(10*SCALE,(HEIGHT0-7)*SCALE,100*SCALE,2*SCALE);

                //タスクバー開始
                //--------------------
                tick   =0;
                scene  =S_READY;
                control=C_READY0;

                //リソース文字列
                //--------------------

                int cc;
                char[] b = new char[70];
                String s;
                InputStreamReader isr = new InputStreamReader(
                      getClass().getResourceAsStream("/t.txt"),"SJIS");
                int k=0;
                int m=0;
                resStr = new String[24];
                while((cc=isr.read()) != -1) {
                  if ((b[k]=(char)cc) == '\n') {
                    for (int l=k+1; l<b.length; l++) b[l] = '\u0000';
                    resStr[m++] = new String(b);
                    k = -1;
                  }
                  k++;
                }

                //データベース生成
                //--------------------
                rs=RecordStore.openRecordStore("akane",true);
                if (rs.getNumRecords()==0) {
                    rs.addRecord(new byte[]{0,0,0},0,3);
                    rs.addRecord(new byte[]{0},0,1);
                }
                rs.closeRecordStore();

                //データベース読み込み
                //--------------------
                rs=RecordStore.openRecordStore("akane",false);
                data=rs.getRecord(1);
                akaneType =data[0] & 0xFF;
                friend    =data[1] & 0xFF;
                friendN   =data[2] & 0xFF;

                if (akaneType!=0) {
                    try {
                        data=rs.getRecord(2);
                        charImg[0]=Image.createImage(data,0,data.length);
                    } catch (Exception e2) {
                        akaneType=0;
                    }
                }
                rs.closeRecordStore();
                if (akaneType==0) charImg[0]=Image.createImage("/data/image/a2.png");
                akaneTypeN=-1;

                //初期化スクリプト
                //--------------------
                talkNum=Integer.parseInt(readText("/data/init.txt"));

                //はじめましてイベント
                //--------------------
                cmd=null;
                if (friendN<=0) {
                    manage("friend/0",true);
                    friendN=10;
                    manage(M_STATE,false);
                }
                //カレンダーイベント
                //--------------------
                else {
                    manage("cal/"+c(cal.get(Calendar.MONTH)+1,2)+
                        c(cal.get(Calendar.DAY_OF_MONTH),2),false);
                }

                //時間イベント
                //--------------------
                if (cmd==null) {
                    i=cal.get(Calendar.HOUR_OF_DAY);
                    j=13;
                    if (i<=3 || 18<=i) {
                        j=14;
                    } else if (i<=11) {
                        j=12;
                    }
                    cmd=new String[]{"",resStr[j],resStr[i/2],"b"};
                }

                //タスクバー終了
                //--------------------
                control=C_READY1;
                while (control!=C_NONE) Thread.sleep(100);
                init=S_TITLE;
            }

//====================
//茜画像読み込み
//====================
            else if (type.equals(M_AKANE)) {
                //タスクバー開始
                //--------------------
                tick   =0;
                scene  =S_READY;
                control=C_READY0;

                //ネット→データベース
                //--------------------
                if (akaneTypeN>0) {
                    InputStream isr = getClass().getResourceAsStream
                                     ("/data/"+"image/a"+akaneTypeN+".png");
                    data = new byte[113400];  //approximate size of a3.png
                    isr.read(data);
                    isr.close();
                    rs=RecordStore.openRecordStore("akane",true);
                    rs.setRecord(2,data,0,data.length);
                    rs.closeRecordStore();
                }
                akaneType=akaneTypeN;
                manage(M_STATE,false);

                //データベース→イメージ
                //--------------------
                if (akaneType!=0) {
                    rs=RecordStore.openRecordStore("akane",false);
                    data=rs.getRecord(2);
                    rs.closeRecordStore();
                    try {
                        charImg[0]=Image.createImage(data,0,data.length);
                    } catch (Exception e2) {
                        akaneType=0;
                    }
                }
                //リソース→イメージ
                //--------------------
                else {
                    charImg[0]=Image.createImage("/data/image/a2.png");
                }

                //タスクバー終了
                //--------------------
                control=C_READY1;
                while (control!=C_NONE) Thread.sleep(100);
            }

//====================
//ステータス書き込み
//====================
            else if (type.equals(M_STATE)) {
                rs=RecordStore.openRecordStore("akane",true);
                data=new byte[]{(byte)akaneType,(byte)friend,(byte)friendN};
                rs.setRecord(1,data,0,3);
                rs.closeRecordStore();
            }

//====================
//スクリプト読み込み
//====================
            else {
                //イメージ
                //--------------------
/*
   error occures in real mobile phone if the size of charImg copied to charBuf is 
   greater than 1. momery problem ?
                for (i=1;i<4;i++) charBuf[i]=charImg[i];
*/
                if (control<=C_VIBRATE) control=C_ACCESS;

                //テキスト読み込み
                //--------------------
                String s=readText("/data/"+type+".txt");
                if (s != "NoFile") {
                  if (!type.equals("init") && s.trim().length()<=4) {
                    return;
                  }
                  cmd=parseString(s,'\n');

                  //画像ファイル
                  //--------------------
                  if (cmd.length>=3 && !cmd[0].trim().equals("") &&
                      !cmd[0].equals(cmdCash)) {
                      cmdCash=cmd[0];
                      str=parseString(cmdCash,'\t');
                      fe = "";
                      for (i=0;i<3;i++) {
                          charImg[i+1]=null;
                          if (i<str.length) {
                              fe = str[i];
                              charImg[i+1]=readImage("/data/"+"image/"+str[i]+".png");
                          }
                      }
                  }
                  control=C_NONE;
                }
            }

//====================
//例外処理
//====================
        } catch (Exception e) {
             try {
                //切断
                //--------------------
                if (rs!=null) rs.closeRecordStore();

                //エラー表示
                //--------------------
                e.printStackTrace();
                if (error) scene=S_ERROR;
            } catch (Exception e2) {
              e2.printStackTrace();
            }
        }
    }

    //コントロール
    public void run() {
        while (scene>=0) {
            try {
                //バイブレーション
                //--------------------
                if (control==C_VIBRATE) {
                    Display.getDisplay(AkaneTalk.a).vibrate(1000);
                    control=C_NONE;
                }
                //タスクバー開始
                //--------------------
                if (control==C_READY0) {
                    if (tick<98) tick++;
                    graOff.setColor((255<<16)+(153<<8)+153);
                    graOff.fillRect(10*SCALE,(HEIGHT0-7)*SCALE,tick*SCALE,2*SCALE);
                    repaint();
                }
                //タスクバー終了
                //--------------------
                else if (control==C_READY1) {
                    tick+=30;
                    if (tick>=100) tick=100;
                    graOff.setColor((255<<16)+(153<<8)+153);
                    graOff.fillRect(10*SCALE,(HEIGHT0-7)*SCALE,tick*SCALE,2*SCALE);
                    repaint();
                    if (tick==100) control=C_NONE;
                }
                //通信中
                //--------------------
                else if (control==C_ACCESS) {
                    if (charPos==0) {
                        graOff.drawImage(charImg[0],0,0,L_T);
                    } else if (charPos<=3) {
//                      graOff.drawImage(charBuf[charPos],0,0,L_T); // to avoid the error
                    }                                               // mentioned above
                    graOff.drawImage(charImg[2],0,0,L_T);
                    if (tick%2==0) graOff.drawImage(resImg[4],2,2,L_T);
                    if (++tick>999) tick=0;
                    repaint();
                }
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
    }


//====================
//イベント
//====================
    //キープレスイベント
    protected void keyPressed(int keyCode) {
        if (scene==S_BUTTON && keyCode==KEY_NUM1) {
            event=KEY_NUM1;
        } else
        if (keyCode==KEY_NUM0 && control==C_NONE && !pause) {
            if (scene==S_BUTTON) {
                event=KEY_NUM0;
            } else if (scene==S_TALK || scene==S_SELECT) {
                pause=true;
            }
        } else {
            event=getGameAction(keyCode);
        }
    }

    //キーリリース
    protected void keyReleased(int keyCode) {
    }

    //コマンドイベント
    public void commandAction(Command c,Displayable s) {
        if (c==cmdMode) {
            event=SOFT1;
//          if (mode==3) {
            if (mode==2) {
                mode=0;
            } else {
                mode++;
            }
            tick=0;
        } else if (c==cmdKaiso) {
            event=SOFT2;
            init=S_KAISOU;
        } else if (c==cmdPrev) {
            event=SOFT2;
            init=S_BUTTON;
        }
    }


//====================
//ユーティリティ
//====================
    private String readText(String url) throws Exception {
        InputStream in=getClass().getResourceAsStream(url);
        if (in == null) {
          return "NoFile";
        }
        byte[] data=new byte[2000]; // approximate size of 21a.txt(in utf-8)
        data = new byte[in.read(data)];
        in.close();

        in=getClass().getResourceAsStream(url); //resize array
        in.read(data);
        in.close();
        return new String(data,"SJIS");
    }

    //ネットからイメージを読み込む
    private static Image readImage(String url) throws Exception {
        return Image.createImage(url);
    }

/*
    //ネットからバイナリを読み込む
    private static byte[] readBinary(String url) throws Exception {
        System.out.println("readBinary>>"+url);
        int    i;
        byte[] data;
        HttpConnection c =null;
        InputStream    in=null;
        try {
            //接続
            //--------------------
            System.gc();
            c=(HttpConnection)Connector.open(url);
            i=c.getResponseCode();
            if ((i<200)||(i>=300)) throw new Exception("");
            in=c.openInputStream();

            //読み込む
            //--------------------
            data=new byte[(int)c.getLength()];
            for (i=0;i<data.length;i++) data[i]=(byte)in.read();

            //切断
            //--------------------
            in.close();
            c.close();
            return data;
        } catch (Exception e) {
            //例外
            //--------------------
            try {
                if (in!=null) in.close();
                if (c !=null) c.close();
            } catch (Exception e2) {
            }
        }
        throw new Exception();
    }
*/

    //文字列を任意の文字で分割
    private static String[] parseString(String str,char sep) {
        int i,j,size=0;
        String[] result;
        if (str.equals("")||str.charAt(str.length()-1)!=sep) str+=sep;
        StringBuffer sb=new StringBuffer();
        for (i=0;i<str.length();i++) {
            if (str.charAt(i)!='\r') sb.append(str.charAt(i));
        }
        str=sb.toString();
        i=str.indexOf(sep);
        while (i>=0) {
            size++;
            i=str.indexOf(sep,i+1);
        }
        result=new String[size];
        size=0;
        j=0;
        i=str.indexOf(sep);
        while (i>=0) {
            result[size++]=str.substring(j,i);
            j=i+1;
            i=str.indexOf(sep,j);
        }
        return result;
    }

    //文字長さ補正
    private static String c(int num,int len) {
        String str=String.valueOf(num);
        while (str.length()<len) str="0"+str;
        return str;
    }
}
