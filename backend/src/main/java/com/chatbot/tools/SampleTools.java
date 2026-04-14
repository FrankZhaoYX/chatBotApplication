package com.chatbot.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SampleTools {

    @Tool(name = "currentTime",
          description = "Returns the current date and time in ISO-8601 for a given timezone (UTC if not provided).")
    public String currentTime(
            @ToolParam(required = false, description = "IANA timezone id, e.g. America/New_York. Defaults to UTC.")
            String timezone) {
        ZoneId zone = (timezone != null && !timezone.isBlank())
                ? ZoneId.of(timezone) : ZoneId.of("UTC");
        return ZonedDateTime.now(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Tool(name = "echo",
          description = "Echoes the provided text back. Useful for testing round-trip tool invocation.")
    public String echo(@ToolParam(description = "The text to echo.") String text) {
        return text;
    }

    @Tool(name = "add",
          description = "Returns the sum of two integers.")
    public int add(@ToolParam(description = "First integer.") int a,
                   @ToolParam(description = "Second integer.") int b) {
        return a + b;
    }
}
