package apps.sample.smsapp.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DateUtility has utility methods for handling Dates
 *
 */
public class DateUtility {

    public static String formatDate(long epoch) {
        String dateStr;
        Date date = new Date(epoch);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss", new Locale("english"));
        dateStr = sdf.format(date);
        return dateStr;
    }
}
