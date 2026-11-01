package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.controller.ControllerWiring;
import com.grahambartley.hardwired.controller.Timeline;
import com.grahambartley.hardwired.controller.TimelineInputs;
import com.grahambartley.hardwired.controller.TimelineOrder;
import com.grahambartley.hardwired.controller.TimelineRun;
import com.grahambartley.hardwired.controller.TimelineRunNbt;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.TouchPoint;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ControllerBlockEntity extends BlockEntity {

  private static final String PROGRAM = "program";
  private static final String RUN = "run";
  private static final String OPEN_ON = "open_on";
  private static final String CLOSE_ON = "close_on";
  private static final float STEP_VOLUME = 0.5F;
  private static final float STEP_PITCH = 1.4F;

  private ControllerProgram program = ControllerProgram.DEFAULT;
  private TimelineRun run = new TimelineRun();
  private boolean openOn;
  private boolean closeOn;

  public ControllerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.CONTROLLER, pos, state);
  }

  public static void tick(
      World world, BlockPos pos, BlockState state, ControllerBlockEntity controller) {
    if (world instanceof ServerWorld server && WireEngine.of(server).power().hasPower(pos)) {
      controller.change(() -> controller.run.tick(controller.timeline()));
    }
  }

  public ControllerProgram program() {
    return program;
  }

  public TimelineRun run() {
    return run;
  }

  private Timeline timeline() {
    return program.timeline();
  }

  public boolean drives(Hue color) {
    return program.wiring().drives(run.outputs(timeline()), color);
  }

  public void readWires(ServerWorld server) {
    WireEngine engine = WireEngine.of(server);
    ControllerWiring wiring = program.wiring();
    boolean open = engine.isOn(pos, wiring.open());
    boolean close = engine.isOn(pos, wiring.close());
    TimelineOrder order =
        TimelineInputs.orderFor(wiring.readsOneColour(), openOn, closeOn, open, close);
    if (open != openOn || close != closeOn) {
      openOn = open;
      closeOn = close;
      markDirty();
    }
    if (order != null) {
      order(order);
    }
  }

  public void order(TimelineOrder order) {
    if (world instanceof ServerWorld server && !WireEngine.of(server).power().hasPower(pos)) {
      change(() -> run.orderDark(timeline(), order));
      return;
    }
    change(() -> run.order(timeline(), order));
  }

  public void toggleRun() {
    order(run.toggleOrder(timeline()));
  }

  public void setProgram(ControllerProgram next) {
    change(
        () -> {
          program = next;
          run.retarget(next.timeline());
        });
    if (world instanceof ServerWorld server) {
      WireEngine engine = WireEngine.of(server);
      openOn = engine.isOn(pos, next.wiring().open());
      closeOn = engine.isOn(pos, next.wiring().close());
      engine.refresh(pos);
    }
    sync();
  }

  private void change(Runnable action) {
    int outputsBefore = run.outputs(timeline());
    int positionBefore = run.position();
    action.run();
    markDirty();
    if (world instanceof ServerWorld server) {
      int outputs = run.outputs(timeline());
      if (outputs != outputsBefore) {
        stepped(server, outputs ^ outputsBefore);
      }
      if (run.position() != positionBefore) {
        sync();
      }
      showRunning(server);
    }
  }

  private void stepped(ServerWorld server, int changedOutputs) {
    WireEngine.of(server).refresh(pos);
    server.playSound(
        null,
        pos,
        SoundEvents.BLOCK_COMPARATOR_CLICK,
        SoundCategory.BLOCKS,
        STEP_VOLUME,
        STEP_PITCH);
    if (movesPiston(server, changedOutputs)) {
      ModCriteria.DEVICE.triggerNear(server, pos, ModCriteria.CONTROLLER_PISTON);
    }
  }

  private boolean movesPiston(ServerWorld server, int changedOutputs) {
    WireEngine engine = WireEngine.of(server);
    return engine.grid().networksTouching(pos).stream()
        .filter(network -> program.wiring().drives(changedOutputs, network.color()))
        .flatMap(network -> network.touches().stream())
        .map(TouchPoint::pos)
        .filter(engine::isLoaded)
        .anyMatch(touched -> server.getBlockState(touched).getBlock() instanceof PistonBlock);
  }

  private void showRunning(ServerWorld server) {
    BlockState state = getCachedState();
    if (state.get(ControllerBlock.RUNNING) != run.isRunning()) {
      server.setBlockState(
          pos, state.with(ControllerBlock.RUNNING, run.isRunning()), Block.NOTIFY_ALL);
    }
  }

  private void sync() {
    markDirty();
    if (world != null) {
      world.updateListeners(pos, getCachedState(), getCachedState(), 0);
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.put(PROGRAM, ControllerProgram.CODEC.encodeStart(NbtOps.INSTANCE, program).getOrThrow());
    nbt.put(RUN, TimelineRunNbt.write(run));
    nbt.putBoolean(OPEN_ON, openOn);
    nbt.putBoolean(CLOSE_ON, closeOn);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    NbtElement saved = nbt.get(PROGRAM);
    program =
        saved == null
            ? ControllerProgram.DEFAULT
            : ControllerProgram.CODEC
                .parse(NbtOps.INSTANCE, saved)
                .resultOrPartial(Hardwired.LOGGER::error)
                .orElse(ControllerProgram.DEFAULT);
    run = TimelineRunNbt.read(nbt.getCompound(RUN), timeline());
    openOn = nbt.getBoolean(OPEN_ON);
    closeOn = nbt.getBoolean(CLOSE_ON);
  }

  @Override
  protected void readComponents(ComponentsAccess components) {
    super.readComponents(components);
    program = components.getOrDefault(ModComponents.CONTROLLER_PROGRAM, ControllerProgram.DEFAULT);
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    if (!program.equals(ControllerProgram.DEFAULT)) {
      builder.add(ModComponents.CONTROLLER_PROGRAM, program);
    }
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(PROGRAM);
    nbt.remove(RUN);
    nbt.remove(OPEN_ON);
    nbt.remove(CLOSE_ON);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    return createNbt(registries);
  }

  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }
}
