
package org.usfirst.frc.team4787.robot;


import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDeviceStatus;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer; 
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Preferences;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.ImageType;




/**
 * This is a demo program showing the use of the RobotDrive class.
 * The SampleRobot class is the base of a robot application that will automatically call your
 * Autonomous and OperatorControl methods at the right time as controlled by the switches on
 * the driver station or the field controls.
 *
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the SampleRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 *
 * WARNING: While it may look like a good choice to use for your code if you're inexperienced,
 * don't. Unless you know what you are doing, complex code will be much more difficult under
 * this system. Use IterativeRobot or Command-Based instead if you're new.
 */
public class Robot extends SampleRobot {
	

    //CANs, PWMs, USB slots, button config
	final int ANG_CAN  = 1, FLY2_CAN = 2, FLY1_CAN = 0;
	final int BOGLEFT1_PWM = 0, BOGLEFT2_PWM = 1, BLEFT_PWM = 2, BRIGHT_PWM = 3, BOGRIGHT1_PWM = 4, BOGRIGHT2_PWM = 5;
	final int JOYSTICK_USB = 0, MECHSTICK_USB = 1;
	final int MECHSERVO_PWM = 9, CAMSERVO_PWM = 6;
    final int FIRE_BTN = 5, FLYIN_BTN = 2, FLYOUT_BTN = 3, SWITCH_BTN = 4, WHEELIE_BTN = 1, RLEFT_BTN = 11, RRIGHT_BTN = 12;
	
    //mechanical configuration and constants
	final int FLYWHEELS_SHOOTRATE = 300, FLYWHEELS_GRABRATE = -30;
    double pusherAnglePos = 00, pusherMinAngle = -5, pusherMaxAngle = 80, pusherAngleStep = .6;
    double mechScaleFactor = 0.01, mechPos, mechNext;
    double mechMinLimit = 0.05, mechMaxLimit = 0.8; //NOT REAL VALUES
    final int PULSE_REVS = 1316; //188:1 gear ratio and 7 pulses for 1316
    
    //testing parameters and cooldowns
    int motorSwitch = 0;
    boolean mechSwitch = true;
    double switchCooldown = 0, lastTime = 0, rearLeftCooldown = 0, rearRightCooldown = 0; //cooldowns
    final double COOLTIME = .5;
    
    boolean rearLeftDisable = false, rearRightDisable = false;

    //initializations
    double mechX, mechY, x, y, z;
   
    //Vision initializations
    int session; Image frame, binaryFrame;

    //Deadzones, ramprates
    final double DEADZONEX = 0.05, DEADZONEY = 0.05, ANGLER_RAMPRATE = 3.0; //No more than a change of 3V per second
    
    //PID values for angler and flywheels
    double flyP = 2;
    double flyI = 0;
    double flyD = 0;
    double angP = 0;
    double angI = 0;
    double angD = 0;

    //Motor and joystick initializations
	CANTalon fly1 = new CANTalon(FLY1_CAN);
	CANTalon fly2 = new CANTalon(FLY2_CAN);
	CANTalon angler = new CANTalon(ANG_CAN);
	
	Talon bogieLeft1 = new Talon(BOGLEFT1_PWM);
	Talon bogieLeft2 = new Talon(BOGLEFT2_PWM);
	Victor backLeft = new Victor(BLEFT_PWM);
	Victor backRight = new Victor(BRIGHT_PWM);
	Talon bogieRight1 = new Talon(BOGRIGHT1_PWM);
	Talon bogieRight2 = new Talon(BOGRIGHT2_PWM);
    
    Joystick drivestick = new Joystick(JOYSTICK_USB); 
    Joystick mechstick = new Joystick(MECHSTICK_USB);
    
    ServoWrapper ballPusher = new ServoWrapper(MECHSERVO_PWM, pusherMinAngle, pusherMaxAngle, pusherAnglePos, pusherAngleStep); 
    Servo camServo = new Servo(CAMSERVO_PWM);
    Preferences prefs;

    public Robot() {
        
        // Vision Dashboard code
    	frame = NIVision.imaqCreateImage(ImageType.IMAGE_RGB, 0);
		binaryFrame = NIVision.imaqCreateImage(ImageType.IMAGE_U8, 0);
		
    	// The camera name (ex "cam0") can be found through the roborio web interface
        session = NIVision.IMAQdxOpenCamera("cam0", NIVision.IMAQdxCameraControlMode.CameraControlModeController);
        NIVision.IMAQdxConfigureGrab(session);
        
    }
    
