package com.netuitive;

import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.source.kafka.KafkaSource;

public class AnalyticsSplitSource extends KafkaSource {

    @Override
    public synchronized ChannelProcessor getChannelProcessor() {
        return new AnalyticsSplitChannelProcessorProxy(super.getChannelProcessor());
    }
}
