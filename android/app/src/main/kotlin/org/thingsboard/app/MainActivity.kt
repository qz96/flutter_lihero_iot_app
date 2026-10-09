package org.thingsboard.app

import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.thingsboard.app.cyx7.Cyx7MethodChannel
import org.thingsboard.app.rtu.RtuMethodChannel

class MainActivity: FlutterActivity() {

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        registerTbWebAuth(flutterEngine)
        RtuMethodChannel.register(this, flutterEngine.dartExecutor.binaryMessenger)
        Cyx7MethodChannel.register(this, flutterEngine.dartExecutor.binaryMessenger)
    }

    fun registerTbWebAuth(flutterEngine: FlutterEngine) {
        val channel = MethodChannel(flutterEngine.dartExecutor, "tb_web_auth")
        channel.setMethodCallHandler(TbWebAuthHandler(this))
    }

}
