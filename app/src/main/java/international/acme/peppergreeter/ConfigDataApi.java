package international.acme.peppergreeter;

import io.reactivex.Single;
import retrofit2.http.GET;

/** Interface for use with Retrofit for loading configuration data (greetings, special deals) from remote server **/
public interface ConfigDataApi
{
    @GET("/api/jsonBlob/1a4d1ef1-66a8-11e7-a38a-3345c3faabc3")
    Single<ConfigData> getConfigData();
}
