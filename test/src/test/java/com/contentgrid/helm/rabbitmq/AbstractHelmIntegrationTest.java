package com.contentgrid.helm.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.helm.HelmInstallCommand.InstallOption;
import com.contentgrid.junit.jupiter.helm.HelmChart;
import com.contentgrid.junit.jupiter.helm.HelmChartHandle;
import com.contentgrid.junit.jupiter.k8s.wait.KubernetesResourceWaiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public abstract class AbstractHelmIntegrationTest {

    static String NAMESPACE;
    static String FULLNAME;

    @HelmChart(chart = "file:../")
    static HelmChartHandle rabbitmq;

    static KubernetesClient kubernetesClient;

    @BeforeAll
    static void installAndWait() {
        var kubernetesSvc = kubernetesClient.services()
                .inNamespace("default")
                .withName("kubernetes")
                .get();
        var apiServerCidr = kubernetesSvc.getSpec().getClusterIP() + "/32";

        // Also include the actual endpoint IP: kube-router enforces NetworkPolicies
        // post-DNAT, so the post-NAT destination (node IP) must also be allowed.
        var kubernetesEndpoints = kubernetesClient.endpoints()
                .inNamespace("default")
                .withName("kubernetes")
                .get();
        var endpointCidr = kubernetesEndpoints.getSubsets().get(0).getAddresses().get(0).getIp() + "/32";

        var result = rabbitmq.install(
                InstallOption.createNamespace(),
                InstallOption.values(Map.of(
                        "persistence.size", "1Gi",
                        "networkPolicy.kubeAPIServerCIDRs[0]", apiServerCidr,
                        "networkPolicy.kubeAPIServerCIDRs[1]", endpointCidr
                ))
        );

        NAMESPACE = result.namespace();
        FULLNAME = result.name() + "-helm-rabbitmq";


        new KubernetesResourceWaiter(kubernetesClient)
                .include(result)
                .await(wait -> wait.atMost(10, TimeUnit.MINUTES));
    }

    @Test
    void bootstrapSecret_hasRequiredKeys() {
        var secret = kubernetesClient.secrets()
                .inNamespace(NAMESPACE)
                .withName(FULLNAME)
                .get();
        assertThat(secret).isNotNull();

        var data = secret.getData();
        assertThat(data).containsKey("password");
        assertThat(data).containsKey("erlang-cookie");

        String password = new String(Base64.getDecoder().decode(data.get("password")), StandardCharsets.UTF_8);
        String erlangCookie = new String(Base64.getDecoder().decode(data.get("erlang-cookie")), StandardCharsets.UTF_8);

        assertThat(password).hasSizeGreaterThan(10);
        assertThat(erlangCookie).hasSize(32);
    }

    @Test
    void statefulSet_isFullyClustered() {
        var sts = kubernetesClient.apps().statefulSets()
                .inNamespace(NAMESPACE)
                .withName(FULLNAME)
                .get();
        assertThat(sts).isNotNull();
        assertThat(sts.getStatus().getReadyReplicas()).isEqualTo(3);
    }

    @Test
    void pods_areRunningAndReady() {
        var pods = kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .withLabel("app.kubernetes.io/name", "helm-rabbitmq")
                .list()
                .getItems();
        assertThat(pods).hasSize(3);
        for (var pod : pods) {
            assertThat(pod.getStatus().getPhase()).isEqualTo("Running");
            for (var cs : pod.getStatus().getContainerStatuses()) {
                assertThat(cs.getReady()).isTrue();
            }
        }
    }

    @Test
    void headlessService_exists() {
        var svc = kubernetesClient.services()
                .inNamespace(NAMESPACE)
                .withName(FULLNAME + "-headless")
                .get();
        assertThat(svc).isNotNull();
        assertThat(svc.getSpec().getClusterIP()).isEqualTo("None");
    }

    @Test
    void amqpConnectivityWorks() throws Exception {
        String password = readPassword();

        var pod = kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .withName(FULLNAME + "-0")
                .get();
        assertThat(pod).isNotNull();

        try (LocalPortForward pf = portForward(FULLNAME + "-0", 5672)) {
            var factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(pf.getLocalPort());
            factory.setUsername("user");
            factory.setPassword(password);
            factory.setVirtualHost("/");

            try (Connection connection = factory.newConnection()) {
                assertThat(connection.isOpen()).isTrue();
            }
        }
    }

    @Test
    void cluster_hasThreeRunningNodes() throws Exception {
        var response = managementApiRequest("/api/nodes");

        assertThat(response.statusCode()).isEqualTo(200);
        var nodes = new ObjectMapper().readTree(response.body());
        assertThat(nodes).hasSize(3);
        for (var node : nodes) {
            assertThat(node.get("running").asBoolean()).isTrue();
        }
    }

    @Test
    void managementApiReturns200() throws Exception {
        var response = managementApiRequest("/api/overview");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> managementApiRequest(String path) throws Exception {
        String password = readPassword();

        try (LocalPortForward pf = portForward(FULLNAME + "-0", 15672)) {
            String credentials = Base64.getEncoder().encodeToString(
                    ("user:" + password).getBytes(StandardCharsets.UTF_8));

            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + pf.getLocalPort() + path))
                    .header("Authorization", "Basic " + credentials)
                    .GET()
                    .build();
            return HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private String readPassword() {
        var secret = kubernetesClient.secrets()
                .inNamespace(NAMESPACE)
                .withName(FULLNAME)
                .get();
        return new String(Base64.getDecoder().decode(secret.getData().get("password")), StandardCharsets.UTF_8);
    }

    private LocalPortForward portForward(String podName, int port) {
        return kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .withName(podName)
                .portForward(port);
    }
}
