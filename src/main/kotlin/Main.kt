package com.flammky.flammbot

import com.flammky.flammbot.env.Bot
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


suspend fun main() {
    val kord = Kord(Bot.TOKEN)
    kord.on<MessageCreateEvent> {
        if (message.author?.isBot == true || !message.content.startsWith("f! ")) {
            return@on
        }
        when(message.content) {
            "f! ping" -> {
                kord.gateway.averagePing?.let {
                    message.channel.createMessage("Pong! " + "${it.inWholeMilliseconds}" + "ms")
                }
            }
            "f! pong" -> {
                kord.gateway.averagePing?.let {
                    message.channel.createMessage("Ping? " + "${it.inWholeMilliseconds}" + "ms")
                }
            }
            "f! shutdown" -> {
                message.channel.createMessage("Bye!")
                kord.gateway.detachAll()
                (kord as CoroutineScope).cancel()
            }
            "f! nuke" -> {
                val nukeMsg = message.channel.createMessage("${message.author?.mention ?: "[unknown]"} used `nuke`")
                message.channel.messages.collect {
                    if (it.id == nukeMsg.id) {
                        return@collect
                    }
                    kord.launch(Dispatchers.IO) {
                        message.channel.deleteMessage(it.id)
                    }
                }
            }
        }
    }
    kord.on<ChatInputCommandInteractionCreateEvent> {
        when (interaction.invokedCommandName) {
            "ping" -> interaction.kord.gateway.averagePing?.let {
                interaction.channel.createMessage("ping, pong! " + "${it.inWholeMilliseconds}" + "ms")
            }
        }
    }
    kord.createGlobalChatInputCommand("ping", "ping the bot")
    kord.login {
        presence { playing("SandBox") }
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}