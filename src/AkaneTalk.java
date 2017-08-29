import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

//あかねと〜く(本体)
public final class AkaneTalk extends MIDlet
    implements Runnable {
    static AkaneTalk a;
    static AkaneTalkCanvas b;

    //コンストラクタ
    public AkaneTalk() {
        a=this;
        b=new AkaneTalkCanvas();
        Display.getDisplay(this).setCurrent(b);
        (new Thread(this)).start();
        (new Thread(b)).start();
    }

    //アプリの開始
    public void startApp() {
    }

    //アプリの一時停止
    public void pauseApp() {
    }

    //アプリの終了
    public void destroyApp(boolean unconditional) {
    }

    //ラン
    public void run() {
        b.exe();
    }
}
