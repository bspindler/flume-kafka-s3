/**
 * Copyright (c) 2014-2016 Netuitive, Inc. <support@netuitive.com> All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Netuitive, Inc. and certain third parties ("Confidential Information").
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Netuitive.
 *
 * NETUITIVE MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * OR NON-INFRINGEMENT. NETUITIVE SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package com.netuitive;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.netuitive.common.model.domain.MetricResult;
import com.netuitive.common.model.domain.Sample;
import com.netuitive.common.model.support.OrderedPair;
import com.netuitive.messaging.birling.dto.analysis.subject.EventDTO;
import com.netuitive.messaging.birling.dto.analysis.subject.MetricResultDTO;
import com.netuitive.messaging.birling.dto.ingest.subject.children.SampleDTO;
import com.netuitive.messaging.birling.publisher.flume.FlumeHeaders;
import com.netuitive.messaging.birling.schema.SimpleSchemaRegistry;
import com.netuitive.messaging.birling.util.To;
import org.apache.avro.Schema;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.serialization.EventSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jasonk
 * Flume Event serializer implementation which takes inbound avro bytes and turns them into CSV lines.
 *
 */
public class CsvSerializer implements EventSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(CsvSerializer.class);

    private final OutputStream stream;
    private final SimpleSchemaRegistry registry;

    public CsvSerializer(OutputStream stream) {
        this.stream = stream;
        this.registry = SimpleSchemaRegistry.create();
    }

    @Override
    public void afterCreate() throws IOException {

    }

    @Override
    public void afterReopen() throws IOException {

    }

    @Override
    public void write(final Event event) throws IOException {
        Map<String,String> headers = event.getHeaders();
        String subject = headers.get("subject");
        LOG.info("Converting to csv format for subject: " + subject);

        String csv = convertToCsv(subject, event);

        if(!Strings.isNullOrEmpty(csv)) {
            stream.write(csv.getBytes(Charset.forName("UTF-8")));
            stream.write(0x0A); //UTF-8 newline.
        }
    }

    String convertToCsv(final String subject, final Event event) {
        switch (subject) {
            case SimpleSchemaRegistry.Subjects.samples:
                return getCsvLineForSamplesDTO(event);
            case SimpleSchemaRegistry.Subjects.metricResults:
                return getCsvLineForMetricResultDTO(event);
            case SimpleSchemaRegistry.Subjects.events:
                return getCsvLineForEventDTO(event);
        }

        return "";
    }

    String getCsvLineForEventDTO(final Event event) {
        OrderedPair<Short, Schema> schema = registry.schemaBySubject().apply("events");
        EventDTO dto = To.DTO.usingOnlyAvroEncodedBytes(event.getBody(), EventDTO.class, schema.right);

        com.netuitive.common.model.domain.Event domainEvent = dto.toDomain();

        StringBuilder sb = new StringBuilder();
        sb.append(domainEvent.getTenantId());
        sb.append(",");
        sb.append(domainEvent.getElementId());
        sb.append(",");
        sb.append(domainEvent.getElementFqn());
        sb.append(",");
        sb.append(domainEvent.getPolicyId());
        sb.append(",");

        // Policy Name
        Object policyName = domainEvent.getData().get("policyName");
        if(policyName != null){
            sb.append("\"" + policyName.toString() + "\"");
        }

        sb.append(",");

        sb.append(String.valueOf(domainEvent.getTimestamp().getTime()));
        sb.append(",");

        String data = domainEvent.getData().entrySet().stream()
                .map(es -> {
                    String encodedString = "";
                    try {
                        String valAsString = es.getValue().toString();
                        encodedString = es.getKey() + "=" + URLEncoder.encode(valAsString, Charsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        LOG.error("Unable to URL encode event data key[" + es.getKey() + "] value[" + es.getValue() + "]", e);
                    }
                    return encodedString;
                })
                .collect(Collectors.joining("&"));

        if(Strings.isNullOrEmpty(data)) {
            sb.append("null");
        }
        else {
            sb.append(data);
        }

        return sb.toString();
    }

    String getCsvLineForMetricResultDTO(final Event event) {
        OrderedPair<Short, Schema> schema = registry.schemaBySubject().apply("metric.results");
        MetricResultDTO dto = To.DTO.usingOnlyAvroEncodedBytes(event.getBody(), MetricResultDTO.class, schema.right);

        MetricResult mr = dto.toDomain();
        StringBuilder sb = new StringBuilder();
        sb.append(mr.getTenantId());
        sb.append(",");
        sb.append(mr.getElementId());
        sb.append(",");
        sb.append(mr.getMetricId());
        sb.append(",");
        sb.append(mr.getRollup().toString());
        sb.append(",");
        sb.append(String.valueOf(mr.getTs().getTime()));
        sb.append(",");

        String data = mr.getData().entrySet().stream()
                .map(es -> es.getKey() + "=" + es.getValue())
                .collect(Collectors.joining("&"));

        sb.append(data);

        return sb.toString();
    }

    String getCsvLineForSamplesDTO(final Event e) {
        OrderedPair<Short, Schema> schema = registry.schemaBySubject().apply(SimpleSchemaRegistry.Subjects.samples);
        SampleDTO dto = To.DTO.usingOnlyAvroEncodedBytes(e.getBody(), SampleDTO.class, schema.right);
        Map<String,String> headers = e.getHeaders();

        Sample sample = dto.toDomain();
        StringBuilder sb = new StringBuilder();
        sb.append(headers.get(FlumeHeaders.TENANT_ID.getValue()));
        sb.append(",");
        sb.append(headers.get(FlumeHeaders.ELEMENT_ID.getValue()));
        sb.append(",");
        sb.append(headers.get(FlumeHeaders.ELEMENT_FQN.getValue()));
        sb.append(",");
        sb.append(sample.getMetricId());
        sb.append(",");
        sb.append(headers.get(FlumeHeaders.METRIC_FQN.getValue()));
        sb.append(",");
        sb.append(String.valueOf(sample.getTimestamp().getTime()));
        sb.append(",");
        sb.append(String.valueOf(sample.getMin()));
        sb.append(",");
        sb.append(String.valueOf(sample.getAvg()));
        sb.append(",");
        sb.append(String.valueOf(sample.getMax()));
        sb.append(",");
        sb.append(String.valueOf(sample.getSum()));
        sb.append(",");
        sb.append(String.valueOf(sample.getCnt()));
        sb.append(",");
        sb.append(String.valueOf(sample.getVal()));
        sb.append(",");
        sb.append(String.valueOf(sample.getActual()));

        return sb.toString();
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @Override
    public void beforeClose() throws IOException {

    }

    @Override
    public boolean supportsReopen() {
        return false;
    }

    public static class Builder implements EventSerializer.Builder {

        @Override
        public EventSerializer build(Context context, OutputStream outputStream) {
            return new CsvSerializer(outputStream);
        }
    }
}
