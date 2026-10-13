El asistente de voz: escucha una orden hablada y, según elija el familiar, la hace
tras confirmarla o explica cómo hacerla. Cómo está montado, en
`docs/arquitectura.md` («Asistente de voz»); sus normas, en
`.claude/rules/asistente.md`.

- `AssistantController`: el flujo entero, como máquina de estados.
- `audio/`: grabar (`VoiceRecorder`) y hablar (`Speaker`).
- `cloud/`: lo único que conoce al proveedor. Cambiarlo es tocar `local.properties`.
- `ui/`: el círculo del inicio y la pantalla del asistente.
