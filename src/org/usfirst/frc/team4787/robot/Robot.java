package org.usfirst.frc.team4787.robot;

import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDeviceStatus;
import edu.wpi.first.wpilibj.SampleRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.SerialPort;
import edu.wpi.first.wpilibj.SpeedController; //is actually necessary

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.ImageType;

public class Robot extends SampleRobot {

	// CANs, PWMs, USB slots, button config
	final int FLY2_CAN = 1, FLY1_CAN = 0;
	final int BOGLEFT1_PWM = 0, BOGLEFT2_PWM = 1, BLEFT_PWM = 2,
			BRIGHT_PWM = 3, BOGRIGHT1_PWM = 4, BOGRIGHT2_PWM = 5;
	final int JOYSTICK_USB = 0;
	final int MECHSERVO_PWM = 9;
	final int FIRE_BTN = 6, FLYIN_BTN = 3, FLYOUT_BTN = 5, 
			WHEELIE_BTN = 1, RLEFT_BTN = 7, RRIGHT_BTN = 8;

	// mechanical configuration and constants
	double pusherAnglePos = 00, pusherMinAngle = -5, pusherMaxAngle = 80,
			pusherAngleStep = .6;
	double mechScaleFactor = 0.01, mechPos, mechNext;
	double mechMinLimit = 0.05, mechMaxLimit = 0.8; // NOT REAL VALUES
	final int PULSE_REVS = 1316; // 188:1 gear ratio and 7 pulses for 1316

	// testing parameters and cooldowns
	int motorSwitch = 0;
	double lastTime = 0, rearLeftCooldown = 0,
			rearRightCooldown = 0; // cooldowns
	final double COOLTIME = .5;

	boolean rearLeftDisable = false, rearRightDisable = false;

	// initializations
	double mechX, mechY, x, y, z, trim, autoPower, autoTime, trimRight, trimLeft;
	boolean alliance; // true = red, false = blue
	String allianceColor;

	// Vision initializations
	int session;
	Image frame, binaryFrame;

	// Deadzones, ramprates
	final double DEADZONEX = 0.05, DEADZONEY = 0.05, ANGLER_RAMPRATE = 3.0;

	// PID values for angler and flywheels
	double flyP = 2;
	double flyI = 0;
	double flyD = 0;

	// Motor and joystick initializations
	CANTalon fly1 = new CANTalon(FLY1_CAN);
	CANTalon fly2 = new CANTalon(FLY2_CAN);

	Talon bogieLeft1 = new Talon(BOGLEFT1_PWM);
	Talon bogieLeft2 = new Talon(BOGLEFT2_PWM);
	Talon backLeft = new Talon(BLEFT_PWM);
	Victor backRight = new Victor(BRIGHT_PWM);
	Talon bogieRight1 = new Talon(BOGRIGHT1_PWM);
	Talon bogieRight2 = new Talon(BOGRIGHT2_PWM);
	//SerialPort serial = new SerialPort(19200, SerialPort.Port.kOnboard);
																			// serial
	/*
	 * HOW TO USE: write newline terminated strings with particular format:
	 * V#XXXXXX for solid color (where X is hex code) G#XXXXXX:#YYYYYY:T for
	 * pulse from X to Y in T seconds (T being a float) L#XXXXXX:#YYYYYY:T for
	 * looped pulse from X to Y in T seconds S#XXXXXX:#YYYYYY:T for smooth loop
	 * between two colors in T seconds F#XXXXXX:#YYYYYY:T for flashing between
	 * two colors
	 */

	Joystick drivestick = new Joystick(JOYSTICK_USB);

	//ServoWrapper ballPusher = new ServoWrapper(MECHSERVO_PWM, pusherMinAngle,
			//pusherMaxAngle, pusherAnglePos, pusherAngleStep);
	Preferences prefs;

