package international.acme.peppergreeter;

import io.reactivex.functions.Consumer;

public interface DataRepository
{
    void loadDataAsync(Consumer<ConfigData> onConfigDataLoaded, Consumer<Throwable> onError);
}
