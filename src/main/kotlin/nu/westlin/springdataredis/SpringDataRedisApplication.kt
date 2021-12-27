package nu.westlin.springdataredis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import nu.westlin.springdataredis.News.Category.ECONOMICS
import nu.westlin.springdataredis.News.Category.SPORTS
import nu.westlin.springdataredis.News.Category.WEATHER
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.stereotype.Component

data class News(val category: Category, val headline: String) {

    enum class Category {
        SPORTS, ECONOMICS, WEATHER
    }

}

private val newsFeed: List<News> = listOf(
    News(SPORTS, "Gretzky to comeback"),
    News(ECONOMICS, "Everything is bad"),
    News(WEATHER, "It's gonna be cloudy"),
    News(SPORTS, "Gretzky will not comeback"),
    News(ECONOMICS, "Everything is good"),
    News(WEATHER, "It's gonna be sunny")
)

@SpringBootApplication
class SpringDataRedisApplication {

    @Bean
    fun container(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisMessageListenerContainer {
        val logger: Logger = LoggerFactory.getLogger(this.javaClass)
        return RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            // As an alternative, we can send all news to the "news" topic and then filter them in Receiver.onMessage(...) by the given pattern
            addMessageListener(
                MessageListenerAdapter(Receiver("Sports 1", objectMapper)),
                PatternTopic(SPORTS.toString())
            )
            addMessageListener(
                MessageListenerAdapter(Receiver("Sports 2", objectMapper)),
                PatternTopic(SPORTS.toString())
            )
            addMessageListener(
                MessageListenerAdapter(Receiver("Weather 1", objectMapper)),
                PatternTopic(WEATHER.toString())
            )
            addMessageListener(
                MessageListenerAdapter(Receiver("Economics 1", objectMapper)),
                PatternTopic(ECONOMICS.toString())
            )

            // Listens to all news 
            addMessageListener(
                MessageListenerAdapter(object : MessageListener {
                    override fun onMessage(message: Message, pattern: ByteArray?) {
                        logger.info("All news: ${objectMapper.readValue<News>(message.toString())}")
                    }
                }),
                ChannelTopic("NEWS")
            )
        }
    }

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): JsonRedisTemplate {
        return JsonRedisTemplate(connectionFactory)
    }
}

@Suppress("unused")
@Component
class NewsFeeder(
    private val jsonRedisTemplate: JsonRedisTemplate
) {

    @EventListener
    fun sendNews(event: ApplicationStartedEvent) {
        newsFeed.forEach { news ->
            jsonRedisTemplate.convertAndSend(news.category.toString(), news)
            jsonRedisTemplate.convertAndSend("NEWS", news)
        }
    }
}

class JsonRedisTemplate(connectionFactory: RedisConnectionFactory) : RedisTemplate<String, Any>() {
    init {
        setConnectionFactory(connectionFactory)

        keySerializer = StringRedisSerializer()
        valueSerializer = GenericJackson2JsonRedisSerializer()
    }
}

fun main(args: Array<String>) {
    runApplication<SpringDataRedisApplication>(*args)
}

class Receiver(private val name: String, private val objectMapper: ObjectMapper) : MessageListener {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun onMessage(message: Message, pattern: ByteArray?) {
        logger.info("$name: message = ${objectMapper.readValue<News>(message.toString())}")
    }
}