# helm-rabbitmq

Helm chart for deploying RabbitMQ as a StatefulSet on Kubernetes.

## Configuration

### General

| Key | Description | Default |
|---|---|---|
| `replicaCount` | Number of RabbitMQ replicas | `3` |
| `configuration` | RabbitMQ configuration | `""` |

### Image

| Key | Description | Default |
|---|---|---|
| `image.repository` | RabbitMQ image repository | `rabbitmq` |
| `image.tag` | Image tag | chart appVersion |
| `image.digest` | Image digest; takes precedence over tag if set | not set |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |
| `imagePullSecrets` | Secrets for pulling from a private registry | `[]` |

### Service account

| Key | Description | Default |
|---|---|---|
| `serviceAccount.create` | Whether to create a ServiceAccount | `true` |
| `serviceAccount.name` | ServiceAccount name; auto-generated if empty | auto-generated |
| `serviceAccount.annotations` | Annotations to add to the ServiceAccount | `{}` |

### Pod configuration

| Key | Description | Default |
|---|---|---|
| `podAnnotations` | Annotations to add to RabbitMQ pods | `{}` |
| `podLabels` | Labels to add to RabbitMQ pods | `{}` |
| `podSecurityContext` | Security context for the RabbitMQ pod | `{}` |
| `securityContext` | Security context for the RabbitMQ container | `{}` |
| `resources` | Resource requests and limits for the RabbitMQ container | `{}` |
| `nodeSelector` | Node selector for RabbitMQ pods | `{}` |
| `tolerations` | Tolerations for RabbitMQ pods | `[]` |
| `affinity` | Affinity rules for RabbitMQ pods | `{}` |
| `additionalVolumes` | Extra volumes to add to the StatefulSet | `[]` |
| `additionalVolumeMounts` | Extra volume mounts for the RabbitMQ container | `[]` |

### Persistence

| Key | Description | Default |
|---|---|---|
| `persistence.storageClassName` | Storage class for the data volume | cluster default |
| `persistence.size` | Size of the RabbitMQ data volume | `8Gi` |

### Bootstrap job

| Key | Description | Default |
|---|---|---|
| `bootstrap.enabled` | Enable the bootstrap job that creates the credentials secret | `true` |
| `bootstrap.image.repository` | Bootstrap job image repository | `ghcr.io/xenit-eu/kubectl` |
| `bootstrap.image.tag` | Bootstrap job image tag | `latest` |
| `bootstrap.resources` | Resource requests and limits for the bootstrap container | `{}` |

### Pod disruption budget

| Key | Description | Default |
|---|---|---|
| `podDisruptionBudget.enabled` | Enable a PodDisruptionBudget for the StatefulSet | `true` |
| `podDisruptionBudget.minAvailable` | Minimum number of pods that must remain available during disruptions; mutually exclusive with `maxUnavailable` | not set |
| `podDisruptionBudget.maxUnavailable` | Maximum number of pods that can be unavailable during disruptions; mutually exclusive with `minAvailable` | `1` |

### Network policies

| Key | Description | Default |
|---|---|---|
| `networkPolicy.enabled` | Enable NetworkPolicy resources | `true` |
| `networkPolicy.kubeAPIServerCIDRs` | kube-apiserver CIDRs for bootstrap egress; not needed with Cilium | `[]` |
| `networkPolicy.kubeAPIServerPorts` | kube-apiserver ports | `[443, 6443, 8443]` |

### Naming

| Key | Description | Default |
|---|---|---|
| `nameOverride` | Override the chart name used in resource names | `""` |
| `fullnameOverride` | Override the fully qualified app name | `""` |
