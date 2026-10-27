package common;



import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Demo {

    protected static final Logger logger = Logger.getLogger(Demo.class.getName());

    protected static void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
