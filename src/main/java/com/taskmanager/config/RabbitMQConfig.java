package com.taskmanager.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean("directExchange")
    public Exchange exchange() {
        return ExchangeBuilder.directExchange("amq.direct").build();
    }

    @Bean("EmailQueue")  // 定义消息队列
    public Queue queue(){
        return QueueBuilder
                .durable("email")  // 持久化类型
                .build();
    }

    @Bean("binding")
    public Binding binding(@Qualifier("directExchange") Exchange exchange,
                           @Qualifier("EmailQueue") Queue queue){
        // 绑定交换机和队列
        return BindingBuilder
                .bind(queue)   // 绑定队列
                .to(exchange)  // 到交换机
                .with("email")   //使用自定义的routingKey
                .noargs();
    }

    @Bean("jacksonConverter")
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
