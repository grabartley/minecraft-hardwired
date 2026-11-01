# Cast light is a client pass over vanilla light

## Decision

Coloured light on nearby blocks is drawn by the client after the world, on top of vanilla light. Server light levels do not change.

Anything that casts colour is a block entity implementing `GlowSource`. It decides its own colour and `Glow.of` applies the shared level rule. Nothing under `client/glow` knows which block a glow came from, so a new glowing block needs no rendering code. Light spreads by the game's own opacity rule, so it stops where vanilla light stops. A block with no block entity cannot be a source, because the client has no cheap way to find one.

The pass has its own core shader and uses two blends that do nothing at zero: one removes the colours the light lacks, and one adds the colours it has. It sets that blend directly through `RenderSystem`, and the mesh is built on a worker thread.

## Why

Vanilla light stores a brightness and no colour. No coloured light library we checked has a Fabric build for 1.21.1, and a lighting engine change would touch code that Sodium replaces.

Shader packs replace the vanilla program for plain coloured quads. One pack we read shades those quads flat and runs them through its own lighting, so no vertex colour stays neutral, the mesh shows as triangles, and the result changes with sky light. The pass therefore uses its own core shader, so packs cannot relight it, and the shader writes the vertex colour unchanged. Pack buffers can hold values above 1, so both blends are neutral at zero rather than at white. Vanilla render layers have no blend of this kind.

Building the mesh for 64 glows took up to 85 ms on the render thread, which shows as a stutter. Block entities are read on the render thread. The rest of the build reads block states, shapes and light, and shapes use an empty block view so no block entity is touched off the main thread.

## Consequences

- Mobs, players, dropped items and held items do not pick up the colour.
- Blocks with a block entity, and blocks with no collision such as plants, take no tint on their own faces.
- Blocks under water or behind other translucent surfaces take no tint, because those surfaces write depth before the pass.
- Cast colour does not show while a shader pack is on. The pack paints over what the core shader draws, so the room falls back to the pack's normal lighting and nothing is corrupted.
