package com.contentgrid.helm.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.helm.Helm;
import com.contentgrid.helm.HelmTemplateCommand.TemplateFlag;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AffinityTemplateTest {

    private StatefulSet renderStatefulSet(TemplateFlag... flags) {
        var output = Helm.builder().build()
                .template()
                .chart("test", "../rabbitmq", flags)
                .getOutput();

        return Arrays.stream(output.split("---\n"))
                .filter(doc -> doc.contains("kind: StatefulSet"))
                .map(doc -> Serialization.unmarshal(doc, StatefulSet.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No StatefulSet found in helm template output"));
    }

    @Test
    void defaultAffinity_appliesSoftPodAntiAffinity() {
        var sts = renderStatefulSet();

        var antiAffinity = sts.getSpec().getTemplate().getSpec().getAffinity().getPodAntiAffinity();
        assertThat(antiAffinity).isNotNull();
        var preferred = antiAffinity.getPreferredDuringSchedulingIgnoredDuringExecution();
        assertThat(preferred).hasSize(1);
        assertThat(preferred.get(0).getPodAffinityTerm().getTopologyKey())
                .isEqualTo("kubernetes.io/hostname");
    }

    @Test
    void emptyAffinity_disablesAffinityRules() {
        var sts = renderStatefulSet(
                TemplateFlag.values(Map.of("affinity", Map.of()))
        );

        assertThat(sts.getSpec().getTemplate().getSpec().getAffinity()).isNull();
    }

    @Test
    void customAffinity_overridesDefaultAntiAffinity() {
        var sts = renderStatefulSet(
                TemplateFlag.values(Map.of("affinity", Map.of(
                        "podAntiAffinity", Map.of(
                                "requiredDuringSchedulingIgnoredDuringExecution", List.of(
                                        Map.of(
                                                "labelSelector", Map.of(
                                                        "matchLabels", Map.of("app.kubernetes.io/name", "rabbitmq")
                                                ),
                                                "topologyKey", "kubernetes.io/hostname"
                                        )
                                )
                        )
                )))
        );

        var antiAffinity = sts.getSpec().getTemplate().getSpec().getAffinity().getPodAntiAffinity();
        assertThat(antiAffinity.getPreferredDuringSchedulingIgnoredDuringExecution()).isNullOrEmpty();
        assertThat(antiAffinity.getRequiredDuringSchedulingIgnoredDuringExecution()).hasSize(1);
        assertThat(antiAffinity.getRequiredDuringSchedulingIgnoredDuringExecution().get(0).getTopologyKey())
                .isEqualTo("kubernetes.io/hostname");
    }
}
