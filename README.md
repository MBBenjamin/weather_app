# Clima Agora

Um aplicativo Android de previsão do tempo — rápido, simples e que funciona mesmo sem internet.

---

## O que é

Clima Agora é um app para quem quer saber a previsão do tempo sem complicação. Abre rápido, mostra o que importa, e não pede permissões desnecessárias nem gasta bateria.

## O que ele faz

- Mostra as condições atuais da sua localização assim que você abre o app
- Exibe a previsão hora a hora para as próximas 24 horas, em gráfico ou lista
- Apresenta a previsão dos próximos 7 dias em cards claros e diretos
- Detecta a sua localização automaticamente, sem precisar digitar nada
- Permite buscar qualquer cidade do mundo pelo nome
- Funciona offline com os dados mais recentes em cache

## Filosofia

Menos é mais. A maioria dos apps de clima tem dezenas de telas, gráficos complexos e dados que ninguém usa no dia a dia. Clima Agora foca no essencial: temperatura, condição do tempo e o que esperar nas próximas horas e dias.

O app foi pensado para quem consulta a previsão do tempo rapidamente antes de sair — não para meteorologistas.

## Pré-requisitos

| Ferramenta     | Versão mínima                   |
|----------------|---------------------------------|
| Android Studio | Ladybug 2024.1+                 |
| Android SDK    | API 24 (min) → API 34 (target)  |
| JDK            | 17 (incluído no Android Studio) |
| Git            | 2.40+                           |

## Setup

```bash
git clone <repo-url>
cd weather_app
# Abrir no Android Studio: File → Open → selecionar pasta
```

### ⚠️ Firebase — configuração obrigatória antes do release

O arquivo `app/google-services.json` incluído no repositório contém **valores fictícios para desenvolvimento**.
Antes de fazer um build de release ou publicar no Firebase, substitua-o pelo arquivo real:

1. Acesse o [Firebase Console](https://console.firebase.google.com)
2. Crie ou selecione seu projeto
3. Adicione o app Android com package name `com.weather`
4. Faça o download do `google-services.json`
5. Substitua o arquivo em `app/google-services.json`

### Build

```bash
# Debug (LeakCanary ativo, logs verbosos)
./gradlew assembleDebug

# Release (ProGuard/R8, sem logs)
./gradlew assembleRelease

# Testes unitários
./gradlew test

# Testes instrumentados (requer emulador ou device)
./gradlew connectedAndroidTest
```

## Status

Em desenvolvimento. Primeira versão (MVP) em construção — ver [tasks.md](specs/001-weather-app-mvp/tasks.md).

---

*Dados meteorológicos fornecidos por [Open-Meteo](https://open-meteo.com) — gratuito e de código aberto.*