	public Robot() {

		// Vision Dashboard code
		frame = NIVision.imaqCreateImage(ImageType.IMAGE_RGB, 0);
		binaryFrame = NIVision.imaqCreateImage(ImageType.IMAGE_U8, 0);

		// The camera name (ex "cam0") can be found through the roborio web
		// interface
		session = NIVision.IMAQdxOpenCamera("cam0",
				NIVision.IMAQdxCameraControlMode.CameraControlModeController);
		NIVision.IMAQdxConfigureGrab(session);

	}

	public void robotInit() {
		//serial.disableTermination();
		//serial.writeString("V#FFFFFF\n");
		System.out.println("Tried setting color to white.");
		//NIVision.IMAQdxSetAttributeU32(session,
				//"AcquisitionAttributes::VideoMode", 93);
		fly1.set(0);
		fly2.set(0);

		//sensorCheck();
		//while (!isEnabled()) {
			//serial.writeString("V#FFFFFF\n");
		//}
	}

	/**
	 * Sanity check for the encoders
	 */
	public void sensorCheck() {
		FeedbackDeviceStatus status1 = fly1
				.isSensorPresent(FeedbackDevice.QuadEncoder);
		FeedbackDeviceStatus status2 = fly2
				.isSensorPresent(FeedbackDevice.QuadEncoder);

		switch (status1) {
		case FeedbackStatusPresent:
			System.out.println("Fly1 sensor functional");

		case FeedbackStatusNotPresent:
			System.out.println("Fly1 sensor nonfunctional");
			// terminate robot? or just a warning?
		case FeedbackStatusUnknown:
			System.out.println("Fly1 sensor status unknown");
		}
		switch (status2) {
		case FeedbackStatusPresent:
			System.out.println("Fly2 sensor functional");
		case FeedbackStatusNotPresent:
			System.out.println("Fly2 sensor nonfunctional");
			// terminate robot? or just a warning?
		case FeedbackStatusUnknown:
			System.out.println("Fly2 sensor status unknown");
		}
	}

	// Run before auto starts. Used for pulling dashboard values to be used
	// in the operation of the bot
	public void autoInit() {
//		flyP = prefs.getDouble("Flywheels P", flyP);
//		flyI = prefs.getDouble("Flywheels I", flyI);
//		flyD = prefs.getDouble("Flywheels D", flyD);
//		trim = prefs.getDouble("Trim", 0); // defaults to 0 trim. -1 is all power to left, 1 is all power to right
//		autoPower = prefs.getDouble("Autonomous power to wheels", 0); // defaults
//																		// to NO
//																		// auto
//		autoTime = prefs.getDouble("Autonomous time before stop", 0); // defaults
//																		// to NO
//																		// auto
//		alliance = prefs.getBoolean("Red Alliance", false); // defaults to blue
//															// alliance
		trim = 0;
		autoPower = .6;
		autoTime = 3;
		alliance = false;
		//serial.writeString("G#FFFFFF:#" + (alliance ? "FF0000" : "0000FF")
			//	+ ":15"); // Gradient at beginning of auto
	}

	public void autonomous() {
		autoInit();
		bogieLeft1.set(autoPower);
		bogieLeft2.set(autoPower);
		backLeft.set(autoPower);
		backRight.set(-autoPower);
		bogieRight1.set(-autoPower);
		bogieRight2.set(-autoPower);
		Timer.delay(autoTime);
		bogieLeft1.set(0);
		bogieLeft2.set(0);
		backLeft.set(0);
		backRight.set(0);
		bogieRight1.set(0);
		bogieRight2.set(0);
	}

