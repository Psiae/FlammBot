package com.flammky.flammbot

import com.flammky.flammbot.env.FlammBot
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val remindMeMap = mutableMapOf<String, Deferred<String>>()

suspend fun main() {
    val kord = Kord(FlammBot.TOKEN)

    // TODO: Inject Consumers

    kord.on<MessageCreateEvent> {
        if (message.author?.isBot != false || !message.content.startsWith(FlammBot.commandPrefix)) {
            return@on
        }
        val sliced =
            message.content.substring(FlammBot.commandPrefix.length)
        when(sliced) {
            "ping" -> {
                kord.gateway.averagePing?.let {
                    message.channel.createMessage {
                        messageReference = message.id
                        content = "Pong! " + "${it.inWholeMilliseconds}" + "ms"
                    }
                }
                return@on
            }
            "pong" -> {
                kord.gateway.averagePing?.let {
                    message.channel.createMessage {
                        messageReference = message.id
                        content = "Ping? " + "${it.inWholeMilliseconds}" + "ms"
                    }
                }
                return@on
            }
            "meAvatar" -> {
                val avatarUrl = message.author!!.avatar?.url
                message.channel.createMessage {
                    messageReference = message.id
                    content = avatarUrl
                        ?.let {
                            "$it?size=512"
                        }
                        ?: "Unable to get Url"
                }
            }
            "meRoles" -> {
                val msgAuthor = message.author
                val msgGuild = message.getGuildOrNull()
                val roles = msgGuild
                    ?.let { guild ->
                        msgAuthor?.let { user ->
                            user.asMemberOrNull(guild.id)?.roleIds?.mapNotNull { guild.getRoleOrNull(it) }?.toSet()
                        }
                    }
                    ?: emptySet()
                if (roles.isEmpty()) {
                    message.channel.createMessage {
                        messageReference = message.id
                        content = "You have no roles"
                    }
                    return@on
                }
                message.channel.createMessage {
                    embed {
                       title = "${message.author!!.tag} roles"
                        roles.forEach {
                            field(it.name)
                        }
                    }
                }
            }
            "shutdown" -> {
                message.channel.createMessage {
                    messageReference = message.id
                    content = "Bye!"
                }
                kord.gateway.detachAll()
                (kord as CoroutineScope).cancel()
                return@on
            }
            "nuke" -> {
                val nukeMsg = message.channel.createMessage("${message.author?.mention ?: "[unknown]"} used `nuke`")
                message.channel.messages.collect {
                    if (it.id == nukeMsg.id) {
                        return@collect
                    }
                    kord.launch(Dispatchers.IO) {
                        message.channel.deleteMessage(it.id)
                    }
                }
                return@on
            }
        }
        when {
            sliced.startsWith("flip ") -> {
                val args = sliced.substring("flip ".length).split(" ")
                message.channel.createMessage {
                    messageReference = message.id
                    content = when {
                        // discord will cut suffix spaces tho ?
                        args.isEmpty() -> "Can Not flip nothing"
                        args.size == 1 -> args[0]
                        else -> args.shuffled()[args.indices.shuffled()[0]]
                    }
                }
                return@on
            }
            sliced.startsWith("remindMe ") -> {
                val time = Clock.System.now()
                val author = message.author ?: return@on
                val remindMeSlice = sliced.substring("remindMe ".length)
                val args = remindMeSlice.split(" ")
                val timeArg = args
                    .getOrElse(0) {
                        message.channel.createMessage {
                            messageReference = message.id
                            content = "Invalid args=$remindMeSlice"
                        }
                        return@on
                    }
                if (!timeArg[0].isDigit()) {
                    message.channel.createMessage {
                        messageReference = message.id
                        content = "Invalid timeArg=$timeArg"
                    }
                    return@on
                }
                val messageArg = args.toMutableList()
                    .apply { removeAt(0) }
                    .ifEmpty {
                        message.channel.createMessage {
                            messageReference = message.id
                            content = "Message can not be Empty"
                        }
                        return@on
                    }
                    .joinToString(separator = " ")
                val duration: Duration = when(val let = timeArg.takeLastWhile(Char::isLetter)) {
                    "ms" -> {
                        timeArg.dropLast(let.length).toInt().milliseconds
                    }
                    "s", "sec" -> {
                        timeArg.dropLast(let.length).toInt().seconds
                    }
                    "m", "min" -> {
                        timeArg.dropLast(let.length).toInt().minutes
                    }
                    "h", "hr" -> {
                        timeArg.dropLast(let.length).toInt().hours
                    }
                    "d", "day" -> {
                        timeArg.dropLast(let.length).toInt().days
                    }
                    else -> {
                        message.channel.createMessage {
                            messageReference = message.id
                            content = "unknown suffix `$let`"
                        }
                        return@on
                    }
                }
                if (duration > 1.days) {
                    message.channel.createMessage {
                        messageReference = message.id
                        content = "cannot remind more than 1 day (24hr)"
                    }
                    return@on
                }
                kord.launch {
                    message.channel.createMessage {
                        messageReference = message.id
                        content = "you will be reminded after $timeArg"
                    }
                    delay(duration)
                    author.getDmChannel().createMessage {
                        embed {
                            title = "reminder"
                            description = messageArg
                            color = author.accentColor
                            timestamp = time
                        }
                    }
                }
                return@on
            }
        }
    }
    kord.on<ChatInputCommandInteractionCreateEvent> {
        when (interaction.invokedCommandName) {
            "ping" -> {
                interaction.kord.gateway.averagePing
                    ?.let {
                        interaction.respondPublic {
                            content = "ping, pong! " + "${it.inWholeMilliseconds}" + "ms"
                        }
                    }
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