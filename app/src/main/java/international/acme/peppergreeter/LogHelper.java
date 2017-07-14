package international.acme.peppergreeter;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class LogHelper
{
    final static String TAG_BASE = "PepperGreeter";
    private static boolean TEST_MODE;
    public static void setTestMode(boolean enabled) { TEST_MODE = enabled; }

    public static void logMsg(Class source, String text, Object... args)
    {
        String threadStr = String.format("[%s] ", Thread.currentThread().getName());
        String msg = String.format(text, args);
        if (TEST_MODE) System.out.print(threadStr + msg + "\n");
        else Log.i(TAG_BASE + "_" + source.getSimpleName(), threadStr + msg);
    }

    public static void logError(Class source, String text, Object... args)
    {
        String threadStr = String.format("[%s] ", Thread.currentThread().getName());
        String msg = String.format(text, args);
        if (TEST_MODE) System.out.print("*** " + threadStr + msg + "\n");
        else Log.e(TAG_BASE + "_" + source.getSimpleName(), threadStr + msg);
    }

    public static void logException(Class source, Throwable ex)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        ex.printStackTrace(pw);
        pw.flush();
        pw.close();
        String stackTraceStr = bos.toString();
        logError(source, stackTraceStr);
    }
}
