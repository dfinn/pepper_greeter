package international.acme.peppergreeter;

import com.google.gson.annotations.SerializedName;

/** Represents customizable configuration data loaded from a remote server: the list of greetings and special deals. **/
public class ConfigData
{
    /** Maximum number of deals that are supported. **/
    public static int MAX_DEAL_COUNT = 6;

    @SerializedName("greetings")
    public String[] Greetings;

    @SerializedName("special_deals")
    public String[] SpecialDeals;
}
