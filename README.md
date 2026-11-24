# Hao Alert

Aplicativo Android nativo (Kotlin + Jetpack Compose) de lembretes recorrentes.
A notificação de uma tarefa se repete no intervalo escolhido **até você abrir a tarefa e tocar em
“Marcar como concluída”**. Tudo funciona offline e fica salvo só no aparelho: sem login, sem
nuvem, sem APIs externas.

## Como funcionam os lembretes

- Cada tarefa tem **um alarme** no `AlarmManager` (`setExactAndAllowWhileIdle`), identificado pelo id da
  tarefa. Reagendar substitui o alarme anterior, então não há notificações duplicadas.
- O `AlarmReceiver` agenda o próximo horário **antes** de mostrar a notificação. Ignorar ou dispensar a
  notificação não interrompe a repetição.
- As tarefas usam **datas escolhidas em um calendário clicável**. Os horários seguem uma grade fixa
  (início + n × intervalo) dentro de cada data. Um novo ciclo começa sozinho na próxima data escolhida.
- Intervalos disponíveis: 30 s, 1 min, 5, 10, 15, 20, 30 e 60 min. Com o aparelho em modo Doze (parado, tela
  apagada) o Android pode agrupar alarmes muito próximos, então 30 s e 1 min são mais confiáveis com o
  aparelho em uso ou o app liberado da economia de bateria.
- Só a conclusão explícita na tela de detalhes encerra o ciclo. A notificação tem as ações
  *Abrir tarefa* e *Adiar 10 min*; não existe ação que conclua sem abrir os detalhes.
- Após reinicialização, troca de hora/fuso ou atualização do app, o `BootReceiver` reconstrói os
  alarmes a partir do banco (Room). Um `WorkManager` periódico apenas confere se algum alarme sumiu.

## Alerta visual com GIF

Quando um lembrete dispara, o `AlarmReceiver` (depois de agendar o próximo aviso e mostrar a notificação)
inicia o `OverlayService`, que desenha uma sobreposição em tela cheia (`WindowManager` +
`TYPE_APPLICATION_OVERLAY`) com o GIF `assets/tung-tung-sahur.gif` em reprodução contínua, o título da
tarefa e “TOQUE PARA ABRIR”.

- Só há uma sobreposição (e um GIF) por vez; um novo disparo substitui a anterior.
- Tocar abre os detalhes da tarefa. O “✕” só dispensa: a tarefa segue pendente e as repetições continuam.
- Concluir, excluir, pausar ou adiar a tarefa fecha a sobreposição.
- Liga/desliga geral em Configurações e opção individual em cada tarefa. A permissão
  “Exibir sobre outros apps” é solicitada com explicação; não usa acessibilidade.
- O GIF é o arquivo original (sem conversão). Android 9+ usa `ImageDecoder`/`AnimatedImageDrawable`
  (decodificação quadro a quadro, com limite de tamanho); Android 8.x usa `Movie`.

## Permissões

| Permissão | Para quê |
|---|---|
| `POST_NOTIFICATIONS` (Android 13+) | Exibir os lembretes |
| `SCHEDULE_EXACT_ALARM` | Avisos no minuto exato |
| `RECEIVE_BOOT_COMPLETED` | Reagendar após reiniciar |
| `VIBRATE` | Vibração das notificações |
| `SYSTEM_ALERT_WINDOW` | Alerta visual com GIF sobre outros apps |
| `FOREGROUND_SERVICE` (+ `_SPECIAL_USE`) | Mantém a sobreposição enquanto ela está na tela |

Em celulares Xiaomi, Samsung, Huawei, Oppo etc., libere o app da economia de bateria
(Configurações do app → Permissões → “Sem restrição de bateria”).

## Compilar

Requer JDK 17 e Android SDK 35.

```bash
gradle assembleRelease -PappVersion=1.0.0 -PappVersionCode=1
```

O APK sai em `app/build/outputs/apk/release/`. Abrir a pasta no Android Studio também funciona.

## GitHub Actions

`.github/workflows/android.yml`:

- roda a cada push na `main` e manualmente (*Run workflow*);
- gera o APK de release e o publica como artefato `HaoAlert-vX.Y.Z.apk`;
- ao enviar uma tag `vX.Y.Z`, cria a Release e anexa o APK.

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Assinatura

Nenhuma chave fica no repositório. Sem configuração, o APK é assinado com a chave de debug e pode ser
instalado diretamente. Para assinar com a sua própria chave, crie estes *secrets* no repositório:
`HAO_KEYSTORE_BASE64`, `HAO_KEYSTORE_PASSWORD`, `HAO_KEY_ALIAS`, `HAO_KEY_PASSWORD`.
Para atualizar um app já instalado, use sempre a mesma chave.

## Estrutura

```
app/src/main/java/com/haoalert/app/
├── data/            Room (tarefas e histórico), repositório, configurações
├── domain/          ScheduleCalculator (cálculo dos horários)
├── notifications/   AlarmScheduler, receivers, Notifier, permissões, worker
└── ui/              tema, componentes, telas e navegação (Compose)
```

O ícone vem de `assets_src/hao_red.png` (arte original recolorida para vermelho).
