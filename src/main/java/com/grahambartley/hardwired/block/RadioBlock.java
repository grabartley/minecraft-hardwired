package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.RadioRun;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RadioBlock extends TableDeviceBlock {

  public static final BooleanProperty PLAYING = BooleanProperty.of("playing");

  private static final double NOTE_HEIGHT = 1.0;

  public RadioBlock(Settings settings, double[] box) {
    super(settings, box);
  }

  @Override
  protected BooleanProperty live() {
    return PLAYING;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new RadioBlockEntity(pos, state);
  }

  @Override
  protected ItemActionResult onUseWithItem(
      ItemStack stack,
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand,
      BlockHitResult hit) {
    if (!stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
      return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    if (world.isClient) {
      return ItemActionResult.SUCCESS;
    }
    if (!(world.getBlockEntity(pos) instanceof RadioBlockEntity radio) || radio.hasDisc()) {
      return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    radio.putDisc(stack.splitUnlessCreative(1, player));
    player.incrementStat(Stats.PLAY_RECORD);
    ElectronicsDriver.pulse((ServerWorld) world, pos);
    ElectronicsDriver.tell(world, pos, player);
    return ItemActionResult.CONSUME;
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    boolean discInOffHand = player.getOffHandStack().contains(DataComponentTypes.JUKEBOX_PLAYABLE);
    if (player instanceof ServerPlayerEntity serverPlayer
        && world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
      if (radio.hasDisc() && player.getMainHandStack().isEmpty()) {
        player.setStackInHand(Hand.MAIN_HAND, radio.takeDisc());
        ElectronicsDriver.pulse(serverPlayer.getServerWorld(), pos);
      } else if (!discInOffHand) {
        ElectronicsDriver.tell(world, pos, player);
      }
    }
    return discInOffHand ? ActionResult.PASS : ActionResult.SUCCESS;
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())
        && world instanceof ServerWorld server
        && world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
      ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), radio.takeDisc());
      radio.setStatus(DeviceStatus.NO_DISC);
      wakeSpeakers(server, pos);
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    if (!(world.getBlockEntity(pos) instanceof RadioBlockEntity radio)) {
      return DeviceStatus.NO_DISC;
    }
    int speakers = WireEngine.of(world).allOnNetwork(pos, SpeakerBlockEntity.class).size();
    return RadioRun.demand(radio.hasDisc(), radio.songEnded(), speakers);
  }

  @Override
  public int chargePerSecond() {
    return Hardwired.SERVER_CONFIG.power().radioChargePerSecond();
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
      radio.advance(status);
    }
    boolean playing = status == DeviceStatus.RUNNING;
    if (state.get(PLAYING) != playing) {
      ElectronicsDriver.show(world, pos, state, state.with(PLAYING, playing));
      wakeSpeakers(world, pos);
    }
  }

  @Override
  public void beat(ServerWorld world, BlockPos pos) {
    Vec3d top = Vec3d.ofBottomCenter(pos).add(0, NOTE_HEIGHT, 0);
    world.spawnParticles(ParticleTypes.NOTE, top.x, top.y, top.z, 0, 0, 0, 0, 1);
  }

  @Override
  public boolean keepsChecking(DeviceStatus status) {
    return status.wantsCharge() || status == DeviceStatus.NO_SPEAKER;
  }

  private static void wakeSpeakers(ServerWorld world, BlockPos pos) {
    WireEngine.of(world)
        .allOnNetwork(pos, SpeakerBlockEntity.class)
        .forEach(speaker -> ElectronicsDriver.wake(world, speaker.getPos()));
  }
}
