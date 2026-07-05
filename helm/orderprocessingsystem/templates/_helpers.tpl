{{- define "ops.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "ops.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "ops.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "ops.postgres.fullname" -}}
{{- printf "%s-postgres" (include "ops.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "ops.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/name: {{ include "ops.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "ops.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ops.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "ops.postgres.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ops.name" . }}-postgres
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
