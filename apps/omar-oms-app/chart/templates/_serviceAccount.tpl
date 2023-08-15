
{{/*
Determine whether the serviceAccount should be created by examining local and global values
*/}}
{{- define "omar-oms.serviceAccount.enabled" -}}
{{- $globals := and (hasKey .Values.global.serviceAccount "enabled") (kindIs "bool" .Values.global.serviceAccount.enabled) -}}
{{- $locals := and (hasKey .Values.serviceAccount "enabled") (kindIs "bool" .Values.serviceAccount.enabled) -}}
{{- if $locals }}
{{-   .Values.serviceAccount.enabled }}
{{- else if $globals }}
{{-  .Values.global.serviceAccount.enabled }}
{{- else }}
{{-   false }}
{{- end -}}
{{- end -}}

{{/*
Determine the serviceAccount class name
*/}}
{{- define "omar-oms.serviceAccount.name" -}}
{{-   if eq (include "omar-oms.serviceAccount.enabled" $) "true" }}
{{-     pluck "name" .Values.serviceAccount .Values.global.serviceAccount | first | default (include "omar-oms.fullname" $) -}}
{{-   else }}
{{-     pluck "name" .Values.serviceAccount .Values.global.serviceAccount | first | default "default" -}}
{{-   end }}
{{- end -}}
