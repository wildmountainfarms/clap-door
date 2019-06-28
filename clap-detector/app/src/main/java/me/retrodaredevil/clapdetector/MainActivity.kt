package me.retrodaredevil.clapdetector

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.icu.util.TimeUnit
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_AUDIO = 801
        const val REQUEST_DEVICE = 802
        const val DEVICE_ACTION = "me.retrodaredevil.clapdetector.device"
    }
    private val timer = Timer()
    private val recorder = MediaRecorder()
    private val patternRecorder = PatternRecorder()

    private lateinit var handler: Handler
    private val player by lazy { MediaPlayer.create(this@MainActivity, R.raw.chest_sound) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler = Handler()
        registerReceiver(deviceReceiver, IntentFilter(DEVICE_ACTION))

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        } else {
            start()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_AUDIO && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            start()
        }
    }

    private fun start(){

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recorder.setOutputFile(File.createTempFile("tempfile", "dat", cacheDir))
        } else {
            recorder.setOutputFile(File.createTempFile("tempfile", "dat", cacheDir).absolutePath)
        }
        recorder.prepare()
        recorder.start()
        timer.scheduleAtFixedRate(task, 10, 50)
    }
    private fun stop(){

        timer.cancel()
        recorder.stop()
        recorder.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
        player.release()
    }

    private val task = object : TimerTask() {
        private var wasDown = false
        private var sent = false
        private val pattern = listOf(Interval.NORMAL, Interval.NORMAL, Interval.LARGE)

        override fun run() {
            val amount = recorder.maxAmplitude
//            println(amount)
            val down = amount > 18000
            if(down && !wasDown){
                patternRecorder.clap()
                handler.post {
                    Toast.makeText(this@MainActivity, "clap $amount", Toast.LENGTH_SHORT).show()
                }
            }
            wasDown = down
            if(patternRecorder.isDone){
                val list = patternRecorder.currentIntervals
                if(list.isNotEmpty() && !sent){
                    val intervals = Interval.toIntervals(list)
                    println(intervals.toEventString("clap"))
                    if(intervals == pattern){
                        println("equals!")
                        player.start()
                        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
                        val device = manager.deviceList.values.firstOrNull()
                        if(device != null) {
                            handler.post {
                                Toast.makeText(this@MainActivity, "requesting permission for ${device.deviceName}", Toast.LENGTH_SHORT).show()
                            }
                            manager.requestPermission(
                                device,
                                PendingIntent.getBroadcast(
                                    this@MainActivity,
                                    0,
                                    Intent(DEVICE_ACTION).putExtra("device123", device.deviceName),
                                    PendingIntent.FLAG_CANCEL_CURRENT
                                )
                            )
                        } else {
                            println("unable to do serial communication")
                            handler.post {
                                Toast.makeText(this@MainActivity, "Couldn't do serial", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        println("not equal!")
                    }
                    println()
                    sent = true
                }
            } else {
                sent = false
            }
        }
    }

    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
//            Toast.makeText(this@MainActivity, "received action: ${intent?.action} ${intent?.extras}  ${intent?.extras?.getString("device123")}", Toast.LENGTH_SHORT).show()
            if(intent!!.action!! == DEVICE_ACTION) {
                val manager = getSystemService(Context.USB_SERVICE) as UsbManager
//                val deviceName = intent.extras!!.getString(UsbManager.EXTRA_DEVICE)!!
                val deviceName = intent.extras!!.getString("device123")
                val device = manager.deviceList[deviceName] ?: error("$deviceName not found!")
                val driver = UsbSerialProber.acquire(manager, device)
                val connection = manager.openDevice(driver.device)
                if(connection == null){
                    Toast.makeText(this@MainActivity, "Permission denied?", Toast.LENGTH_SHORT).show()
                    return
                }
                driver.open()
                driver.setBaudRate(9600)
                driver.write("clap\n".toByteArray(), 200)
                driver.close()
                Toast.makeText(this@MainActivity, "Did serial stuff", Toast.LENGTH_SHORT).show()
            }
        }

    }
}
