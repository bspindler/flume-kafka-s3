package com.netuitive;

import com.google.common.collect.Maps;
import com.netuitive.common.model.support.OrderedPair;
import com.netuitive.common.serde.schema.NetuitiveSchema;
import com.netuitive.messaging.birling.dto.analysis.subject.AnalysisResultsDTO;
import com.netuitive.messaging.birling.dto.analysis.subject.MetricResultDTO;
import com.netuitive.messaging.birling.schema.SchemaRegistry;
import com.netuitive.messaging.birling.schema.SimpleSchemaRegistry;
import com.netuitive.messaging.birling.util.To;
import org.apache.avro.Schema;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnalyticsSplitChannelProcessorProxy extends ChannelProcessor {

    private final static String METRIC_RESULTS_SOURCE = "metricResult";

    SchemaRegistry schemaRegistry = SimpleSchemaRegistry.create();

    public ChannelProcessor downstreamChannelProcessor = null;

    public AnalyticsSplitChannelProcessorProxy(ChannelSelector selector) {
        super(selector);
    }

    public AnalyticsSplitChannelProcessorProxy(ChannelProcessor processor) {
        super(null);
        downstreamChannelProcessor = processor;
    }

    @Override
    public void processEventBatch(List<Event> events) {
        downstreamChannelProcessor.processEventBatch(events.stream().flatMap(this::extractMetricResults).collect(Collectors.toList()));
    }

    private Stream<Event> extractMetricResults(Event event) {
        AnalysisResultsDTO dto = To.DTO.usingEmbeddedSchema(event.getBody(), AnalysisResultsDTO.class, schemaRegistry);
        Set<MetricResultDTO> metricResults = dto.getMetricResultsDTO();
        return metricResults.stream().map(metricResultDto -> constructMetricResult(event, metricResultDto));
    }

    private Event constructMetricResult(Event event, MetricResultDTO metricResultDTO) {
        Map<String,String> headers = Maps.newHashMap(event.getHeaders());
        headers.put("timestamp", String.valueOf(metricResultDTO.getTs()));
        headers.put("source", METRIC_RESULTS_SOURCE);

        NetuitiveSchema schema = netuitiveSchemaFor(SimpleSchemaRegistry.Subjects.metricResults);

        headers.put("subject", schema.getSubject());
        headers.put("version", String.valueOf(schema.getVersion()));

        return EventBuilder.withBody(To.Avro.onlyEncodedBytes(metricResultDTO, schema), headers);
    }

    NetuitiveSchema netuitiveSchemaFor(final String subject) {
        OrderedPair<Short, Schema> schemaTuple = schemaRegistry.schemaBySubject().apply(subject);

        NetuitiveSchema netuitiveSchema = new NetuitiveSchema();
        netuitiveSchema.setSchema(schemaTuple.right);
        netuitiveSchema.setSubject(subject);
        netuitiveSchema.setVersion(schemaTuple.left);

        return netuitiveSchema;
    }
}
