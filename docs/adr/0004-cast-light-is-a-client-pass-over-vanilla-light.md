# Cast light is a client pass over vanilla light

## Decision

Coloured light on nearby blocks is drawn by the client after the world, on top of vanilla light. Server light levels do not change.

Anything that casts colour is a block entity implementing `GlowSource`, and `Glow.of` decides whether it casts anything. Nothing under `client/glow` knows which block a glow came from, so a new glowing block needs no rendering code.

The pass uses two blends that do nothing at zero: one removes the colours the light lacks, and one adds the colours it has. It sets that blend directly through `RenderSystem`, and the mesh is built on a worker thread.

## Why

Vanilla light stores a brightness and no colour. The coloured light mods we checked have no Fabric build for 1.21.1, and a lighting engine change would touch code that Sodium replaces.

Shader packs draw to high range buffers, where a white or grey vertex can come out brighter than it went in. Multiplying by white or grey is not neutral there, so the edge of the mesh shows as a hard line. Black stays black in any range, which is why both blends are neutral at zero. Vanilla render layers have no blend of this kind.

Building the mesh for 64 glows took up to 85 ms on the render thread, which shows as a stutter. Block entities are read on the render thread and the rest of the build reads only block states and light.

## Consequences

- Mobs, players, dropped items and held items do not pick up the colour.
- Blocks with a block entity, and blocks whose outline differs from their collision shape such as plants and fences, take no tint on their own faces.
- Shader packs draw the pass differently from each other. A faint stepped shadow remains at the far edge of the light under one pack we tried.
