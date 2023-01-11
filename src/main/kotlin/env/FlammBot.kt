package com.flammky.flammbot.env

import java.io.FileInputStream
import java.util.*

object FlammBot {
    private val properties = Properties()

    val TOKEN: String = properties
        .apply {
            load(FileInputStream("src/main/resources/dev.properties"))
        }
        .getProperty("bot_token")

    val commandPrefix: String = "f! "
}