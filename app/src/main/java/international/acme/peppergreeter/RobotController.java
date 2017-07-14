package international.acme.peppergreeter;

import com.aldebaran.qi.Future;

import io.reactivex.Completable;

/** Interface defining input/output operations to the robot. **/
public interface RobotController
{
    /** Start async request for the robot to speak the provided text. **/
    Completable speak(String text);

    /** Start async request for the robot to move with the vector provided. **/
    Completable moveWithVector(double x, double y, double z);

    /** Start async request for robot to animate with the animation resource ID provided. **/
    Completable animate(int animationResourceId);

    /** Start watching for humans within the detection distance, and invoke the runnable if one is detected. **/
    void watchForHumans(Runnable onHumanDetected);
}