	/**
	 * Runs the motors with arcade steering.
	 */
	public void operatorControl() {
		allianceColor = alliance ? "FF0000" : "0000FF";
		//serial.writeString("V#" + allianceColor);
		NIVision.IMAQdxStartAcquisition(session);

		while (isOperatorControl() && isEnabled()) {

			NIVision.IMAQdxGrab(session, frame, 1);
			CameraServer.getInstance().setImage(frame);

			x = drivestick.getX();
			y = drivestick.getY();
			z = drivestick.getZ();

			if (Math.abs(x) > DEADZONEX || Math.abs(y) > DEADZONEY) {
				//trimRight = Math.signum(trim) > 0 ? 1-trim : 1;
				//trimLeft = Math.signum(trim) < 0 ? 1-Math.abs(trim) : 1;
				trimRight = trimLeft = 1;
				if (!drivestick.getRawButton(WHEELIE_BTN)) {
					bogieLeft1.set(trimLeft*(x + -y));
					bogieLeft2.set(trimLeft*(x + -y)); // these two need to be reversed
					bogieRight1.set(trimRight*(x + y));
					bogieRight2.set(trimRight*(x + y));
					//System.out.println("Set no wheelie");
				} else {
					bogieLeft1.set(0);
					bogieLeft2.set(0);
					bogieRight1.set(0);
					bogieRight2.set(0);
				}
				if (!rearLeftDisable) {
					backLeft.set(trimLeft*(x - y));
					//System.out.println("Rear left set");
				} else {
					backLeft.set(0);
				}
				if (!rearRightDisable) {
					backRight.set(trimRight*(x + y));
				} else {
					backRight.set(0);
				}
			} else {
				// THIS IS IN THEIR PWM ORDER!!! DO NOT REORDER
				bogieLeft1.set(0);
				bogieLeft2.set(0);
				backLeft.set(0);
				backRight.set(0);
				bogieRight1.set(0);
				bogieRight2.set(0);
			}

			// cooldown



			if (drivestick.getRawButton(RLEFT_BTN)) { // replace magic number
				if ((Timer.getFPGATimestamp() - rearLeftCooldown) > COOLTIME) {
					rearLeftDisable = !rearLeftDisable;
				}
			}
			if (drivestick.getRawButton(RRIGHT_BTN)) { // replace magic number
				if ((Timer.getFPGATimestamp() - rearRightCooldown) > COOLTIME) {
					rearRightDisable = !rearRightDisable;
				}
			}

			//camServo.setAngle(z * 90 + 180);
			

			boolean flyOutButton = drivestick.getRawButton(FLYOUT_BTN);
			boolean flyInButton = drivestick.getRawButton(FLYIN_BTN);
			boolean fireButton = drivestick.getRawButton(FIRE_BTN);

			if (flyOutButton) {
				System.out.println("Fly out");
				fly1.set(-.5);// don't know what to set it to
				fly2.set(.5);
				if (fireButton) {
					//ballPusher.stepFwd();
				}
				//serial.writeString("F#FFFFFF:#" + allianceColor +":.4");
			} else if (flyInButton) {
				System.out.println("Fly in");
				fly1.set(.3);// not sure which should be + and -
				fly2.set(-.3);
				//ballPusher.stepBwd();
				//serial.writeString("F#FFFFFF:#" + allianceColor +":.4");
			}

			else {
				//while not shooting
				//serial.writeString("V#" + allianceColor);
				fly1.set(0);
				fly2.set(0);
				//ballPusher.stepBwd();
			}

			Timer.delay(0.005); // wait for a motor update time
		}
	}
	

	SpeedController[] motorList = { bogieLeft1, bogieLeft2, backLeft,
			backRight, bogieRight1, bogieRight2 };

	public void test() {
		while (isTest() && isEnabled()) {
			SmartDashboard.putNumber("Test Motor: ", motorSwitch);
			y = drivestick.getY();
			if (drivestick.getRawButton(1)) {
				if ((Timer.getFPGATimestamp() - lastTime) > .5) {
					motorList[motorSwitch].set(0);
					lastTime = Timer.getFPGATimestamp();
					motorSwitch = (motorSwitch + 1) % motorList.length;
					System.out.println("Test Motor: " + motorSwitch);
				}
			}

			if (Math.abs(y) > DEADZONEY) {
				motorList[motorSwitch].set(y);
			} else {
				motorList[motorSwitch].set(0);
			}
		}
	}

}
