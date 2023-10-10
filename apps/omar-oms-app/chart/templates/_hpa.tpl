
{{/*
Determine whether the hpa should be created by examining local and global values
*/}}
{{- define "omar-oms.autoscaling.enabled" -}}
{{- $globals := and (hasKey .Values.global.autoscaling "enabled") (kindIs "bool" .Values.global.autoscaling.enabled) -}}
{{- $locals := and (hasKey .Values.autoscaling "enabled") (kindIs "bool" .Values.autoscaling.enabled) -}}
{{- if $locals }}
{{-   .Values.autoscaling.enabled }}
{{- else if $globals }}
{{-  .Values.global.autoscaling.enabled }}
{{- else }}
{{-   true }}
{{- end -}}
{{- end -}}
