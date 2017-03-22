package com.netuitive;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.JSONEvent;
import org.apache.flume.interceptor.Interceptor;

import java.util.List;
import java.util.stream.Collectors;

public class AnalyticsInterceptor implements Interceptor {

    @Override
    public void initialize() {
        System.out.println("Starting");
    }

    @Override
    public Event intercept(Event event) {
        String body = new String(event.getBody());
        JSONEvent jsonEvent = new JSONEvent();
        jsonEvent.setBody("Test payload".getBytes());
        return jsonEvent;
    }

    @Override
    public List<Event> intercept(List<Event> events) {
        return events.stream().map(this::intercept).collect(Collectors.toList());
    }

    @Override
    public void close() {
        System.out.println("Shutdown");
    }

    public static class Builder implements Interceptor.Builder {

        @Override
        public void configure(Context context) { }

        @Override
        public Interceptor build() {
            return new AnalyticsInterceptor();
        }
    }
}