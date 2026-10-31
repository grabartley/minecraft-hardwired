package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftCharge;
import com.grahambartley.hardwired.lift.LiftPhase;
import com.grahambartley.hardwired.lift.LiftShaft;
import com.grahambartley.hardwired.power.ChargeLedger;
import com.grahambartley.hardwired.power.ChargeNetwork;
import com.grahambartley.hardwired.power.ChargeStore;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;

public final class LiftBlock extends TableDeviceBlock {

  public static final BooleanProperty RUNNING = BooleanProperty.of("running");

  private static final double[] FULL_BOX = {0, 0, 0, 16, 16, 16};

  public LiftBlock(Settings settings) {
    super(settings, FULL_BOX);
  }

  @Override
  protected BooleanProperty live() {
    return RUNNING;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ElectronicBlockEntity(pos, state);
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    Optional<LiftCarEntity> car = new LiftShaft(world, pos).car();
    List<ChargeStore> stores = ChargeNetwork.storesFor(world, pos);
    return LiftCharge.demand(
        car.map(found -> found.run().phase()).orElse(LiftPhase.RESTING),
        car.map(found -> found.run().stopReason()).orElse(DeviceStatus.READY),
        Hardwired.SERVER_CONFIG.lift().chargePerBlock(),
        !stores.isEmpty(),
        ChargeLedger.available(stores));
  }

  @Override
  public int chargePerSecond() {
    return 0;
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    LiftShaft shaft = new LiftShaft(world, pos);
    boolean running = shaft.car().filter(car -> car.run().phase().travelling()).isPresent();
    ElectronicsDriver.show(world, pos, state, state.with(RUNNING, running));
    shaft.mirror(shaft.gates(), status);
  }
}
