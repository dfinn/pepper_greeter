package international.acme.peppergreeter;

import com.aldebaran.qi.sdk.object.geometry.Vector3;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;

/** For use in testing. **/
public class FakeRobotController implements RobotController
{
    Runnable mOnHumanDetected;
    List<String> SpokenPhrases = new ArrayList<>();
    List<Vector3> MovementsDone = new ArrayList<>();
    List<Integer> AnimationsPerformed = new ArrayList<>();

    @Override
    public Completable speak(String text)
    {
        SpokenPhrases.add(text);
        return Completable.complete();
    }

    @Override
    public Completable moveWithVector(double x, double y, double z)
    {
        MovementsDone.add(new Vector3(x, y, z));
        return Completable.complete();
    }

    @Override
    public Completable animate(int animationResourceId)
    {
        AnimationsPerformed.add(animationResourceId);
        return Completable.complete();
    }

    @Override
    public void watchForHumans(Runnable onHumanDetected)
    {
        mOnHumanDetected = onHumanDetected;
    }

    public void simulateHumanDetection()
    {
        mOnHumanDetected.run();
    }
}
