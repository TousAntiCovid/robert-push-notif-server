package fr.gouv.stopc.robert.apnsmock;

import com.eatthepath.pushy.apns.server.*;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLSession;

import java.util.Random;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class Application implements CommandLineRunner, PushNotificationHandlerFactory {

    private final Random random = new Random();

    private final Environment env;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        new MockApnsServerBuilder()
                .setServerCredentials(
                        new ClassPathResource("/apns/server-certs.pem").getInputStream(),
                        new ClassPathResource("/apns/server-key.pem").getInputStream(), null
                )
                .setTrustedClientCertificateChain(new ClassPathResource("/apns/ca.pem").getInputStream())
                .setEventLoopGroup(new NioEventLoopGroup(5))
                .setHandlerFactory(this)
                .build()
                .start(env.getProperty("apns.mock.port", Integer.class))
                .get();
    }

    @Override
    public PushNotificationHandler buildHandler(SSLSession sslSession) {
        return (headers, payload) -> {
            final var badDeviceToken = random.nextInt(10) == 0;
            if (badDeviceToken) {
                throw new RejectedNotificationException(RejectionReason.BAD_DEVICE_TOKEN);
            }
        };
    }
}
