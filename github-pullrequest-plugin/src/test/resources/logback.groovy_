import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"
    }
}

logger("org.jenkinsci.plugins.github", DEBUG)
logger("org.jenkinsci.plugins.github_integration", DEBUG)

root(INFO, ["STDOUT"])
