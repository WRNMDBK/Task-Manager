package com.taskmanager.mqListener;

import com.rabbitmq.client.Channel;
import com.taskmanager.entity.dto.EmailMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class EmailListener {

    private final JavaMailSender javaMailSender;
    private final String emailOfSender;

    @Autowired
    public EmailListener(JavaMailSender javaMailSender,@Value("${spring.mail.username}") String emailOfSender) {
        this.javaMailSender = javaMailSender;
        this.emailOfSender = emailOfSender;
    }

    @RabbitListener(queues = "email", messageConverter = "jacksonConverter")
    public void sendEmailCode(EmailMessageDTO info, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject("[TaskManager] 邮箱验证码");
            message.setText(info.getCode() + " 有效期五分钟，请及时查收");
            message.setFrom(emailOfSender);
            message.setTo(info.getEmail());
            javaMailSender.send(message);
            channel.basicAck(deliveryTag,false);  // 发送成功手动确认
        } catch (Exception e) {
            log.error("邮箱验证码发布失败：",e);
            try {
                // 失败则拒绝消息。第三个参数 false 表示不重新入队（防止死循环轰炸用户邮箱）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败", ioException);
            }
        }
    }

}
