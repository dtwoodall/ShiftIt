package com.newdawnproject.shiftit;

import java.io.IOException;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.widget.ImageButton;
import android.widget.TextView;

class ShiftView extends SurfaceView implements Callback {
	class ShiftThread extends Thread {
		private static final int COND_LOSE = 0;
		private static final int COND_RUN = 1;
		private static final int COND_READY = 2;
		
		private static final int FPS = 30;
		
		private static final int DIR_LEFT = 0;
		private static final int DIR_RIGHT = 1;
		private static final int DIR_UP = 2;
		private static final int DIR_DOWN = 3;
		private static final int DIR_NONE = 4;
		
		//private static final int ARROW_LEFT = 0;
		//private static final int ARROW_RIGHT = 1;
		//private static final int ARROW_UP = 2;
		//private static final int ARROW_DOWN = 3;
		//private static final int ARROW_X = 4;
		private static final int ARROW_NONE = 5;
		
		private static final int TIME_STEP_INITIAL = 800;
		private static final int TIME_STEP_DECREASE = 200;
		private static final int TIME_STEP_ARROWS = 10;
		
		private static final int BONUS_CHANCE = 60;
		private static final int SLOW_TIME = 10000;
		
		private Bitmap[] moa_arrows;
		private Bitmap mo_slowBonusGraphic;
		private Bitmap mo_clearBonusGraphic;
		private Bitmap mo_currentSlowGraphic;
		
		private int mi_gameCond;
		
		private Random rand;
		private int mi_arrow;
		private int mi_direction;
		private Context mo_con;
		private int mi_numArrows;
		private int mi_numArrowsStep;
		private SurfaceHolder mo_sholder;
		private float mf_lastX;
		private float mf_lastY;
		private Bitmap backimg;
		private Bitmap mo_gameOverGraphic;
		private boolean mb_run;
		private int mi_swidth;
		private int mi_sheight;
		private boolean mb_mainArrow;
		private int mi_mainArrow;
		private long ml_lastTime;
		private boolean mb_nodraw;
		private int mi_timeStep;
		private boolean mb_bonusStreak;
		private boolean mb_slowBonus;
		private boolean mb_clearBonus;
		private int mi_bonusArrows;
		private boolean mb_bonusSlow;
		private int mi_slowTime;
		private int mi_timeDiff;
		private long ml_bonusTime;
		
		private Handler mo_handler;
		
		private int[] mia_queue;
		
		private MediaPlayer mo_shiftSound;
		private MediaPlayer mo_clockSound;
		
		private boolean mo_updatebutton;
		
