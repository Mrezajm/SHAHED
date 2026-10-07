package com.mreza.shahd;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(new DroneGame(this));
    }
}

class DroneGame extends View {
    Bitmap world;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    float dx=0, dy=0, targetX=0, targetY=0;
    float joyX, joyY;
    boolean joyActive=false, locked=false;
    long start;
    Random rnd = new Random(4);
    ArrayList<float[]> sparks = new ArrayList<>();

    DroneGame(Context c) {
        super(c);
        world = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(
            "world","drawable",c.getPackageName()));
        start=System.currentTimeMillis();
        for(int i=0;i<35;i++) sparks.add(new float[]{rnd.nextFloat(),rnd.nextFloat()});
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
    }

    protected void onDraw(Canvas c) {
        int w=getWidth(), h=getHeight();
        float t=(System.currentTimeMillis()-start)/1000f;
        // Slow camera movement creates a living flight effect.
        float cx=(float)Math.sin(t*0.18)*18 + dx;
        float cy=(float)Math.cos(t*0.13)*8 + dy;

        Rect src=new Rect(0,0,world.getWidth(),world.getHeight());
        RectF dst=new RectF(-cx,-cy,w+cx,h+cy);
        p.setAlpha(255); c.drawBitmap(world,src,dst,p);

        // dark cinematic vignette
        LinearGradient shade=new LinearGradient(0,0,0,h,
            Color.argb(120,0,0,0), Color.argb(15,0,0,0), Shader.TileMode.CLAMP);
        p.setShader(shade); c.drawRect(0,0,w,h,p); p.setShader(null);

        drawHUD(c,w,h,t);
        drawDrone(c,w,h,t);
        drawJoystick(c,w,h);
        invalidate();
    }

    void txt(Canvas c,String s,float x,float y,float size,int color){
        p.setTextSize(size); p.setColor(color); p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.create("sans",Typeface.BOLD));
        c.drawText(s,x,y,p);
    }

    void panel(Canvas c,float l,float top,float r,float bot){
        p.setColor(Color.argb(150,4,18,22)); p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(l,top,r,bot),18,18,p);
        p.setColor(Color.argb(180,0,229,255)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
        c.drawRoundRect(new RectF(l,top,r,bot),18,18,p);
        p.setStyle(Paint.Style.FILL);
    }

    void drawHUD(Canvas c,int w,int h,float t){
        int cyan=Color.rgb(0,235,255), white=Color.WHITE;
        txt(c,"پهپاد شاهد",28,48,27,white);
        txt(c,"SHAHED DRONE",30,72,13,cyan);

        // compass
        txt(c,"N       NE          E          SE          S",w/2-170,38,14,white);
        p.setColor(cyan); c.drawRect(w/2,22,w/2+3,53,p);

        panel(c,20,92,305,235);
        txt(c,"اهداف مأموریت",42,125,18,cyan);
        txt(c,"● شناسایی منطقه        0/1",42,158,14,white);
        txt(c,"● اهداف زمینی          0/5",42,188,14,white);
        txt(c,"● خروج از منطقه        0/1",42,218,14,white);

        panel(c,w-280,82,w-20,170);
        txt(c,"● مأموریت فعال",w-250,113,17,cyan);
        txt(c,"نقطه هدف را پیدا کنید",w-250,145,13,white);

        // reticle
        float rx=w/2, ry=h/2-20;
        p.setColor(cyan); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
        c.drawLine(rx-55,ry-30,rx-25,ry-30,p); c.drawLine(rx-55,ry-30,rx-55,ry,p);
        c.drawLine(rx+55,ry-30,rx+25,ry-30,p); c.drawLine(rx+55,ry-30,rx+55,ry,p);
        c.drawLine(rx-55,ry+30,rx-25,ry+30,p); c.drawLine(rx-55,ry+30,rx-55,ry,p);
        c.drawLine(rx+55,ry+30,rx+25,ry+30,p); c.drawLine(rx+55,ry+30,rx+55,ry,p);
        c.drawCircle(rx,ry,4,p);
        p.setStyle(Paint.Style.FILL);
        txt(c, locked ? "LOCK  TARGET" : "842 m",rx-38,ry+62,16,cyan);

        // radar
        float rad=75, x=105, y=h-95;
        p.setColor(Color.argb(80,0,235,255)); c.drawCircle(x,y,rad,p);
        p.setColor(cyan); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
        c.drawCircle(x,y,rad,p); c.drawCircle(x,y,rad*0.55f,p); c.drawLine(x-rad,y,x+rad,y,p); c.drawLine(x,y-rad,x,y+rad,p);
        p.setStyle(Paint.Style.FILL); c.drawCircle(x+22,y-14,5,p);

        panel(c,190,h-120,440,h-25);
        txt(c,"ALT",215,h-88,14,white); txt(c,"320 m",330,h-88,14,cyan);
        txt(c,"SPD",215,h-61,14,white); txt(c,"220 km/h",300,h-61,14,cyan);
        txt(c,"BAT",215,h-34,14,white); txt(c,"85%",330,h-34,14,cyan);

        panel(c,w-365,h-120,w-35,h-25);
        txt(c,"موشک",w-330,h-85,14,white); txt(c,"4 / 4",w-90,h-85,14,cyan);
        txt(c,"مهمات",w-330,h-55,14,white); txt(c,"2 / 2",w-90,h-55,14,cyan);

        txt(c,"دقت  |  قدرت  |  پیروزی",35,h-8,16,white);
        if(locked) txt(c,"TARGET LOCKED",w/2-72,92,15,Color.RED);
    }

    void drawDrone(Canvas c,int w,int h,float t){
        float x=w/2 + (joyActive?joyX*70:0), y=h-120 + (joyActive?joyY*35:0);
        p.setColor(Color.argb(190,0,0,0)); c.drawOval(new RectF(x-180,y+20,x+180,y+55),p);
        Path d=new Path();
        d.moveTo(x,y-38); d.lineTo(x+150,y+4); d.lineTo(x+45,y+13);
        d.lineTo(x+22,y+32); d.lineTo(x,y+18); d.lineTo(x-22,y+32);
        d.lineTo(x-45,y+13); d.lineTo(x-150,y+4); d.close();
        p.setColor(Color.rgb(155,165,168)); c.drawPath(d,p);
        p.setColor(Color.rgb(55,60,63)); c.drawOval(new RectF(x-20,y-42,x+20,y+25),p);
        p.setColor(cyan()); c.drawCircle(x,y-17,5,p);
    }
    int cyan(){return Color.rgb(0,235,255);}

    void drawJoystick(Canvas c,int w,int h){
        float x=100,y=h-95,r=68;
        if(joyActive){x=100; y=h-95;}
        p.setColor(Color.argb(70,0,235,255)); c.drawCircle(x,y,r,p);
        p.setColor(cyan()); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); c.drawCircle(x,y,r,p); p.setStyle(Paint.Style.FILL);
        c.drawCircle(x+joyX*35,y+joyY*35,22,p);
    }

    public boolean onTouchEvent(android.view.MotionEvent e){
        float x=e.getX(), y=e.getY();
        int w=getWidth(), h=getHeight();
        if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE){
            if(x<210 && y>h-180){
                joyActive=true;
                joyX=Math.max(-1,Math.min(1,(x-100)/70));
                joyY=Math.max(-1,Math.min(1,(y-(h-95))/70));
                dx-=joyX*1.4f; dy-=joyY*0.7f;
            } else if(x>w-220 && y>h-190){
                locked=!locked;
            }
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP){
            joyActive=false; joyX=joyY=0; return true;
        }
        return true;
    }
}
