package international.acme.peppergreeter;

import io.reactivex.Completable;

public interface MainActivityView
{
    /** Enable/disable the on-screen button to manually trigger a greeting. **/
    void setGreetingButtonEnabled(boolean enabled);

    /** Display special deals on the tablet UI. **/
    void showSpecialDeals(String[] specialDeals);

    /** Play an animation to highlight the deal at the specified index. **/
    Completable highlightDeal(int dealNum);
}
