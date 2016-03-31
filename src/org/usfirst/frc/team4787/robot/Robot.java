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
	final int ANG_CAN = 1, FLY2_CAN = 2, FLY1_CAN = 0;
	final int BOGLEFT1_PWM = 0, BOGLEFT2_PWM = 1, BLEFT_PWM = 2,
			BRIGHT_PWM = 3, BOGRIGHT1_PWM = 4, BOGRIGHT2_PWM = 5;
	final int JOYSTICK_USB = 0, MECHSTICK_USB = 1;
	final int MECHSERVO_PWM = 9, CAMSERVO_PWM = 6;
	final int FIRE_BTN = 5, FLYIN_BTN = 2, FLYOUT_BTN = 3, 
			WHEELIE_BTN = 1, RLEFT_BTN = 7, RRIGHT_BTN = 8;

	// mechanical configuration and constants
	final int FLYWHEELS_SHOOTRATE = 300, FLYWHEELS_GRABRATE = -30;
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
	double mechX, mechY, x, y, z, trim, autoPower, autoTime;
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
	double angP = 0;
	double angI = 0;
	double angD = 0;

	// Motor and joystick initializations
	CANTalon fly1 = new CANTalon(FLY1_CAN);
	CANTalon fly2 = new CANTalon(FLY2_CAN);
	CANTalon angler = new CANTalon(ANG_CAN);

	Talon bogieLeft1 = new Talon(BOGLEFT1_PWM);
	Talon bogieLeft2 = new Talon(BOGLEFT2_PWM);
	Victor backLeft = new Victor(BLEFT_PWM);
	Victor backRight = new Victor(BRIGHT_PWM);
	Talon bogieRight1 = new Talon(BOGRIGHT1_PWM);
	Talon bogieRight2 = new Talon(BOGRIGHT2_PWM);
	SerialPort serial = new SerialPort(19200, SerialPort.Port.kOnboard); // onboard
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
	Joystick mechstick = new Joystick(MECHSTICK_USB);

	ServoWrapper ballPusher = new ServoWrapper(MECHSERVO_PWM, pusherMinAngle,
			pusherMaxAngle, pusherAnglePos, pusherAngleStep);
	Servo camServo = new Servo(CAMSERVO_PWM);
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
		NIVision.IMAQdxSetAttributeU32(session,
				"AcquisitionAttributes::VideoMode", 93);
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

		sensorCheck();
	}

	/**
	 * Sanity check for the encoders
	 */
	public void sensorCheck() {
		FeedbackDeviceStatus status1 = fly1
				.isSensorPresent(FeedbackDevice.QuadEncoder);
		FeedbackDeviceStatus status2 = fly2
				.isSensorPresent(FeedbackDevice.QuadEncoder);
		FeedbackDeviceStatus status3 = angler
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
		switch (status3) {
		case FeedbackStatusPresent:
			System.out.println("Angler sensor functional");
		case FeedbackStatusNotPresent:
			System.out.println("Angler sensor nonfunctional");
			// terminate robot? or just a warning?
		case FeedbackStatusUnknown:
			System.out.println("Angler sensor status unknown");
		}
	}

	// Run before auto starts. Used for pulling dashboard values to be used
	// in the operation of the bot
	public void autoInit() {
		flyP = prefs.getDouble("Flywheels P", flyP);
		flyI = prefs.getDouble("Flywheels I", flyI);
		flyD = prefs.getDouble("Flywheels D", flyD);
		trim = prefs.getDouble("Trim", 0); // defaults to 0 trim. -1 is all power to left, 1 is all power to right
		autoPower = prefs.getDouble("Autonomous Power to Wheels", 0); // defaults
																		// to NO
																		// auto
		autoTime = prefs.getDouble("Autonomous time before stop", 0); // defaults
																		// to NO
																		// auto
		alliance = prefs.getBoolean("Red Alliance", false); // defaults to blue
															// alliance
		serial.writeString("G#FFFFFF:#" + (alliance ? "FF0000" : "0000FF")
				+ ":15"); // Gradient at beginning of auto
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
		serial.writeString("V#" + allianceColor);
		NIVision.IMAQdxStartAcquisition(session);
		initializeMechanism();

		while (isOperatorControl() && isEnabled()) {

			NIVision.IMAQdxGrab(session, frame, 1);
			CameraServer.getInstance().setImage(frame);

			x = drivestick.getX();
			y = drivestick.getY();
			mechX = mechstick.getX();
			mechY = mechstick.getY();
			z = drivestick.getZ();

			if (Math.abs(x) > DEADZONEX || Math.abs(y) > DEADZONEY) {
				trimRight = Math.signum(trim) > 0 ? 1-trim : 1
				trimLeft = Math.signum(trim) < 0 ? 1-Math.abs(trim) : 1
				if (!drivestick.getRawButton(WHEELIE_BTN)) {
					bogieLeft1.set(trimLeft*(x + -y));
					bogieLeft2.set(trimLeft*(x + -y)); // these two need to be reversed
					bogieRight1.set(trimRight*(x + y));
					bogieRight2.set(trimRight*(x + y));
				} else {
					bogieLeft1.set(0);
					bogieLeft2.set(0);
					bogieRight1.set(0);
					bogieRight2.set(0);
				}
				if (!rearLeftDisable) {
					backLeft.set(trimLeft*(x - y));
				} else {
					backLeft.set(0);
				}
				if (!rearRightDisable) {
					backRight.set(trimRight*(x + y);
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



			if (mechstick.getRawButton(RLEFT_BTN)) { // replace magic number
				if ((Timer.getFPGATimestamp() - rearLeftCooldown) > COOLTIME) {
					rearLeftDisable = !rearLeftDisable;
				}
			}
			if (mechstick.getRawButton(RRIGHT_BTN)) { // replace magic number
				if ((Timer.getFPGATimestamp() - rearRightCooldown) > COOLTIME) {
					rearRightDisable = !rearRightDisable;
				}
			}

			camServo.setAngle(z * 90 + 180);
			

			boolean flyOutButton = mechstick.getRawButton(FLYOUT_BTN);
			boolean flyInButton = mechstick.getRawButton(FLYIN_BTN);
			boolean fireButton = mechstick.getRawButton(FIRE_BTN);

			if (flyOutButton) {
				fly1.set(FLYWHEELS_SHOOTRATE);// don't know what to set it to
				fly2.set(-FLYWHEELS_SHOOTRATE);
				if (fireButton) {
					ballPusher.stepFwd();
				}
				serial.writeString("F#FFFFFF:#" + allianceColor +":.4");
			} else if (flyInButton) {
				fly1.set(FLYWHEELS_GRABRATE);// not sure which should be + and -
				fly2.set(-FLYWHEELS_GRABRATE);
				ballPusher.stepBwd();
				serial.writeString("F#FFFFFF:#" + allianceColor +":.4");
			}

			else {
				//while not shooting
				serial.writeString("V#" + allianceColor);
				fly1.set(0);
				fly2.set(0);
				ballPusher.stepBwd();
			}

			Timer.delay(0.005); // wait for a motor update time
		}
	}

	/*
	 * This is run as teleop initializes. Some of this stuff is done in
	 * robotInit but it's done redundantly so as to apply changes to the PID
	 * constants as well as the control modes.
	 */
	private void initializeMechanism() {
		angler.changeControlMode(CANTalon.TalonControlMode.Position);
		fly1.changeControlMode(CANTalon.TalonControlMode.Speed);
		fly2.changeControlMode(CANTalon.TalonControlMode.Speed);
		fly1.setPID(flyP, flyI, flyD);
		fly1.setCloseLoopRampRate(0.0);
		fly1.setIZone(0);
		fly2.setPID(flyP, flyI, flyD);
		fly2.setCloseLoopRampRate(0.0);
		fly2.setIZone(0);
		angler.setPID(angP, angI, angD);
		angler.setIZone(0);
		angler.setCloseLoopRampRate(0.0);
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
