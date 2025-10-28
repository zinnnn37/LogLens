package S13P31A306.loglens.global.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        if (!Objects.isNull(password) && !password.isEmpty()) {
            factory.getStandaloneConfiguration().setPassword(password);
        }
        return factory;
    }

    /**
     * Redis 기본 템플릿 Bean을 생성합니다.
     *
     * @param factory      RedisConnectionFactory 객체
     * @param objectMapper ObjectMapper 객체
     * @return RedisTemplate<String, Object> 객체
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory, ObjectMapper objectMapper) {

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Key, Value, Hash Key, Hash Value에 대한 Serializer 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(json);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(json);
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}
