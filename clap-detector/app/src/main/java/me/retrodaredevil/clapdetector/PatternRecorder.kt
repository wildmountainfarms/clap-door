package me.retrodaredevil.clapdetector

class PatternRecorder(
    private val time: () -> Long = System::currentTimeMillis,
    private val idleResetTime: Long = 1500
) {

    private var lastClap: Long? = null
    private var intervals: MutableList<Long> = mutableListOf()

    fun clap(){
        val now = time()
        val lastClap = this.lastClap
        if(lastClap == null || lastClap + idleResetTime < now){ // this is the first clap
            this.lastClap = now // we need to set this.lastClap
            this.intervals = mutableListOf()
            return
        }
        val interval = now - lastClap
        intervals.add(interval)
        this.lastClap = now
    }
    val currentIntervals: List<Long>
        get() = intervals.toList()

    /**
     * @return true if [currentIntervals] will be reset before being added to, false otherwise
     */
    val isDone: Boolean
        get() {
            return (lastClap ?: return true) + idleResetTime < time()
        }

}