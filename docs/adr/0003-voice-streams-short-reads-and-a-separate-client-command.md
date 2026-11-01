# Voice uses short stream reads, and client settings have their own command

## Decision

`VoiceStream` returns a quarter of a second on each read, even though the sound engine asks for a full second. Client settings use `/hardwiredclient config`, not `/hardwired config`.

## Why

The sound engine queues four reads per source. Full reads would hold four seconds of audio before a word is heard. Short reads hold one second, and the measured delay from speaking to hearing is about 1.15 seconds.

Fabric's client command dispatcher runs before the server's. If `hardwired config` exists on the client, any server setting under it fails on the client with an "incorrect argument" error and never reaches the server. Hence a separate root.

## Consequences

- If a client freezes for more than a second, the source runs dry and stops. The next tick starts a new one, so a voice only loses that moment.
- Client settings and server settings are set by two different commands.
