package com.contentgrid.helm.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.junit.jupiter.helm.HelmChart;
import com.contentgrid.junit.jupiter.helm.HelmChartHandle;
import com.contentgrid.junit.jupiter.helm.HelmClient;
import com.contentgrid.junit.jupiter.k8s.KubernetesTestCluster;
import com.contentgrid.junit.jupiter.k8s.providers.K3sTestcontainersClusterProvider;
import com.contentgrid.testcontainers.k3s.customizer.cilium.DefaultDenyCiliumK3sContainerCustomizer;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@KubernetesTestCluster(providers = CiliumHelmIntegrationTest.K3sCilium.class)
@HelmClient
class CiliumHelmIntegrationTest extends AbstractHelmIntegrationTest {

    static class K3sCilium extends K3sTestcontainersClusterProvider {
        K3sCilium() {
            configure(DefaultDenyCiliumK3sContainerCustomizer.class);
        }
    }

    @Test
    void ciliumNetworkPolicy_exists() {
        var cnp = kubernetesClient
                .genericKubernetesResources("cilium.io/v2", "CiliumNetworkPolicy")
                .inNamespace(NAMESPACE)
                .withName(FULLNAME)
                .get();
        assertThat(cnp).isNotNull();
    }
}
