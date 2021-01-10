/*
 * Copyright 2020 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.safepoint.analyzer.commons.shared;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.page.ClassCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.parser.ClassLoaderLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.*;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCJdk8LogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCUnifiedLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitCodeCacheStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitCodeCacheSweeperActivity;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitCompilationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitTieredCompilationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.JitLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.*;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointJdk8LogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointUnifiedLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.page.StringDedupLast;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.page.StringDedupTotal;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser.StringDedupLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.page.ThreadCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.parser.ThreadLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.page.TlabSummary;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.page.TlabThreadStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser.TlabLogFileParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class StatsService {
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public JvmLogFile createAllStatsJdk8(InputStream inputStream, String originalFilename, Consumer<ParsingProgress> notifyProgress, Consumer<JvmLogFile> onComplete) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        SafepointJdk8LogFileParser safepointJdk8LogFileParser = new SafepointJdk8LogFileParser();
        GCJdk8LogFileParser gcJdk8LogFileParser = new GCJdk8LogFileParser();

        long numberOfLine = 1;
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                if (reader.ready()) {
                    // last line may be broken in Java 8 format
                    safepointJdk8LogFileParser.parseLine(line);
                    gcJdk8LogFileParser.parseLine(line);
                    if (numberOfLine % 1000 == 0) {
                        notifyProgress.accept(new ParsingProgress(numberOfLine, false));
                    }
                    numberOfLine++;
                } else {
                    break;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        JvmLogFile jvmLogFile = new JvmLogFile();
        jvmLogFile.setFilename(originalFilename);
        jvmLogFile.setSafepointLogFile(safepointJdk8LogFileParser.fetchData());
        jvmLogFile.setGcLogFile(gcJdk8LogFileParser.fetchData());
        addPages(jvmLogFile);
        onComplete.accept(jvmLogFile);
        notifyProgress.accept(new ParsingProgress(numberOfLine, true));
        return jvmLogFile;

    }

    public JvmLogFile createAllStatsUnifiedLogger(InputStream inputStream, String originalFilename, Consumer<ParsingProgress> notifyProgress, Consumer<JvmLogFile> onComplete) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        SafepointUnifiedLogFileParser safepointUnifiedLogFileParser = new SafepointUnifiedLogFileParser();
        GCUnifiedLogFileParser gcUnifiedLogFileParser = new GCUnifiedLogFileParser();
        ThreadLogFileParser threadLogFileParser = new ThreadLogFileParser();
        ClassLoaderLogFileParser classLoaderLogFileParser = new ClassLoaderLogFileParser();
        JitLogFileParser jitLogFileParser = new JitLogFileParser();
        TlabLogFileParser tlabLogFileParser = new TlabLogFileParser();
        StringDedupLogFileParser stringDedupLogFileParser = new StringDedupLogFileParser();

        long numberOfLine = 1;
        try {
            String line = reader.readLine();
            while (line != null) {
                safepointUnifiedLogFileParser.parseLine(line);
                gcUnifiedLogFileParser.parseLine(line);
                threadLogFileParser.parseLine(line);
                classLoaderLogFileParser.parseLine(line);
                jitLogFileParser.parseLine(line);
                tlabLogFileParser.parseLine(line);
                stringDedupLogFileParser.parseLine(line);
                line = reader.readLine();
                if (numberOfLine % 1000 == 0) {
                    notifyProgress.accept(new ParsingProgress(numberOfLine, false));
                }
                numberOfLine++;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        JvmLogFile jvmLogFile = new JvmLogFile();
        jvmLogFile.setFilename(originalFilename);
        jvmLogFile.setSafepointLogFile(safepointUnifiedLogFileParser.fetchData());
        jvmLogFile.setGcLogFile(gcUnifiedLogFileParser.fetchData());
        jvmLogFile.setThreadLogFile(threadLogFileParser.fetchData());
        jvmLogFile.setClassLoaderLogFile(classLoaderLogFileParser.fetchData());
        jvmLogFile.setJitLogFile(jitLogFileParser.fetchData());
        jvmLogFile.setTlabLogFile(tlabLogFileParser.fetchData());
        jvmLogFile.setStringDedupLogFile(stringDedupLogFileParser.fetchData());

        addPages(jvmLogFile);
        onComplete.accept(jvmLogFile);
        notifyProgress.accept(new ParsingProgress(numberOfLine, true));
        return jvmLogFile;
    }

    private void addPages(JvmLogFile jvmLogFile) {
        createSafepointPages(jvmLogFile);
        createGcPages(jvmLogFile);
        createThreadPages(jvmLogFile);
        createClassLoaderPages(jvmLogFile);
        createJitPages(jvmLogFile);
        createTlabPages(jvmLogFile);
        createStringDedupPages(jvmLogFile);
    }

    private void createStringDedupPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getStringDedupLogFile() == null) {
            return;
        }

        List<PageCreator> stringDedupPageCreators = new ArrayList<>();
        if (!jvmLogFile.getStringDedupLogFile().getEntries().isEmpty()) {
            stringDedupPageCreators.add(new StringDedupTotal());
            stringDedupPageCreators.add(new StringDedupLast());
        }

        for (PageCreator stringDedupPageCreator : stringDedupPageCreators) {
            Page page = stringDedupPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createTlabPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getTlabLogFile() == null) {
            return;
        }

        List<PageCreator> tlabPageCreators = new ArrayList<>();
        if (!jvmLogFile.getTlabLogFile().getTlabSummaries().isEmpty()) {
            tlabPageCreators.add(new TlabSummary());
        }

        if (!jvmLogFile.getTlabLogFile().getThreadTlabsBeforeGC().isEmpty()) {
            tlabPageCreators.add(new TlabThreadStats());
        }

        for (PageCreator jitPageCreator : tlabPageCreators) {
            Page page = jitPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createJitPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getJitLogFile() == null) {
            return;
        }

        List<PageCreator> jitPageCreators = new ArrayList<>();
        if (jvmLogFile.getJitLogFile().getLastStatus() != null) {
            jitPageCreators.add(new JitCompilationCount());
            jitPageCreators.add(new JitTieredCompilationCount());
        }

        if (jvmLogFile.getJitLogFile().getCodeCacheStatuses().size() > 0) {
            jitPageCreators.add(new JitCodeCacheStats());
        }

        if (jvmLogFile.getJitLogFile().getCodeCacheSweeperActivities().size() > 0) {
            jitPageCreators.add(new JitCodeCacheSweeperActivity());
        }

        for (PageCreator jitPageCreator : jitPageCreators) {
            Page page = jitPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createClassLoaderPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getClassLoaderLogFile() == null || jvmLogFile.getClassLoaderLogFile().getLastStatus() == null) {
            return;
        }

        List<PageCreator> classLoaderPageCreators = List.of(
                new ClassCount()
        );

        for (PageCreator classLoaderPageCreator : classLoaderPageCreators) {
            Page page = classLoaderPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createThreadPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getThreadLogFile() == null || jvmLogFile.getThreadLogFile().getLastStatus() == null) {
            return;
        }

        List<PageCreator> threadPageCreators = List.of(
                new ThreadCount()
        );

        for (PageCreator threadPageCreator : threadPageCreators) {
            Page page = threadPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createGcPages(JvmLogFile jvmLogFile) {
        if (CollectionUtils.isEmpty(jvmLogFile.getGcLogFile().getCycleEntries())) {
            return;
        }
        List<PageCreator> gcPageCreators = List.of(
                new GCTableStats(),
                new GCSubphaseStats(),
                new GCPhaseTime(),
                new GCRegionCountBefore(),
                new GCRegionCountAfter(),
                new GCRegionMax(),
                new GCRegionSizeAfter(),
                new GCHeapBefore(),
                new GCHeapAfter(),
                new GCHeapBeforeAfter(),
                new GCSurvivorAndTenuring(),
                new GCAllocationRate(),
                new GCAllocationRateInTime(new BigDecimal(10))
        );

        for (PageCreator safepointPageCreator : gcPageCreators) {
            Page page = safepointPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createSafepointPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getSafepointLogFile() == null || CollectionUtils.isEmpty(jvmLogFile.getSafepointLogFile().getSafepoints())) {
            return;
        }

        List<PageCreator> safepointPageCreators = List.of(
                new SafepointTableStats(),
                new SafepointTotalTimeInPhases(),
                new SafepointApplicationTimeByTime(),
                new SafepoinOperationCount(),
                new SafepoinOperationTime(),
                new SafepointOperationTimeCharts()
        );

        for (PageCreator safepointPageCreator : safepointPageCreators) {
            jvmLogFile.getPages().add(safepointPageCreator.create(jvmLogFile, decimalFormat));
        }
    }
}
