# Robert Push-Notification Server

The following was discovered as part of building the push-notification scheduler module:

1. The original package name 'fr.gouv.stopc.robert-push-notif' is invalid and this project uses 'fr.gouv.stopc.robertpushnotif' instead.

2. The push notification characteristics must be kept as is. A lot of subtle choices are missing documentation and make the system working for different iOS version (11, 12, 13+ ...).

3. Notifications are sent before on `api.push.apple.com`. When the APNS server returns an error _bad device token_ then an attempt to send the notification through `api.sandbox.push.apple.com`. This behavior make it possible to test notifications to a locally compiled Xcode and/or a TestFlight applications.

4. At some point the mobile app was probably deployed as multiple different applications in the Apple Store. That's why there was multiple _channel-id_ (fr.gouv.stopcovid.ios and fr.gouv.stopcovid-int.ios). Now there is only one _channel-id_.

## Reference Documentation

For further reference, please consider the following sections:

- [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
- [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/maven-plugin/reference/html/)
- [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/maven-plugin/reference/html/#build-image)
