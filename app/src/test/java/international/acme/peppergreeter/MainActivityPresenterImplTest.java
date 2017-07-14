package international.acme.peppergreeter;

import android.content.Context;
import android.support.test.espresso.idling.CountingIdlingResource;

import com.aldebaran.qi.sdk.object.geometry.Vector3;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.reactivex.schedulers.Schedulers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests for correct behavior of the MainActivityPresenterImpl. **/
public class MainActivityPresenterImplTest
{
    MainActivityPresenterImpl mPresenter;
    FakeMainView mFakeMainView;
    FakeDataRepository mFakeDataRepository;
    FakeRobotController mFakeRobotController;
    final String SPECIAL_DEALS_INTRO = "Here are the special deals we're offering today!";
    final String FALLBACK_GREETING_1 = "Welcome!";
    final String FALLBACK_GREETING_2 = "Hello, thanks for coming in!";

    @Before
    public void setup()
    {
        // Mock the Context object so that we can test the Presenter independently of the Android framework.
        Context mockContext = mock(Context.class);
        when(mockContext.getString((R.string.here_are_todays_special_deals))).thenReturn(SPECIAL_DEALS_INTRO);
        when(mockContext.getString((R.string.default_greeting_1))).thenReturn(FALLBACK_GREETING_1);
        when(mockContext.getString((R.string.default_greeting_2))).thenReturn(FALLBACK_GREETING_2);

        // Log calls will output to System.out to aid in test debugging.
        LogHelper.setTestMode(true);

        // Create fake objects for use in testing.
        mFakeMainView = new FakeMainView();
        mFakeRobotController = new FakeRobotController();
        mFakeDataRepository = new FakeDataRepository();

        // Create the presenter use the mock and fake objects. So we can run unit tests independently of the Android framework,
        // callbacks will be delivered on a regular Scheduler thread instead of AndroidSchedulers.mainThread()
        mPresenter = new MainActivityPresenterImpl(mockContext, mFakeRobotController, mFakeDataRepository, Schedulers.single(), null);
    }

    @Test
    public void greetingButtonEnabledIfLoadDoneAfterAttach() throws Exception
    {
        Assert.assertFalse(mFakeMainView.GreetingButtonEnabled);
        mPresenter.attach(mFakeMainView);
        mFakeDataRepository.simulateDataLoadCompleted();
        Assert.assertTrue(mFakeMainView.GreetingButtonEnabled);
    }

    @Test
    public void greetingButtonEnabledIfLoadDoneBeforeAttach() throws Exception
    {
        Assert.assertFalse(mFakeMainView.GreetingButtonEnabled);
        mFakeDataRepository.simulateDataLoadCompleted();
        mPresenter.attach(mFakeMainView);
        Assert.assertTrue(mFakeMainView.GreetingButtonEnabled);
    }

    @Test
    public void specialDealsDisplayedAfterDataLoaded() throws Exception
    {
        mPresenter.attach(mFakeMainView);
        mFakeDataRepository.simulateDataLoadCompleted();
        Assert.assertEquals(mFakeMainView.SpecialDeals[0], FakeDataRepository.DEAL_1);
        Assert.assertEquals(mFakeMainView.SpecialDeals[1], FakeDataRepository.DEAL_2);
    }

    /** Simulate config data loading normally, and a greeting getting triggered. Verify expected greeting spoken and all deals announced. **/
    @Test
    public void greetingPerformedNormally() throws Exception
    {
        Semaphore onGreetingCompleted = new Semaphore(0);
        mPresenter.setOnGreetingCompleted(onGreetingCompleted);
        mPresenter.attach(mFakeMainView);
        mFakeDataRepository.simulateDataLoadCompleted();
        Assert.assertEquals(0, onGreetingCompleted.availablePermits());
        mFakeRobotController.simulateHumanDetection();
        boolean result = onGreetingCompleted.tryAcquire(5, TimeUnit.SECONDS);
        Assert.assertTrue(result);
        validateGreetingSequence(mFakeDataRepository.getGreetings(), mFakeDataRepository.getDeals());
    }

    /** Simulate an error when loading config data. Verify that one of the fallback greeting phrases is spoken, with no deals announced. **/
    @Test
    public void fallbackGreetingsUsedIfDataLoadError() throws Exception
    {
        Semaphore onGreetingCompleted = new Semaphore(0);
        mPresenter.setOnGreetingCompleted(onGreetingCompleted);
        mPresenter.attach(mFakeMainView);
        mFakeDataRepository.simulateDataLoadError();
        Assert.assertEquals(0, onGreetingCompleted.availablePermits());
        mFakeRobotController.simulateHumanDetection();
        boolean result = onGreetingCompleted.tryAcquire(5, TimeUnit.SECONDS);
        Assert.assertTrue(result);
        List<String> fallbackGreetings = new ArrayList<>();
        fallbackGreetings.add(FALLBACK_GREETING_1);
        fallbackGreetings.add(FALLBACK_GREETING_2);
        validateGreetingSequence(fallbackGreetings, new ArrayList<>());
    }


    /** Validate that the FakeRobot has a log of performing the entire greeting sequence as expected:
     *  - Spoke: one of the allowed greeting phrases specified, followed by "Here are today's deals....", followed by all of the special deals specified.
     *  - Moved forward by 0.5 meter, then back by 0.5 meter
     *  - Performed the "greeting arms" animation sequence
     *  - Performed the expected animation sequences for pointing to deals on the tablet (which depends on the number of deals specified)
     *
     *  TODO: Enhance the test framework so that we can not only verify that the correct robot calls were made, but also the timing of the calls (ex: speech and animation
     *        having been started at the same time, then movement started when that completed, etc).
     */
    private void validateGreetingSequence(List<String> possibleGreetings, List<String> deals)
    {
        // Assert that the expected phrases have been spoken in the order expected.
        List<String> expectedPhrases = new ArrayList<>();
        if (deals.size() > 0) expectedPhrases.add(SPECIAL_DEALS_INTRO);
        expectedPhrases.addAll(deals);
        Assert.assertTrue(possibleGreetings.contains(mFakeRobotController.SpokenPhrases.get(0)));
        Assert.assertEquals(expectedPhrases.size()+1, mFakeRobotController.SpokenPhrases.size());
        for (int i=0 ; i < expectedPhrases.size() ; i++)
        {
            String expected = expectedPhrases.get(i);
            String spoken = mFakeRobotController.SpokenPhrases.get(i+1);
            Assert.assertEquals(expected, spoken);
        }

        // Assert that the robot moved .5 meter forward, then .5 meter backward.
        Assert.assertEquals(2, mFakeRobotController.MovementsDone.size());
        Assert.assertEquals(new Vector3(0.5, 0, 0), mFakeRobotController.MovementsDone.get(0));
        Assert.assertEquals(new Vector3(-0.5, 0, 0), mFakeRobotController.MovementsDone.get(1));

        // Assert that the robot performed the expected animations: "greeting arms" animation, followed by animations to highlight each deal
        Assert.assertEquals(R.raw.greeting_arms, (int) mFakeRobotController.AnimationsPerformed.get(0));
        int[] resourceIds = new int[] { R.raw.point_to_deal1, R.raw.point_to_deal2, R.raw.point_to_deal3, R.raw.point_to_deal4, R.raw.point_to_deal5, R.raw.point_to_deal6 };
        for (int i=1 ; i < (1+deals.size()) ; i++)
        {
            int expectedResId = resourceIds[i-1];
            Assert.assertEquals(expectedResId, (int)mFakeRobotController.AnimationsPerformed.get(i));
        }
    }
}