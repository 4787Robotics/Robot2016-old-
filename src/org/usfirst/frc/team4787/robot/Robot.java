
package org.usfirst.frc.team4787.robot;


import edu.wpi.first.wpilibj.SampleRobot;

import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer; 
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.CameraServer;

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
	
	//PLACEHOLDER NUMBERS. REPLACE WITH FINALS {
	CANTalon fly1 = new CANTalon(0);
	CANTalon fly2 = new CANTalon(1);
	CANTalon angler = new CANTalon(2);
	
	Jaguar bogieLeft1 = new Jaguar(0);
	Jaguar bogieLeft2 = new Jaguar(1);
	Jaguar backLeft = new Jaguar(2);
	Jaguar backRight = new Jaguar(3);
	Jaguar bogieRight1 = new Jaguar(4);
	Jaguar bogieRight2 = new Jaguar(5);
    Joystick drivestick = new Joystick(0);
    Joystick mechstick = new Joystick(1);

    Servo ballpusher = new Servo(6);
	// }
	int session; Image frame; Image binaryFrame;
	
    final double DEADZONEX = 0.05, DEADZONEY = 0.05;
	
	
    RobotDrive myRobot;
    Joystick stick; 

    public Robot() {
        myRobot = new RobotDrive(0, 1);
        myRobot.setExpiration(0.1);
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
                //fly.set we will assume it sets it to a given speed
                //angler.set we will assume it sets it to a given position between 0 and 1
                //y axis for angler, 2 for pulling in, 3 for out, trigger for shoot
                
                
            }
            else
            {


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
