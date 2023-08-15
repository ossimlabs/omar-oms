{{- define "omar-oms.imagePullSecret" }}
{{- printf "{\"auths\": {\"%s\": {\"auth\": \"%s\"}}}" .Values.global.imagePullSecret.registry (printf "%s:%s" .Values.global.imagePullSecret.username .Values.global.imagePullSecret.password | b64enc) | b64enc }}
{{- end }}

{{/* Template for env vars */}}
{{- define "omar-oms.envVars" -}}
  {{- range $key, $value := merge .Values.envVars .Values.global.envVars }}
  - name: {{ tpl (toString $key) $ | quote }}
    value: {{ tpl (toString $value) $ | quote }}
  {{- end }}
{{- end -}}

{{/*
Expand the name of the chart.
*/}}
{{- define "omar-oms.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "omar-oms.fullname" -}}
{{-   if .Values.fullnameOverride }}
{{-     .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{-   else }}
{{-     $name := default .Chart.Name .Values.nameOverride }}
{{-     if contains $name .Release.Name }}
{{-       .Release.Name | trunc 63 | trimSuffix "-" }}
{{-     else }}
{{-       printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{-     end }}
{{-   end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "omar-oms.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "omar-oms.labels" -}}
omar-oms.sh/chart: {{ include "omar-oms.chart" . }}
{{ include "omar-oms.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "omar-oms.selectorLabels" -}}
app.kubernetes.io/name: {{ include "omar-oms.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Return the proper image name
*/}}
{{- define "omar-oms.image" -}}
{{- $registryName := .Values.image.registry -}}
{{- $imageName := .Values.image.name -}}
{{- $tag := .Values.image.tag | default .Chart.AppVersion | toString -}}
{{- if .Values.global }}
    {{- if .Values.global.image.registry }}
        {{- printf "%s/%s:%s" .Values.global.image.registry $imageName $tag -}}
    {{- else -}}
        {{- printf "%s/%s:%s" $registryName $imageName $tag -}}
    {{- end -}}
{{- else -}}
    {{- printf "%s/%s:%s" $registryName $imageName $tag -}}
{{- end -}}
{{- end -}}

{{- define "omar-oms.pullPolicy" -}}
{{ .Values.image.pullPolicy | default .Values.global.image.pullPolicy | default "IfNotPresent" }}
{{- end -}}
