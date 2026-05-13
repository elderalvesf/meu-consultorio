# Meu Consultório

App Android para gestão de consultório odontológico.

## Funcionalidades

- **Pacientes** — cadastro, busca e histórico completo
- **Agenda** — consultas, tratamentos e compromissos com visualização por dia
- **Prontuário** — registro de evoluções por consulta
- **Tratamentos** — controle de procedimentos por paciente
- **Financeiro** — lançamentos de pagamentos e visão geral

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitetura | MVVM + Repository |
| BD Local | Room |
| Auth | Firebase Authentication |
| Sync | Cloud Firestore |
| DI | Hilt |
| Nav | Navigation Compose |

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35

## CI/CD

| Workflow | Trigger | O que faz |
|---------|---------|-----------|
| `build.yml` | push `main` | Build + gera APK release + cria GitHub Release |
| `run-tests.yml` | `repository_dispatch` / manual | E2E com Appium + WebdriverIO no emulador Android 14 |

Build badges ficam em [Actions](../../actions).

## Testes E2E

Repositório separado: [`meu-consultorio-tests`](https://github.com/elderalvesf/meu-consultorio-tests)

Specs cobertas: Login · Home · Pacientes · Agenda · Formulário de consulta · Detalhe do paciente · Formulário de tratamento

## Setup local

1. Clone o repo
2. Adicione `google-services.json` em `app/`
3. Crie `local.properties` com `sdk.dir` apontando para o Android SDK
4. Build: `./gradlew assembleRelease`

Assinar release: configure `meu-consultorio.jks` + variáveis `KEYSTORE_*` no ambiente ou `local.properties`.
