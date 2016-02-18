package org.usfirst.frc.team4787.robot;

import edu.wpi.first.wpilibj.Servo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a wrapper that provides: a custom Servo() constructor that specifies
 * limits and movement speed stepForward() and stepBackward() methods for easy
 * operation
 *
 * Use only with angles, not 0-to-1 set() stuff.
 *
 */
public class ServoWrapper extends Servo {

    public final static double DEFAULT_ANGLE_POS = 00;
    public final static double DEFAULT_MIN_ANGLE = -5;
    public final static double DEFAULT_MAX_ANGLE = 80;
    public final static double DEFAULT_ANGLE_STEP = .6;

    private double minAngle, maxAngle, degreesPerStep, angularPosition;

    /**
     * Create wrapper with default values for 2016 robot's servo
     */
    ServoWrapper(int pwm_pin) {
        this(pwm_pin, DEFAULT_MIN_ANGLE, DEFAULT_MAX_ANGLE, DEFAULT_ANGLE_POS, DEFAULT_ANGLE_STEP);
    }

    /**
     * Create a new ServoWrapper object, specifying port #, min/max angles, position, and step values
     */
    ServoWrapper(int pwm_pin, double angleMin, double angleMax, double anglePosition, double angleStep) {
        super(pwm_pin);
        this.setMin(angleMin);
        this.setMax(angleMax);
        this.setStep(angleStep);
        this.setPos(anglePosition);
    }

    /** Set a minimum angle for the servo.
     *
     * @param newMinAngle From 0 to 180, unless your servo allows movement out of
     * bounds and Servo allows it (unlikely)
     */
    public final void setMin(double newMinAngle) {
        if (newMinAngle < 0) {
            Logger.getLogger(ServoWrapper.class.getName()).log(Level.WARNING,
                    "Setting servo minimum to strange value:{0}", newMinAngle);
        }
        minAngle = newMinAngle;
        setPos(angularPosition); // update position against new min
    }

    /** Set a maximum angle for the servo.
     *
     * @param newMaxAngle From 0 to 180, unless your servo allows movement out of
     * bounds and Servo allows it (unlikely)
     */
    public final void setMax(double newMaxAngle) {
        if (newMaxAngle > 180) {
            Logger.getLogger(ServoWrapper.class.getName()).log(Level.WARNING,
                    "Setting servo maximum to strange value: {0}", newMaxAngle);
        }
        maxAngle = newMaxAngle;
        setPos(angularPosition); // update position against new max
    }

    /** Set the value, in degrees, that the servo will move every time step
     * methods are called.
     *
     * @param newStepValue In degrees
     */
    public final void setStep(double newStepValue) {
        if (newStepValue > 3 || newStepValue < .01) {
            Logger.getLogger(ServoWrapper.class.getName()).log(Level.WARNING,
                    "Setting servo step to strange value: {0}", newStepValue);
        }
        if (newStepValue <= 0) {
            Logger.getLogger(ServoWrapper.class.getName()).log(Level.SEVERE, 
                    "Illegal value for servo step!  Reverting to builtin value: {0}", DEFAULT_ANGLE_STEP);
            setStep(DEFAULT_ANGLE_STEP);
        } else {
            degreesPerStep = newStepValue;
        }
    }

    /** Move the servo to a position between minAngle and maxAngle.  If out of
     * bounds, move to the boundary specified in min/max.
     *
     * @param angle
     */
    public final void setPos(double angle) {
        if (angle > maxAngle) {
            angularPosition = maxAngle;
            return;
        }
        if (angle < minAngle) {
            angularPosition = maxAngle;
            return;
        }
        angularPosition = angle;
        super.setAngle(angularPosition);
    }

    /** Calls setPos.
     *
     * @param angle
     */
    public final void setAngle(double angle) {
        setPos(angle);
    }
    
    /**
     * Don't want to do any math? Enter the delay between function calls (or 1 /
     * frequency) and desired speed in degrees per second. setStep() will be
     * called with the appropriate value. For example:
     *
     * setStepFromDelay(.005, 15); // delay value taken from your main robot
     * code 15 degrees/sec * .005 seconds = .075 degrees in each tick Therefore
     * setStep(.075) is called.
     *
     * @param updateDelay time between stepFwd() and stepBwd() calls
     * @param angularSpeed desired speed of servo, in degrees per second.
     */
    public void setStepFromUpdateDelay(double updateDelay, double angularSpeed) {
        setStep(updateDelay * angularSpeed);
    }

    /**
     * Don't want to do any math? Enter update frequency in Hz and desired speed
     * in degrees per second. setStep() will be called with the appropriate
     * value. For example:
     *
     * setStepFromDelay(200, 15); 15 degrees/second / 200 ticks/second = 0.75
     * degrees/tick Therefore setStep(.075) is called.
     * @param updateFrequency  frequency of stepFwd() and stepBwd() calls, in Hz
     * @param angularSpeed desired speed of servo, in degrees per second
     */
    public void setStepFromUpdateFrequency(double updateFrequency, double angularSpeed) {
        setStep(angularSpeed / updateFrequency);
    }
    
    /** Move the servo forward by the value earlier specified by setStep.
     * (Forward means a higher angle, or clockwise on Hitec servos.)
     */
    public void stepFwd() {
        setPos(angularPosition + degreesPerStep);
    }
    
    /** Move the servo backward by the value earlier specified by setStep.
     * (Backward means a higher angle, or counterclockwise on Hitec servos.)
     */
    public void stepBwd() {
        setPos(angularPosition - degreesPerStep);
    }

}
