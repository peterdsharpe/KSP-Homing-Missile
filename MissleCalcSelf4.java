/* 
 *  This program simulates the path of a 2D missle. There are two modes: single, and multiplayer. In single player, you can adjust the algorithumns that
 *  both the seeker (blue) and the target (red) are using to determine paths based on the variables: CHOICEALGORITHUM (seeker), and TARGETCHOICEALGORITHUM (target)
 * It has a built in sound track, and as the red (target) wins, his dot becomes larger, making it easier for the seeker
 * As the seeker wins, the red dot gets faster.
 * All options are currently available, but as this is only Beta it will come with more updates later
 *
 * Due to a bug in the code, the Insane algorithm starts to fail around level 11, it trails just behind the target.
 *     This is most likely to the way that the program renders the timesteps.
 *     
 * Thus, there is a level max - BUT it can be disabled in the menu (Game Options)
 *  
 *
 *
 * MissleCalcSelf4 is a program made by a collobartion of Peter Sharpe and John Peurifoy
 *
 * Special acknowledgements go to:
 * 						Suzanne Hadden for mathematical oversight.
 * 						User pTymN on http://www.gamedev.net/topic/451048-best-way-of-solving-a-polynomial-of-the-fourth-degree/ for these algorithms
 *
 *
 *
 * Please e-mail any comments or suggestions to: johnpeurifoy@yahoo.com
 *
 */



import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
//sound
import java.applet.AudioClip;
import java.net.URL;
import java.util.Scanner; 
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Scanner;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/****************************************THERE IS STILL A LARGE AMOUNT OF CODE IN THIS FOR DEBUGGING PURPOSES.********************************/

// Things to add:
// 		fix soundtrack..why is this not working?
//		add more than more seeker
// 		allow the switching of the control keys
//		create a help document
//			Fire aminations
//		More players
//		Difficulty adjuster
 //Delete all the depreciated crapola (aka the checkers commands?)
  // fix the bug in the INSANE algo

public class MissleCalcSelf4 extends JPanel implements Runnable, ActionListener,KeyListener, MouseListener, FocusListener {
	private static final boolean ORBITON = false;
	private static final int BASICTRACK = 0;
	private static final int LEADTRACK = 1;
	private static final int SUPERADVANCED	= 3;
	private static final int ACCELFACT = 2;
	private static final int INSANE = 4;
	private static final int SOUNDTRACKONE = 1;
	private static final int SOUNDTRACKTWO = 2;
	private static final int SOUNDTRACKTHREE = 3;
	private static final int MAXLEVEL = 8;
	private static boolean maxingout;
	private static long currenttime;
	private static long startTime;
	private static final int USERIN = 5;
	private static final int RANDOMRUN = 6;
	private static int CHOICEALGORITHUM = INSANE; //Depreciated now  Possible choices: ACCELFACT, USERIN, SUPERADVANCED,
	private static int TARGETCHOICEALGORITHUM = RANDOMRUN; //Depreciated now. Possible choices: USERIN, RANDOMRUN
	private static AudioClip sound = null;
	private static Color seekerColor = Color.BLUE;
	private static Color targetColor = Color.RED;
			
	//Speed dificulty

	/**
    * A main() routine to allow this class to be run as a stand-alone
    * application.  Just opens a window containing a panel of type
    * MissleCalcSelf4
    */
   public static void main(String[] args) {
      JFrame window = new JFrame("Missle Seeker Program");
     MissleCalcSelf4 content = new MissleCalcSelf4();
      window.setContentPane(content);
	   window.setJMenuBar(content.getMenu());
	      
      window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      window.setResizable(true);
	   window.pack();
      window.setLocation(0,0);
      window.setSize(900,800);
      window.pack();
	window.setVisible(true);
   }


   private static Color BACKGROUND_COLOR = new Color(84,240,173);      //255,255,255); // 4 colors used in drawing.
   private static Color BORDER_COLOR = new Color(100,0,0);
   private static Color OSCBACKGROUNDCOLOR = new Color(191,255,255);
   
   private static Color TARGET_COLOR = new Color(0,0,180);
   private static Color SEEKER_COLOR = new Color(180,180,255);
   private static Color TRAIL_COLOR = Color.WHITE;
   
   private static BufferedImage OSC;   // The off-screen canvas.  Frames are drawn here, then copied to the screen.
   private static boolean solved;

   private int status;   // Controls the execution of the thread; value is one of the following constants.

   private static final int GO = 0;       // a value for status, meaning thread is to run continuously
   private static final int PAUSE = 1;    // a value for status, meaning thread should not run
   private static final int STEP = 2;     // a value for status, meaning thread should run one step then pause
   private static final int RESTART = 3;  // a value for status, meaning thread should start again from the beginning

   /*
    The following variables are the data needed for the animation.  The
     two seekers are represented by arrays: tarvalues and seekvalues.
     Their previous locations are stored in tarpoints and seekpoints respectively.
     The level is the current level, and the DIFFICULTY controls the scalar for the acceleration
     The DIFFICULTY should thus be called the sensitivty.
     
      During the animation, as the positions chnage, the controlaccelx/controlaccely 
      will be modified, thus changing the acceleration for the seeker.
      If set, the controlacceltargetx/controlacceltargety will be changed for the target.
      The wincount target/wincountseek will keep track of the score
    */
   private static int virtualx = 0;
   private static int virtualy = 0;
   private static double controlaccelx = 0;
   private static double controlaccely = 0;
   private static double controlacceltargetx = 0;
   private static double controlacceltargety = 0;
   private static double level = 3;
   private static double wincountarget = 0;
   private static double wincountseek = 0;
   private static final int WON = 1;
   private static final int NOTHING = 0;
   private static final int RESET = 2;
   private static final int LOSS = 3;
   private static final double DADDIFFICULTY = 0.25;
   private static final double MOMDIFFICULTY = 0.15;
   private static final double MYDIFFICULTY = 1;
   private static final double KYLEDIFFICULTY = 0.5;
   
   
   private static int startx = 0;
   private static int starty = 0;
   
	private double[] tarvalues = new double[6]; // posx,posy,velx,vely,accelx,accely
	private double[] seekvalues = new double[6];
	private ArrayList<Point> tarpoints = new ArrayList<Point>();
	private ArrayList<Point> seekpoints = new ArrayList<Point>();
	private static double DIFFICULTY = KYLEDIFFICULTY;
	private static double MAXSPEED = DIFFICULTY*level*100;
	
	private static final int STANDARDHIT = (int)(5 + 1/DIFFICULTY);
	private static int hitdistance = STANDARDHIT;
   
	private static Display display;  // A subpanel where the frames of the animation are shown.

   private JButton runPauseButton;  // 3 control buttons for controlling the animation
   private JButton nextStepButton;
   private JButton startOverButton;
   private boolean win;
	

