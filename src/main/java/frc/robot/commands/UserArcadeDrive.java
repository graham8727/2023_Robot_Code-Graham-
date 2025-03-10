package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Constants.kDrivetrain.Rate;
import frc.robot.subsystems.Drivetrain;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * User arcade drive command One joystick for linear (forward/backward) motion and one for angular
 * motion Applies a nonlinear velocity mapping to the joysticks, and limits the acceleration and
 * deceleration of the drivetrain
 */
public class UserArcadeDrive extends CommandBase {
  private final Drivetrain drivetrain;
  private final SlewRateLimiter slewRateLimiter;
  private final DoubleSupplier linearInput;
  private final DoubleSupplier angularInput;
  private final BooleanSupplier boostInput;

  /**
   * Linear supplier and angular supplier are -1 to 1 double inputs that can be passed as method
   * references or lambda in the ctors Same but boolean for boost Drivetrain subsystem instance is
   * passed in
   */
  public UserArcadeDrive(
      DoubleSupplier linearSupplier,
      DoubleSupplier angularSupplier,
      BooleanSupplier boostSupplier,
      Drivetrain drivetrain) {
    this.drivetrain = drivetrain;
    slewRateLimiter = new SlewRateLimiter(Rate.driverAccel);
    linearInput = linearSupplier;
    angularInput = angularSupplier;
    boostInput = boostSupplier;

    // Use addRequirements() here to declare subsystem dependencies.
    addRequirements(drivetrain);
  }

  // Turn off brakemode when command is scheduled
  @Override
  public void initialize() {
    drivetrain.brakeMode(false);
  }

  // Looping body of the command
  @Override
  public void execute() {
    // Clamp the inputs to the correct range, and apply the nonlinear mapping defined in
    // Drivetrain.java
    double xSpeed = Drivetrain.NonLinear(MathUtil.clamp(linearInput.getAsDouble(), -1.0, 1.0));
    double zRotation = Drivetrain.NonLinear(MathUtil.clamp(angularInput.getAsDouble(), -1.0, 1.0));

    if (Math.abs(xSpeed) <= .1) {
      xSpeed = 0;
    }

    if (Math.abs(zRotation) <= .1) {
      zRotation = 0;
    }
    // Calculate the linear and rotation speeds requested by the inputs using either the boost(max)
    // range, or the driver range
    double linearSpeed = xSpeed * (boostInput.getAsBoolean() ? Rate.maxSpeed : Rate.driverSpeed);
    double angularSpeed = zRotation * Rate.driverAngularSpeed;

    // Apply the calculated speeds to the drivetrain
    drivetrain.driveChassisSpeeds(
        new ChassisSpeeds(slewRateLimiter.calculate(linearSpeed), 0, angularSpeed));
  }

  // Unpower the motors when the command ends or is interuppted
  @Override
  public void end(boolean interrupted) {
    drivetrain.driveChassisSpeeds(new ChassisSpeeds(0, 0, 0));
  }
}
