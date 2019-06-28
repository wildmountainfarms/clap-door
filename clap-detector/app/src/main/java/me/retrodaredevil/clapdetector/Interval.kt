package me.retrodaredevil.clapdetector

enum class Interval {
    NORMAL, LARGE;

    companion object {
        fun toIntervals(intervals: Iterable<Long>): List<Interval> {
            val threshold = intervals.average() * 1.3
            return intervals.map { if(it < threshold) NORMAL else LARGE }
        }
    }
}
fun Iterable<Interval>.toEventString(event: String): String{
    val r = StringBuilder(event)
    forEach {
        r.append(if(it == Interval.NORMAL) " . " else " ... ")
        r.append(event)
    }
    return r.toString()
}