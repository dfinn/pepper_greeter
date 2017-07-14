package international.acme.peppergreeter;

import android.content.Context;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static international.acme.peppergreeter.ConfigData.MAX_DEAL_COUNT;

public class MainActivityPresenterImpl implements MainActivityPresenter
{
    private MainActivityView mView;
    private ConfigData mConfigData;
    private Context mContext;
    private RobotController mRobotController;
    private boolean mGreetingInProgress;

    /** To aid in testing: allow overriding the scheduler on which the greeting sequence will be observed. **/
    private Scheduler mGreetingObserverScheduler;
    public void setGreetingObserverScheduler(Scheduler scheduler) { mGreetingObserverScheduler = scheduler; }

    /** To aid in testing, a semaphore that will be triggered when a greeting has completed. **/
    Semaphore mOnGreetingCompleted;
    public void setOnGreetingCompleted(Semaphore onGreetingCompleted) { mOnGreetingCompleted = onGreetingCompleted; }

    /** Idling resource to aid in Espresso UI testing. **/
    CountingIdlingResource mIdlingResource;

    public MainActivityPresenterImpl(Context context, RobotController robotController, DataRepository dataRepository, Scheduler greetingObserverScheduler, CountingIdlingResource idlingResource)
    {
        mContext = context;
        mRobotController = robotController;
        mGreetingObserverScheduler = greetingObserverScheduler;

        // Set "non idle" state until after data load has finished
        mIdlingResource = idlingResource;
        if (mIdlingResource != null) mIdlingResource.increment();

        // Start async data load for greetings and special deals.
        logMsg("Connecting to data repository");
        dataRepository.loadDataAsync(this::onConfigDataLoaded, this::onConfigDataLoadError);

        // Set up callback that will invoke greeting when human detected within specified range.
        logMsg("Start watching for Humans");
        mRobotController.watchForHumans(this::startGreeting);
    }

    private void onConfigDataLoaded(ConfigData configData)
    {
        mConfigData = configData;
        if (mConfigData.Greetings == null) mConfigData.Greetings = new String[] { mContext.getString(R.string.default_greeting_1), mContext.getString(R.string.default_greeting_2) };
        if (mConfigData.SpecialDeals == null) mConfigData.SpecialDeals = new String[]{};
        updateGreetingButtonState();
        showSpecialDeals();
        if (mIdlingResource != null) mIdlingResource.decrement();
    }

    /** If we fail to load greetings and special deal config data from server, fallback to some pre-defined greetings so the robot will still have basic operation.
     *  Exception detail will be logged from the DataRepository to aid in troubleshooting.
     *  TODO: We probably don't want to display an error message on the robot's tablet, since that would be a poor customer experience.
     *        Probably would want to log the exception to Crashlytics or similar service where a human operator could be notified that the robot encountered an error.
     **/
    private void onConfigDataLoadError(Throwable ex)
    {
        logError("Failed to load config data, will fallback to default greetings");
        mConfigData = new ConfigData();
        onConfigDataLoaded(mConfigData);
    }

    private void updateGreetingButtonState()
    {
        if (mView != null)
        {
            mView.setGreetingButtonEnabled(mConfigData != null && !mGreetingInProgress);
        }
    }

    private void showSpecialDeals()
    {
        if (mView != null && mConfigData != null)
        {
            mView.showSpecialDeals(mConfigData.SpecialDeals);
        }
    }

    @Override
    public void attach(MainActivityView view)
    {
        mView = view;
        updateGreetingButtonState();
        showSpecialDeals();
    }

    @Override
    public void detach()
    {
        mView = null;
    }

    /** Start async operation to perform the greeting process, unless already running.
         - Move forward 1/2 meter
         - When done moving, simultaneously say a random greeting phrase and start arm animation
         - When greeting and animation are done, move 1/2 meter backward to where we started
     For use in testing, a semaphore can be provided which will be released once the greeting sequence has completed.
     **/
    @Override
    public void startGreeting()
    {
        if (mGreetingInProgress)
        {
            logMsg("onGreetingButtonPressed: greeting already in progress, will not start another one");
            return;
        }

        int greetingIndex = new Random().nextInt(mConfigData.Greetings.length);
        String greetingStr = mConfigData.Greetings[greetingIndex];
        mGreetingInProgress = true;
        if (mIdlingResource != null) mIdlingResource.increment();
        updateGreetingButtonState();
        mRobotController.moveWithVector(0.5, 0, 0)
                .andThen(Completable.mergeArray(mRobotController.speak(greetingStr), mRobotController.animate(R.raw.greeting_arms)))
                .andThen(announceDeals())
                .andThen(mRobotController.moveWithVector(-0.5, 0, 0))
                .observeOn(mGreetingObserverScheduler)
                .subscribe(() ->
                {
                    logMsg("Greeting sequence has completed");
                    mGreetingInProgress = false;
                    updateGreetingButtonState();
                    if (mOnGreetingCompleted != null) mOnGreetingCompleted.release();
                    if (mIdlingResource != null) mIdlingResource.decrement();
                }, ex ->
                {
                    // TODO: somehow notify human operator that the robot may need troubleshooting.
                    logError("An error occurred during the greeting sequence");
                    logException(ex);
                    mGreetingInProgress = false;
                    updateGreetingButtonState();
                });
    }

    /** Announce all deals defined in the configuration. Note that a maximum of MAX_DEAL_COUNT deals is supported (more than that will be ignored).
     *  Before announcing the individual deals, say "Here are today's special deals!"
     *  Each announcement involves speaking the deal text, while simultaneously playing an animation that will point to the corresponding area on the robot's tablet with the arms. **/
    private Completable announceDeals()
    {
        int[] resourceIds = new int[] { R.raw.point_to_deal1, R.raw.point_to_deal2, R.raw.point_to_deal3, R.raw.point_to_deal4, R.raw.point_to_deal5, R.raw.point_to_deal6 };
        List<Completable> completableList = new ArrayList<>();
        int numDeals = (mConfigData.SpecialDeals.length > MAX_DEAL_COUNT) ? MAX_DEAL_COUNT : mConfigData.SpecialDeals.length;
        if (numDeals > 0) completableList.add(mRobotController.speak(mContext.getString(R.string.here_are_todays_special_deals)));
        for (int i=0 ; i < numDeals ; i++)
        {
            String dealStr = mConfigData.SpecialDeals[i];
            int resId = resourceIds[i];
            completableList.add(Completable.mergeArray(mRobotController.speak(dealStr), mRobotController.animate(resId), highlightDeal(i)));
        }
        return Completable.concat(completableList);
    }

    /** If the view is attached, return Completable that will play on-screen animation to highlight the deal at the index specified. **/
    private Completable highlightDeal(int dealNum)
    {
        return (mView != null) ? mView.highlightDeal(dealNum) : Completable.complete();
    }

    private void logMsg(String text, Object... args) { LogHelper.logMsg(getClass(), text, args); }
    private void logError(String text, Object... args) { LogHelper.logError(getClass(), text, args); }
    private void logException(Throwable ex) { LogHelper.logException(getClass(), ex); }

}
