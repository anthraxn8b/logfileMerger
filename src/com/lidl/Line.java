package com.lidl;

import java.util.Date;

/**
 * Created by Boris.Kairat on 30.10.2015.
 */
public class Line implements Comparable<Line> {

    String source;
    Date date;
    String message;

    Line(final String _source, final Date _date, final String _message) {
        source = _source;
        date = _date;
        message = _message;
    }

    @Override
    public final int compareTo(final Line lineToCompareWith) {
        return date.compareTo(lineToCompareWith.date);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!this.getClass().equals(obj)) return false;
        return 0 == compareTo((Line) obj);
    }
}
