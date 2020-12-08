package pl.ks.profiling.safepoint.analyzer.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pl.ks.profiling.safepoint.analyzer.commons.shared.KernelConfiguration;

@Configuration
@Import({
        KernelConfiguration.class
})
class AnalyzerWebApplicationConfiguration {
}
