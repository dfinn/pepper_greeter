package international.acme.peppergreeter;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/** Instrumentation test, which will execute on an Android device, to verify behavior of the MainActivity UI **/
@RunWith(AndroidJUnit4.class)
public class MainActivityTest
{
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setup()
    {
        Espresso.registerIdlingResources(mActivityTestRule.getActivity().getIdingResource());
    }

    @Test
    public void greetingButtonEnabledAfterDataLoaded() throws Exception
    {
        onView(withId(R.id.greetingButton)).check(matches(isEnabled()));
    }

    @Test
    public void greetingPerformedWhenGreetingButtonClicked()
    {
        onView(withId(R.id.greetingButton)).perform(click());

        // TODO: Verify that as the greeting proceeds, each special deal is animated in the UI as that deal is announced.
        //       Would need to mock the data repository to know the expected number of deals, and add additional idling resources in order
        //       to synchronize the test with the presenter's greeting state.
    }


}
