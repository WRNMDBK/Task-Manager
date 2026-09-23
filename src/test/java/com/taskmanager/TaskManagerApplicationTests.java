package com.taskmanager;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@SpringBootTest
class TaskManagerApplicationTests {

    @Resource
    RabbitTemplate template;

    @Test
    void publisher() {
        template.convertAndSend("amq.direct","q1","Hello World");
    }


    @Test
    void Producer() throws Exception {
        // 一、创建工厂并配置
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        factory.setVirtualHost("/test");

        // 二、创建连接
        try (Connection connection = factory.newConnection() ) {
            // 1.创建信道
            Channel channel = connection.createChannel();
            // 2.声明队列，如果此队列不存在，会自动创建
            // 参数：队列名、是否持久化、是否排他、是否自动删除、其他参数设置
            channel.queueDeclare("queue1", false, false, false, null);
            // 3.绑定队列到交换机
            // 参数：队列名、交换机名、路由键
            channel.queueBind("queue1","amq.direct","q1");
            // 4.发布新消息，注意消息需要转换为byte[]
            // 参数：交换机名、路由键、其他配置、消息本体
            channel.basicPublish("amq.direct","q1",null,"我是测试消息".getBytes(StandardCharsets.UTF_8));

        }
    }

    @Test
    void Consumer() throws IOException, TimeoutException {
        // 一、创建工厂并配置
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        factory.setVirtualHost("/test");

        // 二、创建连接
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 三、创建消费者
        // 参数：队列名、自动应答、消息接收后的处理、消费者取消订阅时的处理
        channel.basicConsume("queue1",false,(s,delivery)->{
            System.out.println(new String(delivery.getBody()));
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        },s->{});

    }

}