    public void robotInit() {
        //NIVision.IMAQdxSetAttributeU32(session, "AcquisitionAttributes::VideoMode", 93);
    	angler.changeControlMode(CANTalon.TalonControlMode.Position);
    	angler.configEncoderCodesPerRev(PULSE_REVS);
    	fly1.changeControlMode(CANTalon.TalonControlMode.Speed);
    	fly2.changeControlMode(CANTalon.TalonControlMode.Speed);
    	fly1.set(0);
    	fly2.set(0);
    	angler.setVoltageRampRate(ANGLER_RAMPRATE);
    	fly1.setFeedbackDevice(FeedbackDevice.QuadEncoder);
    	fly2.setFeedbackDevice(FeedbackDevice.QuadEncoder);
    	angler.setFeedbackDevice(FeedbackDevice.QuadEncoder);

//        flyP = prefs.getDouble("Flywheels P", flyP);
//        flyI = prefs.getDouble("Flywheels I", flyI);
//        flyD = prefs.getDouble("Flywheels D", flyD);
//        angP = prefs.getDouble("Angler P", angP);
//        angI = prefs.getDouble("Angler I", angI);
//        angD = prefs.getDouble("Angler D", angD);

    	FeedbackDeviceStatus status1 = fly1.isSensorPresent(FeedbackDevice.QuadEncoder);
    	FeedbackDeviceStatus status2 = fly2.isSensorPresent(FeedbackDevice.QuadEncoder);
    	FeedbackDeviceStatus status3 = angler.isSensorPresent(FeedbackDevice.QuadEncoder);

    	switch(status1)
    	{
    		case FeedbackStatusPresent:
    			System.out.println("Fly1 sensor functional");
    			
    		case FeedbackStatusNotPresent:
    			System.out.println("Fly1 sensor nonfunctional");
    			//terminate robot? or just a warning?
    		case FeedbackStatusUnknown:
    			System.out.println("Fly1 sensor status unknown");
    	}
    	switch(status2)
    	{
    		case FeedbackStatusPresent:
    			System.out.println("Fly2 sensor functional");
    		case FeedbackStatusNotPresent:
    			System.out.println("Fly2 sensor nonfunctional");
    			//terminate robot? or just a warning?
    		case FeedbackStatusUnknown:
    			System.out.println("Fly2 sensor status unknown");
    	}
    	switch(status3)
    	{
    		case FeedbackStatusPresent:
    			System.out.println("Angler sensor functional");
    		case FeedbackStatusNotPresent:
    			System.out.println("Angler sensor nonfunctional");
    			//terminate robot? or just a warning?
    		case FeedbackStatusUnknown:
    			System.out.println("Angler sensor status unknown");
    	
    	}
    }

    public void autonomous() {
    	 bogieLeft1.set(.6);
         bogieLeft2.set(.6); //these two need to be reversed
         backLeft.set(.6);
         backRight.set(-.6); //investigate these values
         bogieRight1.set(-.6);
         bogieRight2.set(-.6);
         Timer.delay(3);
         bogieLeft1.set(0);
         bogieLeft2.set(0); //these two need to be reversed
         backLeft.set(0);
         backRight.set(0); //investigate these values
         bogieRight1.set(0);
         bogieRight2.set(0);
         
    }

