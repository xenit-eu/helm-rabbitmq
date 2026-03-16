{{/*
Expand the name of the chart.
*/}}
{{- define "rabbitmq.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "rabbitmq.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "rabbitmq.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "rabbitmq.labels" -}}
helm.sh/chart: {{ include "rabbitmq.chart" . }}
{{ include "rabbitmq.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "rabbitmq.selectorLabels" -}}
app.kubernetes.io/name: {{ include "rabbitmq.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "rabbitmq.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "rabbitmq.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the headless service to use
*/}}
{{- define "rabbitmq.headlessServiceName" -}}
{{- ((include "rabbitmq.fullname" .) | trunc 56) | trimSuffix "-" }}-headless
{{- end }}

{{/*
Create the image reference to use
*/}}
{{- define "rabbitmq.image" -}}
{{- $tail := printf ":%s" .Chart.AppVersion | toString -}}
{{- if .Values.image.tag -}}
    {{- $tail = printf ":%s" .Values.image.tag -}}
{{- end -}}
{{- if .Values.image.digest -}}
    {{- $tail = printf "@%s" .Values.image.digest -}}
{{- end -}}
{{- printf "%s%s" .Values.image.repository $tail -}}
{{- end }}

{{- define "rabbitmq.bootstrap.name" -}}
{{- (include "rabbitmq.fullname" .) | trunc 53 | trimSuffix "-" }}-bootstrap
{{- end }}

{{- define "rabbitmq.bootstrap.metadata" -}}
metadata:
  name: {{ include "rabbitmq.bootstrap.name" . }}
  annotations:
    "helm.sh/hook": pre-install,pre-upgrade
    "helm.sh/hook-weight": "-5"
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
  labels:
    {{- include "rabbitmq.bootstrap.selectorLabels" . | nindent 4 }}
{{- end }}

{{- define "rabbitmq.bootstrap.selectorLabels" -}}
app.kubernetes.io/name: {{ include "rabbitmq.bootstrap.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
