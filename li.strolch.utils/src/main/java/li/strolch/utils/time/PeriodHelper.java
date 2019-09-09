package li.strolch.utils.time;

import static java.time.Period.between;

import java.time.Period;
import java.time.ZonedDateTime;

import li.strolch.utils.dbc.DBC;

public class PeriodHelper {

	public static double daysIn(PeriodDuration periodDuration) {
		return (daysIn(periodDuration.getPeriod()) + (periodDuration.getDuration().toHours() / 24.0));
	}

	public static double daysIn(Period period) {
		return (period.getYears() * 365.0) + (period.getMonths() * 30.0) + period.getDays();
	}

	/**
	 * This special function allows us to shift a date by a multiple of the given {@link PeriodDuration} so that is
	 * before the given to date. It does multiple tries to get as close as possible, due to the inexactness of 30 days
	 * being one month, and 365 days being one year.
	 *
	 * @param date
	 * 		the date to shift
	 * @param to
	 * 		the date before which to stop shifting
	 * @param periodDuration
	 * 		the period shift in multiples by
	 *
	 * @return the shifted date
	 */
	public static ZonedDateTime shiftByMultipleOfPeriod(ZonedDateTime date, ZonedDateTime to,
			PeriodDuration periodDuration) {
		DBC.PRE.assertTrue("date must be before to!", date.isBefore(to));
		DBC.PRE.assertFalse("period duration may not be null!", periodDuration.isZero());
		Period between = between(date.toLocalDate(), to.toLocalDate());
		double daysInBetween = daysIn(between);
		double daysInPeriod = daysIn(periodDuration);
		long shifts = (long) (daysInBetween / daysInPeriod);
		if (shifts < 0)
			return date;

		ZonedDateTime shiftedDate = date.plusDays((long) (shifts * daysInPeriod));

		// see if we are close enough now
		between = between(shiftedDate.toLocalDate(), to.toLocalDate());
		daysInBetween = daysIn(between);
		shifts = (long) (daysInBetween / daysInPeriod);
		if (shifts < 2)
			return shiftedDate;

		return shiftByMultipleOfPeriod(shiftedDate, to, periodDuration);
	}
}
