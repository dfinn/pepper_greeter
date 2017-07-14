package international.acme.peppergreeter;

import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static international.acme.peppergreeter.ConfigData.MAX_DEAL_COUNT;

public class MainActivity extends AppCompatActivity implements MainActivityView
{
    MainActivityPresenter mPresenter;

    /** Idling resource to use in Espresso testing so the test can wait until background processing has completed. **/
    CountingIdlingResource mIdlingResource = new CountingIdlingResource("test");
    public CountingIdlingResource getIdingResource() { return mIdlingResource; }

    @BindView(R.id.specialsTextView) TextView mSpecialsTextView;
    @BindView(R.id.specialsLayout) LinearLayout mSpecialsLayout;
    @BindView(R.id.greetingButton) Button mGreetingButton;
    @BindViews({ R.id.deal0, R.id.deal1, R.id.deal2, R.id.deal3, R.id.deal4, R.id.deal5 }) List<TextView> mDealTextViews;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Remove activity title bar and activate full screen mode so the customer only sees the deal info full screen.
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        for (TextView tv : mDealTextViews) tv.setVisibility(View.INVISIBLE);

        mPresenter = (MainActivityPresenter) getLastCustomNonConfigurationInstance();
        if (mPresenter == null)
        {
            // TODO: Use Dagger or similar to inject dependencies rather than creating them here.
            mPresenter = new MainActivityPresenterImpl(this, new RobotControllerImpl(this), new DataRepositoryImpl(), AndroidSchedulers.mainThread(), mIdlingResource);
        }
        mPresenter.attach(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (isFinishing())
        {
            mPresenter.detach();
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance()
    {
        return mPresenter;
    }

    @Override
    public void setGreetingButtonEnabled(boolean enabled)
    {
        mGreetingButton.setEnabled(enabled);
    }

    @OnClick(R.id.greetingButton)
    public void greetingButtonOnClick(View view)
    {
        mPresenter.startGreeting();
    }

    @Override
    public void showSpecialDeals(String[] specialDeals)
    {
        int numDeals = (specialDeals.length > MAX_DEAL_COUNT ? MAX_DEAL_COUNT : specialDeals.length);
        for (int i=0 ; i < numDeals ; i++)
        {
            String dealStr = specialDeals[i];
            TextView dealTextView = mDealTextViews.get(i);
            dealTextView.setText(dealStr);
            dealTextView.setVisibility(View.VISIBLE);
        }

        // Hide "Today's Specials" title if there aren't any
        if (numDeals == 0) mSpecialsTextView.setVisibility(View.INVISIBLE);
    }

    @Override
    public Completable highlightDeal(int dealNum)
    {
        final int ANIMATION_DURATION_MS = 1000;
        return Completable.fromAction(() ->
        {
            logMsg("Highlighting deal %d", dealNum);
            TextView dealView = mDealTextViews.get(dealNum);
            TransitionDrawable trans = (TransitionDrawable) dealView.getBackground();
            trans.startTransition(ANIMATION_DURATION_MS);
            dealView.postDelayed(() -> trans.reverseTransition(ANIMATION_DURATION_MS), ANIMATION_DURATION_MS);
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    private void logMsg(String text, Object... args) { LogHelper.logMsg(getClass(), text, args); }
    private void logError(String text, Object... args) { LogHelper.logError(getClass(), text, args); }
    private void logException(Throwable ex) { LogHelper.logException(getClass(), ex); }
}
