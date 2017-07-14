package international.acme.peppergreeter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

/** For use in testing.  **/
public class FakeDataRepository implements DataRepository
{
    public static String GREETING_1 = "Hello!";
    public static String GREETING_2 = "Welcome!";
    public static String DEAL_1 = "Popcorn - $1.00";
    public static String DEAL_2 = "Cupcakes - $2.00";

    public List<String> getGreetings()
    {
        List<String> greetings = new ArrayList<>();
        greetings.add(FakeDataRepository.GREETING_1);
        greetings.add(FakeDataRepository.GREETING_2);
        return greetings;
    }

    public List<String> getDeals()
    {
        List<String> deals = new ArrayList();
        deals.add(DEAL_1);
        deals.add(DEAL_2);
        return deals;
    }

    Consumer<ConfigData> mOnConfigDataLoaded;
    Consumer<Throwable> mOnError;

    @Override
    public void loadDataAsync(Consumer<ConfigData> onConfigDataLoaded, Consumer<Throwable> onError)
    {
        mOnConfigDataLoaded = onConfigDataLoaded;
        mOnError = onError;
    }

    public void simulateDataLoadCompleted() throws Exception
    {
        ConfigData configData = new ConfigData();
        configData.Greetings = new String[] { GREETING_1, GREETING_2 };
        configData.SpecialDeals = new String[] { DEAL_1, DEAL_2 };
        mOnConfigDataLoaded.accept(configData);
    }

    public void simulateDataLoadError() throws Exception
    {
        mOnError.accept(new IOException("Simulated error"));
    }
}
