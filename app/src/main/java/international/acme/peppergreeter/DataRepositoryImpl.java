package international.acme.peppergreeter;

import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/** Responsible for loading the list of greetings and special deals from remote server. */
public class DataRepositoryImpl implements DataRepository
{
    ConfigData mConfigData;

    @Override
    public void loadDataAsync(Consumer<ConfigData> onConfigDataLoaded, Consumer<Throwable> onError)
    {
        getConfigDataApi().getConfigData()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                configData ->
                {
                    logMsg("Successfully loaded ConfigData: %d greetings, %d special deals", configData.Greetings.length, configData.SpecialDeals.length);
                    mConfigData = configData;
                    onConfigDataLoaded.accept(configData);
                }, ex ->
                {
                    logError("Error loading config data from server");
                    logException(ex);
                    onError.accept(ex);
                }
        );
    }

    /** Build interface for getting data from the server API.  Set relatively short timeout (5 seconds) so robot can fall back to default greetings if the network is down.
     * TODO: Inject these dependencies via Dagger or similar instead of creating them here.
     **/
    private ConfigDataApi getConfigDataApi()
    {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).connectTimeout(5, TimeUnit.SECONDS).build();
        Retrofit.Builder builder = new Retrofit.Builder();
        return builder.client(okHttpClient)
                .baseUrl("https://jsonblob.com")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(ConfigDataApi.class);
    }

    private void logMsg(String text, Object... args) { LogHelper.logMsg(getClass(), text, args); }
    private void logError(String text, Object... args) { LogHelper.logError(getClass(), text, args); }
    private void logException(Throwable ex) { LogHelper.logException(getClass(), ex); }
}
