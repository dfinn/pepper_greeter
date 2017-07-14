package international.acme.peppergreeter;

import android.content.Context;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.geometry.Quaternion;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.TransformTime;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.aldebaran.qi.sdk.object.interaction.Human;
import com.aldebaran.qi.sdk.object.interaction.Interaction;
import com.aldebaran.qi.sdk.object.interaction.Say;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

/** Responsible for controlling input/output operations to the robot. **/
public class RobotControllerImpl implements RobotController
{
    /** If human is detected within this range, trigger greeting. **/
    final int HUMAN_DETECTION_DISTANCE = 3;

    Context mContext;

    public RobotControllerImpl(Context context)
    {
        mContext = context;
    }

    @Override
    public Completable speak(String message)
    {
        return Completable.defer(() ->
        {
            logMsg("Starting say '%s'", message);
            Say say = new Say(mContext);
            return Completable.fromFuture(say.run(message));
        }).subscribeOn(Schedulers.newThread()).doOnComplete(() -> logMsg("speak(%s) completed", message));
    }

    @Override
    public Completable moveWithVector(double x, double y, double z)
    {
        return Completable.defer(() ->
        {
            logMsg("Starting moveWithVector(%.2f, %.2f, %.2f)", x, y, z);
            Quaternion r = new Quaternion(0, 0, 0, 1);
            Vector3 t = new Vector3(x, y, z);
            Transform tf = new Transform(r, t);
            Frame robotFrame = Actuation.get(mContext).robotFrame();
            Frame robotAtStart = robotFrame.makeDetachedFrame(System.currentTimeMillis());
            Frame targetFrame = robotAtStart.makeStaticChildFrame(tf);
            return Completable.fromFuture(new GoTo(mContext).run(targetFrame));
        }).subscribeOn(Schedulers.newThread()).doOnComplete(() -> logMsg("move(%.2f,%.2f,%.2f) completed", x, y, z));
    }

    @Override
    public Completable animate(int resourceId)
    {
        return Completable.defer(() ->
        {
            logMsg("Starting animation '%d'", resourceId);
            Animation animation = Animation.fromResources(mContext, resourceId);
            Animate animate = new Animate(mContext);
            return Completable.fromFuture(animate.run(animation));
        }).subscribeOn(Schedulers.newThread()).doOnComplete(() -> logMsg("animate(%d) completed", resourceId));
    }

    /** Activate listener to watch for humans.  If human is detected within configured range, then trigger greeting. **/
    @Override
    public void watchForHumans(Runnable onHumanDetected)
    {
        logMsg("Watching for humans");
        final Actuation actuation = Actuation.get(mContext);
        Interaction.get(mContext).setHumansAroundListener(humans ->
        {
            logMsg("Detected %d humans", humans.size());
            try
            {
                Frame robotFrame = actuation.robotFrame();
                int i = 0;
                for (Human human : humans)
                {
                    Frame humanFrame = human.getHeadFrame();
                    TransformTime tf = humanFrame.lastKnownTransform(robotFrame).get();
                    double distance = getDistance(tf.getTransform());
                    logMsg("Human detected at distance %.2f", distance);
                    if (distance < HUMAN_DETECTION_DISTANCE) onHumanDetected.run();
                }
            }
            catch (ExecutionException e)
            {
                // TODO: Notify operator of error condition rather than logging silently.
                logError("Error in human detection");
                logException(e);
            }
        });
    }

    private double getDistance(Transform transform)
    {
        Vector3 t = transform.getT();
        double x = t.getX();
        double y = t.getY();
        return Math.sqrt(x * x + y * y);
    }

    private void logMsg(String text, Object... args) { LogHelper.logMsg(getClass(), text, args); }
    private void logError(String text, Object... args) { LogHelper.logError(getClass(), text, args); }
    private void logException(Throwable ex) { LogHelper.logException(getClass(), ex); }
}
