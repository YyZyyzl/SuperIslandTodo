package com.example.superislandtodo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import dev.rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "super_island_todo_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createNotificationChannel()

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        val btnAuthShizuku = Button(this).apply { text = "1. 授权并写入白名单 (需运行 Shizuku)" }
        val editTitle = EditText(this).apply { hint = "输入待办事项 (例: 复习数学公式)" }
        val editContent = EditText(this).apply { hint = "输入备注/副标题 (例: 进行中...)" }
        val btnPushIsland = Button(this).apply { text = "2. 发送到超级岛" }
        val btnClearIsland = Button(this).apply { text = "3. 完成并下岛" }

        layout.addView(btnAuthShizuku)
        layout.addView(editTitle)
        layout.addView(editContent)
        layout.addView(btnPushIsland)
        layout.addView(btnClearIsland)

        setContentView(layout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnAuthShizuku.setOnClickListener {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Shizuku 未运行，请先启动 Shizuku", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(100)
                return@setOnClickListener
            }
            injectHyperOSWhiteList()
        }

        btnPushIsland.setOnClickListener {
            val title = editTitle.text.toString().ifBlank { "我的待办" }
            val content = editContent.text.toString().ifBlank { "正在执行中" }
            sendFocusNotification(title, content)
            Toast.makeText(this, "已发送至超级岛", Toast.LENGTH_SHORT).show()
        }

        btnClearIsland.setOnClickListener {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(1001)
            Toast.makeText(this, "已从超级岛移除", Toast.LENGTH_SHORT).show()
        }
    }

    private fun injectHyperOSWhiteList() {
        val pkg = packageName
        Thread {
            try {
                val cmds = arrayOf(
                    "cmd appops set $pkg ACCESS_NOTIFICATIONS allow",
                    "settings put system miui_focus_notification_white_list $pkg",
                    "cmd appops set $pkg SYSTEM_ALERT_WINDOW allow"
                )
                for (cmd in cmds) {
                    val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                    process.waitFor()
                }
                runOnUiThread {
                    Toast.makeText(this, "白名单与持久化配置写入成功！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "写入失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun sendFocusNotification(title: String, content: String) {
        val miuiExtras = Bundle().apply {
            putBoolean("miui.focus.enable", true)
            putInt("miui.focus.type", 0)
            putString("miui.focus.capsule.left_text", title)
            putString("miui.focus.capsule.right_text", content)
            putInt("miui.focus.capsule.icon", android.R.drawable.ic_dialog_info)
            putString("miui.focus.expanded.title", title)
            putString("miui.focus.expanded.content", content)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addExtras(miuiExtras)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
           
