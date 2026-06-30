package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.TimeZone;

public final class ClockAliasedStatuslineTimeGuard {

    private ClockAliasedStatuslineTimeGuard() {
    }

    public static boolean isOlderDayWithSameClockTime(long storedTimeMillis, long candidateTimeMillis,
                                                      @NonNull TimeZone timeZone) {
        if (candidateTimeMillis <= storedTimeMillis) {
            return false;
        }
        Calendar storedCalendar = Calendar.getInstance(timeZone);
        storedCalendar.setTimeInMillis(storedTimeMillis);
        Calendar candidateCalendar = Calendar.getInstance(timeZone);
        candidateCalendar.setTimeInMillis(candidateTimeMillis);
        if (!sameClockTimeOfDay(storedCalendar, candidateCalendar)) {
            return false;
        }
        return isOnAnEarlierCalendarDay(storedCalendar, candidateCalendar);
    }

    private static boolean sameClockTimeOfDay(@NonNull Calendar first, @NonNull Calendar second) {
        return first.get(Calendar.HOUR_OF_DAY) == second.get(Calendar.HOUR_OF_DAY)
            && first.get(Calendar.MINUTE) == second.get(Calendar.MINUTE)
            && first.get(Calendar.SECOND) == second.get(Calendar.SECOND)
            && first.get(Calendar.MILLISECOND) == second.get(Calendar.MILLISECOND);
    }

    private static boolean isOnAnEarlierCalendarDay(@NonNull Calendar earlier, @NonNull Calendar later) {
        if (earlier.get(Calendar.YEAR) != later.get(Calendar.YEAR)) {
            return earlier.get(Calendar.YEAR) < later.get(Calendar.YEAR);
        }
        return earlier.get(Calendar.DAY_OF_YEAR) < later.get(Calendar.DAY_OF_YEAR);
    }
}