		public ShiftThread(SurfaceHolder hold, Context con, Handler handler){
			mo_updatebutton = false;
			mb_bonusStreak = false;
			mi_bonusArrows = 0;
			mb_bonusSlow = false;
			mi_slowTime = 0;
			mo_handler = handler;
			mb_nodraw=false;
			mi_timeStep = TIME_STEP_INITIAL;
			mi_timeDiff = mi_timeStep;
			mi_numArrows = 0;
			mi_numArrowsStep = 0;
			mi_direction = DIR_NONE;
			mi_swidth = 0;
			mi_sheight = 0;
			mo_con = con;
			mf_lastX = 0;
	        mf_lastY = 0;
	        mo_sholder = hold;
	        rand = new Random();
	        rand.setSeed(System.currentTimeMillis());
	        mi_arrow = ARROW_NONE;
	        mb_mainArrow = false;
	        ml_lastTime = System.currentTimeMillis();
	        mi_gameCond = COND_READY;
	        moa_arrows = new Bitmap[4];
	        moa_arrows[0] = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.arrow_left);
	        moa_arrows[1] = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.arrow_right);
	        moa_arrows[2] = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.arrow_up);
	        moa_arrows[3] = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.arrow_down);
	        backimg = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.background);
	        mo_gameOverGraphic = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.gameover);
	        mo_slowBonusGraphic = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.slow);
	        mo_clearBonusGraphic = BitmapFactory.decodeResource(mo_con.getResources(), R.drawable.clear);
	        mo_currentSlowGraphic = Bitmap.createScaledBitmap(mo_slowBonusGraphic, 300, 300, true);
			mo_shiftSound = MediaPlayer.create(mo_con, R.raw.shift);
			mo_clockSound = MediaPlayer.create(mo_con, R.raw.clock);
			mo_clockSound.setLooping(true);
	        mia_queue = new int[6];
	        clearQueue();
			CharSequence str = "Touch and drag in the direction of the arrow shown in the center of the screen. Press the button below to begin.";
			sendMessage(str);
			int resid = R.drawable.go;
			setButton(resid);
	    }
		
		public void setRunning(Boolean running){
			mb_run = running;
		}
		
		private void enqueue(int imgCode){
			synchronized(mo_sholder){
				for(int i=0;i<6;i++){
					if(mia_queue[i]==ARROW_NONE){
						mia_queue[i]=imgCode;
						return;
					}
				}
				mi_gameCond = COND_LOSE;
				mo_clockSound.stop();
				CharSequence str = "You Lost!\nYou shifted " + mi_numArrows + " arrows. Press the button below to try again.";
				int resid = R.drawable.try_again;
				sendMessage(str);
				setButton(resid);
			}
		}
		
		private void setButton(int resid){
			Message msg = mo_handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("type", 1);
            b.putInt("img", resid);
            b.putInt("viz", View.VISIBLE);
            msg.setData(b);
            mo_handler.sendMessage(msg);
		}
		
		private void sendMessage(CharSequence str){
			Message msg = mo_handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("type", 0);
            b.putString("text", str.toString());
            b.putInt("viz", View.VISIBLE);
            msg.setData(b);
            mo_handler.sendMessage(msg);
		}
		
		private void clearMessage(){
			Message msg = mo_handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("type", 0);
            b.putString("text", "");
            b.putInt("viz", View.GONE);
            msg.setData(b);
            mo_handler.sendMessage(msg);
		}
		
		private void clearButton(){
			Message msg = mo_handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("type", 1);
            b.putString("text", "");
            b.putInt("viz", View.GONE);
            msg.setData(b);
            mo_handler.sendMessage(msg);
		}
		
		private int dequeue(){
			synchronized(mo_sholder){
				int arrow = mia_queue[0];
				if(arrow!=ARROW_NONE){
					for(int i=1;i<6;i++){
						mia_queue[i-1]=mia_queue[i];
						mia_queue[i]=ARROW_NONE;
					}
				}
				return arrow;
			}
		}
		
		@Override
		public void run(){
			while (mb_run) {
		        try {
					sleep(1000/FPS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int resid;
				if(mi_gameCond == COND_RUN){
					update();
				}
				else if(mi_gameCond == COND_READY){
					boolean pressed = mo_button.isPressed();
					if(pressed){
						mo_updatebutton = true;
						resid = R.drawable.go_down;
						setButton(resid);
					}
					else{
						if(mo_updatebutton == true){
							mo_updatebutton = false;
							resid = R.drawable.go;
							setButton(resid);
						}
					}
					Canvas c = mo_sholder.lockCanvas();
					c.drawBitmap(backimg, 0, 0, null);
					mo_sholder.unlockCanvasAndPost(c);
				}
				else if(mi_gameCond == COND_LOSE){
					boolean pressed = mo_button.isPressed();
					if(pressed){
						mo_updatebutton = true;
						resid = R.drawable.try_again_down;
						setButton(resid);
					}
					else{
						if(mo_updatebutton == true){
							mo_updatebutton = false;
							resid = R.drawable.try_again;
							setButton(resid);
						}
					}
					Canvas c = mo_sholder.lockCanvas();
					c.drawBitmap(mo_gameOverGraphic, 0, 0, null);
					mo_sholder.unlockCanvasAndPost(c);
				}
		    }
		}
			
		public void update(){
			if(mb_run){
				//Canvas c = null;
				try{
					//c = mo_sholder.lockCanvas();
					synchronized(mo_sholder){
						updateTime();
						if(mb_bonusSlow){
							mi_timeDiff = (mi_timeStep*2);
							mi_slowTime = (int)(System.currentTimeMillis() - ml_bonusTime);
							if(mi_slowTime > SLOW_TIME){
								mb_bonusSlow = false;
								mo_clockSound.stop();
								try {
									mo_clockSound.prepare();
								} catch (IllegalStateException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								mi_timeDiff = mi_timeStep;
							}
						}
						else
						{
							mi_timeDiff = mi_timeStep;
						}
						if(ml_lastTime + (long)mi_timeDiff < System.currentTimeMillis()){
							if(rand.nextInt(BONUS_CHANCE)==0 && mb_bonusStreak==false && mb_bonusSlow==false){
								mb_bonusStreak = true;
								if(rand.nextInt(2)==0){
									mb_clearBonus=true;
								}
								else
								{
									mb_slowBonus=true;
								}
							}
							mi_arrow = rand.nextInt(4);
							enqueue(mi_arrow);
							ml_lastTime = System.currentTimeMillis();
						}
						setMainArrow();
						handleInput();
				        doDraw();
					}
				} finally {
					//if(c!=null){
						//mo_sholder.unlockCanvasAndPost(c);
					//}
				}
			}
		}
		
		private void updateTime(){
			if(mi_numArrowsStep>=TIME_STEP_ARROWS){
				int num = mi_numArrows/mi_numArrowsStep;
				int decrease = (int)(TIME_STEP_DECREASE/(1.5*num));
				mi_timeStep-=decrease;
				mi_numArrowsStep=0;
			}
		}
		
		private void setMainArrow(){
			if(mb_mainArrow==false){
				mi_mainArrow = dequeue();
				if(mi_mainArrow!=ARROW_NONE){
					mb_mainArrow=true;
				}
			}
		}
		
		private void handleInput(){
			synchronized(mo_sholder){
				if(mi_direction != DIR_NONE){
					if(mi_direction == mi_mainArrow){
						mi_numArrows++;
						mi_numArrowsStep++;
						mo_shiftSound.start();
						if(mb_bonusStreak){
							mi_bonusArrows++;
							if(mi_bonusArrows==6){
								if(mb_slowBonus){
									mb_bonusSlow = true;
									ml_bonusTime = System.currentTimeMillis();
									mb_slowBonus = false;
									mo_clockSound.start();
								}
								else if(mb_clearBonus){
									clearQueue();
									mb_clearBonus = false;
								}
								mi_bonusArrows = 0;
								mb_bonusStreak = false;
							}
						}
						mb_mainArrow = false;
					}
					else
					{
						mb_clearBonus = false;
						mb_slowBonus = false;
						mb_bonusStreak = false;
						mi_bonusArrows = 0;
					}
					mi_direction = DIR_NONE;
				}
			}
		}
		
		private void drawQueue(Canvas c){
			synchronized(mo_sholder){
				for(int i=0;i<6;i++){
					if(mia_queue[i]!=ARROW_NONE)
					{
						Bitmap img = Bitmap.createScaledBitmap(moa_arrows[mia_queue[i]], (mi_swidth/7), (mi_swidth/7), true);
						c.drawBitmap(img, ((mi_swidth/7)*(i+1)-20), 100, null);
					}
				}
			}
		}
		
		private void doDraw(){
			synchronized(mo_sholder){
				if(mi_gameCond == COND_RUN)
				{
					Canvas c = mo_sholder.lockCanvas();
					c.drawBitmap(backimg, 0, 0, null);
					if(!mb_nodraw) drawQueue(c);
					if(mi_mainArrow!=ARROW_NONE && mb_mainArrow){
						c.drawBitmap(moa_arrows[mi_mainArrow], (mi_swidth/2)-50, (mi_sheight/2)-35, null);
					}
					Paint paint = new Paint();
					paint.setColor(0xFFFFFFFF);
					paint.setTextSize(30);
					Typeface typeface;
					typeface = Typeface.create("Futura Md", Typeface.BOLD);
					paint.setTypeface(typeface);
					Paint paint2 = new Paint();
					paint2.setAlpha(100);
					c.drawText("" + mi_numArrows, 10, (mi_sheight-10), paint);
					if(mb_bonusSlow){
						c.drawBitmap(mo_currentSlowGraphic, ((mi_swidth-300)/2), ((mi_sheight-300)/2), paint2);
					}
					if(mb_slowBonus){
						c.drawBitmap(mo_slowBonusGraphic, mi_swidth-100, mi_sheight-100, null);
					}
					else if(mb_clearBonus){
						c.drawBitmap(mo_clearBonusGraphic, mi_swidth-100, mi_sheight-100, null);
					}
					mo_sholder.unlockCanvasAndPost(c);
				}
			}
		}
		
		public void restart(){
			clearQueue();
			clearMessage();
			mb_mainArrow=false;
			mi_mainArrow=ARROW_NONE;
			mi_direction=DIR_NONE;
			mi_numArrows=0;
			mi_numArrowsStep=0;
			mi_timeStep=TIME_STEP_INITIAL;
			mi_gameCond = COND_RUN;
			mb_bonusStreak = false;
			mi_bonusArrows = 0;
			mb_bonusSlow = false;
			mb_clearBonus = false;
			mb_slowBonus = false;
			mi_slowTime = 0;
			mo_updatebutton = false;
		}
		
		private void clearQueue(){
			for(int i=0;i<6;i++){
	        	mia_queue[i]=ARROW_NONE;
	        }
		}
		
		public void doStart(){
			if(mi_gameCond == COND_READY){
				mi_gameCond = COND_RUN;
				clearMessage();
				return;
			}
		}
		
		public boolean doTouch(MotionEvent event){
			synchronized(mo_sholder){
				int action = event.getAction();
				switch(action)
				{
				case 0:
					mf_lastX = event.getX();
					mf_lastY = event.getY();
					//Toast.makeText(mo_con, mf_lastX + ", " + mf_lastY, Toast.LENGTH_SHORT).show();
					break;
				case 1:
					float newX = event.getX();
					float newY = event.getY();
					float delX = newX - mf_lastX;
					float delY = newY - mf_lastY;
					//Toast.makeText(mo_con, newX + " - " + mf_lastX + "=" + delX +",\n" + newY + " - " + mf_lastY + "=" + delY, Toast.LENGTH_SHORT).show();
					if(Math.abs(delX)<10 && Math.abs(delY)<10)
					{
						mi_direction = DIR_NONE;
						//Toast.makeText(mo_con, "Didn't Move.", Toast.LENGTH_SHORT).show();
					}
					else if(Math.abs(delY)>Math.abs(delX))
					{
						if(delY>0)
						{
							mi_direction = DIR_DOWN;
							//Toast.makeText(mo_con, "Moved Down!", Toast.LENGTH_SHORT).show();
						}
						else
						{
							mi_direction = DIR_UP;
							//Toast.makeText(mo_con, "Moved Up!", Toast.LENGTH_SHORT).show();
						}
					}
					else
					{
						if(delX>0)
						{
							mi_direction = DIR_RIGHT;
							//Toast.makeText(mo_con, "Moved Right!", Toast.LENGTH_SHORT).show();
						}
						else
						{
							mi_direction = DIR_LEFT;
							//Toast.makeText(mo_con, "Moved Left!", Toast.LENGTH_SHORT).show();
						}
					}
					//Toast.makeText(mo_con, (newX-mf_lastX) + ", " + (newY-mf_lastY), Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
				}
				return true;
			}
		}
		
		public void buttonClicked(){
			synchronized(mo_sholder){
				switch(mi_gameCond){
				case COND_READY:
					doStart();
					break;
				case COND_LOSE:
					restart();
					break;
				default:
					break;
				}
				clearButton();
			}
		}
		
		public void resize(int width, int height){
			synchronized(mo_sholder){
				mi_swidth = width;
				mi_sheight = height;
				backimg = Bitmap.createScaledBitmap(backimg, width, height, true);
			}
		}
		
		public Bundle saveState(Bundle map){
            synchronized (mo_sholder){
                if (map != null){
                    map.putBoolean("b_mainArrow", mb_mainArrow);
                    map.putInt("i_mainArrow", mi_mainArrow);
                    map.putInt("i_direction", mi_direction);
                    map.putInt("i_numArrows", mi_numArrows);
                    map.putInt("i_numArrowsStep", mi_numArrowsStep);
                    map.putInt("i_timestep", mi_timeStep);
                    map.putInt("i_gameCond", mi_gameCond);
                    map.putIntArray("ia_queue", mia_queue);
                    map.putBoolean("b_bonusStreak", mb_bonusStreak);
        			map.putInt("i_bonusArrows", mi_bonusArrows);
        			map.putBoolean("b_bonusSlow", mb_bonusSlow);
        			map.putInt("i_slowTime", mi_slowTime);
        			map.putInt("i_timeDiff", mi_timeDiff);
                }
            }
            mi_gameCond = COND_READY;
            return map;
        }
		
		public synchronized void restoreState(Bundle savedState){
            synchronized (mo_sholder){
                mb_mainArrow = savedState.getBoolean("b_mainArrow");
                mi_mainArrow = savedState.getInt("i_mainArrow");
                mi_direction = savedState.getInt("i_direction");
                mi_numArrows = savedState.getInt("i_numArrows");
                mi_numArrowsStep = savedState.getInt("mi_numArrowsStep");
                mi_timeStep = savedState.getInt("i_timeStep");
                mia_queue = savedState.getIntArray("ia_queue");
                mb_bonusStreak = savedState.getBoolean("b_bonusStreak");
                mi_bonusArrows = savedState.getInt("i_bonusArrows");
                mb_bonusSlow = savedState.getBoolean("b_bonusSlow");
                mi_slowTime = savedState.getInt("i_slowTime");
                mi_timeDiff = savedState.getInt("i_timediff");
                mi_gameCond = savedState.getInt("i_gameCond");
                
                if(mi_gameCond == COND_READY){
                	CharSequence str = "Touch and drag in the direction of the arrow shown in the center of the screen. Press the button below to begin.";
        			sendMessage(str);
        			int resid = R.drawable.go;
        			setButton(resid);
                }
                else if(mi_gameCond == COND_LOSE){
                	CharSequence str = "You Lost!\nYou shifted " + mi_numArrows + " arrows. Press the button below to try again.";
    				int resid = R.drawable.try_again;
    				sendMessage(str);
    				setButton(resid);
                }
            }
        }
	}
	
	private ShiftThread mo_thread;
	//private Context mo_con;
	private TextView mo_textCond;
	private ImageButton mo_button;
	
	public ShiftView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        //mo_con = context;
        
        mo_thread = new ShiftThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
            	int type = m.getData().getInt("type");
            	switch(type){
            	case 0:
            		mo_textCond.setVisibility(m.getData().getInt("viz"));
            		mo_textCond.setText(m.getData().getString("text"));
            		break;
            	case 1:
            		mo_button.setVisibility(m.getData().getInt("viz"));
            		mo_button.setImageResource(m.getData().getInt("img"));
            		/*int signal = m.getData().getInt("signal");
            		switch(signal){
            		case 0:
            			mo_button.setOnClickListener(new OnClickListener(){
            				public void onClick(View v){
            					mo_thread.doStart();
            				}
            			});
            			break;
            		case 1:
            			mo_button.setOnClickListener(new OnClickListener(){
            				public void onClick(View v){
            					mo_thread.restart();
            				}
            			});
            			break;
            		}*/
            		break;
            	}
            }
        });
        setFocusable(true);
        requestFocus();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		return mo_thread.doTouch(event);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mo_thread.resize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mo_thread.setRunning(true);
		mo_thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//boolean retry = true;
        mo_thread.setRunning(false);
        //while (retry) {
            try {
                mo_thread.join();
                //retry = false;
            } catch (InterruptedException e) {
            }
        //}
	}

	public ShiftThread getThread(){
		return mo_thread;
	}
	
	public void setTextView(TextView txt){
		mo_textCond = txt;
	}
	
	public void setButton(ImageButton btn){
		mo_button = btn;
		
		mo_button.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				mo_thread.buttonClicked();
			}
		});
	}
}	