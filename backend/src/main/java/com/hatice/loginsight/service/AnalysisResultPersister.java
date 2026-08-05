package com.hatice.loginsight.service;

import com.hatice.loginsight.entity.AnalysisHttpMethodStatEntity;
import com.hatice.loginsight.entity.AnalysisLoggerStatEntity;
import com.hatice.loginsight.entity.AnalysisStatusCodeStatEntity;
import com.hatice.loginsight.entity.AnalysisThreadStatEntity;
import com.hatice.loginsight.entity.LogAnalysisEntity;
import com.hatice.loginsight.repository.AnalysisHttpMethodStatRepository;
import com.hatice.loginsight.repository.AnalysisLoggerStatRepository;
import com.hatice.loginsight.repository.AnalysisStatusCodeStatRepository;
import com.hatice.loginsight.repository.AnalysisThreadStatRepository;
import com.hatice.loginsight.repository.AnalysisTimelineStatRepository;
import com.hatice.loginsight.repository.LogAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisResultPersister {

    private final LogAnalysisRepository logAnalysisRepository;
    private final AnalysisLoggerStatRepository loggerStatRepository;
    private final AnalysisThreadStatRepository threadStatRepository;
    private final AnalysisStatusCodeStatRepository statusCodeStatRepository;
    private final AnalysisHttpMethodStatRepository httpMethodStatRepository;
    private final AnalysisTimelineStatRepository timelineStatRepository;

    public AnalysisResultPersister(LogAnalysisRepository logAnalysisRepository,
                                    AnalysisLoggerStatRepository loggerStatRepository,
                                    AnalysisThreadStatRepository threadStatRepository,
                                    AnalysisStatusCodeStatRepository statusCodeStatRepository,
                                    AnalysisHttpMethodStatRepository httpMethodStatRepository,
                                    AnalysisTimelineStatRepository timelineStatRepository) {
        this.logAnalysisRepository = logAnalysisRepository;
        this.loggerStatRepository = loggerStatRepository;
        this.threadStatRepository = threadStatRepository;
        this.statusCodeStatRepository = statusCodeStatRepository;
        this.httpMethodStatRepository = httpMethodStatRepository;
        this.timelineStatRepository = timelineStatRepository;
    }

    @Transactional
    public LogAnalysisEntity persist(LogAnalysisEntity entity, AnalysisResultAccumulator accumulator,
                                      LogTimelineAggregator timelineAggregator) {
        LogAnalysisEntity savedEntity = logAnalysisRepository.save(entity);

        loggerStatRepository.saveAll(toLoggerStats(savedEntity.getId(), accumulator));
        threadStatRepository.saveAll(toThreadStats(savedEntity.getId(), accumulator));
        statusCodeStatRepository.saveAll(toStatusCodeStats(savedEntity.getId(), accumulator));
        httpMethodStatRepository.saveAll(toHttpMethodStats(savedEntity.getId(), accumulator));
        timelineStatRepository.saveAll(timelineAggregator.toEntities(savedEntity.getId()));

        return savedEntity;
    }

    private List<AnalysisLoggerStatEntity> toLoggerStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        List<AnalysisLoggerStatEntity> stats = new ArrayList<>();
        accumulator.getLoggerCounts().forEach((loggerName, count) ->
                stats.add(new AnalysisLoggerStatEntity(logAnalysisId, loggerName, count)));
        return stats;
    }

    private List<AnalysisThreadStatEntity> toThreadStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        List<AnalysisThreadStatEntity> stats = new ArrayList<>();
        accumulator.getThreadCounts().forEach((threadName, count) ->
                stats.add(new AnalysisThreadStatEntity(logAnalysisId, threadName, count)));
        return stats;
    }

    private List<AnalysisStatusCodeStatEntity> toStatusCodeStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        List<AnalysisStatusCodeStatEntity> stats = new ArrayList<>();
        accumulator.getStatusCodeCounts().forEach((statusCode, count) ->
                stats.add(new AnalysisStatusCodeStatEntity(logAnalysisId, statusCode, count)));
        return stats;
    }

    private List<AnalysisHttpMethodStatEntity> toHttpMethodStats(Long logAnalysisId, AnalysisResultAccumulator accumulator) {
        List<AnalysisHttpMethodStatEntity> stats = new ArrayList<>();
        accumulator.getHttpMethodCounts().forEach((httpMethod, count) ->
                stats.add(new AnalysisHttpMethodStatEntity(logAnalysisId, httpMethod, count)));
        return stats;
    }
}