    /**
     * Runs the motors with arcade steering.
     */
    public void operatorControl() {
    	NIVision.IMAQdxStartAcquisition(session);
    	angler.changeControlMode(CANTalon.TalonControlMode.Position);
    	fly1.changeControlMode(CANTalon.TalonControlMode.Speed);
    	fly2.changeControlMode(CANTalon.TalonControlMode.Speed);
    	fly1.setPID(flyP,flyI,flyD);
    	fly1.setCloseLoopRampRate(0.0);
    	fly1.setIZone(0);
    	fly2.setPID(flyP,flyI,flyD);
    	fly2.setCloseLoopRampRate(0.0);
    	fly2.setIZone(0);
    	angler.setPID(angP,angI,angD);
    	angler.setIZone(0);
    	angler.setCloseLoopRampRate(0.0);
        
        while (isOperatorControl() && isEnabled()) {
        	
        	NIVision.IMAQdxGrab(session, frame, 1);
    		CameraServer.getInstance().setImage(frame);

            x = drivestick.getX();
            y = drivestick.getY();
            mechX = mechstick.getX();
            mechY = mechstick.getY();
            z = drivestick.getZ();
            
            

            if(Math.abs(x) > DEADZONEX || Math.abs(y) > DEADZONEY){
            	if(!drivestick.getRawButton(WHEELIE_BTN))
            	{
	                bogieLeft1.set(x + -y);
	                bogieLeft2.set(x + -y); //these two need to be reversed
	                bogieRight1.set(x + y);
	                bogieRight2.set(x + y);
            	}
            	else
            	{
            		bogieLeft1.set(0);
            		bogieLeft2.set(0);
            		bogieRight1.set(0);
            		bogieRight2.set(0);
            	}
            	if(!rearLeftDisable){
            		backLeft.set(x - y);
            	}
            	else{
            		backLeft.set(0);
            	}
            	if(!rearRightDisable){
            		backRight.set(x + y);
            	}
            	else
            	{
            		backRight.set(0);
            	}        
            }
            else
            {
            	//THIS IS IN THEIR PWM ORDER!!! DO NOT REORDER
                bogieLeft1.set(0);
                bogieLeft2.set(0);
                backLeft.set(0);
                backRight.set(0);
                bogieRight1.set(0);
                bogieRight2.set(0);
            }
            
            //cooldown

            if (mechstick.getRawButton(SWITCH_BTN)){
                if ((Timer.getFPGATimestamp() - switchCooldown) > COOLTIME){
                    mechSwitch = !mechSwitch; //Toggle mechswitch
                    String mechStr = mechSwitch ? "primary" : "secondary";
                    System.out.println("Mech switched to " + mechStr); 
                    SmartDashboard.putString("Mech Mode", mechStr);
                }
            }
            
            if (mechstick.getRawButton(RLEFT_BTN)){ //replace magic number
                if ((Timer.getFPGATimestamp() - rearLeftCooldown) > COOLTIME){
                    rearLeftDisable = !rearLeftDisable;               
                }
            }
            if (mechstick.getRawButton(RRIGHT_BTN)){ //replace magic number
                if ((Timer.getFPGATimestamp() - rearRightCooldown) > COOLTIME){
                    rearRightDisable = !rearRightDisable;               
                }
            }
            
            
            camServo.setAngle(z*90 + 180);
//            if(Math.abs(mechY) > DEADZONEY)
//            {
//                if (mechSwitch){ //primary mech
//                	mechNext = 0;
//                    //angler.set we will assume it sets it to a given position between 0 and 1
//                    //y axis for angler, 2 for pulling in, 3 for out, trigger for shoot
//                    
//                	//ups = updates per second (based on Timer.delay(0.005))
//                	//rpm/60 / ups = how much it revolves per update
//                	
//                	/*
//                	 * int mechSign = Math.sign(mechY)
//                	 * if(angle * mechSign/)
//                	 */
//                	mechPos = angler.getEncPosition(); 
//                	mechNext = mechPos + mechScaleFactor * mechY;
//                	if(mechNext > mechMaxLimit)
//                	{
//                		mechNext = mechMaxLimit;
//                	}
//                	else if (mechNext < mechMinLimit)
//                	{
//                		mechNext = mechMinLimit;
//                	}
//                	
//                	//angler.set(mechNext);      
//                }
//                else
//                {
//                    //Secondary mech code 
//                }   
//            }
//            
//            boolean flyOutButton = mechstick.getRawButton(FLYOUT_BTN);
//            boolean flyInButton = mechstick.getRawButton(FLYIN_BTN);
//            boolean fireButton = mechstick.getRawButton(FIRE_BTN);
//
//            if(flyOutButton)
//            {
//            	fly1.set(FLYWHEELS_SHOOTRATE);//don't know what to set it to
//            	fly2.set(-FLYWHEELS_SHOOTRATE);
//            	if (fireButton)
//            	{
//            		//ballPusher.stepFwd();
//            	}
//            }
//
//            else if(flyInButton)
//            {
//            	fly1.set(FLYWHEELS_GRABRATE);//not sure which should be + and -
//            	fly2.set(-FLYWHEELS_GRABRATE);
//            	//ballPusher.stepBwd();
//            }
//            
//            else
//            {
//            	fly1.set(0);
//            	fly2.set(0);
//            	//ballPusher.stepBwd();
//            }
            
            Timer.delay(0.005);		// wait for a motor update time
        }
    }

    CANTalon[] motorList = {fly1, fly2, angler};
    
    
    
//    public void test() 
//    {
//    	SmartDashboard.putNumber("Test Motor: ", motorSwitch);
//    	for (CANTalon m : motorList)
//        {
//        	m.changeControlMode(CANTalon.TalonControlMode.Current);
//        }
//    	while(isTest() && isEnabled()){
//    		y = drivestick.getY();
//    		if (drivestick.getRawButton(1))
//    		{
//    			if ((Timer.getFPGATimestamp() - lastTime) > .5)
//    			{
//    				motorList[motorSwitch].set(0);
//    				lastTime = Timer.getFPGATimestamp();
//    				motorSwitch = (motorSwitch + 1) % motorList.length;
//    				System.out.println("Test Motor: " + motorSwitch);
//    			}
//    		}
//    		
//    		if(Math.abs(y) > DEADZONEY)
//    		{
//    			motorList[motorSwitch].set(y);
//    		}
//    		else
//    		{
//     			motorList[motorSwitch].set(0);
//     		}
//    	}
//    }
    
//    SpeedController[] motorList = {bogieLeft1, bogieLeft2, backLeft, backRight, bogieRight1, bogieRight2};
//    public void test() 
//    {
//    while(isTest() && isEnabled()){
//    	//System.out.println(drivestick.getAxisCount() + ":" + drivestick.getButtonCount());
//    	SmartDashboard.putNumber("Test Motor: ", motorSwitch);
//    	y = drivestick.getY();
//     		if (drivestick.getRawButton(1))
//     		{
//     			if ((Timer.getFPGATimestamp() - lastTime) > .5)
//     			{
//     				motorList[motorSwitch].set(0);
//     				lastTime = Timer.getFPGATimestamp();
//     				motorSwitch = (motorSwitch + 1) % motorList.length;
//     				System.out.println("Test Motor: " + motorSwitch);
//     			}
//     		}
//    		
//     		if(Math.abs(y) > DEADZONEY)
//     		{
//     			motorList[motorSwitch].set(y);
//     		}
//     		else
//     		{
//     			motorList[motorSwitch].set(0);
//     		}
//     	}
//    }
}

