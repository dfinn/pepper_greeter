package international.acme.peppergreeter;

import java.util.concurrent.Semaphore;

public interface MainActivityPresenter
{
    void attach(MainActivityView view);
    void detach();
    void startGreeting();
}
