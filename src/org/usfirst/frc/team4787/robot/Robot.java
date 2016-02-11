
package org.usfirst.frc.team4787.robot;


import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDeviceStatus;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer; 
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Servo;

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
	
	final int FLY1_CAN = 0, FLY2_CAN = 1, ANG_CAN = 2;
	final int BOGLEFT1_PWM = 0, BOGLEFT2_PWM = 1, BLEFT_PWM = 2, BRIGHT_PWM = 3, BOGRIGHT1_PWM = 4, BOGRIGHT2_PWM = 5;
	final int JOYSTICK_USB = 0, MECHSTICK_USB = 1;
	final int SERVO_PWM = 6;
	
	final int FLYWHEELS_SHOOTRATE = 300, FLYWHEELS_GRABRATE = -30;
	
	CANTalon fly1 = new CANTalon(FLY1_CAN);
	CANTalon fly2 = new CANTalon(FLY2_CAN);
	CANTalon angler = new CANTalon(ANG_CAN);
	
	Jaguar bogieLeft1 = new Jaguar(BOGLEFT1_PWM);
	Jaguar bogieLeft2 = new Jaguar(BOGLEFT2_PWM);
	Jaguar backLeft = new Jaguar(BLEFT_PWM);
	Jaguar backRight = new Jaguar(BRIGHT_PWM);
	Jaguar bogieRight1 = new Jaguar(BOGRIGHT1_PWM);
	Jaguar bogieRight2 = new Jaguar(BOGRIGHT2_PWM);
    
	Joystick drivestick = new Joystick(JOYSTICK_USB);
    Joystick mechstick = new Joystick(MECHSTICK_USB);

    Servo ballpusher = new Servo(SERVO_PWM);
    
    double mechX, mechY, x, y, z;
	
	int session; Image frame, binaryFrame; // Vision bois
	
    final double DEADZONEX = 0.05, DEADZONEY = 0.05, ANGLER_RAMPRATE = 3.0; //No more than a change of 3V per second
	
    RobotDrive myRobot;
    Joystick stick; 

    public Robot() {
        stick = new Joystick(0);
        
        // Vision Dashboard code
    	frame = NIVision.imaqCreateImage(ImageType.IMAGE_RGB, 0);
		binaryFrame = NIVision.imaqCreateImage(ImageType.IMAGE_U8, 0);
		
    	// The camera name (ex "cam0") can be found through the roborio web interface
        session = NIVision.IMAQdxOpenCamera("cam0", NIVision.IMAQdxCameraControlMode.CameraControlModeController);
        NIVision.IMAQdxConfigureGrab(session);
        
    }
    
    public void robotInit() {
    	angler.changeControlMode(CANTalon.TalonControlMode.Position);
    	fly1.changeControlMode(CANTalon.TalonControlMode.Speed);
    	fly2.changeControlMode(CANTalon.TalonControlMode.Speed);
    	fly1.set(0);
    	fly2.set(0);
    	angler.setVoltageRampRate(ANGLER_RAMPRATE);
    	fly1.setFeedbackDevice(FeedbackDevice.QuadEncoder);
    	fly2.setFeedbackDevice(FeedbackDevice.QuadEncoder);
    	angler.setFeedbackDevice(FeedbackDevice.QuadEncoder);
    	
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
    	
    }

    /**
     * Runs the motors with arcade steering.
     */
    public void operatorControl() {
    	NIVision.IMAQdxStartAcquisition(session);
        myRobot.setSafetyEnabled(true);
        
        while (isOperatorControl() && isEnabled()) {
        	
        	NIVision.IMAQdxGrab(session, frame, 1);
    		CameraServer.getInstance().setImage(frame);

            x = drivestick.getX();
            y = drivestick.getY();
            mechX = mechstick.getX();
            mechY = mechstick.getY();
            z = mechstick.getZ();

            if(Math.abs(x) > DEADZONEX || Math.abs(y) > DEADZONEY){
                System.out.println("x: " + x + "  y: " + y + " z: " + z);
                bogieLeft1.set(x + -y );
                bogieLeft2.set(x + -y );
                backLeft.set(x + -y);
                backRight.set(x + y);
                bogieRight1.set(x + y);
                bogieRight2.set(x + y);
                
            }
            else
            {
                bogieLeft1.set(0);
                bogieLeft2.set(0);
                backLeft.set(0);
                backRight.set(0);
                bogieRight1.set(0);
                bogieRight2.set(0);
            }

            if(Math.abs(mechX) > DEADZONEX || Math.abs(mechY) > DEADZONEY)
            {
                //angler.set we will assume it sets it to a given position between 0 and 1
                //y axis for angler, 2 for pulling in, 3 for out, trigger for shoot
                
            	//ups = updates per second (based on Timer.delay(0.005))
            	//rpm/60 / ups = how much it revolves per update
            	
            	//angle (always between -20 and 20 (<-- for example, idk what range of motion we'll actually use))
            	
            	/*
            	 * int mechSign = Math.sign(mechY)
            	 * if(angle * mechSign/)
            	 */
            	
                
            }
            else
            {


            }
            
            
            //buttons 2 & 3
            //trigger = new JoystickButton(drivestick, 1);
            boolean flyOutButton = mechstick.getRawButton(3);
            boolean flyInButton = mechstick.getRawButton(2);
            
            if(flyOutButton)
            {
            	fly1.set(FLYWHEELS_SHOOTRATE);//don't know what to set it to
            	fly2.set(-FLYWHEELS_SHOOTRATE);
            }

            else if(flyInButton)
            {
            	fly1.set(FLYWHEELS_GRABRATE);//not sure which should be + and -
            	fly2.set(-FLYWHEELS_GRABRATE);
            }
            
            else
            {
            	fly1.set(0);
            	fly2.set(0);
            }
            
            Timer.delay(0.005);		// wait for a motor update time
        }
    }

    /**
     * Runs during test mode
     */
    public void test() {
    }
}
