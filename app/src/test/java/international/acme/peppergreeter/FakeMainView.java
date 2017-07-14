package international.acme.peppergreeter;

import io.reactivex.Completable;

/** For use in testing. **/
public class FakeMainView implements MainActivityView
{
    public boolean GreetingButtonEnabled;

    public String[] SpecialDeals;

    @Override
    public void setGreetingButtonEnabled(boolean enabled)
    {
        GreetingButtonEnabled = enabled;
    }

    @Override
    public void showSpecialDeals(String[] specialDeals)
    {
        SpecialDeals = specialDeals;
    }

    @Override
    public Completable highlightDeal(int dealNum)
    {
        return Completable.complete();
    }
}