   /**
    * This class defines the panel that is used as a display, to show
    * the frames of the animation.  The paintComponent() method in this
    * class simply copies the off-screen canvas, OSC, to the screen.
    * This display will be given a preferred size of 900-by-800, which
    * is the same size as the canvas.  But to allow for possible small
    * variations from this size, OSC is drawn centered on the panel.
    */
   private class Display extends JPanel {
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         int x = (getWidth() - OSC.getWidth())/2;
         int y = (getHeight() - OSC.getHeight())/2;
         g.drawImage(OSC, x, y, null);
      }
   }
   // TOADD:
   
	   //sound on command
	   //slider? for speed multiplier/delay?
	   //handicap of size
	   //handicap of speed increase
	   //controls (switch between wasd, and arrow keys)
	   
	   
   public JMenuBar getMenu() {
	   JMenuBar retval = new JMenuBar();
	   JMenu fileMenu = new JMenu("File");
	   JMenu gameOptionsMenu = new JMenu("Game Options");
	   JMenu targetOptionsMenu = new JMenu("Target Options");
	   JMenu seekerOptionsMenu = new JMenu("Seeker Options");
	   JMenu helpMenu = new JMenu("Help");
	   retval.add(fileMenu);
	   retval.add(gameOptionsMenu);
	   retval.add(targetOptionsMenu);
	   retval.add(seekerOptionsMenu);
	   retval.add(helpMenu);
	   
	   ActionListener listener = new MenuHandler();
	   //this is the main one, as well as for the target
	   ActionListener listener2 = new MenuHandler2();
	   //this second listener will be for the target
	   JMenuItem newGameCommand = new JMenuItem("New Game");
	   newGameCommand.addActionListener(listener);
	   fileMenu.add(newGameCommand);
	   
	   JMenuItem quitCommand = new JMenuItem("Quit");
	   quitCommand.addActionListener(listener);
	   fileMenu.add(quitCommand);
	   	   
	   JMenuItem soundOn = new JMenuItem("Sound On");
	   soundOn.addActionListener(listener);
	   gameOptionsMenu.add(soundOn);
	   
	   JMenuItem sizeHandicap = new JMenuItem("Size Handicap On");
	   sizeHandicap.addActionListener(listener);
	   gameOptionsMenu.add(sizeHandicap);
	   
	   JMenuItem speedHandicap = new JMenuItem("Speed Handicap On");
		JRadioButton seekerGreen, seekerRed, seekerBlue;
		
		JCheckBoxMenuItem levelCap = new JCheckBoxMenuItem("Stop at Level 8");
		levelCap.addActionListener(listener);
		levelCap.setSelected(true);
		gameOptionsMenu.add(levelCap);
		
				
				
				
	   ButtonGroup botGroup = new ButtonGroup();
	   seekerGreen = new JRadioButton("Green Color");
	   seekerGreen.addActionListener(listener);
	   botGroup.add(seekerGreen);
	   seekerOptionsMenu.add(seekerGreen);
	   
	   seekerRed = new JRadioButton("Red Color");
	   seekerRed.addActionListener(listener);
	   botGroup.add(seekerRed);
	   seekerOptionsMenu.add(seekerRed);
	   
	   seekerBlue = new JRadioButton("Blue Color");
	   seekerBlue.addActionListener(listener);
	   botGroup.add(seekerBlue);
	   seekerOptionsMenu.add(seekerBlue);
	   
	   seekerBlue.setSelected(true);
	   //now we need to do speed controls and algorithum choice
	   
	   JRadioButton userControl, basicTrackControl, leadTrackControl, accelTrackControl , superTrackControl, randomControl, insaneControl;
		ButtonGroup controlGroup = new ButtonGroup();
		
		userControl = new JRadioButton("User Control");
		userControl.addActionListener(listener);
		controlGroup.add(userControl);
		seekerOptionsMenu.add(userControl);
	   
		basicTrackControl = new JRadioButton("Basic Algo.");
		basicTrackControl.addActionListener(listener);
		controlGroup.add(basicTrackControl);
		seekerOptionsMenu.add(basicTrackControl);
		
		leadTrackControl = new JRadioButton("Lead Algo.");
		leadTrackControl.addActionListener(listener);
		controlGroup.add(leadTrackControl);
		seekerOptionsMenu.add(leadTrackControl);
		
		accelTrackControl = new JRadioButton("Accel Algo.");
		accelTrackControl.addActionListener(listener);
		controlGroup.add(accelTrackControl);
		seekerOptionsMenu.add(accelTrackControl);
		
		superTrackControl = new JRadioButton("Super Algo.");
		superTrackControl.addActionListener(listener);
		controlGroup.add(superTrackControl);
		seekerOptionsMenu.add(superTrackControl);
		
		insaneControl = new JRadioButton("Insane Control");
		insaneControl.addActionListener(listener);
		controlGroup.add(insaneControl);
		seekerOptionsMenu.add(insaneControl);
		
		
		
		
		randomControl = new JRadioButton("Random Move");
		randomControl.addActionListener(listener);
		controlGroup.add(randomControl);
		seekerOptionsMenu.add(randomControl);
		
		
		insaneControl.setSelected(true);
	


		
		   JRadioButton targetGreen, targetRed, targetBlue;
		   ButtonGroup botGroup2 = new ButtonGroup();
		   targetGreen = new JRadioButton("Green Color");
		targetGreen.addActionListener(listener2);
		botGroup2.add(targetGreen);
		targetOptionsMenu.add(targetGreen);
		   
		targetRed = new JRadioButton("Red Color");
		targetRed.addActionListener(listener2);
		botGroup2.add(targetRed);
		targetOptionsMenu.add(targetRed);
		
		targetBlue = new JRadioButton("Blue Color");
		targetBlue.addActionListener(listener2);
		botGroup2.add(targetBlue);
		targetOptionsMenu.add(targetBlue);
		   
		targetRed.setSelected(true);
		//now we need to do speed controls and algorithum choice
		ButtonGroup controlGroup2 = new ButtonGroup();
		
		userControl = new JRadioButton("User Control");
		userControl.addActionListener(listener2);
		controlGroup2.add(userControl);
		targetOptionsMenu.add(userControl);
	   
		basicTrackControl = new JRadioButton("Basic Algo.");
		basicTrackControl.addActionListener(listener2);
		controlGroup2.add(basicTrackControl);
		targetOptionsMenu.add(basicTrackControl);
		
		leadTrackControl = new JRadioButton("Lead Algo.");
		leadTrackControl.addActionListener(listener2);
		controlGroup2.add(leadTrackControl);
		targetOptionsMenu.add(leadTrackControl);
		
		accelTrackControl = new JRadioButton("Accel Algo.");
		accelTrackControl.addActionListener(listener2);
		controlGroup2.add(accelTrackControl);
		targetOptionsMenu.add(accelTrackControl);
		
		superTrackControl = new JRadioButton("Super Algo.");
		superTrackControl.addActionListener(listener2);
		controlGroup2.add(superTrackControl);
		targetOptionsMenu.add(superTrackControl);
		
		insaneControl = new JRadioButton("Insane Control");
		insaneControl.addActionListener(listener);
		controlGroup2.add(insaneControl);
		targetOptionsMenu.add(insaneControl);
		
		randomControl = new JRadioButton("Random Move");
		randomControl.addActionListener(listener2);
		controlGroup2.add(randomControl);
		targetOptionsMenu.add(randomControl);
		
		randomControl.setSelected(true);

		JMenuItem rulesCommand = new JMenuItem("Rules");
		rulesCommand.addActionListener(listener);
		helpMenu.add(rulesCommand);

		JMenuItem websiteCommand = new JMenuItem("Website");
		websiteCommand.addActionListener(listener);
		helpMenu.add(websiteCommand);

		return retval;
	}
	
	//this is the main one, and for the seeker
	private class MenuHandler implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			String command = evt.getActionCommand();
			if (command.equals("Quit")) {
				System.out.println("L8ater");
				System.exit(0);
			}
			else if (command.equals("New Game")) {
				
			}
			else if (command.equals("Stop at Level 8")) {
				JCheckBoxMenuItem toggle = (JCheckBoxMenuItem)evt.getSource();
				maxingout = toggle.isSelected();
			}
			
				
				
				
			else if (command.equals("Green Color")) {
				seekerColor = Color.GREEN;
			}
			else if (command.equals("Red Color")) {
				seekerColor = Color.RED;
			}
			else if (command.equals("Blue Color")) {
				seekerColor = Color.BLUE;
			}
			else if (command.equals("User Control")) {
				CHOICEALGORITHUM = USERIN;
			}
			else if (command.equals("Basic Algo.")) {
				CHOICEALGORITHUM = BASICTRACK;
			}
			else if (command.equals("Lead Algo.")) {
				CHOICEALGORITHUM = LEADTRACK;
			}
			else if (command.equals("Accel Algo.")) {
				CHOICEALGORITHUM = ACCELFACT;
			}
			else if (command.equals("Super Algo.")) {
				CHOICEALGORITHUM = SUPERADVANCED;
			}
			else if (command.equals("Random Move")) {
				CHOICEALGORITHUM = RANDOMRUN;
			}
			else if (command.equals("Insane Control")) {
				CHOICEALGORITHUM = INSANE;
			}
			else if (command.equals("Rules")) {
			}
			else if (command.equals("Website")) {
			}
		}
	}
	//this is for the target
	private class MenuHandler2 implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			String command = evt.getActionCommand();
			if (command.equals("Green Color")) {
				targetColor = Color.GREEN;
			}
			else if (command.equals("Red Color")) {
				targetColor = Color.RED;
			}
			else if (command.equals("Blue Color")) {
				targetColor = Color.BLUE;
			}
			else if (command.equals("User Control")) {
				TARGETCHOICEALGORITHUM = USERIN;
			}
			else if (command.equals("Basic Algo.")) {
				TARGETCHOICEALGORITHUM = BASICTRACK;
			}
			else if (command.equals("Lead Algo.")) {
				TARGETCHOICEALGORITHUM = LEADTRACK;
			}
			else if (command.equals("Accel Algo.")) {
				TARGETCHOICEALGORITHUM = ACCELFACT;
			}
			else if (command.equals("Super Algo.")) {
				TARGETCHOICEALGORITHUM = SUPERADVANCED;
			}

			else if (command.equals("Insane Control")) {
				TARGETCHOICEALGORITHUM = INSANE;
			}
			else if (command.equals("Random Move")) {
				TARGETCHOICEALGORITHUM = RANDOMRUN;
			}
		}
	}
	
	
   /**
    *  Create the panel, containing a display panel and, beneath it,
    *  a sub-panel containing the three control buttons.  This
    *  constructor also creates the off-screen canvas, and creates
    *  and starts the animation thread.
    */
   public MissleCalcSelf4() {
	   hasfocus = false;
	   maxingout = true;
	   addKeyListener(this);
	   addMouseListener(this);
	   
	   requestFocus();
	   
	  OSC = new BufferedImage(900,800,BufferedImage.TYPE_INT_RGB);
      display = new Display();
      display.setPreferredSize(new Dimension(900,800));
      display.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 2));
      display.setBackground(BACKGROUND_COLOR);
      setLayout(new BorderLayout());
      add(display, BorderLayout.CENTER);
      JPanel buttonBar = new JPanel();
      add(buttonBar, BorderLayout.SOUTH);
      buttonBar.setLayout(new GridLayout(1,0));
      runPauseButton = new JButton("Run");
      runPauseButton.addActionListener(this);
      buttonBar.add(runPauseButton);
      nextStepButton = new JButton("Next Step");
      nextStepButton.addActionListener(this);
      buttonBar.add(nextStepButton);
      startOverButton = new JButton("Start Over");
      startOverButton.addActionListener(this);
      startOverButton.setEnabled(false);
      buttonBar.add(startOverButton);
      new Thread(this).start();
   }


   /**
    *  Event-handling method for the control buttons.  Changes in the
    *  value of the status variable will be seen by the animation thread,
    *  which will respond appropriately.
    */
   synchronized public void actionPerformed(ActionEvent evt) {
      Object source = evt.getSource();
      if (source == runPauseButton) {  // Toggle between running and paused.
         if (status == GO) {  // Animation is running.  Pause it.
            status = PAUSE;
            nextStepButton.setEnabled(true);
            runPauseButton.setText("Run");
		 getFocus();
         }
         else {  // Animation is paused.  Start it running.
            status = GO;
            nextStepButton.setEnabled(false);  // Disabled when animation is running
            runPauseButton.setText("Pause");
		 getFocus();
         }
      }
      else if (source == nextStepButton) {  // Set status to make animation run one step.
         status = STEP;
      }
      else if (source == startOverButton) { // Set status to make animation restart.
		level = 1;
	      status = RESTART;
	      System.out.println("Welcome Commander, thank you for your help. Those Germans are relentless. Please get set up");
	      delay(1000);
	      getFocus();
	      
	}
      notify();  // Wake up the thread so it can see the new status value!
   }
   private boolean hasfocus = false;
   public void getFocus() {
	   requestFocus();
   }
	/* This will be all of our key listeners.
            *
           */
   public void focusGained(FocusEvent evt) {
	   hasfocus = true;
	   System.out.println("YOU HAVE FOCUS!");
   }
   public void focusLost(FocusEvent evt) {
	hasfocus = false;
	}
   synchronized public void keyPressed(KeyEvent evt) {
	   int code = evt.getKeyCode();
	   switch (code) {
		   case KeyEvent.VK_LEFT:
			   controlaccelx = -0.5;
			break;
		   case KeyEvent.VK_RIGHT:
			   controlaccelx = 0.5;
			break;
		   case KeyEvent.VK_UP:
			controlaccely = -0.5;
			break;
		   case KeyEvent.VK_DOWN:
				controlaccely = 0.5;
			break;
		   case 87: //w 
			   controlacceltargety = -0.5;
			break;
		   case 65: //a
			   controlacceltargetx = -0.5;
			   break;
		   case 83: //s
			   controlacceltargety = 0.5;
			   break;
		   case 68: //d
			   controlacceltargetx = 0.5;
			   break;
		   default:
			   break;
	   }
	 //  System.out.println("Key pressed, code: " + code);
   }
   public void mousePressed(MouseEvent evt) { 
	   if (!hasfocus) {
		   requestFocus();
	   }
	}
	public void keyReleased(KeyEvent evt) {
		int code = evt.getKeyCode();
		switch (code) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
				//playCollision();
				controlaccelx = 0;
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
				controlaccely = 0;
				break;
			case 87:
			case 83:
				controlacceltargety = 0;
				break;
			case 68:
			case 65:
				controlacceltargetx = 0;
				break;
			default:
				break;
		}
		//System.out.println("key code: " + code);
	}
		
	public void keyTyped(KeyEvent evt) {
		//System.out.println("Key typed: " + evt.getKeyCode());
	}
	public void mouseReleased(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
				   
   /**
    *  The run() method for the animation thread.  Runs in an infinite loop.
    *  In the loop, the thread first sets up the initial state of the missles
    *  and of the buttons.  This includes setting the status to PAUSED, and
    *  calling checkStatus(), which will not return until the user clicks the
    *  "Run" button or the "Next" Button.  Once this happens, it calls
    *  the solve() method to run simulation to solve the MissleSeeker Algorithumn
    */
   //
	public void run() {
		int goon = 0; // 0 is false, 1 is true (hit and won) 2 is loss
		runPauseButton.setText("Run");
			nextStepButton.setEnabled(true);
			startOverButton.setEnabled(false);
		loadAudioResources();
		delay(5000);
			
		if (mainSoundTrack != null) {
		//	mainSoundTrack.stop();
		}
		playSoundtrack((int)(level%2+2));
		status = PAUSE;
		win = false;
		goon = NOTHING;
		while (true) {
			startTime = System.currentTimeMillis();

			solved = false;
			OSC = new BufferedImage(900,800,BufferedImage.TYPE_INT_RGB);
			if (win) {
				//checkStatus();
				status = GO;
				//delay (10000);
				win = false;	 
			}
			setUpProblem();  // Sets up the initial state of the puzzle
			if (goon == NOTHING) {
				status = PAUSE;
			} else {
				delay(1000);
				boolean countinue1 = checkStatus(); // Returns only when user has clicked "Run" or "Next"				
				
			}
			startOverButton.setEnabled(true);
			
			while (true) {
				boolean countinue = checkStatus(); // Returns only when user has clicked "Run" or "Next"				
				if (!countinue) {
					break;
				}
						
				//try {
					seekvalues[0] = seekvalues[0] + seekvalues[2]; 
					seekvalues[1] = seekvalues[1] + seekvalues[3];//nitialchangey;
					
					//you need to add tarvalues[i+1] to tarvalues [i], but normalize it to the past value
					goon = checkDistanceAndTime();
					if (goon == WON) {
						tarpoints.add(new Point((int)tarvalues[0],(int)tarvalues[1]));
						seekpoints.add(new Point((int)seekvalues[0],(int)seekvalues[1]));
						
						while (tarpoints.size() >400) {
							tarpoints.remove(0);
							seekpoints.remove(0);
						}
						
						repaint();
						if (OSC != null) {
					         Graphics g = OSC.getGraphics();
					         drawCurrentFrame(g,(int)(double)seekvalues[4],(int)(double)seekvalues[5],getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
					         g.dispose();
					     }
					     display.repaint();
						
					     System.out.println("tarx: " + tarvalues[0] + " tary: " + tarvalues[1]);
					     System.out.println("sekx: " + seekvalues[0] + " seky: " + seekvalues[1]);
					     
						win = true;
						System.out.println("I HIT IT! YEA MAN I'M SO PROUD!1");
						System.out.println("Congrats on beating level: " + level + " please, wait a moment while I find your next Al Queda missle.");
						//sound.stop();
						playSonicBoom();
						delay(2500);
						wincountseek++;
								
							//status = PAUSE;
						if (maxingout && level <= MAXLEVEL) {
							
						} else {
							level = level + 1;
							
						}
								System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						
						
						break;
						
					} else if (goon == RESET) {
						win = false;
						System.out.println("Sorry, too far away. Please wait a moment while I reset.");
						
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						delay(1000);
						break;
					} else if (goon == LOSS) {
						win = false;
						System.out.println("Congrats target (red), you have won this round! Deal with this!");
						
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						hitdistance = hitdistance + 5;
						wincountarget++;
						delay(1000);
						break;
					}
					//tarvalues[0] = tarvalues[0] + tarvalues[2]*level;
					//tarvalues[1] = tarvalues[1] + tarvalues[3]*level;
					//run the simulation
					ArrayList<Double> accelvector = getAccelerationNeeded(CHOICEALGORITHUM); //this will be normalized
					ArrayList<Double> targetaccelvector = getAccelerationNeededTarget(TARGETCHOICEALGORITHUM ); //USERIN); //RANDOMRUN); //USERIN); //RANDOMRUN); //uUSERIN); //RANDOMRUN); //sRANDOMRUN); //RANDOMRUN); //USERIN);
					seekvalues[0] = seekvalues[0] - seekvalues[2]; 
					seekvalues[1] = seekvalues[1] - seekvalues[3];//nitialchangey;
					
					
					
					//tarvalues[0] = tarvalues[0] - tarvalues[2]*level;
					//tarvalues[1] = tarvalues[1] - tarvalues[3]*level;
					
					
					
					
					//updatePositions((int)(double)accelvector.get(0),(int)(double)accelvector.get(1),(int)(double)targetaccelvector.get(0),(int)(double)targetaccelvector.get(1));
					//accels
					seekvalues[4] = (double) accelvector.get(0);//+seekvalues[4];
					seekvalues[5] = (double)accelvector.get(1);//+seekvalues[5]; 
					tarvalues[4] = (double)targetaccelvector.get(0);
					tarvalues[5] = (double)targetaccelvector.get(1);

					seekvalues[2] = seekvalues[2] + seekvalues[4];
					seekvalues[3] = seekvalues[3] + seekvalues[5];
					//System.out.println("taraccelx: " + tarvalues[4] + " taraccely: " + tarvalues[5]);
					
					
					// position coords
					//I actually want to update positions throughout this step - I want to have it step each 20 pixels.
					//find the number of timesteps - e.g. the distance/20
					//double initialchangex = seekvalues[2] + seekvalues[0];
					//double initialchangey = seekvalues[3] + seekvalues[1];
					int timesteps = (int) Math.sqrt(seekvalues[3]*seekvalues[3] + seekvalues[2]*seekvalues[2]) / 20;
					timesteps++;
					//k iterate through, drawing each 20
					for (int i = 0; i < timesteps; i++) {
						seekvalues [0] = seekvalues[0] + seekvalues[2]/(timesteps);
						seekvalues[1] = seekvalues[1] + seekvalues[3]/(timesteps);
						repaint();
						if (OSC != null) {
					         Graphics g = OSC.getGraphics();
					         drawCurrentFrame(g,(int)(double)accelvector.get(0),(int)(double)accelvector.get(1),getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
					         g.dispose();
					     }
					     display.repaint();
						delay(10);
						//check distance, see if you won. 
						goon = checkDistanceAndTime();
						
						if (goon == WON) {
							tarpoints.add(new Point((int)tarvalues[0],(int)tarvalues[1]));
							seekpoints.add(new Point((int)seekvalues[0],(int)seekvalues[1]));
							while (tarpoints.size() >400) {
								tarpoints.remove(0);
								seekpoints.remove(0);
							}
							repaint();
							if (OSC != null) {
						         Graphics g = OSC.getGraphics();
						         drawCurrentFrame(g,(int)(double)accelvector.get(0),(int)(double)accelvector.get(1),getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
						         g.dispose();
						     }
						     display.repaint();

						     System.out.println("tarx: " + tarvalues[0] + " tary: " + tarvalues[1]);
						     System.out.println("sekx: " + seekvalues[0] + " seky: " + seekvalues[1]);
						     
							win = true;
							System.out.println("I HIT IT! YEA MAN I'M SO PROUD!2");
							System.out.println("Congrats on beating level: " + level + " please, wait a moment while I find your next Al Queda missle.");
							//sound.stop();
							playSonicBoom();
							delay(2500);
							
							//status = PAUSE;
							if (maxingout && level <= MAXLEVEL) {
								
							} else {
								level = level + 1;
								
							}
							wincountseek++;
							//   private static double wincountarget = 0;
//   private static double wincountseek = 0;
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
							break;
							
						} else if (goon == RESET) {
							win = false;
							
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
							System.out.println("Sorry, you lost. I am resettting.");
							delay(1000);
							break;
						} else if (goon == LOSS) {
							win = false;
							System.out.println("Congrats target (red), you have won this round! Deal with this!");
							
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
							hitdistance = hitdistance + 5;
							wincountarget++;
							delay(1000);
							break;
						}
						/*
						if (Math.sqrt(Math.pow(tarvalues[0]-400,2) + Math.pow(tarvalues[1]-450,2))>=800) {
							win = false;
							System.out.println("Resetting");
							goon = true;
							break;
						
						*/
					}
					if (goon == WON || goon == RESET || goon == LOSS) {
						break;
					}
					seekvalues[0] = seekvalues[0] + seekvalues[2]*level; 
					seekvalues[1] = seekvalues[1] + seekvalues[3]*level;//nitialchangey;
					repaint();
					if (OSC != null) {
				         Graphics g = OSC.getGraphics();
				         drawCurrentFrame(g,(int)(double)accelvector.get(0),(int)(double)accelvector.get(1),getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
				         g.dispose();
				     }
				     display.repaint();
					//you need to add tarvalues[i+1] to tarvalues [i], but normalize it to the past value
					goon = checkDistanceAndTime();
					if (goon == WON) {
						tarpoints.add(new Point((int)tarvalues[0],(int)tarvalues[1]));
						seekpoints.add(new Point((int)seekvalues[0],(int)seekvalues[1]));
						while (tarpoints.size() >400) {
							tarpoints.remove(0);
							seekpoints.remove(0);
						}
						repaint();
						if (OSC != null) {
					         Graphics g = OSC.getGraphics();
					         drawCurrentFrame(g,(int)(double)accelvector.get(0),(int)(double)accelvector.get(1),getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
					         g.dispose();
					     }
					     display.repaint();
						

					     System.out.println("tarx: " + tarvalues[0] + " tary: " + tarvalues[1]);
					     System.out.println("sekx: " + seekvalues[0] + " seky: " + seekvalues[1]);
					     
					     win = true;
						System.out.println("I HIT IT! YEA MAN I'M SO PROUD!3");
						System.out.println("Congrats on beating level: " + level + " please, wait a moment while I find your next Al Queda missle.");
						//sound.stop();
						playSonicBoom();
						delay(2500);
						wincountseek++;
							
						//status = PAUSE;
						
						if (maxingout && level <= MAXLEVEL) {
							
						} else {
							level = level + 1;
							
						}
						
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						
						
						break;
						
					} else if (goon == RESET) {
						win = false;
						System.out.println("Sorry, too far away. Please wait a moment while I reset.");
						
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						delay(1000);
						break;
					} else if (goon == LOSS) {
						win = false;
						System.out.println("Congrats target (red), you have won this round! Deal with this!");
						
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						hitdistance = hitdistance + 5;
						wincountarget++;
						delay(1000);
						break;
					}
					tarvalues[0] = tarvalues[0] + tarvalues[2]*level;
					tarvalues[1] = tarvalues[1] + tarvalues[3]*level;
					repaint();
					if (OSC != null) {
				         Graphics g = OSC.getGraphics();
				         drawCurrentFrame(g,(int)(double)accelvector.get(0),(int)(double)accelvector.get(1),getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
				         g.dispose();
				     }
				     display.repaint();
					
					
					//For debugging
					//System.out.print("        tarvalues: " + tarvalues[0] + " y: " + tarvalues[1] + " vx: " + tarvalues[2] + " vy: " + tarvalues[3] + " level " + level);

					ArrayList<Double> newvels = fixTargetVelocity(USERIN);
					if (USERIN == USERIN) {
						tarvalues[2] = newvels.get(0);
						tarvalues[3] = newvels.get(1);
					}
					
					goon = checkDistanceAndTime();
					if (goon == WON) {
						tarpoints.add(new Point((int)tarvalues[0],(int)tarvalues[1]));
						seekpoints.add(new Point((int)seekvalues[0],(int)seekvalues[1]));
						while (tarpoints.size() >400) {
							tarpoints.remove(0);
							seekpoints.remove(0);
						}
						repaint();
						if (OSC != null) {
					         Graphics g = OSC.getGraphics();
					         drawCurrentFrame(g,(int)(double)accelvector.get(0),(int)(double)accelvector.get(1),getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
					         g.dispose();
					     }
					     display.repaint();
						

					     System.out.println("tarx: " + tarvalues[0] + " tary: " + tarvalues[1]);
					     System.out.println("sekx: " + seekvalues[0] + " seky: " + seekvalues[1]);
					     
						win = true;
						System.out.println("I HIT IT! YEA MAN I'M SO PROUD!4");
						System.out.println("Congrats on beating level: " + level + " please, wait a moment while I find your next Al Queda missle.");
						//sound.stop();
						playSonicBoom();
						delay(2500);
						wincountseek++;
							
						//status = PAUSE;
						if (maxingout && level <= MAXLEVEL) {
							
						} else {
							level = level + 1;
							
						}
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						
						
						break;
						
					} else if (goon == RESET) {
						win = false;
						System.out.println("Sorry, too far away. Please wait a moment while I reset.");
						
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						delay(1000);
						break;
					} else if (goon == LOSS) {
						win = false;
						System.out.println("Congrats target (red), you have won this round! Deal with this!");
						
							System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
						hitdistance = hitdistance + 5;
						wincountarget++;
						delay(1000);
						break;
					}
					
					
					
					tarpoints.add(new Point((int)tarvalues[0],(int)tarvalues[1]));
					seekpoints.add(new Point((int)seekvalues[0],(int)seekvalues[1]));
					while (tarpoints.size() >400) {
						tarpoints.remove(0);
						seekpoints.remove(0);
					}
					
					
					
					//velocity

					if (OSC != null) {
				         Graphics g = OSC.getGraphics();
				         drawCurrentFrame(g,(int)(double)accelvector.get(0),(int)(double)accelvector.get(1),getTime()); //drawCurrentFrame(Graphics g,int accelx, int accely,int time)
				         g.dispose();
				      }
				      display.repaint();
					//delay(1000);
					
				//}
				//catch (Exception e) {
				//	System.out.println("Error:" + e.getMessage());
				//}
			}
			
		}
	}
	//this returns the time till impact
	synchronized private double getTime() {
		return ((tarvalues[3]-seekvalues[3])*seekvalues[1]+(tarvalues[2]-seekvalues[2])*seekvalues[0] +(seekvalues[3]-tarvalues[3])*tarvalues[1]+(seekvalues[2]-tarvalues[2])*tarvalues[0])/(Math.pow(tarvalues[3],2)-2*seekvalues[3]*tarvalues[3]+Math.pow(tarvalues[2],2)-2*seekvalues[2]*tarvalues[2]+Math.pow(seekvalues[3],2)+Math.pow(seekvalues[2],2));
	}
	//prints back the needed acceleration to get to the point
	//this is for the seeker
	private static int keepcount2 = 0;
	synchronized private ArrayList<Double> getAccelerationNeeded(int chosenalgo) {
		ArrayList<Double> retval = new ArrayList<Double>();
		double changex = 0, changey = 0, distance;
		//Target values : tarvalues[6] posx, posy, velx, vely,
		//Seek values :    seekvalues[6] posx, posy, velx, vely, accelx, accely
		switch (chosenalgo) {
			case BASICTRACK:				
				//right now pressume that the point is fixed, so just draw a straight line to it
				changex = seekvalues[0] -tarvalues[0];
				changey = seekvalues[1] - tarvalues[1];
				//make sure that this is absolute 1, divide by their complete magnitude
				distance = Math.sqrt(changex*changex+changey*changey);
				retval.add(-1 * changex/distance * .1);
				retval.add(-1 * changey/distance * 0.1);
				break;
			case LEADTRACK:
				//now step it up, lead it by its current velocity 
				changex = seekvalues[0] + seekvalues[2] -tarvalues[0]-tarvalues[2];
				changey = seekvalues[1] + seekvalues[3] - tarvalues[1] - tarvalues[3];
				//make sure that this is absolute 1, divide by their complete magnitude
				distance = Math.sqrt(changex*changex+changey*changey);
				retval.add(-1 * changex/distance * .1);
				retval.add(-1 * changey/distance * 0.1);
				
				break;
			case ACCELFACT:
				//now step it up, lead it by its current velocity 
				changex = seekvalues[0] + seekvalues[2] + 1/2 * Math.pow(seekvalues[4],2)-tarvalues[0]-tarvalues[2] - 1/2 * Math.pow(tarvalues[4],2);
				changey = seekvalues[1] + seekvalues[3] + 1/2 *seekvalues[5]*seekvalues[5]- tarvalues[1] - tarvalues[3] - 1/2 * Math.pow(tarvalues[5],2);
				//make sure that this is absolute 1, divide by their complete magnitude
				distance = Math.sqrt(changex*changex+changey*changey);
				retval.add(-1 * changex/distance * .1);
				retval.add(-1 * changey/distance * 0.1);
				
			
				break;
			case SUPERADVANCED:
				double time = getTime();
				changex = 0;
				changey = 0;
					time = time *0.98;
					double theta = Math.atan((tarvalues[3] + (tarvalues[1]-seekvalues[1])/time - seekvalues[3])/(tarvalues[2]+(tarvalues[0]-seekvalues[0])/time - seekvalues[2]));
					if (tarvalues[2]+(tarvalues[0]-seekvalues[0])/time - seekvalues[2] < 0) {
						theta = theta + Math.acos(-1);
					}
					changex = Math.cos(theta);
					changey = Math.sin(theta);

				if (time < 0) {
					changex = changex* -1;
					changey = changey*-1;
				}
				retval.add( changex*DIFFICULTY);
				retval.add( changey*DIFFICULTY);
				
				break;
			case INSANE:
				retval = useTheInsane();
				break;
				
			case USERIN:
				
				changex = controlaccelx;
				changey = controlaccely;
				retval.add(changex*DIFFICULTY);
				retval.add(changey*DIFFICULTY);
				break;
			case RANDOMRUN:
				keepcount2++;
				if ((keepcount2 % 50) == 0) {
							
					changex = Math.random()*100;
					changey = Math.random()*100;
					int negative = (int)Math.random()*3;
					if (negative == 0) {
						changex = changex * 100 * -1;
						changey = changey*100*-1;
					} else if (negative == 1) {
						changex = changex * 100 * -1;
					} else if (negative == 2) {
						changey = changey * 100*-1;
					}
				}
				
				break;
			default:
				break;
		}		
		retval.add(changex);
		retval.add(changey);
		return retval;
	}
	private static int keepcount = 0;
	synchronized private ArrayList<Double> getAccelerationNeededTarget(double chosenalgo) {
		ArrayList<Double> retval = new ArrayList<Double>();
		double changex = 0, changey = 0, distance;
		switch (((int)chosenalgo)) {
			
			case BASICTRACK:				
				//right now pressume that the point is fixed, so just draw a straight line to it
				changex = tarvalues[0] -seekvalues[0];
				changey = tarvalues[1] - seekvalues[1];
				//make sure that this is absolute 1, divide by their complete magnitude
				distance = Math.sqrt(changex*changex+changey*changey);
				retval.add(-1 * changex/distance * .1);
				retval.add(-1 * changey/distance * 0.1);
				break;
			case LEADTRACK:
				//now step it up, lead it by its current velocity 
				changex = tarvalues[0] + tarvalues[2] -seekvalues[0]-seekvalues[2];
				changey = tarvalues[1] + tarvalues[3] - seekvalues[1] - seekvalues[3];
				//make sure that this is absolute 1, divide by their complete magnitude
				distance = Math.sqrt(changex*changex+changey*changey);
				retval.add(-1 * changex/distance * .1);
				retval.add(-1 * changey/distance * 0.1);
				
				break;
			case ACCELFACT:
				//now step it up, lead it by its current velocity 
				changex = tarvalues[0] + tarvalues[2] + 1/2 * Math.pow(tarvalues[4],2)-seekvalues[0]-seekvalues[2] - 1/2 * Math.pow(seekvalues[4],2);
				changey = tarvalues[1] + tarvalues[3] + 1/2 *tarvalues[5]*tarvalues[5]- seekvalues[1] - seekvalues[3] - 1/2 * Math.pow(seekvalues[5],2);
				//make sure that this is absolute 1, divide by their complete magnitude
				distance = Math.sqrt(changex*changex+changey*changey);
				retval.add(-1 * changex/distance * .1);
				retval.add(-1 * changey/distance * 0.1);
				
			
				break;
			case SUPERADVANCED:
				double time = getTime();
				changex = 0;
				changey = 0;
				//if (time>0) {
					time = time *0.98;
					double theta = Math.atan((seekvalues[3] + (seekvalues[1]-tarvalues[1])/time - tarvalues[3])/(seekvalues[2]+(seekvalues[0]-tarvalues[0])/time - tarvalues[2]));
					if (seekvalues[2]+(seekvalues[0]-tarvalues[0])/time - tarvalues[2] < 0) {
						theta = theta + Math.acos(-1);
					}
					changex = Math.cos(theta);
					changey = Math.sin(theta);
//}
				if (time < 0) {
					changex = changex* -1;
					changey = changey*-1;
				}
				retval.add( changex*DIFFICULTY);
				retval.add( changey*DIFFICULTY);
				
				break;
				
			case ((int)USERIN):
				//this is where we do the bus
				changex = controlacceltargetx;
				changey = controlacceltargety;
				break;
			case ((int)RANDOMRUN) :
				keepcount++;
				if ((keepcount % 50) == 0) {
							
					changex = Math.random()*100;
					changey = Math.random()*100;
					int negative = (int)Math.random()*3;
					if (negative == 0) {
						changex = changex * 100 * -1;
						changey = changey*100*-1;
					} else if (negative == 1) {
						changex = changex * 100 * -1;
					} else if (negative == 2) {
						changey = changey * 100*-1;
					}
				}
				break;
			case INSANE:
				retval = useTheInsane();
				break;
				
			default:
				break;
		}
		retval.add(changex);
		retval.add(changey);

		return retval;
	}
	
	private ArrayList<Double> useTheInsane() {
		ArrayList<Double> retval = new ArrayList<Double>();
		double i = DIFFICULTY;
		double l = -tarvalues[2] + seekvalues[2]; //+seekvalues[4];
		double m = -tarvalues[3] + seekvalues[3]; //+seekvalues[5];

		/*
		 * tarvalues[0] = tarvalues[0] + tarvalues[2]*level;
					tarvalues[1] = tarvalues[1] + tarvalues[3]*level;
		 */
		double j = -(tarvalues[0]) + seekvalues[0];
		double h = tarvalues[3];
		double k = -(tarvalues[1]) + seekvalues[1];
		double a = (-i * i/ 4);
		double b = 0;
		double c = (l*l + m*m);
		double d = 2*j*l+2*k*m;
		double e = j*j+k*k;
		/// ax^4 + b x^3 + c x^2 + d x + e
		double[] ts = solveQuartic(a,b,c,d,e);
		double t = 0;
		double theta = 0;
		//so just pick the first
		if (ts == null || ts.length == 0) {
			
			System.out.println("Yo peta, check your math!");
			//k I need to put a failsafe in here. we are simply going to use another algo for rightnow.
			/*
			double time = getTime();
			double changex = 0;
			double changey = 0;
			//if (time>0) {
			time = time *0.98;
			theta = Math.atan((seekvalues[3] + (seekvalues[1]-tarvalues[1])/time - tarvalues[3])/(seekvalues[2]+(seekvalues[0]-tarvalues[0])/time - tarvalues[2]));
			System.out.println("theta: " + theta);
			if (seekvalues[2]+(seekvalues[0]-tarvalues[0])/time - tarvalues[2] < 0) {
				theta = theta + Math.acos(-1);
			}
			changex = Math.cos(theta);
			changey = Math.sin(theta);
//}
			if (time < 0) {
				changex = changex* -1;
				changey = changey*-1;
			}
			retval.add( changex*DIFFICULTY);
			retval.add( changey*DIFFICULTY);

			System.out.println("x: " + changex*DIFFICULTY + " y: " + changey*DIFFICULTY);
			return retval;
			*/
			for (int q = 0; q < tarvalues.length; q++) {
				//System.out.println("tar: " + q + " is: " + tarvalues[q]);
				//System.out.println("sek: " + q + " is: " + seekvalues[q]);
			}
			double changex = tarvalues[0] + tarvalues[2] + 1/2 * Math.pow(tarvalues[4],2)-seekvalues[0]-seekvalues[2] - 1/2 * Math.pow(seekvalues[4],2);
			double changey = tarvalues[1] + tarvalues[3] + 1/2 *tarvalues[5]*tarvalues[5]- seekvalues[1] - seekvalues[3] - 1/2 * Math.pow(seekvalues[5],2);
			//System.out.println("changex: " + changex + " changey: " + changey);
			//make sure that this is absolute 1, divide by their complete magnitude
			double distance = Math.sqrt(changex*changex+changey*changey);
			//System.out.println("dis: " + distance);
			retval.add(-1 * changex/distance * .1);
			retval.add(-1 * changey/distance * 0.1);
			return retval;
			
		}
		else {
			for (int q = 0; q < ts.length; q++) {
				//System.out.println("root: " + ts[q]);
			}
			t = ts[0];
		}
		if ((m*t+k)<= 0) {
			theta = Math.acos((-2*(l*t+j)/(i*t*t)));
		} else {
			theta = 2* Math.PI - Math.acos((-2*(l*t+j)/(i*t*t)));
		}
		double changex = Math.cos(theta);
		double changey = Math.sin(theta);
		if (!(changex >= 0 || changex < 0)) {
			//backup plan
			changex = tarvalues[0] + tarvalues[2] + 1/2 * Math.pow(tarvalues[4],2)-seekvalues[0]-seekvalues[2] - 1/2 * Math.pow(seekvalues[4],2);
			changey = tarvalues[1] + tarvalues[3] + 1/2 *tarvalues[5]*tarvalues[5]- seekvalues[1] - seekvalues[3] - 1/2 * Math.pow(seekvalues[5],2);
			//System.out.println("changex: " + changex + " changey: " + changey);
			//make sure that this is absolute 1, divide by their complete magnitude
			double distance = Math.sqrt(changex*changex+changey*changey);
			//System.out.println("dis: " + distance);
			retval.add(-1 * changex/distance * .1);
			retval.add(-1 * changey/distance * 0.1);
			return retval;
			
		}
		if (!(changey >= 0 || changey < 0)) {
			//backup plan
			changex = tarvalues[0] + tarvalues[2] + 1/2 * Math.pow(tarvalues[4],2)-seekvalues[0]-seekvalues[2] - 1/2 * Math.pow(seekvalues[4],2);
			changey = tarvalues[1] + tarvalues[3] + 1/2 *tarvalues[5]*tarvalues[5]- seekvalues[1] - seekvalues[3] - 1/2 * Math.pow(seekvalues[5],2);
			//System.out.println("changex: " + changex + " changey: " + changey);
			//make sure that this is absolute 1, divide by their complete magnitude
			double distance = Math.sqrt(changex*changex+changey*changey);
			//System.out.println("dis: " + distance);
			retval.add(-1 * changex/distance * .1);
			retval.add(-1 * changey/distance * 0.1);
			return retval;
			
		}
		
		
		retval.add(changex*DIFFICULTY);
		retval.add(changey*DIFFICULTY);
		//System.out.println("x: " + changex*DIFFICULTY + " y: " + changey*DIFFICULTY);
		return retval;	
	}
					
	synchronized private ArrayList<Double> fixTargetVelocity(double inputalgo) {
		ArrayList<Double> retval = new ArrayList<Double>();
		
		double speed = Math.sqrt(Math.pow(tarvalues[2],2) + Math.pow(tarvalues[3],2));
			//this is the speed that we will normalize them to, after we get the unit vectors.
		double newspeed = Math.sqrt(Math.pow(tarvalues[2]+tarvalues[4],2)+Math.pow(tarvalues[3]+tarvalues[5],2));
		
		double newspeedx = (tarvalues[2] + tarvalues[4])/newspeed*speed;
		double newspeedy = (tarvalues[3] + tarvalues[5])/newspeed*speed;
		//double newspeedx2 = (tarvalues[4]);
		//double newspeedy2 = (tarvalues[5]);
		/*
		old code, depreciated
		double newspeedspeed = Math.sqrt(Math.pow(tarvalues[4],2)+Math.pow(tarvalues[5],2));
		double newspeedx2 = tarvalues[2]; 
		double newspeedy2 = tarvalues[3];
		if (tarvalues[4] != 0 && tarvalues[5] != 0) {
		
		newspeedx2 = (tarvalues[4])/newspeedspeed*speed;
		
		newspeedy2 = (tarvalues[5])/newspeedspeed*speed;
		}
		*/
		/*
		double theta = Math.atan((tarvalues[3]+tarvalues[5])/(tarvalues[2]+tarvalues[4]));
		double newspeedx = speed * Math.cos(theta);
		double newspeedy = speed * Math.sin(theta);
		*/
		retval.add(newspeedx);
		retval.add(newspeedy);
		return retval;
	}
	//see if the   between the target (tarvalues) and the seeker (seekvalues) is less than 30
	synchronized private int checkDistanceAndTime() {
		double distance = Math.sqrt(Math.pow(tarvalues[0]-seekvalues[0],2) + Math.pow(tarvalues[1]-seekvalues[1],2));
		if (distance <= (hitdistance)) {
			return WON;
		}
		else if (distance >= (4000 + 0.25 * (4000*level* DIFFICULTY))) {
			return LOSS;
		}
		else if (Math.abs(System.currentTimeMillis() - startTime) >= (level*5000 + 10*level*500)) {
			System.out.println("You took too long.");
			return LOSS;
		}
		else {
			return NOTHING;
		}
		//return NOTHING;
		
	}
	/**
	  * Sets up the initial state of the Towers Of Hanoi puzzle, with
	  * all the disks on the first pile.
	  */
	synchronized private void setUpProblem() {
		keepcount = 0;
		System.out.println("Hello Commander! I have located the next ICBM from Russia for you to intercept.");
		//fill the arrays with 0's
		for (int i = 0; i < tarvalues.length ; i++) {
			tarvalues[i] = 0;
			seekvalues[i] = 0;
		}
		tarpoints.clear();
		seekpoints.clear();
		
	      /*
		   moveDisk= 0;
	      tower = new int[3][10];
	      for (int i = 0; i < 10; i++)
	         tower[0][i] = 10 - i;
	      towerHeight = new int[3];
	      towerHeight[0] = 10;
	      */ //draw the board
		//generate random tar and seek coords
		startx = (int) (Math.random()*680) + 10;
		starty = (int) (Math.random()*780) + 10;
		seekvalues[0] = startx;
		seekvalues[1] = starty;
		double vel2x = (int)(Math.random()*680) + 10;
		double vel2y = (int)(Math.random()*780) + 10;
		double normalizer2 = Math.sqrt(vel2x*vel2x + vel2y*vel2y);
		vel2x = vel2x/normalizer2;
		vel2y = vel2y/normalizer2;
		vel2x = vel2x*3;
		vel2y = vel2y*3;
		seekvalues[2] = vel2x*DIFFICULTY;
		seekvalues[3] = vel2y*DIFFICULTY;
		
		
		
		tarvalues[0] = (int) (Math.random()*680) + 10;
		tarvalues[1] = (int) (Math.random()*780) + 10;
		//now for velocity O.o
		//aim the target at the center
		double velx = (int)(400-tarvalues[0]);
		double vely = (int)(450-tarvalues[1]);
		double normalizer = Math.sqrt(velx*velx + vely*vely);
		velx = velx/normalizer;
		vely = vely/normalizer;
		velx = velx*3;
		vely = vely*3;
		
		
		
		tarvalues[2] = velx*DIFFICULTY;
		tarvalues[3] = vely*DIFFICULTY;
		tarvalues[4] = 0;
		tarvalues[5] = 0;
		 //playSoundtrack((int)(level%2+2));
		
		//target at 450, 400
		//tarvalues[2] = (int) (Math.random()*3) + 1;
		//tarvalues[3] = (int) (Math.random()*3) + 1;
		
		
		//now to randomly set the velocity of the target, nah nvm that right now
		   if (OSC != null) {
	         Graphics g = OSC.getGraphics();
	         drawCurrentFrame(g,0,0,getTime());
	         g.dispose();
	      }
	      display.repaint();
	   }
	synchronized private Point updatePositions(int accelx, int accely,int taraccelx, int taraccely) {
		//this takes in what ever data you want, spits back to the Point for the accelerator vector.
		
		return new Point(accelx,accely);
	}

   /**
    *  This method is called before starting the solution and after each
    *  move of the solution.  If the status is PAUSE, it waits until
    *  the status changes.  If the status is RESTART, it throws
    *  an IllegalStateExcpetion that will abort the solution.
    *  When this method returns, the value of status must be
    *  RUN oR STEP.
    *     (Note that this method requires synchronization, since
    *  otherwise calling wait() would produce an IllegalMonitorStateException.
    *  However, in fact, it is only called from other synchronized methods,
    *  so it would not be necessary to declare this method synchronized.
    *  Any method that calls it already owns the synchronization lock.)
    */
   synchronized private boolean checkStatus() {
      while (status == PAUSE) {
         try {
            wait();
         }
         catch (InterruptedException e) {
         }
      }
      // At this point, status is RUN, STEP, or RESTART.
      if (status == RESTART) {
    	  return false;
      }
      return true;
      
      // At this point, status is RUN or STEP.
   }


   /**
   









   /**
    * Simple utility method for inserting a delay of a specified
    * number of milliseconds.
    */
   synchronized private void delay(int milliseconds) {
      try {
         wait(milliseconds);
      }
      catch (InterruptedException e) {
      }
   }

   /*
   /**
    * Draw a specified disk to the off-screen canvas.  This is
    * used only during the moveOne() method, to draw the disk
    * that is being moved.  Calls display.repaint() to redraw
    * display using the newly modified image.
    * @param color the color of the disk (use background color to erase).
    * @param disk the number of the disk that is to be drawn, 1 to 10.
    * @param t the number of the pile on top of which the disk is drawn.
    
   //this labels the old area,
   private void putDisk(Color color, int disk, int t) {
      Graphics g = OSC.getGraphics();
      g.setColor(color);



      g.fillRoundRect(75+140*t - 5*disk - 5, 116-12*towerHeight[t], 10*disk+10, 10, 10, 10);
      g.dispose();
      display.repaint();
   }
   //This draws the red lines. call it after you draw the board
   public void drawLines(ArrayList<move> moves, int state) {
		Graphics g = OSC.getGraphics();
	   if (state == 0) {
		   g.setColor(Color.RED);
	   } else {
		   g.setColor(Color.BLUE);
	   }
	   for (move mov: moves) {
			g.drawLine(2*2+mov.toy*40+10+10,2*2 + mov.tox*40+20,2*2+mov.fromy*40+20,2*2 + mov.fromx*40+20);
		}
		g.dispose();
		display.repaint();
	}
   */

   /**
    * Called to draw the current frame, not including the moving disk,
    * if any, which is drawn as part of the moveOne() method.
    */
    /* 
	private static int virtualx = 0;
	private static int virtualy = 0;
      */
	synchronized private void drawCurrentFrame(Graphics g,int accelx, int accely,double time) {
      // Called to draw the current frame.  
		if (!ORBITON) {
			//g.setColor(Color.BLUE);
			super.paintComponent(g); //clear it
			//g.setColor(OSCBACKGROUNDCOLOR);
			//g.fillRect(0,0,getSize().width-1,getSize().height-1);
			
		} 
		g.setColor(Color.RED);
		g.drawRect(0,0,getSize().width-1,getSize().height-1);
		g.drawRect(1,1,getSize().width-3,getSize().height-3);
		//draw the frame
		//get the old cordinates, find the center
		//recenterAASa
		int centerx = (int)((tarvalues[0]) + seekvalues[0])/2;
		int centery = (int)((tarvalues[1]) + seekvalues[1])/2;
		//repoisition the cords. 
		int tempx = (int)(tarvalues[0] - centerx) + 400;
		int tempy = (int)(tarvalues[1] - centery) + 450;
		int tempseekx = (int)(seekvalues[0]-centerx) + 400;
		int tempseeky = (int)(seekvalues[1]-centery) + 450;
		//System.out.println("Center coords: + x: " + centerx + " y: " + centery + " tx: " + tempx + " ty: " + tempy + " sx: " + tempseekx + " sy: " + tempseeky);
		
		//System.out.println("Seeker coords + x: " + tempseekx + " y: " + tempseeky);
		//System.out.println("Target coords + x: " + tempx + " y: " + tempy);
		
		//g.setColor(Color.BLUE);
		if (!ORBITON) {
			if (tarpoints.size() >3) {
				g.setColor(targetColor);
				for (int i = 0; i < tarpoints.size()-1; i++) {
					g.drawLine((int)tarpoints.get(i).getX()-centerx+400 ,(int)tarpoints.get(i).getY()-centery+450,(int)tarpoints.get(i+1).getX()-centerx+400,(int)tarpoints.get(i+1).getY()-centery+450);
				}
			}
			
			if (seekpoints.size() > 3) {
				g.setColor(seekerColor);
				for (int i = 0; i < seekpoints.size() -2; i++) {
					g.drawLine((int)seekpoints.get(i).getX()-centerx+400,(int)seekpoints.get(i).getY()-centery+450,(int)seekpoints.get(i+1).getX()-centerx+400,(int)seekpoints.get(i+1).getY()-centery+450);
				}
			}
		}
		g.setColor(Color.WHITE);
		g.drawOval((int)tempx-3,(int)tempy-3,6,6);
		g.setColor(targetColor);
		g.fillOval((int)tempx-(hitdistance)/2,(int)tempy-(hitdistance)/2,hitdistance,hitdistance);
		
		g.setColor(Color.WHITE);
		g.drawOval((int)tempseekx-3, (int)tempseeky-3,6,6);
		g.setColor(seekerColor);
		g.fillOval((int)tempseekx-3,(int)tempseeky-3,6,6);
		
		//g.setColor(Color.BLUE);
		//g.drawLine((int)seekvalues[0],(int)seekvalues[1], (int)seekvalues[0]-10*accelx, (int)seekvalues[1]-10*accely);
		
		g.setColor(seekerColor);
		g.drawLine((int)seekvalues[0]-centerx,(int)seekvalues[1]-centery, (int)seekvalues[0]+5*accelx-centerx, (int)seekvalues[1]+5*accely-centery);
		//g.setColor(Color.WHITE);
		
		//g.fillRect(800,0,900,800);
		g.setColor(Color.BLACK);
		g.setFont(new Font("Serif", Font.BOLD, 14));
		String message = "Lowest time to impact: " + time;
		g.drawString(message,710,605);
		
		int dis = (int)(Math.sqrt(Math.pow((seekvalues[0]-tarvalues[0]),2)+Math.pow((seekvalues[1]-tarvalues[1]),2)));
		message = "Distance to impact: " + dis;
		g.drawString(message,710,625);
		double closingvel = (Math.sqrt(Math.pow((seekvalues[2]-tarvalues[2]),2)+Math.pow((seekvalues[3]-tarvalues[3]),2)));
		message = "Closing velocity: " + closingvel;
		g.drawString(message,710,645);
		message = "Time To Hit: " + (System.currentTimeMillis()-(startTime + level*5000 + 10*level*500));
		g.drawString(message,710,665);
		
		//				System.out.println("Scoreboard. Target: " + wincountarget + "   Seeker: " + wincountseek);
		message = " Target:  " + wincountarget + " ||   Seeker: " + wincountseek;
		g.drawString(message,60,625);
		
		//now draw the little blue arrow in the bottom right
		/*
		changex = seekvalues[0] -tarvalues[0];
				changey = seekvalues[1] - tarvalues[1];
				//make sure that this is absolute 1, divide by their complete magnitude
				distance = Math.sqrt(changex*changex+changey*changey);
				retval.add(-1 * changex/distance * .1);
				retval.add(-1 * changey/distance * 0.1);
		*/
		//the magnitude of these lines will be 25 radius
		double distance = getDistance();
		double magnitude = 25;
		double minimag = 5;
		double changex = (tarvalues[0]-seekvalues[0])/distance * magnitude;
		double changey = (tarvalues[1]-seekvalues[1])/distance * magnitude;
		double minichangex = changex/magnitude*minimag;
		double minichangey = changey/magnitude*minimag;
		g.setColor(Color.BLUE);
		g.drawOval(425,325,50,50);
		g.drawLine(450,350, (int)(450 + changex),(int)( 350 + changey)	 ); //draw two little tiny thingies at the end of this.
		g.setColor(new Color(185,47,176));//Color.PURPLE);
		g.drawLine((int)(450+changex),(int)(350+changey),(int)(450+changex+minichangex),(int)(350+changey));//-minichangey));
		
		g.drawLine((int)(450+changex),(int)(350+changey),(int)(450+changex-minichangex),(int)(350+changey+minichangey));
		//g.drawLine((int)(
		
		

	}
	public static synchronized void playSoundtrack() {
		playSoundtrack(SOUNDTRACKONE);
	}
	public static synchronized void playSoundtrack(int track) {
		secondSoundTrack.play();
		/*
		switch (track) {
			case SOUNDTRACKONE:
				if (mainSoundTrack != null) {
					mainSoundTrack.play(); //playAudioResource("soundtrack.wav",SOUNDTRACKONE);
				}
				break;
			case SOUNDTRACKTWO:
				if (secondSoundTrack != null) {
					secondSoundTrack.play(); // playAudioResource("soundtrack1.wav",SOUNDTRACKTWO);
				}
				break;
			case SOUNDTRACKTHREE:
				if (thirdSoundTrack != null) {
					thirdSoundTrack.play(); // playAudioResource("soundtrack2.wav",SOUNDTRACKTHREE);
				}
				break;
			default:
				if (mainSoundTrack != null) {
					mainSoundTrack.play();//playAudioResource("soundtrack.wav",SOUNDTRACKONE);
				}
		}
		//volume off
		*/
		
	}
	public static synchronized void playCollision() {
		if (collisionTrack != null) {
			collisionTrack.play(); // playAudioResource("747crash.wav");
		}
	}
	public static synchronized void playSonicBoom() {
		if (sonicTrack != null) {
			sonicTrack.play(); // playAudioResource("lion.wav");
		}
	}
	/*
	public static synchronized void playSound(final String url) {
		new Thread(new Runnable() {
			public void run() {
				try {
					Clip clip = new AudioSystem.getClip();
					AudioInputStream inputStream = AudioSystem.getAudioInputStream(Main.class.getResourcesAsStream(url));
					clip.open(inputStream);
					clip.start();
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
		}).start();
		private static final int SOUNDTRACKONE = 1;
	private static final int SOUNDTRACKTWO = 2;
	private static final int SOUNDTRACKTHREE = 3;
	}*/
	private static AudioClip mainSoundTrack = null;
	private static AudioClip secondSoundTrack = null;
	private static AudioClip thirdSoundTrack = null; //sound = null;
	private static AudioClip collisionTrack = null;
	private static AudioClip sonicTrack = null;
	private static URL resourceURL = null;
	
	public static synchronized void playAudioResource(String audioResourceName) {
		
		ClassLoader cl = MissleCalcSelf4.class.getClassLoader();
		URL resourceURL = cl.getResource(audioResourceName);
		if (resourceURL != null) {
			sound = JApplet.newAudioClip(resourceURL);
			sound.play();
		}
		
	}
	
	public static synchronized void loadAudioResources() {
		ClassLoader cl = MissleCalcSelf4.class.getClassLoader();
		//now we iterate through
		resourceURL = cl.getResource("Resources/lion.wav");
		if (resourceURL != null) {
			sonicTrack = JApplet.newAudioClip(resourceURL);
		}
		resourceURL = cl.getResource("Resources/soundtrack.wav");
		if (resourceURL != null) {
			mainSoundTrack = JApplet.newAudioClip(resourceURL);
		}
		resourceURL = cl.getResource("Resources/soundtrack1.wav");
		if (resourceURL != null) {
			secondSoundTrack = JApplet.newAudioClip(resourceURL);
		}
		resourceURL = cl.getResource("Resources/soundtrack2.wav");
		if (resourceURL != null) {
			thirdSoundTrack = JApplet.newAudioClip(resourceURL);
		}
		resourceURL = cl.getResource("Resources/747crash.wav");
		if (resourceURL != null) {
			collisionTrack = JApplet.newAudioClip(resourceURL);
		}
		//now you can just play each track individually.
	}
	
	public double getDistance() {
		return Math.sqrt(Math.pow(tarvalues[0]-seekvalues[0],2) + Math.pow(tarvalues[1]-seekvalues[1],2));
	}

	/* Whoa, this actually works. I was not expecting that. 
	  * These are polynomial solver methods.
            */
	public static boolean checkSolutions(double[] roots) {
		if (roots != null) {
			if (roots.length > 0) {
				return true;
			}
		}
		return false;
		
	}
	// ax + b = 0;
	//Special acknowledgements go to: pTymN on http://www.gamedev.net/topic/451048-best-way-of-solving-a-polynomial-of-the-fourth-degree/ for these algorithumns
	public static double[] solveLinear(double a, double b) {
		double[] retval = new double[1];
		if ((a == 0) && (b != 0)) {
			return null;
		} else if (a == 0) {
			retval[0] = 0;
			return retval;
		} else {
		
			retval[0] = -b/a;
		}
		
		return retval;
	
	}
	//ax^2 + bx + c = 0
	public static double[] solveQuadratic(double a, double b, double c) {
		double[] retval = new double[2];
		
		if (a == 0 || Math.abs(a/b) < .00001) {
			return solveLinear(b,c);
		}
		
		double discriminant = b*b - 4.0 * a * c;
		if (discriminant >= 0.0) {
			discriminant = Math.sqrt(discriminant);
			double root1 = (-1 * b + discriminant) / (2 * a);
			double root2 = (-1 *b - discriminant) / (2* a);
			retval[0] = root1;
			retval[1] = root2;
			return retval;
		}
		else {
			return null;
		}
		
	}
	// ax^3 + bx^2 + cx + d = 0;
	public static double[] solveCubic(double a, double b, double c, double d) {
		double[] retval = new double[3];
		
		
		if (a == 0 || Math.abs(a/b) < .00001) {
			return solveQuadratic(b,c,d);
		}
		
		double B = b/a;
		double C = c/a;
		double D = d/a;
		double Q = (B*B - C*3.0)/9.0;
		double QQQ = Q * Q * Q;
		double R = (2.0*B*B*B - 9.0*B*C + 27.0 * D)/54.0;
		double RR = R*R;
		double root = 0;
		
		if (RR<QQQ) {
			double theta = Math.acos(R/Math.sqrt(QQQ));
			double r1,r2,r3;
			r1 = r2 = r3 = -2.0*Math.sqrt(Q);
			r1 = r1 *Math.cos(theta/3.0);
			r2 = r2 * Math.cos((theta+2*Math.PI)/3.0);
			r3 = r3 * Math.cos((theta-2*Math.PI)/3.0);
			
			r1 = r1 - B/3.0;
			r2 = r2 - B/3.0;
			r3 = r3 - B/3.0;
			//double root = 1000000.0;
			retval[0] = r1;
			retval[1] = r2;
			retval[2] = r3;
			return retval;
		} else { //1 real root
			double A2 = -Math.pow(Math.abs(R)+Math.sqrt(RR-QQQ), 1.0/3.0);
			if (A2 != 0.0) {
				if (R<0.0)
					A2 = -A2;
				root = A2 + Q/A2;
			}
			root = root - B/3.0;
			double[] ret = new double[1];
			ret[0] = root;
			return ret;

		}
	
	}
	
	
	public static double[] solveQuartic(double a, double b,  double c, double d, double e) {
		double[] retval = new double[4];
		
		if ( a== 0 || Math.abs(a/b) < .000001 || Math.abs(a/c) < 0.000001 || Math.abs(a/d) < 0.000001) {
			return solveCubic(b,c,d,e);
		}
		
		double B = b/a;
		double C = c/a;
		double D = d/a;
		double E = e/a;
		double BB = B * B;
		double I = -3.0 * BB * 0.125 + C;
		double J = BB*B*0.125 - B*C*0.5 + D;
		double K = -3 * BB * BB/256.0 + C*BB/16.0 -B*D*0.25 + E;
		
		double z;
		boolean foundRoot2 = false;
		boolean foundRoot3 = false;
		boolean foundRoot4 = false;
		boolean foundRoot5 = false;
		//one of these needs to be stoed as z
		double[] values = solveCubic(1.0,2*I, I*I - 4*K, -1 * (J*J));
		if (checkSolutions(values)) {
			z = values[0];			
			double value = z*z*z + z*z*(2*I) + z* (I*I - 4*K) - J*J;
			double p = Math.sqrt(z);
			double r = -1 * p;
			double q = (I + z - J/p) *0.5;
			double s = (I + z + J/p) * 0.5;
			double root;
			
			boolean foundRoot = false;
			double aRoot;
			double[] quadroots = solveQuadratic(1.0,p,q);
			if (checkSolutions(quadroots)) {
				for (int i = 0; i < quadroots.length; i++) {
					quadroots[i] = quadroots[i] - B/4.0;
				}			
			}
			double[] quad2roots = solveQuadratic(1.0,r,s);
			if (checkSolutions(quad2roots)) {
				for (int i = 0; i < quad2roots.length; i++) {
					quad2roots[i] = quad2roots[i] - B/4.0;
				}
			}
			int lengtharray = 0;
			if (quadroots != null) {
				lengtharray = quadroots.length + lengtharray;
			}
			if (quad2roots != null) {
				lengtharray = quad2roots.length + lengtharray;
			}
			//now create that array, add the values, and return it
			int x = 0;
			double[] retarray = new double[lengtharray];
			if (quadroots != null) {
				for (int i = 0; i < quadroots.length; i++) {
					retarray[i] = quadroots[i];
				}
				x = quadroots.length;
			}
			
			if (quad2roots != null) {
				for (int i = 0; i < quad2roots.length; i++) {
					retarray[i+x] = quad2roots[i];
				}
			}
			return retarray;
			
		}
			
		
		
		return null;
		
	}


} // end class
