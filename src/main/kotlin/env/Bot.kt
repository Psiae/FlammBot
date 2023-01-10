package com.flammky.flammbot.env

import java.io.FileInputStream
import java.util.*

object Bot {
    private val properties = Properties()

    val TOKEN: String = properties
        .apply {
            load(FileInputStream("src/main/resources/dev.properties"))
        }
        .getProperty("bot_token")
}