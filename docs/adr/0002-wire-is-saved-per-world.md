# Wire is saved per world, not per chunk

## Decision

All wire in a dimension is saved in one file in that dimension's data folder, not in chunk data.

## Why

A signal must reach every loaded output on the next tick, even when the wire between them runs through unloaded chunks (WIRE-8, WIRE-12). Cutting a wire must also tell whether a network has split. Both need the whole network to be known without loading a chunk (PERF-6).

Saving wire in chunks would mean keeping a second copy of how chunks connect, so the two could drift apart. One store has one source of truth.

## Consequences

- Finding a network's parts never loads a chunk, and chunks with no wire cost nothing to load.
- Wire stays in memory for the whole session, and the file is rewritten when it changes, at most once per autosave. The network size limit keeps this bounded.
- Tools that delete or replace chunks outside the game leave the wire on those chunks in place. The game drops it the next time the block under it changes.
