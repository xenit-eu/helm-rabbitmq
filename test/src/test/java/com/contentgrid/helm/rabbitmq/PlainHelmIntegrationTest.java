package com.contentgrid.helm.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.junit.jupiter.helm.HelmClient;
import com.contentgrid.junit.jupiter.k8s.KubernetesTestCluster;
import com.contentgrid.junit.jupiter.k8s.providers.K3sTestcontainersClusterProvider;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@KubernetesTestCluster(providers = K3sTestcontainersClusterProvider.class)
@HelmClient
class PlainHelmIntegrationTest extends AbstractHelmIntegrationTest {

    @Test
    void standardNetworkPolicy_exists() {
        var np = kubernetesClient.network().networkPolicies()
                .inNamespace(NAMESPACE)
                .withName(FULLNAME)
                .get();
        assertThat(np).isNotNull();
    }
}
