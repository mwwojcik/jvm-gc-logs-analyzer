package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcCycleInfo;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;

public class GCRegionCountBefore implements PageCreator{
    @Override
    public Page create(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        if (safepointLogFile.getGcStats().getGcRegions().isEmpty()) {
            return null;
        }
        return Page.builder()
                .menuName("GC region stats - before GC")
                .fullName("Garbage Collector region stats - before GC")
                .info("Page presents charts with count of G1 regions before Garbage Collection. Charts are generated for every Garbage Collector phase.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        safepointLogFile.getGcStats().getGcAggregatedPhases().stream()
                                .map(phase -> Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title(phase)
                                        .data(getChart(phase, safepointLogFile))
                                        .build())
                                .filter(chart -> chart.getData() != null)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static Object[][] getChart(String aggregatedPhase, SafepointLogFile safepointLogFile) {
        List<GcCycleInfo> cycles = safepointLogFile.getGcLogFile().getGcCycleInfos().stream()
                .filter(gcCycleInfo -> aggregatedPhase.equals(gcCycleInfo.getAggregatedPhase()))
                .collect(Collectors.toList());
        Set<String> regions = cycles.stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsBeforeGC().keySet().stream())
                .collect(Collectors.toSet());
        if (regions.size() == 0) {
            return null;
        }
        List<String> regionsSorted = new ArrayList<>(regions);
        regionsSorted.sort(String::compareTo);
        Object[][] stats = new Object[cycles.size() + 1][regionsSorted.size() + 1];
        stats[0][0] = "GC sequence";
        int i = 1;
        for (String region : regionsSorted) {
            stats[0][i] = region;
            i++;
        }

        int j = 1;
        for (GcCycleInfo cycle : cycles) {
            stats[j][0] = cycle.getTimeStamp();
            i = 1;
            for (String region : regionsSorted) {
                stats[j][i] = cycle.getRegionsBeforeGC().get(region);
                i++;
            }
            j++;
        }

        return stats;
    }
}
